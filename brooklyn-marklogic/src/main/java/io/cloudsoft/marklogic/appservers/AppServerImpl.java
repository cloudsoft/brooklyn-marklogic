package io.cloudsoft.marklogic.appservers;

import brooklyn.entity.basic.AbstractEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppServerImpl extends AbstractEntity implements AppServer {

    private static final Logger LOG = LoggerFactory.getLogger(AppServerImpl.class);

    @Override
    public String getDisplayName() {
        return getName();
    }

    @Override
    public String getDatabaseName() {
        return getConfig(DATABASE_NAME);
    }

    @Override
    public String getName() {
        return getConfig(NAME);
    }

    @Override
    public String getGroupName() {
        return getConfig(GROUP_NAME);
    }

    @Override
    public AppServerKind getKind() {
        return getConfig(KIND);
    }

    @Override
    public Integer getPort() {
        return getConfig(PORT);
    }
}
