package io.cloudsoft.marklogic;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

//todo: we need to clean up this test.. best to be able to run on localhost
public class ForestTest {

    public static final Logger LOG = LoggerFactory.getLogger(ForestTest.class);

    public static final String PROVIDER = "aws-ec2";

    public static final String MEDIUM_HARDWARE_ID = "m1.small";

    protected BrooklynProperties brooklynProperties;
    protected ManagementContext ctx;

    protected TestApplication app;
    protected Location jcloudsLocation;
    private MarkLogicNode markLogicNode;

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

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        if (app != null) Entities.destroyAll(app);
    }

    @Test
    public void testMarkLogicNodeOnEC2() throws Exception {
        String regionName = "us-east-1";
        String amiId = "ami-3275ee5b";
        String loginUser = "ec2-user";
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
        markLogicNode = app.createAndManageChild(BasicEntitySpec.newInstance(MarkLogicNode.class)
                .configure(MarkLogicNode.MASTER_ADDRESS, "localhost"));

        app.start(ImmutableList.of(loc));
        EntityTestUtils.assertAttributeEqualsEventually(markLogicNode, SoftwareProcess.SERVICE_UP, true);

        markLogicNode.createForest("peter", "/data", "/largeData", "/fastData", UpdatesAllowed.ALL, true, false);

        //todo: verify that this forest is created.
    }
}
