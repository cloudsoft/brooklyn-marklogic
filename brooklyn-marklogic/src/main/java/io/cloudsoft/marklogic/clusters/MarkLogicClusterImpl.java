package io.cloudsoft.marklogic.clusters;

import static brooklyn.util.JavaGroovyEquivalents.elvis;
import static brooklyn.entity.proxying.EntitySpecs.spec;
import io.cloudsoft.marklogic.appservers.AppServices;
import io.cloudsoft.marklogic.databases.Databases;
import io.cloudsoft.marklogic.forests.Forests;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import io.cloudsoft.marklogic.nodes.NodeType;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.enricher.basic.SensorPropagatingEnricher;
import brooklyn.entity.basic.AbstractEntity;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxy.nginx.NginxController;
import brooklyn.entity.trait.Startable;
import brooklyn.location.Location;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class MarkLogicClusterImpl extends AbstractEntity implements MarkLogicCluster {

    private static final Logger LOG = LoggerFactory.getLogger(MarkLogicClusterImpl.class);

    private MarkLogicGroup eNodeGroup;
    private MarkLogicGroup dNodeGroup;
    private Databases databases;
    private AppServices appservices;
    private Forests forests;
    private NginxController loadBalancer;

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

        loadBalancer = addChild(spec(NginxController.class)
                .displayName("LoadBalancer")
                .configure("cluster", getENodeGroup())
                .configure("port", 80)
                        //todo: temporary hack to feed the app port to nginx.
                .configure("portNumberSensor", MarkLogicNode.APP_SERVICE_PORT)
        );
    }

    @Override
    public NginxController getLoadBalancer() {
        return loadBalancer;
    }

    @Override
    public void restart() {
        eNodeGroup.restart();
        dNodeGroup.restart();
        loadBalancer.restart();
    }

    @Override
    public void start(Collection<? extends Location> locations) {
        Entities.invokeEffectorList(
        		this, 
        		ImmutableList.of(eNodeGroup, dNodeGroup, loadBalancer), 
        		Startable.START, 
        		ImmutableMap.of("locations", locations)).getUnchecked();

        connectSensors();
    }

    void connectSensors() {
        SensorPropagatingEnricher.newInstanceListeningTo(loadBalancer, NginxController.HOSTNAME, SERVICE_UP, NginxController.ROOT_URL)
            	.addToEntityAndEmitAll(this);
    }
    
    @Override
    public void stop() {
        Entities.invokeEffectorList(
        		this, 
        		ImmutableList.of(eNodeGroup, dNodeGroup, loadBalancer), 
        		Startable.STOP).getUnchecked();
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