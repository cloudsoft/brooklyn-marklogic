package io.cloudsoft.marklogic.brooklynapplications;

import static brooklyn.entity.proxying.EntitySpecs.spec;
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

import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpecs;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.location.Location;
import brooklyn.util.CommandLineUtil;
import brooklyn.util.exceptions.Exceptions;
import brooklyn.util.text.Identifiers;

import com.google.common.collect.Lists;

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

    private MarkLogicGroup dgroup;
    private Databases databases;
    private Forests forests;
    MarkLogicCluster markLogicCluster;

    @Override
    public void init() {
        markLogicCluster = addChild(spec(MarkLogicCluster.class)
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
        for (Entity entity : dgroup.getMembers()) {
            LOG.info("   " + k + " MarkLogic node http://" + entity.getAttribute(MarkLogicNode.HOSTNAME) + ":8001");
            k++;
        }

        LOG.info("MarkLogic server is available at 'http://" +
                dgroup.getAnyStartedMember().getHostName() + ":8000'");
        LOG.info("MarkLogic Cluster summary is available at 'http://" +
                dgroup.getAnyStartedMember().getHostName() +
                ":8001'");
        LOG.info("MarkLogic Monitoring Dashboard is available at 'http://" +
                dgroup.getAnyStartedMember().getHostName() +
                ":8002/dashboard'");

        MarkLogicNode node1 = dgroup.getAnyStartedMember();
        MarkLogicNode node2 = dgroup.getAnyOtherStartedMember(node1.getHostName());
        MarkLogicNode node3 = dgroup.getAnyOtherStartedMember(node1.getHostName(),node2.getHostName());

        Database database = databases.createDatabaseWithSpec(spec(Database.class)
                .configure(Database.NAME, "database-peter")
                .configure(Database.JOURNALING,"strict")
        );

        String primaryForestId = Identifiers.makeRandomId(8);
        Forest primaryForest = forests.createForestWithSpec(spec(Forest.class)
                .configure(Forest.HOST, node1.getHostName())
                .configure(Forest.NAME, "peter-forest")
                .configure(Forest.DATA_DIR, "/var/opt/mldata/"+primaryForestId)
                .configure(Forest.LARGE_DATA_DIR, "/var/opt/mldata/"+primaryForestId)
                .configure(Forest.FAST_DATA_DIR, "/var/opt/mldata/"+primaryForestId)
                .configure(Forest.UPDATES_ALLOWED, UpdatesAllowed.ALL)
                .configure(Forest.REBALANCER_ENABLED, true)
                .configure(Forest.FAILOVER_ENABLED, true)
        );

        String replicaForestId = Identifiers.makeRandomId(8);
        Forest replicaForest = forests.createForestWithSpec(spec(Forest.class)
                .configure(Forest.HOST, node2.getHostName())
                .configure(Forest.NAME, "peter-forest-replica")
                .configure(Forest.DATA_DIR, "/var/opt/mldata/"+replicaForestId)
                .configure(Forest.LARGE_DATA_DIR, "/var/opt/mldata/"+replicaForestId)
                .configure(Forest.FAST_DATA_DIR, "/var/opt/mldata/"+replicaForestId)
                .configure(Forest.UPDATES_ALLOWED, UpdatesAllowed.ALL)
                .configure(Forest.REBALANCER_ENABLED, true)
                .configure(Forest.FAILOVER_ENABLED, true));

        sleep(60);

        forests.attachReplicaForest(primaryForest.getName(),replicaForest.getName());

        sleep(60);

        databases.attachForestToDatabase(primaryForest.getName(), database.getName());

        sleep(60);

        forests.enableForest(primaryForest.getName(), false);

        sleep(60);

        forests.setForestHost(primaryForest.getName(), node3.getHostName());

        sleep(60);

        forests.enableForest(primaryForest.getName(), true);

        sleep(60);

        forests.enableForest(replicaForest.getName(), false);
        forests.enableForest(replicaForest.getName(), true);


        //  forests.enableForest(primaryForest.getName(), true);


        //databases.attachForestToDatabase(replicaForest.getName(), database.getName());



        //now we are going to convert our primary to replica
        //    forests.enableForest(primaryForest.getName(), false);

       // sleep(30);

        //forests.deleteForestConfiguration(primaryForest.getName());

        //primaryForest = forests.createForestWithSpec(primaryForestSpec);
        //sleep(30);
        //
        //
        //forests.attachReplicaForest(replicaForest.getName(), primaryForest.getName());
        //sleep(30);
        //
        //
        //forests.enableForest(replicaForest.getName(), false);
        //sleep(30);
        //
        //sforests.enableForest(replicaForest.getName(), true);

        //forests.enableForest(primaryForest.getName(),true);
    }

    private void sleep(long seconds) {
        try {
            Thread.sleep(seconds*1000);
        } catch (InterruptedException e) {
            throw Exceptions.propagate(e);
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
                .application(EntitySpecs.appSpec(MarkLogicTestApplication.class).displayName("Brooklyn MarkLogic Application"))
                .webconsolePort(port)
                .location(location)
                .start();

        Entities.dumpInfo(launcher.getApplications());
    }
}
