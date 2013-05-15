package io.cloudsoft.marklogic.clusters;

import brooklyn.enricher.basic.SensorPropagatingEnricher;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractEntity;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxy.AbstractController;
import brooklyn.entity.proxy.nginx.NginxController;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.Startable;
import brooklyn.location.Location;
import com.google.common.collect.ImmutableMap;
import io.cloudsoft.marklogic.appservers.AppServices;
import io.cloudsoft.marklogic.databases.Databases;
import io.cloudsoft.marklogic.forests.Forests;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import io.cloudsoft.marklogic.nodes.NodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static brooklyn.entity.proxying.EntitySpecs.spec;
import static brooklyn.entity.proxying.EntitySpecs.wrapSpec;

public class MarkLogicClusterImpl extends AbstractEntity implements MarkLogicCluster {

    private static final Logger LOG = LoggerFactory.getLogger(MarkLogicClusterImpl.class);

    private MarkLogicGroup eNodeGroup;
    private MarkLogicGroup dNodeGroup;
    private Databases databases;
    private AppServices appservices;
    private Forests forests;
    private AbstractController loadBalancer;

    @Override
    public void init() {
        //we give it a bit longer timeout for starting up
        setConfig(ConfigKeys.START_TIMEOUT, 240);

        eNodeGroup = addChild(spec(MarkLogicGroup.class)
                .displayName("E-Nodes")
                .configure(MarkLogicGroup.INITIAL_SIZE, getConfig(INITIAL_E_NODES_SIZE))
                .configure(MarkLogicGroup.NODE_TYPE, NodeType.E_NODE)
                .configure(MarkLogicGroup.GROUP_NAME, "E-Nodes")
                .configure(MarkLogicGroup.CLUSTER, this)
        );

        dNodeGroup = addChild(spec(MarkLogicGroup.class)
                .displayName("D-Nodes")
                .configure(MarkLogicGroup.INITIAL_SIZE, getConfig(INITIAL_D_NODES_SIZE))
                .configure(MarkLogicGroup.CLUSTER, this)
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

        EntitySpec<? extends AbstractController> loadBalancerSpec = getConfig(LOAD_BALANCER_SPEC);
        if (loadBalancerSpec != null) {
            //the cluster needs to be set.
            loadBalancerSpec = wrapSpec(loadBalancerSpec).configure("cluster",getENodeGroup());
            loadBalancer = addChild(loadBalancerSpec);
        }
    }

    private final AtomicBoolean initialHostClaimed = new AtomicBoolean();

    @Override
    public boolean claimToBecomeInitialHost() {
        return initialHostClaimed.compareAndSet(false, true);
    }

    @Override
    public MarkLogicNode getAnyNodeOrWait() {
        for (; ; ) {

            MarkLogicNode node = dNodeGroup.getAnyStartedMember();
            if (node != null) return node;

            node = eNodeGroup.getAnyStartedMember();
            if (node != null) return node;

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public AbstractController getLoadBalancer() {
        return loadBalancer;
    }

    @Override
    public void restart() {
        Entities.invokeEffectorList(
                this,
                getStartableChildren(),
                Startable.RESTART).getUnchecked();
    }

    protected List<? extends Entity> getStartableChildren() {
        LinkedList result = new LinkedList();
        for (Entity entity : getChildren()) {
            if (entity instanceof Startable) {
                result.add(entity);
            }
        }
        return result;
    }

    @Override
    public void start(Collection<? extends Location> locations) {
        Entities.invokeEffectorList(
                this,
                getStartableChildren(),
                Startable.START,
                ImmutableMap.of("locations", locations)).getUnchecked();

        connectSensors();
    }

    void connectSensors() {
        if (loadBalancer != null) {
            SensorPropagatingEnricher.newInstanceListeningTo(loadBalancer, NginxController.HOSTNAME, SERVICE_UP, NginxController.ROOT_URL)
                    .addToEntityAndEmitAll(this);
        }
    }

    @Override
    public void stop() {
        Entities.invokeEffectorList(
                this,
                getStartableChildren(),
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