package io.cloudsoft.marklogic.databases;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;

import static brooklyn.entity.basic.ConfigKeys.newStringConfigKey;

@ImplementedBy(DatabaseImpl.class)
public interface Database extends Entity {

    ConfigKey<String> NAME = newStringConfigKey(
            "marklogic.database.name",
            "The name of the database");

    ConfigKey<String> JOURNALING = newStringConfigKey(
            "marklogic.database.journaling",
            "Specifies how robust transaction journaling should be (strict, fast, off)",
            "fast");

    String getName();

    String getJournaling();
}
