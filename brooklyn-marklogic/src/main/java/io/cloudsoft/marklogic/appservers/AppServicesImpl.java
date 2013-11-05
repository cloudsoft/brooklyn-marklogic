package io.cloudsoft.marklogic.appservers;

import static com.google.common.base.Preconditions.checkState;

import brooklyn.entity.basic.AbstractGroupImpl;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpec;
import io.cloudsoft.marklogic.databases.Database;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public AppServer createAppServer(AppServerKind kind, String name, Database database, String group, int port) {
        return createAppServer(kind.name(), name, database.getName(), group, port);
    }

    @Override
    public AppServer createAppServer(String kind, String name, String database, String groupName, int port) {
        LOG.info("Creating appServer: " + name);
        // TODO: This should go through an e-node to maintain the e/d distinction.
        MarkLogicNode node = getCluster().getAnyUpMember();
        checkState(node != null, "Can't create appServer: found no up members in the cluster");
        AppServerKind serverKind = AppServerKind.valueOf(kind);
        checkState(serverKind != AppServerKind.UNRECOGNIZED, "Unknown server kind: %s", kind);

        AppServer appServer = addChild(EntitySpec.create(AppServer.class)
                .configure(AppServer.NAME, name)
                .configure(AppServer.KIND, serverKind)
                .configure(AppServer.DATABASE_NAME, database)
                .configure(AppServer.PORT, port)
                .configure(AppServer.GROUP_NAME, groupName));
        Entities.manage(appServer);

        node.createAppServer(appServer);
        LOG.info("Successfully created appServer: " + name);

        return appServer;
    }

}
