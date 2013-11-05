package io.cloudsoft.marklogic.forests;

import io.cloudsoft.marklogic.AbstractMarkLogicLiveTest;
import io.cloudsoft.marklogic.databases.Database;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

public class ForestLiveTest extends AbstractMarkLogicLiveTest {

    @Test(groups = {"Live"})
    public void testCreateForest() throws Exception {
        LOG.info("-----------------testCreateForest-----------------");

        Forest forest = createForest(dNode1);
        forest.awaitStatus("open");

        assertEquals(dNode1.getHostName(), forest.getHostname());
    }

    @Test(groups = {"WIP"})
    public void testDisableAndEnableForest() throws Exception {
        LOG.info("-----------------testDisableAndEnableForest-----------------");

        Forest forest = createForest(dNode1);
        forest.awaitStatus("open");

        forests.disableForest(forest);
        forest.awaitStatus("disabled");

        forests.enableForest(forest);
        forest.awaitStatus("open");
    }

    @Test(groups = {"Live"})
    public void testAttachForestToDatabase() throws Exception {
        LOG.info("-----------------testAttachForestToDatabase-----------------");

        Database database = createDatabase();

        Forest primaryForest = createForest(dNode1);
        primaryForest.awaitStatus("open");

        databases.attachForestToDatabase(primaryForest, database);
        primaryForest.awaitStatus("open");
        assertEquals(dNode1.getHostName(), primaryForest.getHostname());
    }

    @Test(groups = {"Live"})
    public void testAttachForestWithReplicaToDatabase() throws Exception {
        LOG.info("-----------------testAttachForestWithReplicaToDatabase-----------------");

        Database database = createDatabase();

        Forest primaryForest = createForest(dNode1);
        primaryForest.awaitStatus("open");

        Forest replicaForest = createForest(dNode2, primaryForest.getName());

        databases.attachForestToDatabase(primaryForest, database);
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("sync replicating");

        assertEquals(dNode1.getHostName(), primaryForest.getHostname());
        assertEquals(dNode2.getHostName(), replicaForest.getHostname());
    }

    @Test(groups = {"Live"})
    public void testUnmountMountOfForestWithoutReplica() throws Exception {
        LOG.info("-----------------testUnmountMountOfForestWithoutReplica-----------------");

        Database database = createDatabase();

        Forest forest = createForest(dNode1);
        forest.awaitStatus("open");

        databases.attachForestToDatabase(forest, database);
        forest.awaitStatus("open");

        forests.disableForest(forest);
        forest.awaitStatus("unmounted");

        forests.unmountForest(forest);
        forests.mountForest(forest);
        forests.enableForest(forest);
        forest.awaitStatus("open");

        assertEquals(dNode1.getHostName(), forest.getHostname());
    }
}
