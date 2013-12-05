package io.cloudsoft.marklogic.forests;

import static brooklyn.entity.basic.ConfigKeys.newConfigKey;

import java.util.List;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.annotation.Effector;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.Startable;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;

@ImplementedBy(ForestsImpl.class)
public interface Forests extends Entity, Startable, Iterable<Forest> {

    ConfigKey<MarkLogicGroup> GROUP = newConfigKey(
            MarkLogicGroup.class, "marklogic.forests.group", "The group");

    Forest createForestWithSpec(EntitySpec<Forest> forestSpec);

    @Effector(description = "Creates a new forest")
    Forest createForest(
            @EffectorParam(name = "name", description = "The name of the forest") String name,
            @EffectorParam(name = "hostname", description = "The name of the forest") String hostname,
            @EffectorParam(name = "dataDir", description = "Specifies a public directory in which the forest is located.") String dataDir,
            @EffectorParam(name = "large-data-dir", description = "Specifies a directory in which large objects are stored. If the directory is not specified, large objects will be stored under the data directory") String largeDataDir,
            @EffectorParam(name = "fast-data-dir", description = "Specifies a directory that is smaller but faster than the data directory. The directory should be on a different storage device than the data directory.") String fastDataDir,
            @EffectorParam(name = "rebalancer_enabled", description = "Enable automatic rebalancing after configuration changes.") boolean rebalancerEnabled,
            @EffectorParam(name = "failover_enabled", description = "Enable assignment to a failover host if the primary host is down.") boolean failoverEnabled);

    void attachReplicaForest(Forest primary, Forest replica);
    void attachReplicaForest(String primaryForestName, String replicaForestName);

    @Effector(description = "Enables a forest")
    void enableForest(
            @EffectorParam(name = "forestName", description = "The name of the forest") String forestName);

    void enableForest(Forest forest);

    @Effector(description = "Disables a forest")
    void disableForest(
            @EffectorParam(name = "forestName", description = "The name of the forest") String forestName);

    void disableForest(Forest forest);

    void setForestHost(Forest forest, String hostname);
    void setForestHost(String forestName, String hostname);

    void unmountForest(Forest forest);
    void unmountForest(String forestName);

    void mountForest(Forest forest);
    void mountForest(String forestName);

    @Effector(description = "Moves a forest from one host to another")
    void moveForest(
            @EffectorParam(name = "forest", description = "The name of the forest") String forestName,
            @EffectorParam(name = "host", description = "The new hostname") String hostName);

    void moveForest(Forest forest, String hostName);

    void rebalance();

    @Effector(description = "Move all forests from this node (this is useful for removing a node within the cluster)")
    void moveAllForestsFromNode(
            @EffectorParam(name = "hostName", description = "The name of the host node which should lose all its forests.") String hostName);

    void moveAllForestsFromNode(MarkLogicNode node);
}
