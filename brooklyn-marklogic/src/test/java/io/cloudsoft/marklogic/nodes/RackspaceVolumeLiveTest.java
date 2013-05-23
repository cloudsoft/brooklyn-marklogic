package io.cloudsoft.marklogic.nodes;

import static brooklyn.util.ssh.CommonCommands.sudo;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import org.jclouds.openstack.cinder.v1.domain.Volume;
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
import brooklyn.util.collections.MutableMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class RackspaceVolumeLiveTest {

    private static final Logger LOG = LoggerFactory.getLogger(RackspaceVolumeLiveTest.class);

    public static final String PROVIDER = "rackspace-cloudservers-uk";
    public static final String LOCATION_SPEC = PROVIDER;
    public static final String TINY_HARDWARE_ID = "1";
    public static final String SMALL_HARDWARE_ID = "2";

    private static final int VOLUME_SIZE = 100; // min on rackspace is 100
    
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
        if (volumeId != null) RackspaceVolumeCustomizer.deleteVolume(jcloudsLocation, volumeId);
    }

    @Test(groups="Live")
    public void testCreateVolume() throws Exception {
        volumeId = RackspaceVolumeCustomizer.createNewVolume(jcloudsLocation, null, VOLUME_SIZE, ImmutableMap.of("user", System.getProperty("user.name"), "purpose", "markLogic-RackspaceVolumeLiveTest"));
        
        Volume volume = RackspaceVolumeCustomizer.describeVolume(jcloudsLocation, volumeId);
        assertNotNull(volume);
        assertEquals(volume.getStatus(), Volume.Status.AVAILABLE);

    }

    @Test(groups="Live")
    public void testCreateAndAttachVolume() throws Throwable {
    	// TODO For speed of my initial testing, I created a VM in advance and used that.
    	// Test could be re-written to first create a VM.
    	// Worth running the test twice in a row on the same VM though, to ensure that cleanup all 
    	// worked as expected and one can attach+mount another volume.
    	
        Map<String, ?> machineFlags = MutableMap.of("id", "LON/dab52345-9b6f-4b60-94de-64881a3f91d9", 
        		"hostname", "162.13.8.61", 
        		"user", "root", 
        		"password", "Bko8gLpZs6CJ",
        		JcloudsLocation.PUBLIC_KEY_FILE.getName(), "/Users/aled/.ssh/id_rsa");
        JcloudsSshMachineLocation machine = jcloudsLocation.rebindMachine(jcloudsLocation.getConfigBag().putAll(machineFlags));
        
		String availabilityZone = null;
        String deviceSuffix = "h";
        String ec2DeviceName = "/dev/sd" + deviceSuffix;
        String osDeviceName = "/dev/xvd" + deviceSuffix;
		String mountPoint = "/var/opt2/test1";
		String filesystemType = "ext3";
		
		try {
			// Create and mount the initial volume
			volumeId = RackspaceVolumeCustomizer.createAttachAndMountNewVolume(machine, ec2DeviceName, osDeviceName, mountPoint, filesystemType, availabilityZone, VOLUME_SIZE,
					ImmutableMap.of("user", System.getProperty("user.name"), "purpose", "markLogic-RackspaceVolumeLiveTest"));

			assertExecSucceeds(machine, "show mount points", ImmutableList.of("mount -l", "mount -l | grep \""+mountPoint+"\" | grep \""+osDeviceName+"\""));
			assertExecSucceeds(machine, "list mount contents", ImmutableList.of("ls -la "+mountPoint));

			String tmpDestFile = "/tmp/myfile.txt";
			String destFile = mountPoint+"/myfile.txt";
			machine.copyTo(new ByteArrayInputStream("abc".getBytes()), tmpDestFile);
			assertExecSucceeds(machine, "list mount contents", ImmutableList.of(sudo("cp "+tmpDestFile+" "+destFile)));
			
			// Unmount and detach the volume
			RackspaceVolumeCustomizer.unmountFilesystem(machine, osDeviceName);
			RackspaceVolumeCustomizer.detachVolume(machine, volumeId, ec2DeviceName);

			assertExecFails(machine, "show mount points", ImmutableList.of("mount -l", "mount -l | grep \""+mountPoint+"\" | grep \""+osDeviceName+"\""));
			assertExecFails(machine, "check file contents", ImmutableList.of("cat "+destFile, "grep abc "+destFile));
			
			// Re-attach and mount the volume
			RackspaceVolumeCustomizer.attachAndMountVolume(machine, volumeId, ec2DeviceName, osDeviceName, mountPoint, filesystemType);

			assertExecSucceeds(machine, "show mount points", ImmutableList.of("mount -l", "mount -l | grep \""+mountPoint+"\" | grep \""+osDeviceName+"\""));
			assertExecSucceeds(machine, "list mount contents", ImmutableList.of("ls -la "+mountPoint));
			assertExecSucceeds(machine, "check file contents", ImmutableList.of("cat "+destFile, "grep abc "+destFile));

		} catch (Throwable t) {
			LOG.error("Error creating and attaching volume", t);
			throw t;
			
		} finally {
			if (volumeId != null) {
				try {
					RackspaceVolumeCustomizer.unmountFilesystem(machine, osDeviceName);
					RackspaceVolumeCustomizer.detachVolume(machine, volumeId, ec2DeviceName);
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
