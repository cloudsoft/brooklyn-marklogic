package io.cloudsoft.marklogic;

import brooklyn.entity.basic.AbstractEntity;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Collection;

public class AppServerImpl extends AbstractEntity implements AppServer {

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

    public int getPort() {
        return getConfig(PORT);
    }
}
