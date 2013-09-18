package brooklyn.location.volumes;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.Map;

import org.jclouds.ec2.domain.Volume;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

import brooklyn.location.NoMachinesAvailableException;
import brooklyn.location.jclouds.JcloudsLocation;
import brooklyn.location.jclouds.JcloudsSshMachineLocation;
import brooklyn.util.collections.MutableMap;

public class EbsVolumeManagerLiveTest extends AbstractVolumeManagerLiveTest {

    // Note we're using the region-name with an explicit availability zone, as is done in the demo-app so
    // that new VMs will be able to see the existing volumes within that availability zone.
    
    public static final String PROVIDER = "aws-ec2";
    public static final String REGION_NAME = "us-east-1c";
    public static final String AVAILABILITY_ZONE_NAME = REGION_NAME;
    public static final String LOCATION_SPEC = PROVIDER + (REGION_NAME == null ? "" : ":" + REGION_NAME);
    public static final String TINY_HARDWARE_ID = "t1.micro";
    public static final String SMALL_HARDWARE_ID = "m1.small";
    
    @Test(groups="Live", enabled = false)
    public void testCreateVolume() throws Exception {
        super.testCreateVolume();
    }
    
    @Override
    protected String getProvider() {
        return PROVIDER;
    }

    @Override
    protected JcloudsLocation createJcloudsLocation() {
        return (JcloudsLocation) ctx.getLocationRegistry().resolve(LOCATION_SPEC);
    }
    
    @Override
    protected VolumeManager createVolumeManager() {
        return new EbsVolumeManager();
    }

    @Override
    protected int getVolumeSize() {
        return 1;
    }

    @Override
    protected String getDefaultAvailabilityZone() {
        return AVAILABILITY_ZONE_NAME;
    }

    @Override
    protected void assertVolumeAvailable(String volumeId) {
        Volume volume = ((EbsVolumeManager)volumeManager).describeVolume(jcloudsLocation, volumeId);
        assertNotNull(volume);
        assertEquals(volume.getStatus(), Volume.Status.AVAILABLE);
    }

    @Override
    protected JcloudsSshMachineLocation rebindJcloudsMachine() throws NoMachinesAvailableException {
        Map<String, ?> machineFlags = MutableMap.of("id", "i-4e904625",
                "hostname", "ec2-54-224-215-144.compute-1.amazonaws.com",
                "user", "aled",
                JcloudsLocation.PUBLIC_KEY_FILE.getName(), "/Users/aled/.ssh/id_rsa");
        return jcloudsLocation.rebindMachine(jcloudsLocation.getRawLocalConfigBag().putAll(machineFlags));
    }
}
