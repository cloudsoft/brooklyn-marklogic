package io.cloudsoft.marklogic.databases;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractGroupImpl;
import brooklyn.entity.proxying.BasicEntitySpec;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static brooklyn.entity.proxying.EntitySpecs.spec;
import static java.lang.String.format;

public class DatabasesImpl extends AbstractGroupImpl implements Databases {

    private static final Logger LOG = LoggerFactory.getLogger(DatabasesImpl.class);
    private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private final Object mutex = new Object();

    public MarkLogicGroup getGroup() {
        return getConfig(GROUP);
    }

    private boolean databaseExists(String databaseName) {
        for (Entity member : getChildren()) {
            if (member instanceof Database) {
                Database db = (Database) member;
                if (databaseName.equals(db.getName())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void init() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    MarkLogicNode node = getGroup().getAnyUpMember();
                    if (node == null) {
                        LOG.debug("Can't discover forests, no nodes in cluster");
                        return;
                    }

                    Set<String> databaseNames = node.scanDatabases();
                    for (String databaseName : databaseNames) {
                        synchronized (mutex) {
                            if (!databaseExists(databaseName)) {
                                LOG.info("Discovered database {}", databaseName);
                                addChild(BasicEntitySpec.newInstance(Database.class)
                                        .displayName(databaseName)
                                        .configure(Database.NAME, databaseName)
                                );
                            }
                        }
                    }
                } catch (Exception e) {
                //    LOG.error("Failed to discover databases", e);
                }
            }
        };
        scheduler.scheduleAtFixedRate(task, 0, 30, TimeUnit.SECONDS);
    }

    @Override
    public void createDatabaseWithForest(String name) {
        LOG.info("Creating database: " + name);

        synchronized (mutex) {
            if (databaseExists(name)) {
                throw new IllegalArgumentException(format("A database with name '%s' already exists", name));
            }

            addChild(spec(Database.class)
                    .configure(Database.NAME, name)
            );
        }
        MarkLogicNode node = getAnyNode();
        node.createDatabaseWithForest(name);

        LOG.info("Successfully created database: " + name);
    }

    private MarkLogicNode getAnyNode() {
        MarkLogicGroup cluster = getGroup();
        final Iterator<Entity> iterator = cluster.getMembers().iterator();
        if (!iterator.hasNext()) {
            throw new IllegalStateException("Can't create a database, there are no members in the cluster");
        }
        return (MarkLogicNode) iterator.next();
    }

    @Override
    public Database createDatabase(String name) {
        return createDatabaseWithSpec(spec(Database.class).configure(Database.NAME, name));
    }

    @Override
    public Database createDatabaseWithSpec(BasicEntitySpec<Database, ?> databaseSpec) {
        String databaseName = (String) databaseSpec.getConfig().get(Database.NAME);
        LOG.info("Creating database {}", databaseName);
        Database database;
        synchronized (mutex) {
            if (databaseExists(databaseName)) {
                throw new IllegalArgumentException(format("A database with name '%s' already exists", databaseName));
            }

            database = addChild(databaseSpec);
        }
        MarkLogicNode node = getAnyNode();
        node.createDatabase(database);
        LOG.info("Successfully created database: " + database.getName());
        return database;
    }

    @Override
    public void attachForestToDatabase(String forestName, String databaseName) {
        LOG.info("Attaching forest {} to database {}", forestName, databaseName);

        MarkLogicNode node = getAnyNode();
        node.attachForestToDatabase(forestName, databaseName);

        LOG.info("Finished attach forest {} to database {}", forestName, databaseName);
    }
}
