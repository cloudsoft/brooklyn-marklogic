package io.cloudsoft.marklogic.clusters;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.proxy.AbstractController;
import brooklyn.entity.proxy.LoadBalancer;
import brooklyn.entity.proxy.nginx.NginxController;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.Startable;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;
import io.cloudsoft.marklogic.appservers.AppServices;
import io.cloudsoft.marklogic.databases.Databases;
import io.cloudsoft.marklogic.forests.Forests;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;

@ImplementedBy(MarkLogicClusterImpl.class)
public interface MarkLogicCluster extends Entity, Startable {

    ConfigKey<Integer> INITIAL_D_NODES_SIZE = new BasicConfigKey<Integer>(
            Integer.class, "marklogic.cluster.d-nodes.initial", "The initial number of d-nodes.", 1);

    ConfigKey<Integer> INITIAL_E_NODES_SIZE = new BasicConfigKey<Integer>(
            Integer.class, "marklogic.cluster.e-nodes.initial", "The initial number of e-nodes.", 1);

    //the cluster doesn't need to be set on this loadbalancer spec, that will be done by the MarkLogicCluster
    @SetFromFlag("loadBalancerSpec")
    BasicAttributeSensorAndConfigKey<EntitySpec<? extends AbstractController>> LOAD_BALANCER_SPEC = new BasicAttributeSensorAndConfigKey(
            EntitySpec.class, "marklogic.cluster.loadbalancer.spec", "Spec for nginx in front of marklogic", null);

    AppServices getAppservices();

    Databases getDatabases();

    MarkLogicGroup getDNodeGroup();

    MarkLogicGroup getENodeGroup();

    Forests getForests();

    AbstractController getLoadBalancer();

    boolean claimToBecomeInitialHost();

    MarkLogicNode getAnyNodeOrWait();
}

