package io.cloudsoft.marklogic.clusters;

import brooklyn.entity.basic.AbstractEntity;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.location.Location;
import io.cloudsoft.marklogic.appservers.AppServices;
import io.cloudsoft.marklogic.databases.Databases;
import io.cloudsoft.marklogic.forests.Forests;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.NodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static brooklyn.entity.proxying.EntitySpecs.spec;

public class MarkLogicClusterImpl extends AbstractEntity implements MarkLogicCluster {

    private static final Logger LOG = LoggerFactory.getLogger(MarkLogicClusterImpl.class);

    private MarkLogicGroup eNodeGroup;
    private MarkLogicGroup dNodeGroup;
    private Databases databases;
    private AppServices appservices;
    private Forests forests;

    @Override
    public void init() {
        //we give it a bit longer timeout for starting up
        setConfig(ConfigKeys.START_TIMEOUT, 240);

        eNodeGroup = addChild(spec(MarkLogicGroup.class)
                .displayName("E-Nodes")
                .configure(MarkLogicGroup.INITIAL_SIZE, getConfig(INITIAL_E_NODES_SIZE))
                .configure(MarkLogicGroup.NODE_TYPE, NodeType.E_NODE)
                .configure(MarkLogicGroup.GROUP_NAME, "E-Nodes")
        );

        dNodeGroup = addChild(spec(MarkLogicGroup.class)
                .displayName("D-Nodes")
                .configure(MarkLogicGroup.INITIAL_SIZE, getConfig(INITIAL_D_NODES_SIZE))
                .configure(MarkLogicGroup.PRIMARY_STARTUP_GROUP, eNodeGroup)
                .configure(MarkLogicGroup.NODE_TYPE, NodeType.D_NODE)
                .configure(MarkLogicGroup.GROUP_NAME, "D-Nodes")
        );

        databases = addChild(spec(Databases.class)
                .displayName("Databases")
                .configure(Databases.GROUP, dNodeGroup)
        );

        forests = addChild(spec(Forests.class)
                .displayName("Forests")
                .configure(Forests.GROUP, dNodeGroup)
        );

        appservices = addChild(spec(AppServices.class)
                .displayName("AppServices")
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

    @Override
    public Forests getForests() {
        return forests;
    }

    @Override
    public AppServices getAppservices() {
        return appservices;
    }

    @Override
    public Databases getDatabases() {
        return databases;
    }

    @Override
    public MarkLogicGroup getDNodeGroup() {
        return dNodeGroup;
    }

    @Override
    public MarkLogicGroup getENodeGroup() {
        return eNodeGroup;
    }
}