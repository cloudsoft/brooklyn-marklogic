package io.cloudsoft.marklogic.forests;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.annotation.Effector;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.Startable;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import io.cloudsoft.marklogic.nodes.MarkLogicNodeImpl;

import java.util.List;

import static brooklyn.entity.basic.ConfigKeys.newConfigKey;

@ImplementedBy(ForestsImpl.class)
public interface Forests extends Entity, Startable {

    ConfigKey<MarkLogicGroup> GROUP = newConfigKey(
            MarkLogicGroup.class, "marklogic.forests.group", "The group");

    Forest createForestWithSpec(BasicEntitySpec<Forest, ?> forestSpec);

    @Effector(description = "Creates a new forest")
    Forest createForest(
            @EffectorParam(name = "name", description = "The name of the forest") String name,
            @EffectorParam(name = "hostname", description = "The name of the forest") String hostname,
            @EffectorParam(name = "dataDir", description = "Specifies a public directory in which the forest is located.") String dataDir,
            @EffectorParam(name = "large-data-dir", description = "Specifies a directory in which large objects are stored. If the directory is not specified, large objects will be stored under the data directory") String largeDataDir,
            @EffectorParam(name = "fast-data-dir", description = "Specifies a directory that is smaller but faster than the data directory. The directory should be on a different storage device than the data directory.") String fastDataDir,
            @EffectorParam(name = "updates_allowed", description = "Specifies which operations are allowed on this forest. Options are: all, delete-only, read-only, flash-backup.") String updatesAllowed,
            @EffectorParam(name = "rebalancer_enabled", description = "Enable automatic rebalancing after configuration changes.") boolean rebalancerEnabled,
            @EffectorParam(name = "failover_enabled", description = "Enable assignment to a failover host if the primary host is down.") boolean failoverEnabled);

    void attachReplicaForest(String primaryForestName, String replicaForestName);

    @Effector(description = "Enables a forest")
    void enableForest(
            @EffectorParam(name = "forestName", description = "The name of the forest") String forestName,
            @EffectorParam(name = "enabled", description = "If the forest should be enabled") boolean enabled);

    void setForestHost(String forestName, String hostname);

    void unmountForest(String forestName);

    void mountForest(String forestName);

    @Effector(description = "Moves a forest from one host to another")
    void moveForest(
            @EffectorParam(name = "forest", description = "The name of the forest") String forestName,
            @EffectorParam(name = "host", description = "The new hostname") String hostName);

    List<Forest> asList();

    void rebalance();

    @Effector(description = "Move all forests from this node (this is useful for removing a node within the cluster)")
    void moveAllForestFromNode(
            @EffectorParam(name = "hostName", description = "The name of the host node which should loose all its forests.") String hostName);
}
