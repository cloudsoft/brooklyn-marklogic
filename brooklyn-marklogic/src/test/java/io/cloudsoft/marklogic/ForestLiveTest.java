package io.cloudsoft.marklogic;

import io.cloudsoft.marklogic.databases.Database;
import io.cloudsoft.marklogic.forests.Forest;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

public class ForestLiveTest extends AbstractMarklogicFullClusterLiveTest {

    @Test(groups = {"Live"})
    public void testCreateForest() throws Exception {
        LOG.info("-----------------testCreateForest-----------------");

        Forest forest = createForest(dNode1);
        forest.awaitStatus("open");

        assertEquals(dNode1.getHostName(), forest.getHostname());
    }

    @Test(groups = {"Live"})
    public void testDisableAndEnableForest() throws Exception {
        LOG.info("-----------------testDisableAndEnableForest-----------------");

        Forest forest = createForest(dNode1);
        forest.awaitStatus("open");

        forests.enableForest(forest.getName(), false);
        forest.awaitStatus("disabled");

        forests.enableForest(forest.getName(), true);
        forest.awaitStatus("open");
    }

    @Test(groups = {"Live"})
    public void testAttachForestToDatabase() throws Exception {
        LOG.info("-----------------testAttachForestToDatabase-----------------");

        Database database = createDatabase();

        Forest primaryForest = createForest(dNode1);
        primaryForest.awaitStatus("open");

        databases.attachForestToDatabase(primaryForest.getName(), database.getName());
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

        databases.attachForestToDatabase(primaryForest.getName(), database.getName());
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

        databases.attachForestToDatabase(forest.getName(), database.getName());
        forest.awaitStatus("open");

        forests.enableForest(forest.getName(), false);
        forest.awaitStatus("unmounted");

        forests.unmountForest(forest.getName());
        forests.mountForest(forest.getName());
        forests.enableForest(forest.getName(), true);
        forest.awaitStatus("open");

        assertEquals(dNode1.getHostName(), forest.getHostname());
    }
}
