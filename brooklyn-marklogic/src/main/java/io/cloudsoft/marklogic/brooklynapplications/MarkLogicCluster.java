package io.cloudsoft.marklogic.brooklynapplications;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.Startable;
import brooklyn.event.basic.BasicConfigKey;
import io.cloudsoft.marklogic.appservers.AppServices;
import io.cloudsoft.marklogic.databases.Databases;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;

@ImplementedBy(MarkLogicClusterImpl.class)
public interface MarkLogicCluster extends Entity , Startable {

    ConfigKey<Integer> INITIAL_D_NODES_SIZE = new BasicConfigKey<Integer>(
            Integer.class, "marklogic.cluster.d-nodes.initial", "The initial number of d-nodes.", 1);

    ConfigKey<Integer> INITIAL_E_NODES_SIZE = new BasicConfigKey<Integer>(
            Integer.class, "marklogic.cluster.e-nodes.initial", "The initial number of e-nodes.", 1);

    AppServices getAppservices();

    Databases getDatabases();

    MarkLogicGroup getDNodeGroup();

    MarkLogicGroup getENodeGroup();
}

