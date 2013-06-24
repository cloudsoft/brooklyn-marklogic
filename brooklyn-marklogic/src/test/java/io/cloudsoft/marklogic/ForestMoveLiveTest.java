package io.cloudsoft.marklogic;

import io.cloudsoft.marklogic.databases.Database;
import io.cloudsoft.marklogic.forests.Forest;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

public class ForestMoveLiveTest extends AbstractMarklogicFullClusterLiveTest {


    @Test
    public void testMoveForestWithReplica() throws Exception {
        LOG.info("-----------------testMoveForestWithReplica-----------------");

        Database database = createDatabase();

        Forest primaryForest = createForest(dNode1);
        primaryForest.awaitStatus("open");

        Forest replicaForest = createForest(dNode2);
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("open");

        forests.attachReplicaForest(primaryForest.getName(), replicaForest.getName());

        databases.attachForestToDatabase(primaryForest.getName(), database.getName());
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("sync replicating");

        forests.enableForest(primaryForest.getName(), false);
        primaryForest.awaitStatus("unmounted");
        replicaForest.awaitStatus("open");

        forests.unmountForest(primaryForest.getName());
        forests.setForestHost(primaryForest.getName(), dNode3.getHostName());
        forests.mountForest(primaryForest.getName());
        forests.enableForest(primaryForest.getName(), true);
        primaryForest.awaitStatus("sync replicating");
        replicaForest.awaitStatus("open");

        forests.enableForest(replicaForest.getName(), false);
        forests.enableForest(replicaForest.getName(), true);
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("sync replicating");
        assertEquals(dNode3.getHostName(), primaryForest.getHostname());
    }

    @Test
    public void testMoveForestWithoutReplica() throws Exception {
        LOG.info("-----------------testMoveForestWithoutReplica-----------------");

        Database database = createDatabase();

        Forest primaryForest = createForest(dNode1);
        primaryForest.awaitStatus("open");

        databases.attachForestToDatabase(primaryForest.getName(), database.getName());
        primaryForest.awaitStatus("open");

        forests.moveForest(primaryForest.getName(), dNode3.getHostName());

        assertEquals("open", primaryForest.getStatus());
        assertEquals(dNode3.getHostName(), primaryForest.getHostname());
    }

    @Test
    public void testMoveForestWithReplica2() throws Exception {
        LOG.info("-----------------testMoveForestWithReplica2-----------------");

        Database database = createDatabase();

        Forest primaryForest = createForest(dNode1);
        primaryForest.awaitStatus("open");

        Forest replicaForest = createForest(dNode2);
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("open");

        forests.attachReplicaForest(primaryForest.getName(), replicaForest.getName());

        databases.attachForestToDatabase(primaryForest.getName(), database.getName());
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("sync replicating");

        forests.moveForest(primaryForest.getName(), dNode3.getHostName());

        assertEquals("open", primaryForest.getStatus());
        assertEquals("sync replicating", replicaForest.getStatus());
        assertEquals(dNode3.getHostName(), primaryForest.getHostname());
    }


    @Test
    public void testMoveReplicaForest() throws Exception {
        LOG.info("-----------------testMoveReplicaForest-----------------");

        Database database = createDatabase();

        Forest primaryForest = createForest(dNode1);
        primaryForest.awaitStatus("open");

        Forest replicaForest = createForest(dNode2);
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("open");

        forests.attachReplicaForest(primaryForest.getName(), replicaForest.getName());

        databases.attachForestToDatabase(primaryForest.getName(), database.getName());
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("sync replicating");

        forests.moveForest(replicaForest.getName(), dNode3.getHostName());

        assertEquals("open", primaryForest.getStatus());
        assertEquals("sync replicating", replicaForest.getStatus());
        assertEquals(dNode3.getHostName(), replicaForest.getHostname());
    }

    @Test
    public void testMoveWithoutReplica() throws Exception {
        LOG.info("-----------------testMoveWithoutReplica-----------------");

        Database database = createDatabase();

        Forest forest = createForest(dNode1);
        forest.awaitStatus("open");

        databases.attachForestToDatabase(forest.getName(), database.getName());
        forest.awaitStatus("open");

        forests.enableForest(forest.getName(), false);
        forest.awaitStatus("unmounted");

        forests.unmountForest(forest.getName());
        forests.setForestHost(forest.getName(), dNode3.getHostName());
        forests.mountForest(forest.getName());
        forests.enableForest(forest.getName(), true);
        forest.awaitStatus("open");

        assertEquals(dNode3.getHostName(), forest.getHostname());
    }


}
