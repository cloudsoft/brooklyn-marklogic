package io.cloudsoft.marklogic.nodes;

import brooklyn.entity.basic.SoftwareProcessDriver;
import io.cloudsoft.marklogic.api.MarkLogicApi;
import io.cloudsoft.marklogic.appservers.AppServer;
import io.cloudsoft.marklogic.databases.Database;
import io.cloudsoft.marklogic.forests.Forest;

import java.util.Set;

public interface MarkLogicNodeDriver extends SoftwareProcessDriver {

    void createForest(Forest forest);

    void createDatabase(Database database);

    void createGroup(String name);

    void createAppServer(AppServer appServer);

    void assignHostToGroup(String hostAddress, String groupName);

    Set<String> scanDatabases();

    Set<String> scanAppServices();

    Set<String> scanForests();

    String getForestStatus(String forestName);

    void attachForestToDatabase(String forestName, String databaseName);

    void attachReplicaForest(Forest primaryForest, Forest replicaForest);

    void enableForest(String forestName);

    void disableForest(String forestName);

    void setForestHost(String forestName, String hostName);

    void unmountForest(Forest forest);

    void mountForest(Forest forest);

    void removeNodeFromCluster();

    MarkLogicApi getApi();
}
