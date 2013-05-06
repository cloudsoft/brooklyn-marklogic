package io.cloudsoft.marklogic.databases;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractGroupImpl;
import brooklyn.entity.proxying.EntitySpecs;
import io.cloudsoft.marklogic.MarkLogicCluster;
import io.cloudsoft.marklogic.MarkLogicNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class DatabasesImpl extends AbstractGroupImpl implements Databases {

    private static final Logger LOG = LoggerFactory.getLogger(DatabasesImpl.class);

    public MarkLogicCluster getCluster() {
        return getConfig(CLUSTER);
    }

    @Override
    public String getDisplayName(){
        return "Databases";
    }

    @Override
    public void createDatabase(String name) {
        LOG.info("Creating database: " + name);
        MarkLogicNode node = getMarkLogicNode();

        node.createDatabase(name);
        Database database = addChild(EntitySpecs.spec(Database.class)
                .configure(Database.NAME, name)
        );

        LOG.info("Successfully created database: " + name);
    }

    private MarkLogicNode getMarkLogicNode() {
        MarkLogicCluster cluster = getCluster();
        final Iterator<Entity> iterator = cluster.getMembers().iterator();
        if(!iterator.hasNext()){
            throw new IllegalStateException("Can't create a database, there are no members in the cluster");
        }
        return (MarkLogicNode) iterator.next();
    }
}
