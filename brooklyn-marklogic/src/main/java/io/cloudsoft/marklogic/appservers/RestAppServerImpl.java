package io.cloudsoft.marklogic.appservers;

import brooklyn.entity.basic.AbstractEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestAppServerImpl extends AbstractEntity implements RestAppServer {

    private static final Logger LOG = LoggerFactory.getLogger(RestAppServer.class);


    //@Override
    //protected Collection<Integer> getRequiredOpenPorts() {
    //    // TODO What ports need to be open?
    //    // I got these from `sudo netstat -antp` for the MarkLogic daemon
    //    // TODO If want to use a pre-existing security group instead, can add to
    //    //      obtainProvisioningFlags() something like:
    //    //      .put("securityGroups", groupName)
    //    //TODO: the 8011 port has been added so we can register an application on that port. In the future this needs to come
    //    //from the application, but for the time being it is hard coded.
    //
    //    return ImmutableSet.copyOf(ImmutableList.of(getPort()));
    //}


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
    public Integer getPort() {
        return getConfig(PORT);
    }
}
