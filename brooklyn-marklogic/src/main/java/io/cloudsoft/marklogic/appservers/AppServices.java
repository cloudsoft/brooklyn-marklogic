package io.cloudsoft.marklogic.appservers;

import brooklyn.config.ConfigKey;
import brooklyn.entity.annotation.Effector;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.basic.AbstractGroup;
import brooklyn.entity.proxying.ImplementedBy;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;

import static brooklyn.entity.basic.ConfigKeys.newConfigKey;

@ImplementedBy(AppServicesImpl.class)
public interface AppServices extends AbstractGroup {

    ConfigKey<MarkLogicGroup> CLUSTER = newConfigKey(
            MarkLogicGroup.class,
            "marklogic.appservices.cluster",
            "The cluster");

    @Effector(description = "Creates a new Rest AppServer")
    RestAppServer createRestAppServer(
            @EffectorParam(name = "name", description = "The name of the appServer") String name,
            @EffectorParam(name = "database", description = "The name of the database") String database,
            @EffectorParam(name = "group", description = "The name of the group this appServer belongs to") String group,
            @EffectorParam(name = "port", description = "The port of the appServer") int port);

}
