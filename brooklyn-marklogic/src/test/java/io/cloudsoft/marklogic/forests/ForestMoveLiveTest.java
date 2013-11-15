package io.cloudsoft.marklogic.forests;

import io.cloudsoft.marklogic.AbstractMarkLogicLiveTest;
import io.cloudsoft.marklogic.databases.Database;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

public class ForestMoveLiveTest extends AbstractMarkLogicLiveTest {

    // TODO: Delete?
    //  @Test(groups = {"Live"})
    public void testMoveForestWithReplica() throws Exception {
        try {

            LOG.info("-----------------testMoveForestWithReplica-----------------");

            Database database = createDatabase();

            Forest primaryForest = createForest(dNode1);
            primaryForest.awaitStatus("open");

            LOG.info("1-------------------------------------------");

            Forest replicaForest = createForest(dNode2);
            primaryForest.awaitStatus("open");
            replicaForest.awaitStatus("open");

            LOG.info("2-------------------------------------------");


            forests.attachReplicaForest(primaryForest, replicaForest);
            LOG.info("3-------------------------------------------");

            databases.attachForestToDatabase(primaryForest, database);
            LOG.info("4-------------------------------------------");

            primaryForest.awaitStatus("open");
            replicaForest.awaitStatus("sync replicating");

            LOG.info("5-------------------------------------------");


            forests.disableForest(primaryForest);

            LOG.info("6-------------------------------------------");

            primaryForest.awaitStatus("unmounted");
            replicaForest.awaitStatus("open");

            forests.unmountForest(primaryForest);

            LOG.info("7-------------------------------------------");


            forests.setForestHost(primaryForest, dNode3.getHostname());

            LOG.info("8-------------------------------------------");


            forests.mountForest(primaryForest);

            LOG.info("9-------------------------------------------");


            forests.enableForest(primaryForest);

            LOG.info("10-------------------------------------------");

            primaryForest.awaitStatus("sync replicating");
            replicaForest.awaitStatus("open");

            forests.disableForest(replicaForest);
            LOG.info("11-------------------------------------------");

            forests.enableForest(replicaForest);
            LOG.info("12-------------------------------------------");

            primaryForest.awaitStatus("open");
            replicaForest.awaitStatus("sync replicating");
            assertEquals(dNode3.getHostname(), primaryForest.getHostname());

            LOG.info("13-------------------------------------------");

        } catch (Exception e) {
            LOG.error("", e);
        } finally {
            Thread.sleep(1000000000);
        }

        LOG.info("14-------------------------------------------");
    }

    @Test(groups = {"Live"})
    public void testMoveForestWithoutReplica() throws Exception {
        LOG.info("-----------------testMoveForestWithoutReplica-----------------");

        Database database = createDatabase();
        Forest primaryForest = createForest(dNode1);
        primaryForest.awaitStatus("open");

        databases.attachForestToDatabase(primaryForest, database);
        primaryForest.awaitStatus("open");

        forests.moveForest(primaryForest, dNode3.getHostname());

        assertEquals("open", primaryForest.getStatus());
        assertEquals(dNode3.getHostname(), primaryForest.getHostname());
    }

    @Test(groups = {"Live"})
    public void testMoveForestWithReplica2() throws Exception {
        LOG.info("-----------------testMoveForestWithReplica2-----------------");

        Database database = createDatabase();

        Forest primaryForest = createForest(dNode1);
        primaryForest.awaitStatus("open");

        Forest replicaForest = createForest(dNode2, primaryForest.getName());
        databases.attachForestToDatabase(primaryForest, database);
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("sync replicating");

        forests.moveForest(primaryForest, dNode3.getHostname());

        assertEquals("open", primaryForest.getStatus());
        assertEquals("sync replicating", replicaForest.getStatus());
        assertEquals(dNode3.getHostname(), primaryForest.getHostname());
    }


    @Test(groups = {"WIP"})
    public void testMoveReplicaForest() throws Exception {
        LOG.info("-----------------testMoveReplicaForest-----------------");

        Database database = createDatabase();

        Forest primaryForest = createForest(dNode1);
        primaryForest.awaitStatus("open");

        Forest replicaForest = createForest(dNode2, primaryForest.getName());

        databases.attachForestToDatabase(primaryForest, database);
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("sync replicating");

        forests.moveForest(replicaForest, dNode3.getHostname());

        assertEquals("open", primaryForest.getStatus());
        assertEquals("sync replicating", replicaForest.getStatus());
        assertEquals(dNode3.getHostname(), replicaForest.getHostname());
    }

    @Test(groups = {"Live"})
    public void testMoveWithoutReplica() throws Exception {
        LOG.info("-----------------testMoveWithoutReplica-----------------");

        Database database = createDatabase();

        Forest forest = createForest(dNode1);
        forest.awaitStatus("open");

        databases.attachForestToDatabase(forest, database);
        forest.awaitStatus("open");

        forests.disableForest(forest);
        forest.awaitStatus("unmounted");

        forests.unmountForest(forest);
        forests.setForestHost(forest, dNode3.getHostname());
        forests.mountForest(forest);
        forests.enableForest(forest);
        forest.awaitStatus("open");

        assertEquals(dNode3.getHostname(), forest.getHostname());
    }


}
