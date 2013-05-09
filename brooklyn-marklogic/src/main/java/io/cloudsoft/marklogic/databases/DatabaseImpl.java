package io.cloudsoft.marklogic.databases;

import brooklyn.entity.basic.AbstractEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseImpl extends AbstractEntity implements Database {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseImpl.class);

    @Override
    public String getName() {
        return getConfig(NAME);
    }

    @Override
    public String getDisplayName() {
        return getName();
    }
}
