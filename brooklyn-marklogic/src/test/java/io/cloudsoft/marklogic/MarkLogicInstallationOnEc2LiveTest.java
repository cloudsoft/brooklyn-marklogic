package io.cloudsoft.marklogic;

import io.cloudsoft.marklogic.nodes.MarkLogicNode;

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
import brooklyn.location.Location;
import brooklyn.management.ManagementContext;
import brooklyn.management.internal.LocalManagementContext;
import brooklyn.test.entity.TestApplication;
import brooklyn.util.collections.MutableMap;

import com.google.common.collect.ImmutableMap;

@Test(groups = {"Live"})
public class MarkLogicInstallationOnEc2LiveTest {

    public static final Logger LOG = LoggerFactory.getLogger(MarkLogicInstallationOnEc2LiveTest.class);

    public static final String PROVIDER = "aws-ec2";

    private static final String EU_WEST_REGION_NAME = "eu-west-1";
    private static final String US_EAST_REGION_NAME = "us-east-1";
    public static final String MEDIUM_HARDWARE_ID = "m1.small";

    protected BrooklynProperties brooklynProperties;
    protected ManagementContext ctx;

    protected TestApplication app;
    protected Location jcloudsLocation;
    private MarkLogicNode markLogicNode;

    @DataProvider(name = "regionImageIdLoginUser")
    public Object[][] regionImageIdLoginUser() {
        return new Object[][]{
                // Amazon Linux AMI
                {US_EAST_REGION_NAME, "ami-3275ee5b", "ec2-user"}, {EU_WEST_REGION_NAME, "ami-44939930", "ec2-user"},
                // Centos 5.6
                {US_EAST_REGION_NAME, "ami-49e32320", null}, {EU_WEST_REGION_NAME, "ami-da3003ae", null},
                // Centos 6.3
                {US_EAST_REGION_NAME, "ami-7d7bfc14", null}, {EU_WEST_REGION_NAME, "ami-0ca7a878", null},
                // RHEL 6
                {US_EAST_REGION_NAME, "ami-b30983da", null}, {EU_WEST_REGION_NAME, "ami-c07b75b4", null}
        };
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        // Don't let any defaults from brooklyn.properties (except credentials) interfere with test
        brooklynProperties = BrooklynProperties.Factory.newDefault();
        brooklynProperties.remove("brooklyn.jclouds." + PROVIDER + ".image-description-regex");
        brooklynProperties.remove("brooklyn.jclouds." + PROVIDER + ".image-name-regex");
        brooklynProperties.remove("brooklyn.jclouds." + PROVIDER + ".image-id");
        brooklynProperties.remove("brooklyn.jclouds." + PROVIDER + ".inboundPorts");
        brooklynProperties.remove("brooklyn.jclouds." + PROVIDER + ".hardware-id");

        // Also removes scriptHeader (e.g. if doing `. ~/.bashrc` and `. ~/.profile`, then that can cause "stdin: is not a tty")
        brooklynProperties.remove("brooklyn.ssh.config.scriptHeader");
        ctx = new LocalManagementContext(brooklynProperties);
        app = ApplicationBuilder.newManagedApp(TestApplication.class, ctx);
    }

    @Test(dataProvider = "regionImageIdLoginUser")
    public void testMarkLogicNodeOnEC2(String regionName, String amiId, String loginUser) throws Exception {
        String imageId = String.format("%s/%s", regionName, amiId);
        Map<?, ?> flags = ImmutableMap.of("imageId", imageId,
                "hardwareId", MEDIUM_HARDWARE_ID,
                "loginUser", loginUser == null ? "root" : loginUser);
        runTest(flags, PROVIDER, regionName);
    }

    protected void runTest(Map<?, ?> flags, String provider, String regionName) throws Exception {
        Map<?, ?> jcloudsFlags = MutableMap.builder().putAll(flags).build();
        String locationSpec = String.format("%s:%s", provider, regionName);
        jcloudsLocation = ctx.getLocationRegistry().resolve(locationSpec, jcloudsFlags);
        doTest(jcloudsLocation);
    }

    private void doTest(Location loc) throws Exception {
        //markLogicNode = app.createAndManageChild(BasicEntitySpec.newInstance(MarkLogicNode.class)
        //        .configure(MarkLogicNode.INITIAL_HOST_ADDRESS, "localhost"));
        //
        //app.start(ImmutableList.of(loc));
        //EntityTestUtils.assertAttributeEqualsEventually(markLogicNode, SoftwareProcess.SERVICE_UP, true);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        if (app != null) Entities.destroyAll(app);
    }
}
