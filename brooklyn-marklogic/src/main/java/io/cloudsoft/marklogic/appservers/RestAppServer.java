package io.cloudsoft.marklogic.appservers;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.proxying.ImplementedBy;

import static brooklyn.entity.basic.ConfigKeys.newIntegerConfigKey;
import static brooklyn.entity.basic.ConfigKeys.newStringConfigKey;

@ImplementedBy(RestAppServerImpl.class)
public interface RestAppServer extends Entity {

    ConfigKey<String> NAME = newStringConfigKey(
            "marklogic.appserver.name",
            "The name of the AppServer");

    ConfigKey<String> DATABASE_NAME = newStringConfigKey(
            "marklogic.appserver.databasename",
            "The name of the database");

    ConfigKey<String> GROUP_NAME = newStringConfigKey(
            "marklogic.appserver.groupname",
            "The name of the group this RestAppServer belongs to");

    //todo: should be a port type
    ConfigKey<Integer> PORT = newIntegerConfigKey(
            "marklogic.appserver.port",
            "The port this application can be connected to");

    String getName();

    String getDatabaseName();

    Integer getPort();

    String getGroupName();
}
