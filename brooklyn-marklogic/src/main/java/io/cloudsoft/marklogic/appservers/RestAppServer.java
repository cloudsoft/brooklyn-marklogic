package io.cloudsoft.marklogic.appservers;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;

@ImplementedBy(RestAppServerImpl.class)
public interface RestAppServer extends Entity {

    @SetFromFlag("name")
    ConfigKey<String> NAME = new BasicConfigKey<String>(
            String.class, "marklogic.appserver.name",
            "The name of the AppServer", null);

    @SetFromFlag("databasename")
    ConfigKey<String> DATABASE_NAME = new BasicConfigKey<String>(
            String.class, "marklogic.appserver.databasename",
            "The name of the database", null);

    //todo: should be a port type
    @SetFromFlag("port")
    ConfigKey<String> PORT = new BasicConfigKey<String>(
            String.class, "marklogic.appserver.port",
            "The port this application can be connected to", null);



    String getName();

    String getDatabaseName();

    String getPort();
}
