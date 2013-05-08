package io.cloudsoft.marklogic.brooklynapplications;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractEntity;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.NamedParameter;
import brooklyn.entity.trait.Startable;
import brooklyn.entity.webapp.jboss.JBoss7Server;
import brooklyn.location.Location;
import io.cloudsoft.marklogic.appservers.AppServices;
import io.cloudsoft.marklogic.databases.Databases;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import io.cloudsoft.marklogic.nodes.NodeType;

import java.util.Collection;

import static brooklyn.entity.proxying.EntitySpecs.spec;

public class MarkLogicClusterImpl extends AbstractEntity implements MarkLogicCluster, Startable {

    private MarkLogicGroup eNodeGroup;
    private MarkLogicGroup dNodeGroup;
    private Databases databases;
    private AppServices appservices;


    @Override
    public void init() {
        //we give it a bit longer timeout for starting up
        setConfig(ConfigKeys.START_TIMEOUT, 240);

        //todo: we need to split up in d-node group size and e-node group size.

        String initialClusterSizeValue = getManagementContext().getConfig().getFirst("brooklyn.marklogicCluster.initial-cluster-size");
        int initialClusterSize = 2;
        if (initialClusterSizeValue != null && !initialClusterSizeValue.isEmpty()) {
            initialClusterSize = Integer.parseInt(initialClusterSizeValue);
        }

        eNodeGroup = addChild(spec(MarkLogicGroup.class)
                .configure(MarkLogicGroup.INITIAL_SIZE, 1)
                .configure(MarkLogicGroup.NODE_TYPE, NodeType.E_NODE)
                .configure(MarkLogicGroup.GROUP_NAME, "ENodes")
        );

        dNodeGroup = addChild(spec(MarkLogicGroup.class)
                .configure(MarkLogicGroup.INITIAL_SIZE, 1)
                .configure(MarkLogicGroup.PRIMARY_STARTUP_GROUP, eNodeGroup)
                .configure(MarkLogicGroup.NODE_TYPE, NodeType.D_NODE)
                .configure(MarkLogicGroup.GROUP_NAME, "DNodes")
        );

        databases = addChild(spec(Databases.class)
                .configure(Databases.GROUP, eNodeGroup)
        );

        appservices = addChild(spec(AppServices.class)
                .configure(AppServices.CLUSTER, eNodeGroup)
        );
    }

    @Override
    public void restart() {
        eNodeGroup.restart();
        dNodeGroup.restart();
    }

    @Override
    public void start(Collection<? extends Location> locations) {
        eNodeGroup.start(locations);
        dNodeGroup.start(locations);
    }

    @Override
    public void stop() {
        eNodeGroup.stop();
        dNodeGroup.stop();
    }


    public AppServices getAppservices() {
        return appservices;
    }

    public Databases getDatabases() {
        return databases;
    }

    public MarkLogicGroup getDNodeGroup() {
        return dNodeGroup;
    }

    public MarkLogicGroup getENodeGroup() {
        return eNodeGroup;
    }

    @Override
    public void postStart(Collection<? extends Location> locations) {
        LOG.info("=========================== MarkLogicDemoApp: Starting postStart =========================== ");


        printInfo();

        //getDatabases().createDatabase(databaseName);
        //getAppservices().createRestAppServer(appServiceName, databaseName, "Default", "" + appServicePort);

        LOG.info("=========================== MarkLogicDemoApp: Finished postStart =========================== ");
    }

    private void printInfo() {
        MarkLogicNode masterNode = getENodeGroup().getAttribute(MarkLogicGroup.MASTER_NODE);
        String masterHost = masterNode.getAttribute(Attributes.HOSTNAME);


        LOG.info("MarkLogic master server is available at 'http://" + masterHost + ":8000'");
        LOG.info("MarkLogic Cluster summary is available at 'http://" + masterHost + ":8001'");
        LOG.info("E-Nodes");
        int k = 1;
        for (Entity entity : getENodeGroup().getMembers()) {
            LOG.info("   " + k + " MarkLogic node http://" + entity.getAttribute(MarkLogicNode.HOSTNAME) + ":8000");
            k++;
        }


        LOG.info("D-Nodes");
        k = 1;
        for (Entity entity : getDNodeGroup().getMembers()) {
            LOG.info("   " + k + " MarkLogic node http://" + entity.getAttribute(MarkLogicNode.HOSTNAME) + ":8000");
            k++;
        }
        LOG.info("MarkLogic Monitoring Dashboard is available at 'http://" + masterHost + ":8002/dashboard'");
    }

}