package io.cloudsoft.marklogic.appservers;

import static com.google.common.base.Preconditions.checkState;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractGroupImpl;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpec;
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
    public RestAppServer createRestAppServer(String name, String database, String groupName, int port) {
        LOG.info("Creating REST appServer: " + name);
        MarkLogicNode node = getCluster().getAnyUpMember();
        checkState(node != null, "Can't create a REST appserver: found no available members in the cluster");

        RestAppServer appServer = addChild(EntitySpec.create(RestAppServer.class)
                .configure(RestAppServer.NAME, name)
                .configure(RestAppServer.DATABASE_NAME, database)
                .configure(RestAppServer.PORT, port)
                .configure(RestAppServer.GROUP_NAME, groupName));
        Entities.manage(appServer);

        node.createRestAppServer(appServer);
        LOG.info("Successfully created REST appServer: " + name);

        return appServer;
    }

}
