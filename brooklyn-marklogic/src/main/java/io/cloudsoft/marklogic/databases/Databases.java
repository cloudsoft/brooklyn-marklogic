package io.cloudsoft.marklogic.databases;

import brooklyn.entity.basic.AbstractGroup;
import brooklyn.entity.basic.Description;
import brooklyn.entity.basic.MethodEffector;
import brooklyn.entity.basic.NamedParameter;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;
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
    void createDatabase(
            @NamedParameter("name") @Description("The name of the database") String name);

}
