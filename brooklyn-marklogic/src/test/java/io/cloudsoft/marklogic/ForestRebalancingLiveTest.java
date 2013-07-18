package io.cloudsoft.marklogic;

import io.cloudsoft.marklogic.databases.Database;
import io.cloudsoft.marklogic.forests.Forest;
import org.testng.annotations.Test;

import static com.mongodb.util.MyAsserts.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

public class ForestRebalancingLiveTest extends AbstractMarklogicFullClusterLiveTest {

    @Test
    public void testMoveAllForestsFromNode_noReplica() throws Exception {
        LOG.info("-----------------testMoveAllForestsFromNode_noReplica-----------------");

        Database database = createDatabase();

        Forest forest = createForest(dNode1);
        forest.awaitStatus("open");

        databases.attachForestToDatabase(forest.getName(), database.getName());
        forest.awaitStatus("open");

        forests.moveAllForestFromNode(dNode1.getHostName());

        String hostName = forest.getHostname();
        assertTrue(hostName.equals(dNode2.getHostName())||hostName.equals(dNode3.getHostName()));
        assertEquals("open", forest.getStatus());

        //poor mans cleanup
        forest.setParent(null);
    }

    //@Test
    public void testMoveAllForestsFromNode_movingPrimary() throws Exception {
        LOG.info("-----------------testMoveAllForestsFromNode_movingPrimary-----------------");

        Database database = createDatabase();

        Forest primaryForest = createForest(dNode1);
        primaryForest.awaitStatus("open");

        Forest replicaForest = createForest(dNode2, primaryForest.getName());

        databases.attachForestToDatabase(primaryForest.getName(), database.getName());
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("sync replicating");

        forests.moveAllForestFromNode(dNode1.getHostName());

        assertEquals("open", primaryForest.getStatus());
        assertEquals(dNode3.getHostName(), primaryForest.getHostname());
        assertEquals("sync replicating", replicaForest.getStatus());
        assertEquals(dNode2.getHostName(), replicaForest.getHostname());

        //poor mans cleanup
        primaryForest.setParent(null);
        replicaForest.setParent(null);
    }

    //@Test
    public void testMoveAllForestsFromNode_movingReplica() throws Exception {
        LOG.info("-----------------testMoveAllForestsFromNode_movingReplica-----------------");

        Database database = createDatabase();

        Forest primaryForest = createForest(dNode1);
        primaryForest.awaitStatus("open");

        Forest replicaForest = createForest(dNode2, primaryForest.getName());

        databases.attachForestToDatabase(primaryForest.getName(), database.getName());
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("sync replicating");

        forests.moveAllForestFromNode(dNode2.getHostName());

        assertEquals("open", primaryForest.getStatus());
        assertEquals(dNode1.getHostName(), primaryForest.getHostname());
        assertEquals("sync replicating", replicaForest.getStatus());
        assertEquals(dNode3.getHostName(), replicaForest.getHostname());

        //poor mans cleanup
        primaryForest.setParent(null);
        replicaForest.setParent(null);
    }
}
