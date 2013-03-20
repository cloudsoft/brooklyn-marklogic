package io.cloudsoft.marklogic;

import java.util.Arrays;

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.ec2.EC2ApiMetadata;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.compute.options.EC2TemplateOptions;
import org.jclouds.ec2.services.ElasticBlockStoreClient;

import brooklyn.location.jclouds.BasicJcloudsLocationCustomizer;
import brooklyn.location.jclouds.JcloudsLocationCustomizer;
import brooklyn.location.jclouds.JcloudsSshMachineLocation;

/**
 * Customization hooks to ensure that any EC2 instances provisioned via a corresponding jclouds location become associated
 * with an EBS volume (either an existing volume, specified by ID, or newly created).
 */
public class EbsVolumeCustomizer {

    /**
     * Returns a location customizer that:
     * <ul>
     *   <li>configures the AWS availability zone</li>
     *   <li>creates a new EBS volume of the requested size in the given availability zone</li>
     *   <li>attaches the new volume to the newly-provisioned EC2 instance</li>
     *   <li>formats the new volume with the requested filesystem</li>
     *   <li>mounts the filesystem under the requested path</li>
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
     *   <li>configures the AWS availability zone</li>
     *   <li>obtains a new EBS volume from the specified snapshot in the given availability zone</li>
     *   <li>attaches the new volume to the newly-provisioned EC2 instance</li>
     *   <li>mounts the filesystem under the requested path</li>
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
     *   <li>configures the AWS availability zone</li>
     *   <li>attaches the specified (existing) volume to the newly-provisioned EC2 instance</li>
     *   <li>mounts the filesystem under the requested path</li>
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
                ebsClient.attachVolumeInRegion(region, volumeId, machine.getJcloudsId(), ec2DeviceName);
                mountFilesystem(machine, osDeviceName, mountPoint);
            }
        };
    }

    private static void createFilesystem(JcloudsSshMachineLocation machine, String osDeviceName, String filesystemType) {
        machine.execCommands("Creating filesystem on EBS volume", Arrays.asList(
            "mkfs." + filesystemType + " " + osDeviceName
        ));
    }

    private static void mountFilesystem(JcloudsSshMachineLocation machine, String osDeviceName, String mountPoint) {
        // NOTE: also adds an entry to fstab so the mount remains available after a reboot.
        machine.execCommands("Mounting EBS volume", Arrays.asList(
            "mkdir -m 000 " + mountPoint,
            "echo \"" + osDeviceName + " " + mountPoint + " auto noatime 0 0\" | sudo tee -a /etc/fstab",
            "mount " + mountPoint
        ));
    }

    // Prevent construction: helper class.
    private EbsVolumeCustomizer() { }

}
