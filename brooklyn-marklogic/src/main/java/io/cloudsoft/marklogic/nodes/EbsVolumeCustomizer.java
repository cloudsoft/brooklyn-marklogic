package io.cloudsoft.marklogic.nodes;

import static brooklyn.util.ssh.CommonCommands.dontRequireTtyForSudo;
import static brooklyn.util.ssh.CommonCommands.sudo;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.ec2.EC2ApiMetadata;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.compute.options.EC2TemplateOptions;
import org.jclouds.ec2.domain.Attachment;
import org.jclouds.ec2.domain.Volume;
import org.jclouds.ec2.features.TagApi;
import org.jclouds.ec2.options.DetachVolumeOptions;
import org.jclouds.ec2.services.ElasticBlockStoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.location.jclouds.BasicJcloudsLocationCustomizer;
import brooklyn.location.jclouds.JcloudsLocation;
import brooklyn.location.jclouds.JcloudsLocationCustomizer;
import brooklyn.location.jclouds.JcloudsSshMachineLocation;
import brooklyn.util.MutableMap;
import brooklyn.util.internal.Repeater;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * Customization hooks to ensure that any EC2 instances provisioned via a corresponding jclouds location become associated
 * with an EBS volume (either an existing volume, specified by ID, or newly created).
 */
public class EbsVolumeCustomizer {

    private static final Logger LOG = LoggerFactory.getLogger(EbsVolumeCustomizer.class);

    // Prevent construction: helper class.
    private EbsVolumeCustomizer() {
    }
    
    /**
     * Returns a location customizer that:
     * <ul>
     * <li>configures the AWS availability zone</li>
     * <li>creates a new EBS volume of the requested size in the given availability zone</li>
     * <li>attaches the new volume to the newly-provisioned EC2 instance</li>
     * <li>formats the new volume with the requested filesystem</li>
     * <li>mounts the filesystem under the requested path</li>
     * </ul>
     */
    public static JcloudsLocationCustomizer withNewVolume(final String volumeDeviceName, final String osDeviceName, final String mountPoint, final String filesystemType,
            final String availabilityZone, final int sizeInGib, final boolean deleteOnTermination) {

        return new BasicJcloudsLocationCustomizer() {
            public void customize(ComputeService computeService, TemplateBuilder templateBuilder) {
                templateBuilder.locationId(availabilityZone);
            }

            public void customize(ComputeService computeService, TemplateOptions templateOptions) {
                ((EC2TemplateOptions) templateOptions).mapNewVolumeToDeviceName(volumeDeviceName, sizeInGib, deleteOnTermination);
            }

            public void customize(ComputeService computeService, JcloudsSshMachineLocation machine) {
                createFilesystem(machine, osDeviceName, filesystemType);
                mountFilesystem(machine, osDeviceName, mountPoint, filesystemType);
            }
        };
    }

    /**
     * Returns a location customizer that:
     * <ul>
     * <li>configures the AWS availability zone</li>
     * <li>obtains a new EBS volume from the specified snapshot in the given availability zone</li>
     * <li>attaches the new volume to the newly-provisioned EC2 instance</li>
     * <li>mounts the filesystem under the requested path</li>
     * </ul>
     */
    public static JcloudsLocationCustomizer withExistingSnapshot(final String volumeDeviceName, final String osDeviceName, final String mountPoint,
            final String availabilityZone, final String snapshotId, final int sizeInGib, final boolean deleteOnTermination) {

        return new BasicJcloudsLocationCustomizer() {
            public void customize(ComputeService computeService, TemplateBuilder templateBuilder) {
                templateBuilder.locationId(availabilityZone);
            }

            public void customize(ComputeService computeService, TemplateOptions templateOptions) {
                ((EC2TemplateOptions) templateOptions).mapEBSSnapshotToDeviceName(volumeDeviceName, snapshotId, sizeInGib, deleteOnTermination);
            }

            public void customize(ComputeService computeService, JcloudsSshMachineLocation machine) {
                mountFilesystem(machine, osDeviceName, mountPoint);
            }
        };
    }

    /**
     * Returns a location customizer that:
     * <ul>
     * <li>configures the AWS availability zone</li>
     * <li>attaches the specified (existing) volume to the newly-provisioned EC2 instance</li>
     * <li>mounts the filesystem under the requested path</li>
     * </ul>
     */
    public static JcloudsLocationCustomizer withExistingVolume(final String volumeDeviceName, final String osDeviceName, final String mountPoint,
            final String region, final String availabilityZone, final String volumeId) {

        return new BasicJcloudsLocationCustomizer() {
            public void customize(ComputeService computeService, TemplateBuilder templateBuilder) {
                templateBuilder.locationId(availabilityZone);
            }

            public void customize(ComputeService computeService, JcloudsSshMachineLocation machine) {
            	attachVolume(machine, volumeId, volumeDeviceName);
                mountFilesystem(machine, osDeviceName, mountPoint);
            }
        };
    }

    /**
     * Creates a new volume in the same availability zone as the given machine, and attaches + mounts it.
     */
    public static String createAttachAndMountNewVolume(JcloudsSshMachineLocation machine,
            final String volumeDeviceName, final String osDeviceName, final String mountPoint, final String filesystemType,
            final String availabilityZone, final int sizeInGib, Map<String,String> tags) {
    	String volumeId = createNewVolume(machine.getParent(), machine.getNode().getLocation().getId(), sizeInGib, tags);
    	attachVolume(machine, volumeId, volumeDeviceName);
		createFilesystem(machine, osDeviceName, filesystemType);
		mountFilesystem(machine, osDeviceName, mountPoint);
		return volumeId;
    }

    /**
     * Creates a new volume in the given availability zone.
     */
    public static String createNewVolume(JcloudsLocation location, String availabilityZone, int size, Map<String,String> tags) {
        LOG.info("Creating volume: location={}; availabilityZone={}; size={}", new Object[] {location, availabilityZone, size});
        
        EC2Client ec2Client = location.getComputeService().getContext().unwrap(EC2ApiMetadata.CONTEXT_TOKEN).getApi();
        ElasticBlockStoreClient ebsClient = ec2Client.getElasticBlockStoreServices();
        TagApi tagClient = ec2Client.getTagApi().get();
        
        Volume volume = ebsClient.createVolumeInAvailabilityZone(availabilityZone, size);
        if (tags != null && tags.size() > 0) {
        	tagClient.applyToResources(tags, ImmutableList.of(volume.getId()));
        }
        
        return volume.getId();
    }
    
    /**
     * Attaches the given volume to the given VM.
     */
    public static void attachVolume(JcloudsSshMachineLocation machine, String volumeId, String volumeDeviceName) {
        LOG.info("Attaching volume: machine={}; volume={}; volumeDeviceName={}", new Object[] {machine, volumeId, volumeDeviceName});
        
    	JcloudsLocation location = machine.getParent();
    	String region = location.getRegion();
        EC2Client ec2Client = location.getComputeService().getContext().unwrap(EC2ApiMetadata.CONTEXT_TOKEN).getApi();
        ElasticBlockStoreClient ebsClient = ec2Client.getElasticBlockStoreServices();
        Attachment attachment = ebsClient.attachVolumeInRegion(region, volumeId, machine.getNode().getProviderId(), volumeDeviceName);
        // TODO return attachment.getId();
    }

    /**
     * Attaches the given volume to the given VM, and mounts it.
     */
    public static void attachAndMountVolume(JcloudsSshMachineLocation machine, String volumeId, String volumeDeviceName, String osDeviceName, String mountPoint) {
        attachAndMountVolume(machine, volumeId, volumeDeviceName, osDeviceName, mountPoint, "auto");
    }
    
    public static void attachAndMountVolume(JcloudsSshMachineLocation machine, String volumeId, String volumeDeviceName, String osDeviceName, String mountPoint, String filesystemType) {
    	attachVolume(machine, volumeId, volumeDeviceName);
    	mountFilesystem(machine, osDeviceName, mountPoint, filesystemType);
    }
    
    /**
     * Detaches the given volume from the given VM.
     */
    public static void detachVolume(JcloudsSshMachineLocation machine, final String volumeId, String volumeDeviceName) {
        LOG.info("Detaching volume: machine={}; volume={}; volumeDeviceName={}", new Object[] {machine, volumeId, volumeDeviceName});
        
        final JcloudsLocation location = machine.getParent();
    	String region = location.getRegion();
    	String instanceId = machine.getNode().getProviderId();
        EC2Client ec2Client = location.getComputeService().getContext().unwrap(EC2ApiMetadata.CONTEXT_TOKEN).getApi();
        ElasticBlockStoreClient ebsClient = ec2Client.getElasticBlockStoreServices();
		ebsClient.detachVolumeInRegion(region, volumeId, true, DetachVolumeOptions.Builder.fromDevice(volumeDeviceName).fromInstance(instanceId));
		
		// Wait for detached
		boolean detached = Repeater.create("wait for detached "+volumeId+" from "+machine)
				.every(1, TimeUnit.SECONDS)
				.limitTimeTo(60, TimeUnit.SECONDS)
				//.repeat(Callables.returning(null))
				.until(new Callable<Boolean>() {
					@Override public Boolean call() throws Exception {
						Volume volume = describeVolume(location, volumeId);
						return volume.getStatus() == Volume.Status.AVAILABLE;
					}})
				.run();
		
		if (!detached) {
			LOG.error("Volume {}->{} still not detached from {}; continuing...", new Object[] {volumeId, volumeDeviceName, machine});
		}
    }
    
    /**
     * Deletes the given volume.
     */
    public static void deleteVolume(JcloudsLocation location, String volumeId) {
        LOG.info("Deleting volume: location={}; volume={}", new Object[] {location, volumeId});
        
    	String region = location.getRegion();
        EC2Client ec2Client = location.getComputeService().getContext().unwrap(EC2ApiMetadata.CONTEXT_TOKEN).getApi();
        ElasticBlockStoreClient ebsClient = ec2Client.getElasticBlockStoreServices();
        ebsClient.deleteVolumeInRegion(region, volumeId);
    }
    
    /**
     * Describes the given volume. Or returns null if it is not found.
     */
    public static Volume describeVolume(JcloudsLocation location, String volumeId) {
        if (LOG.isDebugEnabled()) LOG.debug("Describing volume: location={}; volume={}", new Object[] {location, volumeId});
        
    	String region = location.getRegion();
        EC2Client ec2Client = location.getComputeService().getContext().unwrap(EC2ApiMetadata.CONTEXT_TOKEN).getApi();
        ElasticBlockStoreClient ebsClient = ec2Client.getElasticBlockStoreServices();
        Set<Volume> volumes = ebsClient.describeVolumesInRegion(region, volumeId);
        return Iterables.getFirst(volumes, null);
    }
    
    public static void createFilesystem(JcloudsSshMachineLocation machine, String osDeviceName, String filesystemType) {
        LOG.info("Creating filesystem: machine={}; osDeviceName={}; filesystemType={}", new Object[] {machine, osDeviceName, filesystemType});
        
        // NOTE: also adds an entry to fstab so the mount remains available after a reboot.
        Map<String,?> flags = MutableMap.of("allocatePTY", true);

        machine.execCommands(flags, "Creating filesystem on EBS volume", ImmutableList.of(
                dontRequireTtyForSudo(),
                waitForFileCmd(osDeviceName, 60),
                sudo("/sbin/mkfs -t " + filesystemType + " " + osDeviceName)));
    }

    public static void mountFilesystem(JcloudsSshMachineLocation machine, String osDeviceName, String mountPoint) {
        mountFilesystem(machine, osDeviceName, mountPoint, "auto");
    }
    
    public static void mountFilesystem(JcloudsSshMachineLocation machine, String osDeviceName, String mountPoint, String filesystemType) {
        LOG.info("Mounting filesystem: machine={}; osDeviceName={}; mountPoint={}; filesystemType={}", new Object[] {machine, osDeviceName, mountPoint, filesystemType});
        
        // NOTE: also adds an entry to fstab so the mount remains available after a reboot.
        Map<String,?> flags = MutableMap.of("allocatePTY", true);
        machine.execCommands(flags, "Mounting EBS volume", ImmutableList.of(
                dontRequireTtyForSudo(),
                "echo making dir",
                sudo(" mkdir -p -m 755 " + mountPoint),
                "echo updating fstab",
                waitForFileCmd(osDeviceName, 60),
                "echo \"" + osDeviceName + " " + mountPoint + " " + filesystemType + " noatime 0 0\" | " + sudo("tee -a /etc/fstab"),
                "echo mounting device",
                sudo("mount " + mountPoint),
                "echo device mounted"
        ));
    }
    
    public static void unmountFilesystem(JcloudsSshMachineLocation machine, String osDeviceName) {
        LOG.info("Unmounting filesystem: machine={}; osDeviceName={}", new Object[] {machine, osDeviceName});
        String osDeviceNameEscaped = osDeviceName.replaceAll("/", "\\\\/");
        
        // NOTE: also strips out entry from fstab
        Map<String,?> flags = MutableMap.of("allocatePTY", true);
        machine.execCommands(flags, "Unmounting EBS volume", ImmutableList.of(
                dontRequireTtyForSudo(),
                "echo unmounting "+osDeviceName,
                sudo("sed -i.bk '/"+osDeviceNameEscaped+"/d' /etc/fstab"),
                sudo("umount " + osDeviceName),
                "echo unmounted "+osDeviceName
        ));
    }
    
    // TODO Move to CommonCommands
    private static String waitForFileCmd(String file, int timeoutSecs) {
        return "found=false; "+
                "for i in {1.."+timeoutSecs+"}; do "+
                    "if [ -a "+file+" ]; then "+
                        "echo \"file "+file+" found\"; "+
                        "found=true; "+
                        "break; "+
                    "else "+
                        "echo \"file "+file+" does not exist (waiting)\"; "+
                        "sleep 1; "+
                    "fi; "+
                "done; "+
                "if [ \"$found\" == \"false\" ]; then "+
                    "exit 1; "+
                "fi";
    }
}
