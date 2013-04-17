package io.cloudsoft.marklogic;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import brooklyn.config.BrooklynProperties;
import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.location.Location;
import brooklyn.management.ManagementContext;
import brooklyn.management.internal.LocalManagementContext;
import brooklyn.test.EntityTestUtils;
import brooklyn.test.entity.TestApplication;
import brooklyn.util.MutableMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@Test(groups = { "Live" })
public class MarkLogicInstallationOnMultipleCloudProvidersLiveTest {

   public static final Logger LOG = LoggerFactory.getLogger(MarkLogicInstallationOnMultipleCloudProvidersLiveTest.class);

   public static final String AWS_PROVIDER = "aws-ec2";
   private static final String AWS_EU_WEST_REGION_NAME = "eu-west-1";
   private static final String AWS_US_EAST_REGION_NAME = "us-east-1";
   public static final String AWS_SMALL_HARDWARE_ID = "m1.small";

   public static final String RACKSPACE_PROVIDER = "cloudservers-uk";
   public static final String RACKSPACE_SMALL_HARDWARE_ID = "2";
   
   protected BrooklynProperties brooklynProperties;
   protected ManagementContext ctx;

   protected TestApplication app;
   protected Location jcloudsLocation;
   private MarkLogicNode markLogicNode;

   @DataProvider(name = "providerRegionImageIdHardwareLoginUser")
   public Object[][] providerRegionImageIdHardwareLoginUser() {
      return new Object[][] {
              {RACKSPACE_PROVIDER, null, "127", RACKSPACE_SMALL_HARDWARE_ID, null},
              // Amazon Linux AMI
              {AWS_PROVIDER, AWS_US_EAST_REGION_NAME, "ami-3275ee5b", AWS_SMALL_HARDWARE_ID, "ec2-user"}, {AWS_EU_WEST_REGION_NAME, "ami-44939930", AWS_SMALL_HARDWARE_ID, "ec2-user"},
              // Centos 5.6
              {AWS_PROVIDER, AWS_US_EAST_REGION_NAME, "ami-49e32320", AWS_SMALL_HARDWARE_ID, null}, {AWS_EU_WEST_REGION_NAME, "ami-da3003ae", AWS_SMALL_HARDWARE_ID, null},
              // Centos 6.3
              {AWS_PROVIDER, AWS_US_EAST_REGION_NAME, "ami-7d7bfc14", AWS_SMALL_HARDWARE_ID, null}, {AWS_EU_WEST_REGION_NAME, "ami-0ca7a878", AWS_SMALL_HARDWARE_ID, null},
              // RHEL 6
              {AWS_PROVIDER, AWS_US_EAST_REGION_NAME, "ami-b30983da", AWS_SMALL_HARDWARE_ID, null}, {AWS_EU_WEST_REGION_NAME, "ami-c07b75b4", AWS_SMALL_HARDWARE_ID, null}
      };
   }

   @BeforeMethod(alwaysRun = true)
   public void setUp() throws Exception {
       // Don't let any defaults from brooklyn.properties (except credentials)
       // interfere with test
       for (String provider : ImmutableList.of(AWS_PROVIDER, RACKSPACE_PROVIDER)) {
           brooklynProperties = BrooklynProperties.Factory.newDefault();
           brooklynProperties.remove("brooklyn.jclouds." + provider + ".image-description-regex");
           brooklynProperties.remove("brooklyn.jclouds." + provider + ".image-name-regex");
           brooklynProperties.remove("brooklyn.jclouds." + provider + ".image-id");
           brooklynProperties.remove("brooklyn.jclouds." + provider + ".inboundPorts");
           brooklynProperties.remove("brooklyn.jclouds." + provider + ".hardware-id");
           // Also removes scriptHeader (e.g. if doing `. ~/.bashrc` and `.
           // ~/.profile`, then that can cause "stdin: is not a tty")
           brooklynProperties.remove("brooklyn.ssh.config.scriptHeader");
       }
       ctx = new LocalManagementContext(brooklynProperties);
       app = ApplicationBuilder.newManagedApp(TestApplication.class, ctx);
    }

   @Test(dataProvider = "providerRegionImageIdHardwareLoginUser")
   public void testMarkLogicNodeOnEC2(String provider, String regionName, String imageId, String hardwareId, 
           String loginUser) throws Exception {
      String fullyQualifiedImageId = regionName != null ? String.format("%s/%s", regionName, imageId) : imageId;
      Map<?,?> flags = ImmutableMap.of(
              "imageId", fullyQualifiedImageId,
              "hardwareId", hardwareId,
              "loginUser", loginUser == null ? "root" : loginUser
              );
      runTest(flags, provider, regionName);
   }

   protected void runTest(Map<?,?> flags, String provider, String regionName) throws Exception {
      Map<?,?> jcloudsFlags = MutableMap.builder().putAll(flags).build();
      String locationSpec = regionName != null ? String.format("%s:%s", provider, regionName) : provider;
      jcloudsLocation = ctx.getLocationRegistry().resolve(locationSpec, jcloudsFlags);
      doTest(jcloudsLocation);
   }

   private void doTest(Location loc) throws Exception {
      markLogicNode = app.createAndManageChild(BasicEntitySpec.newInstance(MarkLogicNode.class)
              .configure(MarkLogicNode.IS_MASTER, true)
              .configure(MarkLogicNode.MASTER_ADDRESS, "localhost"));
      app.start(ImmutableList.of(loc));
      EntityTestUtils.assertAttributeEqualsEventually(markLogicNode, SoftwareProcess.SERVICE_UP, true);
   }

   @AfterMethod(alwaysRun=true)
   public void tearDown() throws Exception {
      if (app != null) Entities.destroyAll(app);
   }
}
