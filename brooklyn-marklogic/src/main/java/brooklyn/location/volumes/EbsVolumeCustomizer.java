package brooklyn.location.volumes;

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.ec2.compute.options.EC2TemplateOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.location.jclouds.BasicJcloudsLocationCustomizer;
import brooklyn.location.jclouds.JcloudsLocation;
import brooklyn.location.jclouds.JcloudsLocationCustomizer;
import brooklyn.location.jclouds.JcloudsSshMachineLocation;
import brooklyn.location.volumes.EbsVolumeManager;

/**
 * Customization hooks to ensure that any EC2 instances provisioned via a corresponding jclouds location become associated
 * with an EBS volume (either an existing volume, specified by ID, or newly created).
 */
public class EbsVolumeCustomizer {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(EbsVolumeCustomizer.class);

    private static final EbsVolumeManager ebsVolumeManager = new EbsVolumeManager();
    
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
            @Override
            public void customize(JcloudsLocation location, ComputeService computeService, TemplateBuilder templateBuilder) {
                templateBuilder.locationId(availabilityZone);
            }

            @Override
            public void customize(JcloudsLocation location, ComputeService computeService, TemplateOptions templateOptions) {
                ((EC2TemplateOptions) templateOptions).mapNewVolumeToDeviceName(volumeDeviceName, sizeInGib, deleteOnTermination);
            }

            @Override
            public void customize(JcloudsLocation location, ComputeService computeService, JcloudsSshMachineLocation machine) {
                ebsVolumeManager.createFilesystem(machine, osDeviceName, filesystemType);
                ebsVolumeManager.mountFilesystem(machine, osDeviceName, mountPoint, filesystemType);
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
            @Override
            public void customize(JcloudsLocation location, ComputeService computeService, TemplateBuilder templateBuilder) {
                templateBuilder.locationId(availabilityZone);
            }

            @Override
            public void customize(JcloudsLocation location, ComputeService computeService, TemplateOptions templateOptions) {
                ((EC2TemplateOptions) templateOptions).mapEBSSnapshotToDeviceName(volumeDeviceName, snapshotId, sizeInGib, deleteOnTermination);
            }

            @Override
            public void customize(JcloudsLocation location, ComputeService computeService, JcloudsSshMachineLocation machine) {
                ebsVolumeManager.mountFilesystem(machine, osDeviceName, mountPoint);
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
            @Override
            public void customize(JcloudsLocation location, ComputeService computeService, TemplateBuilder templateBuilder) {
                templateBuilder.locationId(availabilityZone);
            }

            @Override
            public void customize(JcloudsLocation location, ComputeService computeService, JcloudsSshMachineLocation machine) {
                ebsVolumeManager.attachVolume(machine, volumeId, volumeDeviceName);
                ebsVolumeManager.mountFilesystem(machine, osDeviceName, mountPoint);
            }
        };
    }
}
