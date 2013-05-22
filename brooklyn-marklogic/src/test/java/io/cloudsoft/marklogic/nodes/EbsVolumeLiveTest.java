package io.cloudsoft.marklogic.nodes;

import static brooklyn.util.ssh.CommonCommands.sudo;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import org.jclouds.ec2.domain.Volume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import brooklyn.config.BrooklynProperties;
import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.location.jclouds.JcloudsLocation;
import brooklyn.location.jclouds.JcloudsSshMachineLocation;
import brooklyn.management.ManagementContext;
import brooklyn.management.internal.LocalManagementContext;
import brooklyn.test.entity.TestApplication;
import brooklyn.util.MutableMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class EbsVolumeLiveTest {

    private static final Logger LOG = LoggerFactory.getLogger(EbsVolumeLiveTest.class);

    public static final String PROVIDER = "aws-ec2";
    public static final String REGION_NAME = "us-east-1";
    public static final String AVAILABILITY_ZONE_NAME = REGION_NAME+"c";
    public static final String LOCATION_SPEC = PROVIDER + (REGION_NAME == null ? "" : ":" + REGION_NAME);
    public static final String TINY_HARDWARE_ID = "t1.micro";
    public static final String SMALL_HARDWARE_ID = "m1.small";
    
    private BrooklynProperties brooklynProperties;
    private ManagementContext ctx;
    
    private TestApplication app;
    private JcloudsLocation jcloudsLocation;
    private String volumeId;
    
    @BeforeMethod(alwaysRun=true)
    public void setUp() throws Exception {
        ctx = new LocalManagementContext();
        brooklynProperties = (BrooklynProperties) ctx.getConfig();

        // Don't let any defaults from brooklyn.properties (except credentials) interfere with test
        brooklynProperties.remove("brooklyn.jclouds."+PROVIDER+".image-description-regex");
        brooklynProperties.remove("brooklyn.jclouds."+PROVIDER+".image-name-regex");
        brooklynProperties.remove("brooklyn.jclouds."+PROVIDER+".image-id");
        brooklynProperties.remove("brooklyn.jclouds."+PROVIDER+".inboundPorts");
        brooklynProperties.remove("brooklyn.jclouds."+PROVIDER+".hardware-id");

        // Also removes scriptHeader (e.g. if doing `. ~/.bashrc` and `. ~/.profile`, then that can cause "stdin: is not a tty")
        brooklynProperties.remove("brooklyn.ssh.config.scriptHeader");

        app = ApplicationBuilder.newManagedApp(TestApplication.class, ctx);

//        Map<String,?> jcloudsFlags = MutableMap.of("imageId", "us-east-1/ami-7ce17315", "loginUser", "aled", "hardwareId", SMALL_HARDWARE_ID);

        jcloudsLocation = (JcloudsLocation) ctx.getLocationRegistry().resolve(LOCATION_SPEC);
    }

    @AfterMethod(alwaysRun=true)
    public void tearDown() throws Exception {
        if (app != null) Entities.destroyAll(app);
        if (volumeId != null) EbsVolumeCustomizer.deleteVolume(jcloudsLocation, volumeId);
    }

    @Test(groups="Live")
    public void testCreateVolume() throws Exception {
        volumeId = EbsVolumeCustomizer.createNewVolume(jcloudsLocation, AVAILABILITY_ZONE_NAME, 1, ImmutableMap.of("user", System.getProperty("user.name"), "purpose", "markLogic-EbsVolumeLiveTest"));
        
        Volume volume = EbsVolumeCustomizer.describeVolume(jcloudsLocation, volumeId);
        assertNotNull(volume);
        assertEquals(volume.getStatus(), Volume.Status.AVAILABLE);

    }

    @Test(groups="Live")
    public void testCreateAndAttachVolume() throws Throwable {
    	// TODO For speed of my initial testing, I created a VM in advance and used that.
    	// Test could be re-written to first create a VM.
    	// Worth running the test twice in a row on the same VM though, to ensure that cleanup all 
    	// worked as expected and one can attach+mount another volume.
    	
        Map<String, ?> machineFlags = MutableMap.of("id", "i-4e904625", 
        		"hostname", "ec2-54-224-215-144.compute-1.amazonaws.com", 
        		"user", "aled", 
        		JcloudsLocation.PUBLIC_KEY_FILE.getName(), "/Users/aled/.ssh/id_rsa");
        JcloudsSshMachineLocation machine = jcloudsLocation.rebindMachine(jcloudsLocation.getConfigBag().putAll(machineFlags));
        
		String availabilityZone = machine.getNode().getLocation().getId();
        String deviceSuffix = "h";
        String ec2DeviceName = "/dev/sd" + deviceSuffix;
        String osDeviceName = "/dev/xvd" + deviceSuffix;
		String mountPoint = "/var/opt2/test1";
		String filesystemType = "ext3";
		
		try {
			// Create and mount the initial volume
			volumeId = EbsVolumeCustomizer.createAttachAndMountNewVolume(machine, ec2DeviceName, osDeviceName, mountPoint, filesystemType, availabilityZone, 1,
					ImmutableMap.of("user", System.getProperty("user.name"), "purpose", "markLogic-EbsVolumeLiveTest"));

			assertExecSucceeds(machine, "show mount points", ImmutableList.of("mount -l", "mount -l | grep \""+mountPoint+"\" | grep \""+osDeviceName+"\""));
			assertExecSucceeds(machine, "list mount contents", ImmutableList.of("ls -la "+mountPoint));

			String tmpDestFile = "/tmp/myfile.txt";
			String destFile = mountPoint+"/myfile.txt";
			machine.copyTo(new ByteArrayInputStream("abc".getBytes()), tmpDestFile);
			assertExecSucceeds(machine, "list mount contents", ImmutableList.of(sudo("cp "+tmpDestFile+" "+destFile)));
			
			// Unmount and detach the volume
			EbsVolumeCustomizer.unmountFilesystem(machine, osDeviceName);
			EbsVolumeCustomizer.detachVolume(machine, volumeId, ec2DeviceName);

			assertExecFails(machine, "show mount points", ImmutableList.of("mount -l", "mount -l | grep \""+mountPoint+"\" | grep \""+osDeviceName+"\""));
			assertExecFails(machine, "check file contents", ImmutableList.of("cat "+destFile, "grep abc "+destFile));
			
			// Re-attach and mount the volume
			EbsVolumeCustomizer.attachAndMountVolume(machine, volumeId, ec2DeviceName, osDeviceName, mountPoint, filesystemType);

			assertExecSucceeds(machine, "show mount points", ImmutableList.of("mount -l", "mount -l | grep \""+mountPoint+"\" | grep \""+osDeviceName+"\""));
			assertExecSucceeds(machine, "list mount contents", ImmutableList.of("ls -la "+mountPoint));
			assertExecSucceeds(machine, "check file contents", ImmutableList.of("cat "+destFile, "grep abc "+destFile));

		} catch (Throwable t) {
			LOG.error("Error creating and attaching volume", t);
			throw t;
			
		} finally {
			if (volumeId != null) {
				try {
					EbsVolumeCustomizer.unmountFilesystem(machine, osDeviceName);
					EbsVolumeCustomizer.detachVolume(machine, volumeId, ec2DeviceName);
				} catch (Exception e) {
					LOG.error("Error umounting/detaching volume", e);
				}
			}
		}
    }

    private void assertExecSucceeds(SshMachineLocation machine, String description, List<String> cmds) {
    	int success = machine.execCommands(description, cmds);
    	assertEquals(success, 0);
    }
    
    private void assertExecFails(SshMachineLocation machine, String description, List<String> cmds) {
    	int success = machine.execCommands(description, cmds);
    	assertNotEquals(success, 0);
    }
}
