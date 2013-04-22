package io.cloudsoft.marklogic;

import java.util.Map;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

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

public abstract class AbstractMarkLogicLiveTest {

    protected BrooklynProperties brooklynProperties;
    protected ManagementContext ctx;

    protected TestApplication app;
    protected Location jcloudsLocation;

    protected MarkLogicNode markLogicNode;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        cleanUpBrooklynProperties();
        ctx = new LocalManagementContext(brooklynProperties);
        app = ApplicationBuilder.newManagedApp(TestApplication.class, ctx);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        if (app != null)
            Entities.destroyAll(app);
    }

    protected void testMarkLogicNode(String provider, String regionName, String imageId, String hardwareId,
            String loginUser) throws Exception {
        Map<?, ?> flags = ImmutableMap.of(
                "imageId", regionName != null ? String.format("%s/%s", regionName, imageId) : imageId,
                "hardwareId", hardwareId,
                "loginUser", loginUser
                );
        runTest(flags, provider, regionName);
    }

    protected void runTest(Map<?, ?> flags, String provider, String regionName) throws Exception {
        Map<?, ?> jcloudsFlags = MutableMap.builder().putAll(flags).build();
        String locationSpec = regionName != null ? String.format("%s:%s", provider, regionName) : provider;
        jcloudsLocation = ctx.getLocationRegistry().resolve(locationSpec, jcloudsFlags);
        markLogicNode = app.createAndManageChild(BasicEntitySpec.newInstance(MarkLogicNode.class)
                .configure(MarkLogicNode.IS_MASTER, true).configure(MarkLogicNode.MASTER_ADDRESS, "localhost"));
        app.start(ImmutableList.of(jcloudsLocation));
        EntityTestUtils.assertAttributeEqualsEventually(markLogicNode, SoftwareProcess.SERVICE_UP, true);
    }

    protected void cleanUpBrooklynProperties() {
        // Don't let any defaults from brooklyn.properties (except credentials)
        // interfere with test
        brooklynProperties = BrooklynProperties.Factory.newDefault();
        brooklynProperties.remove("brooklyn.jclouds." + getProvider() + ".image-description-regex");
        brooklynProperties.remove("brooklyn.jclouds." + getProvider() + ".image-name-regex");
        brooklynProperties.remove("brooklyn.jclouds." + getProvider() + ".image-id");
        brooklynProperties.remove("brooklyn.jclouds." + getProvider() + ".inboundPorts");
        brooklynProperties.remove("brooklyn.jclouds." + getProvider() + ".hardware-id");
        // Also removes scriptHeader (e.g. if doing `. ~/.bashrc` and `.
        // ~/.profile`, then that can cause "stdin: is not a tty")
        brooklynProperties.remove("brooklyn.ssh.config.scriptHeader");        
    }

    public abstract String getProvider();
}
