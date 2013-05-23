package io.cloudsoft.marklogic.brooklynapplications;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpecs;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.location.Location;
import brooklyn.util.CommandLineUtil;
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

import static brooklyn.entity.proxying.EntitySpecs.spec;

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

    private MarkLogicGroup group;
    private Databases databases;
    private Forests forests;
    MarkLogicCluster markLogicCluster;

    @Override
    public void init() {
        markLogicCluster = addChild(spec(MarkLogicCluster.class)
                .displayName("MarkLogic Cluster")
                .configure(MarkLogicCluster.INITIAL_D_NODES_SIZE, 3)
                .configure(MarkLogicCluster.INITIAL_E_NODES_SIZE, 0)
         );
        databases = markLogicCluster.getDatabases();
        forests = markLogicCluster.getForests();
        group = markLogicCluster.getDNodeGroup();
    }

    @Override
    public void postStart(Collection<? extends Location> locations) {
        super.postStart(locations);

        LOG.info("MarkLogic Cluster Members:");
        int k = 1;
        for (Entity entity : group.getMembers()) {
            LOG.info("   " + k + " MarkLogic node http://" + entity.getAttribute(MarkLogicNode.HOSTNAME) + ":8001");
            k++;
        }

        LOG.info("MarkLogic server is available at 'http://" +
                group.getAnyStartedMember().getHostName() + ":8000'");
        LOG.info("MarkLogic Cluster summary is available at 'http://" +
                group.getAnyStartedMember().getHostName() +
                ":8001'");
        LOG.info("MarkLogic Monitoring Dashboard is available at 'http://" +
                group.getAnyStartedMember().getHostName() +
                ":8002/dashboard'");

        MarkLogicNode node1 = group.getAnyStartedMember();
        MarkLogicNode node2 = group.getAnyOtherStartedMember(node1.getHostName());

        Database database = databases.createDatabase("database-peter");

        Forest replicaForest = forests.createForestWithSpec(spec(Forest.class)
                .configure(Forest.NAME, "peter-forest-replica")
                .configure(Forest.DATA_DIR,"/tmp")
                .configure(Forest.LARGE_DATA_DIR,"/tmp")
                .configure(Forest.FAST_DATA_DIR,"/tmp")
                .configure(Forest.HOST, node2.getHostName())
                .configure(Forest.UPDATES_ALLOWED, UpdatesAllowed.ALL)
                .configure(Forest.REBALANCER_ENABLED, true)
                .configure(Forest.FAILOVER_ENABLED, true)
        );

        Forest primaryForest = forests.createForestWithSpec(spec(Forest.class)
                .configure(Forest.NAME, "peter-forest")
                .configure(Forest.DATA_DIR,"/tmp")
                .configure(Forest.LARGE_DATA_DIR,"/tmp")
                .configure(Forest.FAST_DATA_DIR,"/tmp")
                .configure(Forest.HOST, node1.getHostName())
                .configure(Forest.UPDATES_ALLOWED, UpdatesAllowed.ALL)
                .configure(Forest.REBALANCER_ENABLED, true)
                .configure(Forest.FAILOVER_ENABLED, true)
        );

        databases.attachForestToDatabase(primaryForest.getName(), database.getName());
        forests.attachReplicaForest(primaryForest.getName(),replicaForest.getName());

        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
        }

        //now we are going to convert our primary to replica
        forests.enableForest(primaryForest.getName(), false);
        forests.deleteForestConfiguration(primaryForest.getName());
        //forests.enableForest(primaryForest.getName(),true);
    }

    /**
     * Launches the application, along with the brooklyn web-console.
     */
    public static void main(String[] argv) throws Exception {
        List<String> args = Lists.newArrayList(argv);
        String port = CommandLineUtil.getCommandLineOption(args, "--port", "8081+");
        String location = CommandLineUtil.getCommandLineOption(args, "--location", "localhost");

        BrooklynLauncher launcher = BrooklynLauncher.newInstance()
                .application(EntitySpecs.appSpec(MarkLogicTestApplication.class).displayName("Brooklyn MarkLogic Application"))
                .webconsolePort(port)
                .location(location)
                .start();

        Entities.dumpInfo(launcher.getApplications());
    }
}
