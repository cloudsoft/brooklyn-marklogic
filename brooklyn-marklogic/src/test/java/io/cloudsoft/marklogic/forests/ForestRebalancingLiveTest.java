package io.cloudsoft.marklogic.forests;

import io.cloudsoft.marklogic.AbstractMarkLogicLiveTest;
import io.cloudsoft.marklogic.databases.Database;

import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

public class ForestRebalancingLiveTest extends AbstractMarkLogicLiveTest {

    @Test(groups = {"WIP"})
    public void testMoveAllForestsFromNode_noReplica() throws Exception {
        LOG.info("-----------------testMoveAllForestsFromNode_noReplica-----------------");

        Database database = createDatabase();

        Forest forest = createForest(dNode1);
        forest.awaitStatus("open");

        databases.attachForestToDatabase(forest, database);
        forest.awaitStatus("open");

        forests.moveAllForestsFromNode(dNode1);

        String hostName = forest.getHostname();
        assertTrue(hostName.equals(dNode2.getHostname())||hostName.equals(dNode3.getHostname()));
        assertEquals("open", forest.getStatus());

        //poor mans cleanup
        forest.setParent(null);
    }

    //@Test(groups = {"Live", "WIP"})
    public void testMoveAllForestsFromNode_movingPrimary() throws Exception {
        LOG.info("-----------------testMoveAllForestsFromNode_movingPrimary-----------------");

        Database database = createDatabase();

        Forest primaryForest = createForest(dNode1);
        primaryForest.awaitStatus("open");

        Forest replicaForest = createForest(dNode2, primaryForest.getName());

        databases.attachForestToDatabase(primaryForest, database);
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("sync replicating");

        forests.moveAllForestsFromNode(dNode1);

        assertEquals("open", primaryForest.getStatus());
        assertEquals(dNode3.getHostname(), primaryForest.getHostname());
        assertEquals("sync replicating", replicaForest.getStatus());
        assertEquals(dNode2.getHostname(), replicaForest.getHostname());

        //poor mans cleanup
        primaryForest.setParent(null);
        replicaForest.setParent(null);
    }

    //@Test(groups = {"Live", "WIP"})
    public void testMoveAllForestsFromNode_movingReplica() throws Exception {
        LOG.info("-----------------testMoveAllForestsFromNode_movingReplica-----------------");

        Database database = createDatabase();

        Forest primaryForest = createForest(dNode1);
        primaryForest.awaitStatus("open");

        Forest replicaForest = createForest(dNode2, primaryForest.getName());

        databases.attachForestToDatabase(primaryForest, database);
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("sync replicating");

        forests.moveAllForestsFromNode(dNode2);

        assertEquals("open", primaryForest.getStatus());
        assertEquals(dNode1.getHostname(), primaryForest.getHostname());
        assertEquals("sync replicating", replicaForest.getStatus());
        assertEquals(dNode3.getHostname(), replicaForest.getHostname());

        //poor mans cleanup
        primaryForest.setParent(null);
        replicaForest.setParent(null);
    }
}
