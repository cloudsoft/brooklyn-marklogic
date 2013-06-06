package io.cloudsoft.marklogic;

import brooklyn.config.BrooklynProperties;
import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.trait.Startable;
import brooklyn.location.Location;
import brooklyn.management.ManagementContext;
import brooklyn.management.internal.LocalManagementContext;
import brooklyn.test.EntityTestUtils;
import brooklyn.test.entity.TestApplication;
import brooklyn.util.MutableMap;
import brooklyn.util.text.Identifiers;
import io.cloudsoft.marklogic.clusters.MarkLogicCluster;
import io.cloudsoft.marklogic.databases.Database;
import io.cloudsoft.marklogic.databases.Databases;
import io.cloudsoft.marklogic.forests.Forest;
import io.cloudsoft.marklogic.forests.Forests;
import io.cloudsoft.marklogic.forests.UpdatesAllowed;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static brooklyn.entity.proxying.EntitySpecs.spec;
import static org.testng.AssertJUnit.assertEquals;

public class ForestMoveLiveTest  {

    private static final Logger LOG = LoggerFactory.getLogger(ForestLiveTest.class);
    private final String user = System.getProperty("user.name");

    public static final String PROVIDER = "aws-ec2";
    public static final String REGION_NAME = "us-east-1";
    public static final String TINY_HARDWARE_ID = "t1.micro";
    public static final String SMALL_HARDWARE_ID = "m1.small";
    public static final String MEDIUM_HARDWARE_ID = "m1.medium";

    private static BrooklynProperties brooklynProperties;
    private static ManagementContext ctx;

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

    protected static TestApplication app;
    protected static Location jcloudsLocation;
    private static Forests forests;
    private static MarkLogicCluster markLogicCluster;
    private static Databases databases;
    private static MarkLogicGroup dgroup;
    private static MarkLogicNode node3;
    private static MarkLogicNode node1;
    private static MarkLogicNode node2;

    @BeforeClass
    public static void beforeClass() throws Exception {
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
        markLogicCluster = app.createAndManageChild(spec(MarkLogicCluster.class)
                .configure(MarkLogicCluster.INITIAL_D_NODES_SIZE, 3)
                .configure(MarkLogicCluster.INITIAL_E_NODES_SIZE, 0)
                .configure(MarkLogicNode.IS_FORESTS_EBS, true)
                .configure(MarkLogicNode.IS_VAR_OPT_EBS, false)
                .configure(MarkLogicNode.IS_BACKUP_EBS, false)
        );

        Map<String, ?> jcloudsFlags = MutableMap.of("imageId", REGION_NAME + "/ami-3275ee5b", "loginUser", "ec2-user", "hardwareId", MEDIUM_HARDWARE_ID);
        jcloudsLocation = ctx.getLocationRegistry().resolve(PROVIDER + ":" + REGION_NAME, jcloudsFlags);

        app.start(Arrays.asList(jcloudsLocation));
        EntityTestUtils.assertAttributeEqualsEventually(app, Startable.SERVICE_UP, true);

        databases = markLogicCluster.getDatabases();
        forests = markLogicCluster.getForests();
        dgroup = markLogicCluster.getDNodeGroup();
        node1 = dgroup.getAnyStartedMember();
        node2 = dgroup.getAnyOtherStartedMember(node1.getHostName());
        node3 = dgroup.getAnyOtherStartedMember(node1.getHostName(), node2.getHostName());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (app != null) Entities.destroyAll(app);
    }

    private Forest createForest(MarkLogicNode node) {
        String forestId = Identifiers.makeRandomId(8);
        return forests.createForestWithSpec(spec(Forest.class)
                .configure(Forest.HOST, node.getHostName())
                .configure(Forest.NAME, user + "-forest" + ID_GENERATOR.incrementAndGet())
                .configure(Forest.DATA_DIR, "/var/opt/mldata/" + forestId)
                .configure(Forest.LARGE_DATA_DIR, "/var/opt/mldata/" + forestId)
                        //.configure(Forest.FAST_DATA_DIR, "/var/opt/mldata/" + forestId)
                .configure(Forest.UPDATES_ALLOWED, UpdatesAllowed.ALL)
                .configure(Forest.REBALANCER_ENABLED, true)
                .configure(Forest.FAILOVER_ENABLED, true)
        );
    }

    private Database createDatabase() {
        return databases.createDatabaseWithSpec(spec(Database.class)
                .configure(Database.NAME, "database-" + user + ID_GENERATOR.incrementAndGet())
                .configure(Database.JOURNALING, "strict")
        );
    }

    @Test
    public void testMoveForestWithReplica() throws Exception {
        LOG.info("-----------------testMoveForestWithReplica-----------------");

        Database database = createDatabase();

        Forest primaryForest = createForest(node1);
        primaryForest.awaitStatus("open");

        Forest replicaForest = createForest(node2);
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("open");

        forests.attachReplicaForest(primaryForest.getName(), replicaForest.getName());

        databases.attachForestToDatabase(primaryForest.getName(), database.getName());
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("sync replicating");

        forests.enableForest(primaryForest.getName(), false);
        primaryForest.awaitStatus("unmounted");
        replicaForest.awaitStatus("open");

        forests.unmountForest(primaryForest.getName());
        forests.setForestHost(primaryForest.getName(), node3.getHostName());
        forests.mountForest(primaryForest.getName());
        forests.enableForest(primaryForest.getName(), true);
        primaryForest.awaitStatus("sync replicating");
        replicaForest.awaitStatus("open");

        forests.enableForest(replicaForest.getName(), false);
        forests.enableForest(replicaForest.getName(), true);
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("sync replicating");
        assertEquals(node3.getHostName(), primaryForest.getHostname());
    }

    @Test
    public void testMoveForestWithoutReplica() throws Exception {
        LOG.info("-----------------testMoveForestWithoutReplica-----------------");

        Database database = createDatabase();

        Forest primaryForest = createForest(node1);
        primaryForest.awaitStatus("open");

        databases.attachForestToDatabase(primaryForest.getName(), database.getName());
        primaryForest.awaitStatus("open");

        forests.moveForest(primaryForest.getName(), node3.getHostName());

        assertEquals("open", primaryForest.getStatus());
        assertEquals(node3.getHostName(), primaryForest.getHostname());
    }

    @Test
    public void testMoveForestWithReplica2() throws Exception {
        LOG.info("-----------------testMoveForestWithReplica2-----------------");

        Database database = createDatabase();

        Forest primaryForest = createForest(node1);
        primaryForest.awaitStatus("open");

        Forest replicaForest = createForest(node2);
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("open");

        forests.attachReplicaForest(primaryForest.getName(), replicaForest.getName());

        databases.attachForestToDatabase(primaryForest.getName(), database.getName());
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("sync replicating");

        forests.moveForest(primaryForest.getName(), node3.getHostName());

        assertEquals("open", primaryForest.getStatus());
        assertEquals("sync replicating", replicaForest.getStatus());
        assertEquals(node3.getHostName(), primaryForest.getHostname());
    }

    @Test
    public void testMoveWithoutReplica() throws Exception {
        LOG.info("-----------------testMoveWithoutReplica-----------------");

        Database database = createDatabase();

        Forest forest = createForest(node1);
        forest.awaitStatus("open");

        databases.attachForestToDatabase(forest.getName(), database.getName());
        forest.awaitStatus("open");

        forests.enableForest(forest.getName(), false);
        forest.awaitStatus("unmounted");

        forests.unmountForest(forest.getName());
        forests.setForestHost(forest.getName(), node3.getHostName());
        forests.mountForest(forest.getName());
        forests.enableForest(forest.getName(), true);
        forest.awaitStatus("open");

        assertEquals(node3.getHostName(), forest.getHostname());
    }
}
