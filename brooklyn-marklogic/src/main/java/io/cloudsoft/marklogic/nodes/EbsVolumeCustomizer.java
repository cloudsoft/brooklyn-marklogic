package io.cloudsoft.marklogic.nodes;

import brooklyn.location.jclouds.BasicJcloudsLocationCustomizer;
import brooklyn.location.jclouds.JcloudsLocationCustomizer;
import brooklyn.location.jclouds.JcloudsSshMachineLocation;
import brooklyn.util.MutableMap;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.ec2.EC2ApiMetadata;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.compute.options.EC2TemplateOptions;
import org.jclouds.ec2.services.ElasticBlockStoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;

import static brooklyn.util.ssh.CommonCommands.dontRequireTtyForSudo;
import static brooklyn.util.ssh.CommonCommands.sudo;

/**
 * Customization hooks to ensure that any EC2 instances provisioned via a corresponding jclouds location become associated
 * with an EBS volume (either an existing volume, specified by ID, or newly created).
 */
public class EbsVolumeCustomizer {

    private static final Logger LOG = LoggerFactory.getLogger(EbsVolumeCustomizer.class);


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
    public static JcloudsLocationCustomizer withNewVolume(final String ec2DeviceName, final String osDeviceName, final String mountPoint, final String filesystemType,
                                                          final String availabilityZone, final int sizeInGib, final boolean deleteOnTermination) {

        return new BasicJcloudsLocationCustomizer() {
            public void customize(ComputeService computeService, TemplateBuilder templateBuilder) {
                templateBuilder.locationId(availabilityZone);
            }

            public void customize(ComputeService computeService, TemplateOptions templateOptions) {
                ((EC2TemplateOptions) templateOptions).mapNewVolumeToDeviceName(ec2DeviceName, sizeInGib, deleteOnTermination);
            }

            public void customize(ComputeService computeService, JcloudsSshMachineLocation machine) {
                createFilesystem(machine, osDeviceName, filesystemType);
                mountFilesystem(machine, osDeviceName, mountPoint);
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
    public static JcloudsLocationCustomizer withExistingSnapshot(final String ec2DeviceName, final String osDeviceName, final String mountPoint,
                                                                 final String availabilityZone, final String snapshotId, final int sizeInGib, final boolean deleteOnTermination) {

        return new BasicJcloudsLocationCustomizer() {
            public void customize(ComputeService computeService, TemplateBuilder templateBuilder) {
                templateBuilder.locationId(availabilityZone);
            }

            public void customize(ComputeService computeService, TemplateOptions templateOptions) {
                ((EC2TemplateOptions) templateOptions).mapEBSSnapshotToDeviceName(ec2DeviceName, snapshotId, sizeInGib, deleteOnTermination);
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
    public static JcloudsLocationCustomizer withExistingVolume(final String ec2DeviceName, final String osDeviceName, final String mountPoint,
                                                               final String region, final String availabilityZone, final String volumeId) {

        return new BasicJcloudsLocationCustomizer() {
            public void customize(ComputeService computeService, TemplateBuilder templateBuilder) {
                templateBuilder.locationId(availabilityZone);
            }

            public void customize(ComputeService computeService, JcloudsSshMachineLocation machine) {
                EC2Client ec2Client = computeService.getContext().unwrap(EC2ApiMetadata.CONTEXT_TOKEN).getApi();
                ElasticBlockStoreClient ebsClient = ec2Client.getElasticBlockStoreServices();
                LOG.info("volumeId: "+volumeId+" ec2DeviceName: "+ec2DeviceName);
                ebsClient.attachVolumeInRegion(region, volumeId, machine.getJcloudsId(), ec2DeviceName);
                mountFilesystem(machine, osDeviceName, mountPoint);
            }
        };
    }

    private static void createFilesystem(JcloudsSshMachineLocation machine, String osDeviceName, String filesystemType) {
        // NOTE: also adds an entry to fstab so the mount remains available after a reboot.
        Map flags = MutableMap.of("allocatePTY", true);

        machine.execCommands(flags, "Creating filesystem on EBS volume", Arrays.asList(
                dontRequireTtyForSudo(),
                sudo("/sbin/mkfs -t " + filesystemType + " " + osDeviceName)
        ));
    }

    private static void mountFilesystem(JcloudsSshMachineLocation machine, String osDeviceName, String mountPoint) {
        // NOTE: also adds an entry to fstab so the mount remains available after a reboot.
        Map flags = MutableMap.of("allocatePTY", true);
        machine.execCommands(flags, "Mounting EBS volume", Arrays.asList(
                dontRequireTtyForSudo(),
                "echo making dir",
                sudo(" mkdir -p -m 000 " + mountPoint),
                "echo updating fstab",
                "echo \"" + osDeviceName + " " + mountPoint + " auto noatime 0 0\" | " + sudo("tee -a /etc/fstab"),
                "echo mounting device",
                sudo("mount " + mountPoint),
                "echo device mounted"
        ));
    }

    // Prevent construction: helper class.
    private EbsVolumeCustomizer() {
    }

}
