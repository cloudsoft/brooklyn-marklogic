package io.cloudsoft.marklogic.nodes;

import brooklyn.entity.basic.SoftwareProcessDriver;
import io.cloudsoft.marklogic.forests.Forest;

import java.util.Set;

public interface MarkLogicNodeDriver extends SoftwareProcessDriver {

    void createForest(Forest forest);

    void createDatabaseWithForest(String name);

    void createDatabase(String name);

    void createGroup(String name);

    void createAppServer(String name, String database, String groupName,String port);

    void assignHostToGroup(String hostAddress, String groupName);

    Set<String> scanDatabases();

    Set<String> scanAppServices();

    Set<String> scanForests();
}
