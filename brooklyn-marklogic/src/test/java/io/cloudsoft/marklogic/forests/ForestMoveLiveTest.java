package io.cloudsoft.marklogic.forests;

import io.cloudsoft.marklogic.AbstractMarkLogicFullClusterLiveTest;
import io.cloudsoft.marklogic.databases.Database;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

public class ForestMoveLiveTest extends AbstractMarkLogicFullClusterLiveTest {

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


            forests.attachReplicaForest(primaryForest.getName(), replicaForest.getName());
            LOG.info("3-------------------------------------------");

            databases.attachForestToDatabase(primaryForest.getName(), database.getName());
            LOG.info("4-------------------------------------------");

            primaryForest.awaitStatus("open");
            replicaForest.awaitStatus("sync replicating");

            LOG.info("5-------------------------------------------");


            forests.disableForest(primaryForest.getName());

            LOG.info("6-------------------------------------------");

            primaryForest.awaitStatus("unmounted");
            replicaForest.awaitStatus("open");

            forests.unmountForest(primaryForest.getName());

            LOG.info("7-------------------------------------------");


            forests.setForestHost(primaryForest.getName(), dNode3.getHostName());

            LOG.info("8-------------------------------------------");


            forests.mountForest(primaryForest.getName());

            LOG.info("9-------------------------------------------");


            forests.enableForest(primaryForest.getName());

            LOG.info("10-------------------------------------------");

            primaryForest.awaitStatus("sync replicating");
            replicaForest.awaitStatus("open");

            forests.disableForest(replicaForest.getName());
            LOG.info("11-------------------------------------------");

            forests.enableForest(replicaForest.getName());
            LOG.info("12-------------------------------------------");

            primaryForest.awaitStatus("open");
            replicaForest.awaitStatus("sync replicating");
            assertEquals(dNode3.getHostName(), primaryForest.getHostname());

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

        databases.attachForestToDatabase(primaryForest.getName(), database.getName());
        primaryForest.awaitStatus("open");

        forests.moveForest(primaryForest.getName(), dNode3.getHostName());

        assertEquals("open", primaryForest.getStatus());
        assertEquals(dNode3.getHostName(), primaryForest.getHostname());
    }

    @Test(groups = {"Live"})
    public void testMoveForestWithReplica2() throws Exception {
        LOG.info("-----------------testMoveForestWithReplica2-----------------");

        Database database = createDatabase();

        Forest primaryForest = createForest(dNode1);
        primaryForest.awaitStatus("open");

        Forest replicaForest = createForest(dNode2, primaryForest.getName());
        databases.attachForestToDatabase(primaryForest.getName(), database.getName());
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("sync replicating");

        forests.moveForest(primaryForest.getName(), dNode3.getHostName());

        assertEquals("open", primaryForest.getStatus());
        assertEquals("sync replicating", replicaForest.getStatus());
        assertEquals(dNode3.getHostName(), primaryForest.getHostname());
    }


    @Test(groups = {"WIP"})
    public void testMoveReplicaForest() throws Exception {
        LOG.info("-----------------testMoveReplicaForest-----------------");

        Database database = createDatabase();

        Forest primaryForest = createForest(dNode1);
        primaryForest.awaitStatus("open");

        Forest replicaForest = createForest(dNode2, primaryForest.getName());

        databases.attachForestToDatabase(primaryForest.getName(), database.getName());
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("sync replicating");

        forests.moveForest(replicaForest.getName(), dNode3.getHostName());

        assertEquals("open", primaryForest.getStatus());
        assertEquals("sync replicating", replicaForest.getStatus());
        assertEquals(dNode3.getHostName(), replicaForest.getHostname());
    }

    @Test(groups = {"Live"})
    public void testMoveWithoutReplica() throws Exception {
        LOG.info("-----------------testMoveWithoutReplica-----------------");

        Database database = createDatabase();

        Forest forest = createForest(dNode1);
        forest.awaitStatus("open");

        databases.attachForestToDatabase(forest.getName(), database.getName());
        forest.awaitStatus("open");

        forests.disableForest(forest.getName());
        forest.awaitStatus("unmounted");

        forests.unmountForest(forest.getName());
        forests.setForestHost(forest.getName(), dNode3.getHostName());
        forests.mountForest(forest.getName());
        forests.enableForest(forest.getName());
        forest.awaitStatus("open");

        assertEquals(dNode3.getHostName(), forest.getHostname());
    }


}
