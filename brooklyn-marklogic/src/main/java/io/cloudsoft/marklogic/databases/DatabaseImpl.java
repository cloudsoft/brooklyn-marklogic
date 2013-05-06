package io.cloudsoft.marklogic.databases;

import brooklyn.entity.basic.AbstractEntity;

public class DatabaseImpl extends AbstractEntity implements Database{

    @Override
    public String getName() {
        return getConfig(NAME);
    }

    @Override
    public String getDisplayName(){
        return getName();
    }
}
