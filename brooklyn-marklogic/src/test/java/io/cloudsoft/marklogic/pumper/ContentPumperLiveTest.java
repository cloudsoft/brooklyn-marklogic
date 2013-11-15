package io.cloudsoft.marklogic.pumper;

import static org.testng.Assert.assertTrue;

import java.util.Arrays;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.Startable;
import brooklyn.location.Location;
import brooklyn.management.ManagementContext;
import brooklyn.management.internal.LocalManagementContext;
import brooklyn.test.Asserts;
import brooklyn.test.EntityTestUtils;
import brooklyn.test.entity.TestApplication;
import brooklyn.util.collections.MutableMap;
import io.cloudsoft.marklogic.clusters.MarkLogicCluster;
import io.cloudsoft.marklogic.dto.ForestCounts;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;

// Would like this to be in content pumper module but causes a circular dependency.
public class ContentPumperLiveTest {

    public ManagementContext ctx;
    private TestApplication app;

    @BeforeMethod(alwaysRun=true)
    public void setUp() throws Exception {
        ctx = new LocalManagementContext();
        app = ApplicationBuilder.newManagedApp(TestApplication.class, ctx);
    }

    @AfterMethod(alwaysRun=true)
    public void tearDown() throws Exception {
        if (app != null) Entities.destroyAll(app.getManagementContext());
    }

    @Test(groups = "Live")
    public void testPumpToServer() {
        final ContentPumper pumper = app.createAndManageChild(EntitySpec.create(ContentPumper.class));
        final MarkLogicCluster marklogic = app.createAndManageChild(EntitySpec.create(MarkLogicCluster.class)
                .configure(MarkLogicCluster.INITIAL_D_NODES_SIZE, 0)
                .configure(MarkLogicCluster.INITIAL_E_NODES_SIZE, 1)
                .configure(MarkLogicNode.IS_BACKUP_EBS, false)
                .configure(MarkLogicNode.IS_FORESTS_EBS, false)
                .configure(MarkLogicNode.IS_VAR_OPT_EBS, false));
        Location jcloudsLocation = ctx.getLocationRegistry().resolve("aws-ec2:us-east-1", MutableMap.of(
                "imageId", "us-east-1/ami-3275ee5b",
                "hardwareId", "m1.medium"));

        app.start(Arrays.asList(jcloudsLocation));
        EntityTestUtils.assertAttributeEqualsEventually(app, Startable.SERVICE_UP, true);

        final MarkLogicNode node = marklogic.getAnyUpNodeOrWait();
        marklogic.getAppServices().createAppServer("XDBC", "mlcp", "Documents", "Default", 8006);

        pumper.pumpTo(node.getHostname(), 8006,
                marklogic.getConfig(MarkLogicNode.USER), marklogic.getConfig(MarkLogicNode.PASSWORD));

        Asserts.succeedsEventually(new Runnable() {
            @Override
            public void run() {
                ForestCounts forestCounts = node.getApi().getForestApi().getForestCounts("Documents");
                assertTrue(forestCounts.getDocumentCount() > 0,
                        "Expected at least one document in Documents forest. Was: " + forestCounts.getDocumentCount());
            }
        });
    }

}
