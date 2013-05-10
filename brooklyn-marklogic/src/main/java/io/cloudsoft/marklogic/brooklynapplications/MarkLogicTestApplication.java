package io.cloudsoft.marklogic.brooklynapplications;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpecs;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.location.Location;
import brooklyn.util.CommandLineUtil;
import com.google.common.collect.Lists;
import io.cloudsoft.marklogic.databases.Databases;
import io.cloudsoft.marklogic.forests.Forests;
import io.cloudsoft.marklogic.forests.UpdatesAllowed;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;

import java.util.Collection;
import java.util.List;

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

    MarkLogicGroup cluster;
    private Databases databases;
    private Forests forests;

    @Override
    public void init() {
        String initialClusterSizeValue = getManagementContext().getConfig().getFirst("brooklyn.marklogic.initial-cluster-size");
        int initialClusterSize = 2;
        if (initialClusterSizeValue != null && !initialClusterSizeValue.isEmpty()) {
            initialClusterSize = Integer.parseInt(initialClusterSizeValue);
        }

        cluster = addChild(EntitySpecs.spec(MarkLogicGroup.class)
                .configure(MarkLogicGroup.INITIAL_SIZE, 1));

        databases = addChild(EntitySpecs.spec(Databases.class)
                .configure(Databases.GROUP, cluster)
        );

        forests = addChild(EntitySpecs.spec(Forests.class)
                .configure(Forests.GROUP, cluster)
        );
    }

    @Override
    public void postStart(Collection<? extends Location> locations) {
        super.postStart(locations);

        MarkLogicNode node = (MarkLogicNode)cluster.getMembers().iterator().next();
        String hostname = node.getHostName();
        String forestName = "demoForest";
        //databases.createDatabase("peter");
        forests.createForest("peter",hostname,null, null, null, UpdatesAllowed.ALL.toString(), true, false);

        //MarkLogicNode node = ((MarkLogicNode) cluster.getMembers().iterator().next());
        //node.createGroup("E-Nodes");
        //node.createGroup("D-Nodes");
        //node.assignHostToGroup(node.getHostName(),"E-Nodes");


        LOG.info("MarkLogic Cluster Members:");
        int k = 1;
        for (Entity entity : cluster.getMembers()) {
            LOG.info("   " + k + " MarkLogic node http://" + entity.getAttribute(MarkLogicNode.HOSTNAME) + ":8001");
            k++;
        }


        LOG.info("MarkLogic server is available at 'http://" +
                cluster.getAttribute(MarkLogicGroup.MASTER_NODE).getAttribute(Attributes.HOSTNAME) + ":8000'");
        LOG.info("MarkLogic Cluster summary is available at 'http://" +
                cluster.getAttribute(MarkLogicGroup.MASTER_NODE).getAttribute(Attributes.HOSTNAME) +
                ":8001'");
        LOG.info("MarkLogic Monitoring Dashboard is available at 'http://" +
                cluster.getAttribute(MarkLogicGroup.MASTER_NODE).getAttribute(Attributes.HOSTNAME) +
                ":8002/dashboard'");
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
