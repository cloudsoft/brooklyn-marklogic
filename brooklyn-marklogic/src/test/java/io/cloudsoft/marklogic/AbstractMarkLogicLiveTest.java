package io.cloudsoft.marklogic;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import brooklyn.config.BrooklynProperties;
import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.Startable;
import brooklyn.location.Location;
import brooklyn.management.ManagementContext;
import brooklyn.management.internal.LocalManagementContext;
import brooklyn.test.EntityTestUtils;
import brooklyn.test.entity.TestApplication;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.text.Identifiers;
import io.cloudsoft.marklogic.appservers.AppServices;
import io.cloudsoft.marklogic.clusters.MarkLogicCluster;
import io.cloudsoft.marklogic.databases.Database;
import io.cloudsoft.marklogic.databases.Databases;
import io.cloudsoft.marklogic.forests.Forest;
import io.cloudsoft.marklogic.forests.Forests;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;

public abstract class AbstractMarkLogicLiveTest {

    public static final Logger LOG = LoggerFactory.getLogger(AbstractMarkLogicLiveTest.class);
    public final String user = System.getProperty("user.name");

    public final String PROVIDER = "aws-ec2";
    public final String REGION_NAME = "us-east-1";
    public final String TINY_HARDWARE_ID = "t1.micro";
    public final String SMALL_HARDWARE_ID = "m1.small";
    public final String MEDIUM_HARDWARE_ID = "m1.medium";

    public BrooklynProperties brooklynProperties;
    public ManagementContext ctx;

    public final AtomicInteger ID_GENERATOR = new AtomicInteger();
    public AppServices appServices;
    public MarkLogicGroup egroup;
    public TestApplication app;
    public Location jcloudsLocation;
    public Forests forests;
    public MarkLogicCluster markLogicCluster;
    public Databases databases;
    public MarkLogicGroup dgroup;
    public MarkLogicNode dNode1;
    public MarkLogicNode dNode2;
    public MarkLogicNode dNode3;

    // Default to running in full-cluster mode
    public int getNumberOfDNodes() {
        return 3;
    }

    public int getNumberOfENodes() {
        return 1;
    }

    @BeforeClass(alwaysRun = true)
    public void beforeClass() throws Exception {
        try {
            ctx = new LocalManagementContext();
            brooklynProperties = (BrooklynProperties) ctx.getConfig();

            // Don't let any defaults from brooklyn.properties (except credentials) interfere with test
            brooklynProperties.remove("brooklyn.jclouds." + PROVIDER + ".image-description-regex");
            brooklynProperties.remove("brooklyn.jclouds." + PROVIDER + ".image-name-regex");
            brooklynProperties.remove("brooklyn.jclouds." + PROVIDER + ".image-id");
            brooklynProperties.remove("brooklyn.jclouds." + PROVIDER + ".inboundPorts");
            brooklynProperties.remove("brooklyn.jclouds." + PROVIDER + ".hardware-id");

            // Also removes scriptHeader (e.g. if doing `. ~/.bashrc` and `. ~/.profile`, then that can cause "stdin: is not a tty")
            brooklynProperties.remove("brooklyn.ssh.config.scriptHeader");

            app = ApplicationBuilder.newManagedApp(TestApplication.class, ctx);
            markLogicCluster = app.createAndManageChild(EntitySpec.create(MarkLogicCluster.class)
                    .configure(MarkLogicCluster.INITIAL_D_NODES_SIZE, getNumberOfDNodes())
                    .configure(MarkLogicCluster.INITIAL_E_NODES_SIZE, getNumberOfENodes())
                    .configure(MarkLogicNode.IS_FORESTS_EBS, true)
                    .configure(MarkLogicNode.IS_VAR_OPT_EBS, false)
                    .configure(MarkLogicNode.IS_BACKUP_EBS, false)
            );

            //general purpose centos image
            String ami = "ami-3275ee5b";
            Map<String, ?> jcloudsFlags = MutableMap.of(
                    "imageId", REGION_NAME + "/" + ami,
                    "hardwareId", MEDIUM_HARDWARE_ID);
            jcloudsLocation = ctx.getLocationRegistry().resolve(PROVIDER + ":" + REGION_NAME, jcloudsFlags);

            app.start(Arrays.asList(jcloudsLocation));
            EntityTestUtils.assertAttributeEqualsEventually(app, Startable.SERVICE_UP, true);

            databases = markLogicCluster.getDatabases();
            appServices = markLogicCluster.getAppServices();
            forests = markLogicCluster.getForests();
            dgroup = markLogicCluster.getDNodeGroup();
            egroup = markLogicCluster.getENodeGroup();
            if (getNumberOfDNodes() >= 1)
                dNode1 = dgroup.getAnyUpMember();
            if (getNumberOfDNodes() >= 2)
                dNode2 = dgroup.getAnyOtherUpMember(dNode1.getHostname());
            if (getNumberOfDNodes() >= 3)
                dNode3 = dgroup.getAnyOtherUpMember(dNode1.getHostname(), dNode2.getHostname());
        } catch (Exception e) {
            LOG.error("Failed to setup cluster", e);
            throw e;
        }
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() throws Exception {
        if (app != null) {
            //for (Entity entity : forests.getChildren()) {
            //    if (entity instanceof Forest) {
            //        Forest forest = (Forest) entity;
            //        if (forest.getDataDir() != null) {
            //            forests.enableForest(forest.getName(), false);
            //            forests.unmountForest(forest.getName());
            //        }
            //    }
            //}
            Entities.destroyAll(app.getManagementContext());
        }
    }

    public Forest createForest(MarkLogicNode node) {
        return createForest(node,null);
    }

    public Forest createForest(MarkLogicNode node, String master) {
        String forestId = Identifiers.makeRandomId(8);
        return forests.createForestWithSpec(EntitySpec.create(Forest.class)
                .configure(Forest.HOST, node.getHostname())
                .configure(Forest.NAME, user + "Forest" + ID_GENERATOR.incrementAndGet())
                .configure(Forest.DATA_DIR, "/var/opt/mldata/" + forestId)
                .configure(Forest.LARGE_DATA_DIR, "/var/opt/mldata/" + forestId)
                        //.configure(Forest.FAST_DATA_DIR, "/var/opt/mldata/" + forestId)
                .configure(Forest.MASTER, master)
                .configure(Forest.REBALANCER_ENABLED, true)
                .configure(Forest.FAILOVER_ENABLED, true)
        );
    }

    public Database createDatabase() {
        return databases.createDatabaseWithSpec(EntitySpec.create(Database.class)
                .configure(Database.NAME, "database-" + user + ID_GENERATOR.incrementAndGet())
                .configure(Database.JOURNALING, "strict")
        );
    }
}
