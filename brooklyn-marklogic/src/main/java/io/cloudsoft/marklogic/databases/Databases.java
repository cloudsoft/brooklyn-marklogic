package io.cloudsoft.marklogic.databases;

import brooklyn.config.ConfigKey;
import brooklyn.entity.annotation.Effector;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.basic.AbstractGroup;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;

import static brooklyn.entity.basic.ConfigKeys.newConfigKey;

@ImplementedBy(DatabasesImpl.class)
public interface Databases extends AbstractGroup {

    ConfigKey<MarkLogicGroup> GROUP = newConfigKey(MarkLogicGroup.class, "marklogic.databases.group", "The group");

    @Effector(description = "Creates a new database and automatically creates forests and attaches them to the database.")
    void createDatabaseWithForest(
            @EffectorParam(name = "name", description = "The name of the database") String name);

    @Effector(description = "Creates a new database without forests attached to it.")
    Database createDatabase(
            @EffectorParam(name = "name", description = "The name of the database") String name);

    Database createDatabaseWithSpec(BasicEntitySpec<Database, ?> databaseSpec);

    @Effector(description = "Attaches a forest to a database.")
    void attachForestToDatabase(
            @EffectorParam(name = "forestName", description = "The name of the forest") String forestName,
            @EffectorParam(name = "databaseName", description = "The name of the database") String databaseName);
}
