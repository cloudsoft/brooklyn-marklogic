package io.cloudsoft.marklogic.appservers;

import brooklyn.config.ConfigKey;
import brooklyn.entity.annotation.Effector;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.basic.AbstractGroup;
import brooklyn.entity.proxying.ImplementedBy;
import io.cloudsoft.marklogic.databases.Database;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;

import static brooklyn.entity.basic.ConfigKeys.newConfigKey;

@ImplementedBy(AppServicesImpl.class)
public interface AppServices extends AbstractGroup {

    ConfigKey<MarkLogicGroup> CLUSTER = newConfigKey(
            MarkLogicGroup.class,
            "marklogic.appservices.cluster",
            "The cluster");

    AppServer createAppServer(AppServerKind kind, String name, Database database, String group, int port);

    @Effector(description = "Creates a new AppServer")
    AppServer createAppServer(
            @EffectorParam(name = "kind", description = "The kind of application server, e.g. HTTP, XDBC") String kind,
            @EffectorParam(name = "name", description = "The name of the application server") String name,
            @EffectorParam(name = "database", description = "The database the application server should connect to") String database,
            @EffectorParam(name = "group", description = "The name of the group this application server should belong to") String group,
            @EffectorParam(name = "port", description = "The port the application server should listen on") int port);

}
