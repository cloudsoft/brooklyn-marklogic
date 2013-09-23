package io.cloudsoft.marklogic.clusters;

import static brooklyn.entity.basic.ConfigKeys.newIntegerConfigKey;
import io.cloudsoft.marklogic.appservers.AppServices;
import io.cloudsoft.marklogic.databases.Databases;
import io.cloudsoft.marklogic.forests.Forests;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.Lifecycle;
import brooklyn.entity.proxy.AbstractController;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.Startable;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;

/**
 * A cluster is one or more instances of {@link MarkLogicNode}, all running the same version.
 * All members of the cluster must run on the same platform and run identical software.
 * <p/>
 * Notes on cluster admin:
 * <ul>
 *     <li>There are no master/slave relationships in a cluster.</li>
 *     <li>Hosts can be added to and removed from clusters without taking the cluster offline.</li>
 *     <li>Cluster administration can be performed from any member, excluding leaving and altering license keys.</li>
 * </ul>
 */
@ImplementedBy(MarkLogicClusterImpl.class)
public interface MarkLogicCluster extends Entity, Startable {

    ConfigKey<Integer> INITIAL_D_NODES_SIZE = newIntegerConfigKey("marklogic.cluster.d-nodes.initial", "The initial number of d-nodes.", 1);

    ConfigKey<Integer> INITIAL_E_NODES_SIZE = newIntegerConfigKey("marklogic.cluster.e-nodes.initial", "The initial number of e-nodes.", 1);

    AttributeSensor<Lifecycle> SERVICE_STATE = Attributes.SERVICE_STATE;

    //todo: simplify
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

