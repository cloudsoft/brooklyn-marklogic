package brooklyn.location.volumes;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.Map;

import org.jclouds.ec2.domain.Volume;

import brooklyn.location.NoMachinesAvailableException;
import brooklyn.location.jclouds.JcloudsLocation;
import brooklyn.location.jclouds.JcloudsSshMachineLocation;
import brooklyn.util.collections.MutableMap;

public class EbsVolumeManagerLiveTest extends AbstractVolumeManagerLiveTest {

    public static final String PROVIDER = "aws-ec2";
    public static final String REGION_NAME = "us-east-1";
    public static final String AVAILABILITY_ZONE_NAME = REGION_NAME + "c";
    public static final String LOCATION_SPEC = PROVIDER + (REGION_NAME == null ? "" : ":" + REGION_NAME);
    public static final String TINY_HARDWARE_ID = "t1.micro";
    public static final String SMALL_HARDWARE_ID = "m1.small";
    
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
        return jcloudsLocation.rebindMachine(jcloudsLocation.getConfigBag().putAll(machineFlags));
    }
}
