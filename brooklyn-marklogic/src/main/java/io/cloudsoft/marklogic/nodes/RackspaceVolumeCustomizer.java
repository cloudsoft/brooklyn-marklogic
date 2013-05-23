package io.cloudsoft.marklogic.nodes;

import static brooklyn.util.ssh.CommonCommands.dontRequireTtyForSudo;
import static brooklyn.util.ssh.CommonCommands.sudo;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.encryption.bouncycastle.config.BouncyCastleCryptoModule;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.cinder.v1.CinderApi;
import org.jclouds.openstack.cinder.v1.CinderApiMetadata;
import org.jclouds.openstack.cinder.v1.CinderAsyncApi;
import org.jclouds.openstack.cinder.v1.domain.Volume;
import org.jclouds.openstack.cinder.v1.features.VolumeApi;
import org.jclouds.openstack.cinder.v1.options.CreateVolumeOptions;
import org.jclouds.openstack.cinder.v1.predicates.VolumePredicates;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaApiMetadata;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeAttachmentApi;
import org.jclouds.rest.RestContext;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.location.jclouds.JcloudsLocation;
import brooklyn.location.jclouds.JcloudsSshMachineLocation;
import brooklyn.util.MutableMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;

/**
 * For managing volumes in rackspace.
 */
public class RackspaceVolumeCustomizer {

    private static final Logger LOG = LoggerFactory.getLogger(EbsVolumeCustomizer.class);

    // FIXME Don't hard-code as UK
    private static final String UK_ZONE = "LON";
    private static final String US_ZONE = "DFW";

    // Prevent construction: helper class.
    private RackspaceVolumeCustomizer() {
    }
    
    /**
     * Creates a new volume in the same availability zone as the given machine, and attaches + mounts it.
     * @throws TimeoutException 
     */
    public static String createAttachAndMountNewVolume(JcloudsSshMachineLocation machine,
            final String volumeDeviceName, final String osDeviceName, final String mountPoint, final String filesystemType,
            final String availabilityZone, final int sizeInGib, Map<String,String> tags) throws TimeoutException {
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
        LOG.info("Creating volume: location={}; size={}", new Object[] {location, size});
        
        CinderApi cinderApi = getCinderApi(location);
        VolumeApi volumeApi = cinderApi.getVolumeApiForZone(UK_ZONE);
        
        CreateVolumeOptions options = CreateVolumeOptions.Builder
                .name("brooklyn-something") // FIXME
                .metadata(tags != null ? tags : ImmutableMap.<String,String>of());
        
        Volume volume = volumeApi.create(size, options);
        return volume.getId();
    }
    
    /**
     * Attaches the given volume to the given VM.
     * 
     * @throws TimeoutException 
     */
    public static void attachVolume(JcloudsSshMachineLocation machine, String volumeId, String volumeDeviceName) throws TimeoutException {
        LOG.info("Attaching volume: machine={}; volume={}; volumeDeviceName={}", new Object[] {machine, volumeId, volumeDeviceName});

        JcloudsLocation location = machine.getParent();
        String instanceId = machine.getNode().getProviderId();
        CinderApi cinderApi = getCinderApi(location);
        NovaApi novaApi = getNovaApi(location);
        VolumeAttachmentApi attachmentApi = novaApi.getVolumeAttachmentExtensionForZone(UK_ZONE).get();
        VolumeApi volumeApi = cinderApi.getVolumeApiForZone(UK_ZONE);
        Volume volume = volumeApi.get(volumeId);
        
        attachmentApi.attachVolumeToServerAsDevice(volumeId, instanceId, volumeDeviceName);
        
        // Wait for the volume to become Attached (aka In Use) before moving on
        if (!VolumePredicates.awaitInUse(volumeApi).apply(volume)) {
            throw new TimeoutException("Timeout on attaching volume: volumeId="+volumeId+"; machine="+machine);
        }
    }

    /**
     * Attaches the given volume to the given VM, and mounts it.
     * @throws TimeoutException 
     */
    public static void attachAndMountVolume(JcloudsSshMachineLocation machine, String volumeId, String volumeDeviceName, String osDeviceName, String mountPoint) throws TimeoutException {
        attachAndMountVolume(machine, volumeId, volumeDeviceName, osDeviceName, mountPoint, "auto");
    }
    
    public static void attachAndMountVolume(JcloudsSshMachineLocation machine, String volumeId, String volumeDeviceName, String osDeviceName, String mountPoint, String filesystemType) throws TimeoutException {
    	attachVolume(machine, volumeId, volumeDeviceName);
    	mountFilesystem(machine, osDeviceName, mountPoint, filesystemType);
    }
    
    /**
     * Detaches the given volume from the given VM.
     * @throws TimeoutException 
     */
    public static void detachVolume(JcloudsSshMachineLocation machine, final String volumeId, String volumeDeviceName) throws TimeoutException {
        LOG.info("Detaching volume: machine={}; volume={}; volumeDeviceName={}", new Object[] {machine, volumeId, volumeDeviceName});
        
        JcloudsLocation location = machine.getParent();
        String instanceId = machine.getNode().getProviderId();
        CinderApi cinderApi = getCinderApi(location);
        NovaApi novaApi = getNovaApi(location);
        VolumeAttachmentApi attachmentApi = novaApi.getVolumeAttachmentExtensionForZone(UK_ZONE).get();
        VolumeApi volumeApi = cinderApi.getVolumeApiForZone(UK_ZONE);
        Volume volume = volumeApi.get(volumeId);
        
        attachmentApi.detachVolumeFromServer(volumeId, instanceId);
        
        // Wait for the volume to become Attached (aka In Use) before moving on
        if (!VolumePredicates.awaitAvailable(volumeApi).apply(volume)) {
            throw new TimeoutException("Timeout on attaching volume: volumeId="+volumeId+"; machine="+machine);
        }
    }
    
    /**
     * Deletes the given volume.
     */
    public static void deleteVolume(JcloudsLocation location, String volumeId) {
        LOG.info("Deleting volume: location={}; volume={}", new Object[] {location, volumeId});
        
        CinderApi cinderApi = getCinderApi(location);
        VolumeApi volumeApi = cinderApi.getVolumeApiForZone(UK_ZONE);
        
        volumeApi.delete(volumeId);
    }
    
    /**
     * Describes the given volume. Or returns null if it is not found.
     */
    public static Volume describeVolume(JcloudsLocation location, String volumeId) {
        if (LOG.isDebugEnabled()) LOG.debug("Describing volume: location={}; volume={}", new Object[] {location, volumeId});
        
        CinderApi cinderApi = getCinderApi(location);
        VolumeApi volumeApi = cinderApi.getVolumeApiForZone(UK_ZONE);
        return volumeApi.get(volumeId);
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
    
    private static CinderApi getCinderApi(JcloudsLocation location) {
        String provider = "rackspace-cloudblockstorage-uk";
        String identity = location.getIdentity();
        String credential = location.getCredential();
        final String ZONE = "DFW";
        Iterable<Module> modules = ImmutableSet.<Module> of(
                new SshjSshClientModule(), 
                new SLF4JLoggingModule(),
                new BouncyCastleCryptoModule());

        RestContext<CinderApi, CinderAsyncApi> cinder = ContextBuilder.newBuilder(provider)
              .credentials(identity, credential)
              .modules(modules)
              .build(CinderApiMetadata.CONTEXT_TOKEN);
        
        return cinder.getApi();
    }

    private static NovaApi getNovaApi(JcloudsLocation location) {
        String provider = "rackspace-cloudservers-uk";
        String identity = location.getIdentity();
        String credential = location.getCredential();
        final String ZONE = "DFW";
        Iterable<Module> modules = ImmutableSet.<Module> of(
                new SshjSshClientModule(), 
                new SLF4JLoggingModule(),
                new BouncyCastleCryptoModule());

        RestContext<NovaApi, NovaAsyncApi> nova = ContextBuilder.newBuilder(provider)
                .credentials(identity, credential)
                .modules(modules)
                .build(NovaApiMetadata.CONTEXT_TOKEN);
        return nova.getApi();
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
