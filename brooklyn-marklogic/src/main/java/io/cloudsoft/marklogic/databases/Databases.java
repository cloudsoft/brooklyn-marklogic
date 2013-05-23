package io.cloudsoft.marklogic.databases;

import brooklyn.entity.basic.AbstractGroup;
import brooklyn.entity.basic.Description;
import brooklyn.entity.basic.MethodEffector;
import brooklyn.entity.basic.NamedParameter;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;
import io.cloudsoft.marklogic.forests.Forest;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;

@ImplementedBy(DatabasesImpl.class)
public interface Databases extends AbstractGroup {

    @SetFromFlag("cluster")
    public static final BasicConfigKey<MarkLogicGroup> GROUP = new BasicConfigKey<MarkLogicGroup>(
            MarkLogicGroup.class, "marklogic.databases.group", "The group");

    MethodEffector<Void> CREATE_DATABASE_WITH_FOREST =
            new MethodEffector<Void>(Databases.class, "createDatabaseWithForest");

    @Description("Creates a new database and automatically creates forests and attaches them to the database.")
    void createDatabaseWithForest(
            @NamedParameter("name") @Description("The name of the database") String name);

    MethodEffector<Void> CREATE_DATABASE =
            new MethodEffector<Void>(Databases.class, "createDatabase");

    @Description("Creates a new database without forests attached to it.")
    Database createDatabase(
            @NamedParameter("name") @Description("The name of the database") String name);

    MethodEffector<Void> ATTACH_FOREST_TO_DATABASE =
            new MethodEffector<Void>(Databases.class, "attachForestToDatabase");

    @Description("Attaches a forest to a database.")
    void attachForestToDatabase(@NamedParameter("forestName") @Description("The name of the forest") String forestName,
                                @NamedParameter("databaseName") @Description("The name of the database") String databaseName);

    Database createDatabaseWithSpec(BasicEntitySpec<Database, ?> databaseSpec);

}
