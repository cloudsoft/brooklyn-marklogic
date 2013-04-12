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
public class MarkLogicMultiOsOnEc2LiveTest {

    public static final Logger LOG = LoggerFactory.getLogger(MarkLogicMultiOsOnEc2LiveTest.class);
    
    public static final String PROVIDER = "aws-ec2";
    
    private static final String EU_WEST_REGION_NAME = "eu-west-1";
    private static final String US_EAST_REGION_NAME = "us-east-1";
    public static final String MEDIUM_HARDWARE_ID = "m1.medium";
    
    protected BrooklynProperties brooklynProperties;
    protected ManagementContext ctx;
    
    protected TestApplication app;
    protected Location jcloudsLocation;
    private MarkLogicNode markLogicNode;
    
    @DataProvider(name = "centOS_6_3")
    public Object[][] centOS_6_3() {
        return new Object[][] { { US_EAST_REGION_NAME, "ami-7d7bfc14" }, { EU_WEST_REGION_NAME, "ami-0ca7a878" }};
    }
    
    @DataProvider(name = "centOS_5_6")
    public Object[][] centOS_5_6() {
        return new Object[][] { { US_EAST_REGION_NAME, "ami-49e32320" }, { EU_WEST_REGION_NAME, "ami-da3003ae" }};
    }
    
    @DataProvider(name = "rhel_6")
    public Object[][] rhel_6() {
        return new Object[][] { { US_EAST_REGION_NAME, "ami-b30983da" }, { EU_WEST_REGION_NAME, "ami-c07b75b4" }};
    }
    
    @DataProvider(name = "amazon_linux_ami")
    public Object[][] amazon_linux_ami() {
        return new Object[][] { { US_EAST_REGION_NAME, "ami-3275ee5b" }, { EU_WEST_REGION_NAME, "ami-44939930" }};
    }
    
    @BeforeMethod(alwaysRun=true)
    public void setUp() throws Exception {
        // Don't let any defaults from brooklyn.properties (except credentials) interfere with test
        brooklynProperties = BrooklynProperties.Factory.newDefault();
        brooklynProperties.remove("brooklyn.jclouds."+PROVIDER+".image-description-regex");
        brooklynProperties.remove("brooklyn.jclouds."+PROVIDER+".image-name-regex");
        brooklynProperties.remove("brooklyn.jclouds."+PROVIDER+".image-id");
        brooklynProperties.remove("brooklyn.jclouds."+PROVIDER+".inboundPorts");
        brooklynProperties.remove("brooklyn.jclouds."+PROVIDER+".hardware-id");

        // Also removes scriptHeader (e.g. if doing `. ~/.bashrc` and `. ~/.profile`, then that can cause "stdin: is not a tty")
        brooklynProperties.remove("brooklyn.ssh.config.scriptHeader");
        ctx = new LocalManagementContext(brooklynProperties);
        app = ApplicationBuilder.newManagedApp(TestApplication.class, ctx);
    }

    @Test(dataProvider = "amazon_linux_ami")
    public void test_AmazonLinuxAMI(String regionName, String amiId) throws Exception {
        String imageId = String.format("%s/%s", regionName, amiId);
        runTest(ImmutableMap.of("imageId", imageId, "hardwareId", MEDIUM_HARDWARE_ID, "loginUser", "ec2-user"), 
                PROVIDER, regionName);
    }

    @Test(dataProvider = "centOS_6_3")
    public void test_CentOS_6_3(String regionName, String amiId) throws Exception {
        String imageId = String.format("%s/%s", regionName, amiId);
        runTest(ImmutableMap.of("imageId", imageId, "hardwareId", MEDIUM_HARDWARE_ID), PROVIDER, regionName);
    }

    @Test(dataProvider = "centOS_5_6")
    public void test_CentOS_5_6(String regionName, String amiId) throws Exception {
        String imageId = String.format("%s/%s", regionName, amiId);
        runTest(ImmutableMap.of("imageId", imageId, "hardwareId", MEDIUM_HARDWARE_ID), PROVIDER, regionName);
    }

    @Test(dataProvider = "rhel_6")
    public void test_Red_Hat_Enterprise_Linux_6(String regionName, String amiId) throws Exception {
        String imageId = String.format("%s/%s", regionName, amiId);
        runTest(ImmutableMap.of("imageId", imageId, "hardwareId", MEDIUM_HARDWARE_ID), PROVIDER, regionName);
    }
    
    protected void runTest(Map<?,?> flags, String provider, String regionName) throws Exception {
        Map<?,?> jcloudsFlags = MutableMap.builder().putAll(flags).build();
        String locationSpec = String.format("%s:%s", provider, regionName);
        jcloudsLocation = ctx.getLocationRegistry().resolve(locationSpec, jcloudsFlags);
        doTest(jcloudsLocation);
    }

    private void doTest(Location loc) throws Exception {
        markLogicNode = app.createAndManageChild(BasicEntitySpec.newInstance(MarkLogicNode.class)
                .configure(MarkLogicNode.MASTER_ADDRESS, "localhost"));
        
        app.start(ImmutableList.of(loc));
        EntityTestUtils.assertAttributeEqualsEventually(markLogicNode, SoftwareProcess.SERVICE_UP, true);
    }
    
    @AfterMethod(alwaysRun=true)
    public void tearDown() throws Exception {
        if (app != null) Entities.destroyAll(app);
    }
}
