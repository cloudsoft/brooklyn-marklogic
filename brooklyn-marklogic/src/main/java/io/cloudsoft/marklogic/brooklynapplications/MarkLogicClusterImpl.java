package io.cloudsoft.marklogic.brooklynapplications;

import brooklyn.entity.basic.AbstractEntity;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.location.Location;
import io.cloudsoft.marklogic.appservers.AppServices;
import io.cloudsoft.marklogic.databases.Databases;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.NodeType;

import java.util.Collection;

import static brooklyn.entity.proxying.EntitySpecs.spec;

public class MarkLogicClusterImpl extends AbstractEntity implements MarkLogicCluster {

    private MarkLogicGroup eNodeGroup;
    private MarkLogicGroup dNodeGroup;
    private Databases databases;
    private AppServices appservices;

    @Override
    public void init() {
        //we give it a bit longer timeout for starting up
        setConfig(ConfigKeys.START_TIMEOUT, 240);

        eNodeGroup = addChild(spec(MarkLogicGroup.class)
                .configure(MarkLogicGroup.INITIAL_SIZE, getConfig(INITIAL_E_NODES_SIZE))
                .configure(MarkLogicGroup.NODE_TYPE, NodeType.E_NODE)
                .configure(MarkLogicGroup.GROUP_NAME, "E-Nodes")
        );

        dNodeGroup = addChild(spec(MarkLogicGroup.class)
                .configure(MarkLogicGroup.INITIAL_SIZE, getConfig(INITIAL_D_NODES_SIZE))
                .configure(MarkLogicGroup.PRIMARY_STARTUP_GROUP, eNodeGroup)
                .configure(MarkLogicGroup.NODE_TYPE, NodeType.D_NODE)
                .configure(MarkLogicGroup.GROUP_NAME, "D-Nodes")
        );

        databases = addChild(spec(Databases.class)
                .configure(Databases.GROUP, dNodeGroup)
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