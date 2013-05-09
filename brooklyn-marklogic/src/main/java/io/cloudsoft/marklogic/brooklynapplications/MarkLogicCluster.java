package io.cloudsoft.marklogic.brooklynapplications;

import brooklyn.entity.Entity;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.Startable;
import io.cloudsoft.marklogic.appservers.AppServices;
import io.cloudsoft.marklogic.databases.Databases;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;

@ImplementedBy(MarkLogicClusterImpl.class)
public interface MarkLogicCluster extends Entity , Startable {

    AppServices getAppservices();

    Databases getDatabases();

    MarkLogicGroup getDNodeGroup();

    MarkLogicGroup getENodeGroup();
}

