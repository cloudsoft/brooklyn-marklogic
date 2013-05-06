package io.cloudsoft.marklogic.databases;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;

@ImplementedBy(DatabaseImpl.class)
public interface Database extends Entity {

    @SetFromFlag("name")
    ConfigKey<String> NAME = new BasicConfigKey<String>(
            String.class, "marklogic.database.name",
            "The name of the database", null);

    String getName();

}
