package io.cloudsoft.marklogic.forests;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.Description;
import brooklyn.entity.basic.MethodEffector;
import brooklyn.entity.basic.NamedParameter;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;

@ImplementedBy(ForestsImpl.class)
public interface Forests extends Entity {

    @SetFromFlag("cluster")
    public static final BasicConfigKey<MarkLogicGroup> GROUP = new BasicConfigKey<MarkLogicGroup>(
            MarkLogicGroup.class, "marklogic.forests.group", "The group");


    MethodEffector<Void> CREATE_FOREST =
            new MethodEffector<Void>(Forests.class, "createForest");

    Forest createForestWithSpec(BasicEntitySpec<Forest, ?> forestSpec);

    @Description("Creates a new forest")
    Forest createForest(
            @NamedParameter("name") @Description("The name of the forest") String name,
            @NamedParameter("hostname") @Description("The name of the forest") String hostname,
            @NamedParameter("dataDir") @Description("Specifies a public directory in which the forest is located.") String dataDir,
            @NamedParameter("large-data-dir") @Description("Specifies a directory in which large objects are stored. If the directory is not specified, large objects will be stored under the data directory") String largeDataDir,
            @NamedParameter("fast-data-dir") @Description("Specifies a directory that is smaller but faster than the data directory. The directory should be on a different storage device than the data directory.") String fastDataDir,
            @NamedParameter("updates_allowed") @Description("Specifies which operations are allowed on this forest. Options are: all, delete-only, read-only, flash-backup.") String updatesAllowed,
            @NamedParameter("rebalancer_enabled") @Description("Enable automatic rebalancing after configuration changes.") boolean rebalancerEnabled,
            @NamedParameter("failover_enabled") @Description("Enable assignment to a failover host if the primary host is down.") boolean failoverEnabled);

    void attachReplicaForest(String databaseName, String primaryForestName, String replicaForestName);
}
