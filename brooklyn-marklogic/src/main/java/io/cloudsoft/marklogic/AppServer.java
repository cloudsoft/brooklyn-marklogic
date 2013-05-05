package io.cloudsoft.marklogic;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;

@ImplementedBy(AppServerImpl.class)
public interface AppServer extends Entity {

    @SetFromFlag("name")
    ConfigKey<String> NAME = new BasicConfigKey<String>(
            String.class, "marklogic.appserver.name",
            "The name of the AppServer", null);

    @SetFromFlag("database")
    ConfigKey<String> DATABASE = new BasicConfigKey<String>(
            String.class, "marklogic.appserver.database",
            "The name of the database", null);

    @SetFromFlag("port")
    ConfigKey<Integer> PORT = new BasicConfigKey<Integer>(
            Integer.class, "marklogic.appserver.port",
            "The port this application can be connected to", null);
}
