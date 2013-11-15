package io.cloudsoft.marklogic.brooklynapplications;

import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.location.Location;
import brooklyn.util.CommandLineUtil;
import brooklyn.util.text.Identifiers;
import com.google.common.collect.Lists;
import io.cloudsoft.marklogic.clusters.MarkLogicCluster;
import io.cloudsoft.marklogic.databases.Database;
import io.cloudsoft.marklogic.databases.Databases;
import io.cloudsoft.marklogic.forests.Forest;
import io.cloudsoft.marklogic.forests.Forests;
import io.cloudsoft.marklogic.forests.UpdatesAllowed;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * App to create a MarkLogic cluster (in a single availability zone).
 * <p/>
 * This can be launched by either:
 * <ul>
 * <li>Running the main method
 * <li>Running {@code export BROOKLYN_CLASSPATH=$(pwd)/target/classes; brooklyn launch --app io.cloudsoft.marklogic.MarkLogicApp}
 * </ul>
 */
public class MarkLogicTestApplication extends AbstractApplication {

    private static final Logger LOG = LoggerFactory.getLogger(MarkLogicTestApplication.class);

    // For naming databases/forests, so can tell in cloud provider's console who ran it
    private final String user = System.getProperty("user.name");

    private MarkLogicGroup dgroup;
    private Databases databases;
    private Forests forests;
    private MarkLogicCluster markLogicCluster;

    @Override
    public void init() {
        markLogicCluster = addChild(EntitySpec.create(MarkLogicCluster.class)
                .displayName("MarkLogic Cluster")
                .configure(MarkLogicCluster.INITIAL_D_NODES_SIZE, 3)
                .configure(MarkLogicCluster.INITIAL_E_NODES_SIZE, 0)
                .configure(MarkLogicNode.IS_FORESTS_EBS, true)
                .configure(MarkLogicNode.IS_VAR_OPT_EBS, false)
                .configure(MarkLogicNode.IS_BACKUP_EBS, false)
        );
        databases = markLogicCluster.getDatabases();
        forests = markLogicCluster.getForests();
        dgroup = markLogicCluster.getDNodeGroup();
    }

    @Override
    public void postStart(Collection<? extends Location> locations) {
        super.postStart(locations);

        LOG.info("MarkLogic Cluster Members:");
        int k = 1;
        for (MarkLogicNode entity : dgroup) {
            LOG.info("   " + k + " MarkLogic node http://" + entity.getAttribute(MarkLogicNode.HOSTNAME) + ":8001");
            k++;
        }

        String anyHostName = dgroup.getAnyUpMember().getHostname();
        LOG.info("MarkLogic server is available at 'http://{}:8000'", anyHostName);
        LOG.info("MarkLogic Cluster summary is available at 'http://{}:8001'", anyHostName);
        LOG.info("MarkLogic Monitoring Dashboard is available at 'http://{}:8002/dashboard'", anyHostName);

        try {
            MarkLogicNode node1 = dgroup.getAnyUpMember();
            MarkLogicNode node2 = dgroup.getAnyOtherUpMember(node1.getHostname());
            MarkLogicNode node3 = dgroup.getAnyOtherUpMember(node1.getHostname(), node2.getHostname());

            LOG.info("Creating a database in {}", databases);
            Database database = databases.createDatabaseWithSpec(EntitySpec.create(Database.class)
                    .configure(Database.NAME, Identifiers.makeRandomId(8))
                    .configure(Database.JOURNALING, "strict"));
            LOG.info("Created database: {}", database);

            LOG.info("Creating primary forest in {}", forests);
            String primaryForestId = Identifiers.makeRandomId(8);
            Forest primaryForest = forests.createForestWithSpec(EntitySpec.create(Forest.class)
                    .configure(Forest.HOST, node1.getHostname())
                    .configure(Forest.NAME, primaryForestId)
                    .configure(Forest.DATA_DIR, "/var/opt/mldata/" + primaryForestId)
                    .configure(Forest.LARGE_DATA_DIR, "/var/opt/mldata/" + primaryForestId)
                    .configure(Forest.UPDATES_ALLOWED, UpdatesAllowed.ALL)
                    .configure(Forest.REBALANCER_ENABLED, true)
                    .configure(Forest.FAILOVER_ENABLED, true)
            );
            LOG.info("Created primary forest: {}", primaryForest);

            //forests.enableForest(primaryForest.getName(),false);

            //forests.enableForest(primaryForest.getName(),true);
            //primaryForest.awaitStatus("open");

            LOG.info("Creating replica forest in {}", forests);
            String replicaForestId = Identifiers.makeRandomId(8);
            Forest replicaForest = forests.createForestWithSpec(
                    EntitySpec.create(Forest.class)
                            .configure(Forest.HOST, node2.getHostname())
                            .configure(Forest.NAME, replicaForestId)
                            .configure(Forest.MASTER,primaryForestId)
                            .configure(Forest.DATA_DIR, "/var/opt/mldata/" + replicaForestId)
                            .configure(Forest.LARGE_DATA_DIR, "/var/opt/mldata/" + replicaForestId)
                            .configure(Forest.UPDATES_ALLOWED, UpdatesAllowed.ALL)
                            .configure(Forest.REBALANCER_ENABLED, true)
                            .configure(Forest.FAILOVER_ENABLED, true));
            LOG.info("Created replica forest: {}", replicaForest);

          //  primaryForest.awaitStatus("open");
          //  replicaForest.awaitStatus("open");
           // forests.attachReplicaForest(primaryForest.getName(), replicaForest.getName());
            databases.attachForestToDatabase(primaryForest, database);

            LOG.info("Waiting for {} to have status 'open'", primaryForest);
            primaryForest.awaitStatus("open");
            LOG.info("Waiting for {} to have status 'sync replicating'", replicaForest);
            replicaForest.awaitStatus("sync replicating");

         //   forests.enableForest(primaryForest.getName(), false);
         //   forests.enableForest(primaryForest.getName(), true);


            LOG.info("Moving forest {} to new node: {}", primaryForest, node3);
            forests.moveForest(primaryForest.getName(), node3.getHostname());

//            replicaForest.awaitStatus("sync replicating");

//            forests.enableForest(primaryForest.getName(), false);
///
//            primaryForest.awaitStatus("unmounted");
//            replicaForest.awaitStatus("open");
//
//            forests.unmountForest(primaryForest.getName());
//
//            forests.setForestHost(primaryForest.getName(), node3.getHostname());
//
//            forests.mountForest(primaryForest.getName());
//
//            forests.enableForest(primaryForest.getName(), true);
//
//            primaryForest.awaitStatus("sync replicating");
//            replicaForest.awaitStatus("open");
//
//            forests.enableForest(replicaForest.getName(), false);
//            forests.enableForest(replicaForest.getName(), true);
//
//            primaryForest.awaitStatus("open");
//            replicaForest.awaitStatus("sync replicating");
//
            LOG.info("Done");
        } catch (Exception e) {
            LOG.error("Error starting MarkLogic app", e);
        }
    }

    /**
     * Launches the application, along with the brooklyn web-console.
     */
    public static void main(String[] argv) throws Exception {
        List<String> args = Lists.newArrayList(argv);
        String port = CommandLineUtil.getCommandLineOption(args, "--port", "8081+");
        String location = CommandLineUtil.getCommandLineOption(args, "--location", "localhost");

        BrooklynLauncher launcher = BrooklynLauncher.newInstance()
                .application(EntitySpec.create(MarkLogicTestApplication.class).displayName("Brooklyn MarkLogic Application"))
                .webconsolePort(port)
                .location(location)
                .start();

        Entities.dumpInfo(launcher.getApplications());
    }
}
