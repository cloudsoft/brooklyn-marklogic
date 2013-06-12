package io.cloudsoft.marklogic.appservers;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractGroupImpl;
import brooklyn.entity.proxying.EntitySpecs;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class AppServicesImpl extends AbstractGroupImpl implements AppServices {

    private static final Logger LOG = LoggerFactory.getLogger(AppServicesImpl.class);

    public MarkLogicGroup getCluster() {
        return getConfig(CLUSTER);
    }

    @Override
    public String getDisplayName() {
        return "AppServices";
    }

    @Override
    public RestAppServer createRestAppServer(String name, String database, String groupName, String port) {
        LOG.info("Creating REST appServer: " + name);
        MarkLogicNode node = getMarkLogicNode();

        node.createRestAppServer(name, database, groupName, "" + port);

        RestAppServer appServer = addChild(EntitySpecs.spec(RestAppServer.class)
                .configure(RestAppServer.NAME, name)
                .configure(RestAppServer.DATABASE_NAME, database)
                .configure(RestAppServer.PORT, port)
                .configure(RestAppServer.GROUP_NAME, groupName)
        );
        //todo: should be moved to the appServer

        LOG.info("Successfully created REST appServer: " + name);

        return appServer;
    }

    private MarkLogicNode getMarkLogicNode() {
        MarkLogicGroup cluster = getCluster();
        final Iterator<Entity> iterator = cluster.getMembers().iterator();
        if (!iterator.hasNext()) {
            throw new IllegalStateException("Can't create a database, there are no members in the cluster");
        }
        return (MarkLogicNode) iterator.next();
    }
}
