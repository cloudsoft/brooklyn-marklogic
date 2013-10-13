package io.cloudsoft.marklogic.databases;

import static java.lang.String.format;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

import brooklyn.entity.basic.AbstractGroupImpl;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.location.Location;
import brooklyn.management.Task;
import brooklyn.util.task.BasicTask;
import brooklyn.util.task.ScheduledTask;
import io.cloudsoft.marklogic.forests.Forest;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;

public class DatabasesImpl extends AbstractGroupImpl implements Databases {

    private static final Logger LOG = LoggerFactory.getLogger(DatabasesImpl.class);
    private final Object mutex = new Object();

    @Override
    public Iterator<Database> iterator() {
        // Can Databases have children that aren't instances of Database?
        return FluentIterable.from(getChildren())
                .filter(Database.class)
                .iterator();
    }

    private boolean databaseExists(final String databaseName) {
        Predicate<Database> nameMatcher = new Predicate<Database>() {
            @Override public boolean apply(Database input) {
                return databaseName.equals(input.getName());
            }};
        return Iterables.any(this, nameMatcher);
    }

    @Override
    public Database createDatabase(String name) {
        return createDatabaseWithSpec(EntitySpec.create(Database.class).configure(Database.NAME, name));
    }

    @Override
    public Database createDatabaseWithSpec(EntitySpec<Database> databaseSpec) {
        String databaseName = (String) databaseSpec.getConfig().get(Database.NAME);
        LOG.info("Creating database {}", databaseName);
        MarkLogicNode node = getGroup().getAnyUpMember();
        if(node == null){
            throw new IllegalStateException("No available member found in group: "+getGroup().getGroupName());
        }

        Database database;
        synchronized (mutex) {
            if (databaseExists(databaseName)) {
                throw new IllegalArgumentException(format("A database with name '%s' already exists", databaseName));
            }

            database = addChild(databaseSpec);
            Entities.manage(database);
        }
        node.createDatabase(database);
        LOG.info("Successfully created database: " + database.getName());
        return database;
    }

    // TODO: Assume should check database exists and is member of group
    @Override
    public void attachForestToDatabase(String forestName, String databaseName) {
        LOG.info("Attaching forest {} to database {}", forestName, databaseName);

        MarkLogicNode node = getGroup().getAnyUpMember();
        if (node == null){
            throw new IllegalStateException("No available member found in group: "+getGroup().getGroupName());
        }
        node.attachForestToDatabase(forestName, databaseName);

        LOG.info("Finished attach forest {} to database {}", forestName, databaseName);
    }

    @Override
    public void attachForestToDatabase(Forest forest, Database database) {
        attachForestToDatabase(forest.getName(), database.getName());
    }

    public MarkLogicGroup getGroup() {
        return getConfig(GROUP);
    }

    @Override
    public void start(Collection<? extends Location> locations) {
        Callable<Task<?>> taskFactory = new Callable<Task<?>>() {
            @Override public Task<Void> call() {
                return new BasicTask<Void>(new Callable<Void>() {
                    public Void call() {
                        try {
                            MarkLogicNode node = getGroup().getAnyUpMember();
                            if (node == null) {
                                LOG.debug("Can't discover databases, no nodes in cluster");
                                return null;
                            }

                            Set<String> databaseNames = node.scanDatabases();
                            for (String databaseName : databaseNames) {
                                synchronized (mutex) {
                                    if (!databaseExists(databaseName)) {
                                        LOG.info("Discovered database {}", databaseName);
                                        Database database = addChild(EntitySpec.newInstance(Database.class)
                                                .displayName(databaseName)
                                                .configure(Database.NAME, databaseName));
                                        Entities.manage(database);
                                    }
                                }
                            }
                            return null;
                        } catch (Exception e) {
                            LOG.warn("Problem scanning databases", e);
                            return null;
                        } catch (Throwable t) {
                            LOG.warn("Problem scanning databases (rethrowing)", t);
                            throw Throwables.propagate(t);
                        }
                    }});
            }
        };
        ScheduledTask scheduledTask = new ScheduledTask(taskFactory).period(TimeUnit.SECONDS.toMillis(30));
        getManagementContext().getExecutionManager().submit(scheduledTask);
    }

    @Override
    public void restart() {
    }

    @Override
    public void stop() {
    }
}
