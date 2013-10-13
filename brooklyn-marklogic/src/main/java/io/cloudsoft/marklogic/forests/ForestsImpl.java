package io.cloudsoft.marklogic.forests;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractEntity;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.Startable;
import brooklyn.location.Location;
import brooklyn.management.Task;
import brooklyn.util.task.BasicTask;
import brooklyn.util.task.ScheduledTask;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Manages the {@link Forest forests} in a {@link io.cloudsoft.marklogic.clusters.MarkLogicCluster cluster}.
 */
public class ForestsImpl extends AbstractEntity implements Forests {

    private static final Logger LOG = LoggerFactory.getLogger(ForestsImpl.class);
    private final Object mutex = new Object();

    @Override
    public Iterator<Forest> iterator() {
        // Can Forests have children that aren't instances of Forest?
        return FluentIterable.from(getChildren())
                .filter(Forest.class)
                .iterator();
    }

    @Override
    public void moveAllForestFromNode(String hostName) {
        LOG.info("MoveAllForestsFromNode:" + hostName);

        final List<Forest> forests = getBrooklynCreatedForestsOnHosts(hostName);
        if (forests.isEmpty()) {
            LOG.info("There are no forests on host: " + hostName);
            return;
        }

        LOG.info(format("Moving %s forests from host %s", forests.size(), hostName));
        for (Forest forest : forests) {
            MarkLogicNode targetNode = findTargetNode(hostName, forest);

            if (targetNode == null) {
                throw new IllegalStateException("Can't move forest: " + forest.getName() + " from node: " + hostName + ", there are no candidate nodes available");
            }

            moveForest(forest.getName(), targetNode.getHostName());
        }
    }

    private MarkLogicNode findTargetNode(String host, Forest forest) {
        Set<String> nonDesiredHostNames = new HashSet<String>();
        nonDesiredHostNames.add(host);

        if (forest.getMaster() != null) {
            Forest master = getForest(forest.getMaster());
            nonDesiredHostNames.add(master.getHostname());
        }

        for (Forest replica : getReplicasForMaster(forest.getName())) {
            nonDesiredHostNames.add(replica.getHostname());
        }

        List<MarkLogicNode> upNodes = getGroup().getAllUpMembers();
        if (upNodes.isEmpty()) return null;

        List<MarkLogicNode> filteredUpNodes = new LinkedList<MarkLogicNode>();
        for (MarkLogicNode upNode : upNodes) {
            if (!nonDesiredHostNames.contains(upNode.getHostName())) {
                filteredUpNodes.add(upNode);
            }
        }

        if (filteredUpNodes.isEmpty()) return null;

        //and now we select the node with the lowest number of forests
        MarkLogicNode bestNode = null;
        int lowestForestCount = Integer.MAX_VALUE;
        for (MarkLogicNode upNode : filteredUpNodes) {
            if (lowestForestCount == Integer.MAX_VALUE) {
                bestNode = upNode;
                lowestForestCount = Iterables.size(getBrooklynCreatedForestsOnHosts(upNode.getHostName()));
            } else {
                int forestCount = Iterables.size(getBrooklynCreatedForestsOnHosts(upNode.getHostName()));
                if (forestCount < lowestForestCount) {
                    bestNode = upNode;
                    lowestForestCount = forestCount;
                }
            }
        }
        return bestNode;
    }

    @Override
    public void rebalance() {
        LOG.info("Rebalance doing nothing");
    }

    private List<Forest> getBrooklynCreatedForestsOnHosts(final String hostName) {
        checkNotNull(hostName, "hostName");
        Predicate<Forest> filter = new Predicate<Forest>() {
            @Override public boolean apply(Forest forest) {
                return forest.createdByBrooklyn() && hostName.equals(forest.getHostname());
            }
        };
        return FluentIterable.from(this)
                .filter(filter)
                .toList();
    }

    public MarkLogicGroup getGroup() {
        return getConfig(GROUP);
    }

    private MarkLogicNode getNodeOrFail(String hostname) {
        MarkLogicGroup cluster = getGroup();
        Set<String> availableHostnames = Sets.newLinkedHashSet();

        for (Entity member : cluster.getChildren()) {
            if (member instanceof MarkLogicNode) {
                MarkLogicNode node = (MarkLogicNode) member;
                if (hostname.equals(node.getHostName())) {
                    return node;
                }
                availableHostnames.add(node.getHostName());
            }
        }

        throw new IllegalStateException(format("Can't create a forest, no node with hostname '%s' found - available were %s", hostname, availableHostnames));
    }

    private boolean forestExists(String forestName) {
        return getForest(forestName) != null;
    }

    private Forest getForest(String forestName) {
        for (Forest forest : this) {
            if (forestName.equals(forest.getName())) {
                return forest;
            }
        }

        return null;
    }

    private Forest getForestOrFail(String forestName) {
        Forest forest = getForest(forestName);
        if (forest == null) {
            throw new IllegalArgumentException("Failed to find forest: " + forestName);
        }
        return forest;
    }

    @Override
    public Forest createForestWithSpec(EntitySpec<Forest> forestSpec) {
        String forestName = (String) forestSpec.getConfig().get(Forest.NAME);
        String hostName = (String) forestSpec.getConfig().get(Forest.HOST);

        LOG.info("Creating forest {} on host {}", forestName, hostName);

        MarkLogicNode node = getNodeOrFail(hostName);

        forestSpec = EntitySpec.create(forestSpec)
                .configure(Forest.GROUP, getGroup())
                .configure(Forest.CREATED_BY_BROOKLYN, true)
                .displayName(forestName);

        Forest forest;
        synchronized (mutex) {
            if (forestExists(forestName)) {
                throw new IllegalArgumentException(format("A forest with name '%s' already exists", forestName));
            }

            forest = addChild(forestSpec);
            Entities.manage(forest);
        }

        node.createForest(forest);

        forest.start(new LinkedList<Location>());

        LOG.info("Finished creating forest {} on host {}", forestName, hostName);

        return forest;
    }

    @Override
    public Forest createForest(
            String forestName,
            String hostname,
            String dataDir,
            String largeDataDir,
            String fastDataDir,
            String updatesAllowedStr,
            boolean rebalancerEnabled,
            boolean failoverEnabled) {

        EntitySpec<Forest> forestSpec = EntitySpec.create(Forest.class)
                .configure(Forest.NAME, forestName)
                .configure(Forest.HOST, hostname)
                .configure(Forest.DATA_DIR, dataDir)
                .configure(Forest.LARGE_DATA_DIR, largeDataDir)
                .configure(Forest.FAST_DATA_DIR, fastDataDir)
                .configure(Forest.UPDATES_ALLOWED, UpdatesAllowed.get(updatesAllowedStr))
                .configure(Forest.REBALANCER_ENABLED, rebalancerEnabled)
                .configure(Forest.FAILOVER_ENABLED, failoverEnabled);
        return createForestWithSpec(forestSpec);
    }

    @Override
    public void attachReplicaForest(String primaryForestName, String replicaForestName) {
        LOG.info("Attaching replica-forest {} to primary-forest {}", replicaForestName, primaryForestName);

        MarkLogicNode node = getGroup().getAnyUpMember();
        Forest primaryForest = getForestOrFail(primaryForestName);
        Forest replicaForest = getForestOrFail(replicaForestName);

        node.attachReplicaForest(primaryForest, replicaForest);
        replicaForest.setConfig(Forest.MASTER, primaryForestName);

        LOG.info("Finished attaching replica-forest {} to primary-forest {}", replicaForestName, primaryForestName);
    }

    @Override
    public void enableForest(String forestName, boolean enabled) {
        if (enabled) {
            LOG.info("Enabling forest {}", forestName);
        } else {
            LOG.info("Disabling forest {}", forestName);
        }

        if (getGroup() == null) {
            throw new RuntimeException("Group is null, not possible");
        }
        MarkLogicNode node = getGroup().getAnyUpMember();
        if (node == null) {
            LOG.info("Group: " + getGroup().getGroupName());
            LOG.info("Group.size: " + getGroup().getCurrentSize());
            for (Entity entity : getGroup().getChildren()) {
                if (entity instanceof MarkLogicNode) {
                    MarkLogicNode child = (MarkLogicNode) entity;
                    LOG.info("child.hostname:" + child.getHostName() + " isUp: " + child.isUp());
                }
            }
            throw new IllegalStateException("No up members found in group: " + getGroup().getGroupName());
        }
        node.enableForest(forestName, enabled);

        if (enabled) {
            LOG.info("Finished enabling forest {}", forestName);
        } else {
            LOG.info("Finished disabling forest {}", forestName);
        }
    }

    @Override
    public void setForestHost(String forestName, String newHostName) {
        LOG.info("Setting Forest {} host {}", forestName, newHostName);

        Forest forest = getForestOrFail(forestName);
        MarkLogicNode node = getNodeOrFail(newHostName);

        if (forest.getHostname().equals(newHostName)) {
            LOG.info("Finished setting Forest {}, no host change.", forestName, newHostName);
            return;
        }

        forest.setConfig(Forest.HOST, newHostName);
        node.setForestHost(forestName, newHostName);

        LOG.info("Finished setting Forest {} host {}", forestName, newHostName);
    }

    @Override
    public void restart() {
        final List<? extends Entity> startableChildren = getStartableChildren();
        if (startableChildren.isEmpty())
            return;

        Entities.invokeEffectorList(
                this,
                startableChildren,
                Startable.RESTART).getUnchecked();
    }

    protected List<? extends Entity> getStartableChildren() {
        List<Entity> result = new LinkedList<Entity>();
        for (Entity entity : getChildren()) {
            if (entity instanceof Startable) {
                result.add(entity);
            }
        }
        return result;
    }

    @Override
    public void start(Collection<? extends Location> locations) {
        final List<? extends Entity> startableChildren = getStartableChildren();

        if (!startableChildren.isEmpty()) {
            final Task<List<Void>> task = Entities.invokeEffectorList(
                    this,
                    startableChildren,
                    Startable.START,
                    ImmutableMap.of("locations", locations));
            task.getUnchecked();
        }

        Callable<Task<?>> taskFactory = new Callable<Task<?>>() {
            @Override
            public Task<Void> call() {
                return new BasicTask<Void>(new Callable<Void>() {
                    public Void call() {
                        try {
                            MarkLogicNode node = getGroup().getAnyUpMember();
                            if (node == null) {
                                LOG.debug("Can't discover forests, no nodes in cluster");
                                return null;
                            }

                            Set<String> forests = node.scanForests();
                            for (String forestName : forests) {
                                synchronized (mutex) {
                                    if (!forestExists(forestName)) {
                                        LOG.info("Discovered forest {}", forestName);

                                        EntitySpec<Forest> spec = EntitySpec.create(Forest.class)
                                                .displayName(forestName)
                                                .configure(Forest.CREATED_BY_BROOKLYN, false)
                                                .configure(Forest.GROUP, getGroup())
                                                .configure(Forest.NAME, forestName);

                                        Forest forest = addChild(spec);
                                        Entities.manage(forest);

                                        Entities.invokeEffectorList(
                                                ForestsImpl.this,
                                                Lists.newArrayList(forest),
                                                Startable.START,
                                                ImmutableMap.of("locations", getLocations())).getUnchecked();
                                    }
                                }
                            }
                            return null;
                        } catch (Exception e) {
                            LOG.warn("Problem scanning forests", e);
                            return null;
                        } catch (Throwable t) {
                            LOG.warn("Problem scanning forests (rethrowing)", t);
                            throw Throwables.propagate(t);
                        }
                    }
                });
            }
        };
        ScheduledTask scheduledTask = new ScheduledTask(taskFactory).period(TimeUnit.SECONDS.toMillis(30));
        getManagementContext().getExecutionManager().submit(scheduledTask);
    }

    @Override
    public void stop() {
        final List<? extends Entity> startableChildren = getStartableChildren();
        if (startableChildren.isEmpty())
            return;

        Entities.invokeEffectorList(
                this,
                startableChildren,
                Startable.STOP).getUnchecked();
    }

    @Override
    public void unmountForest(String forestName) {

        Forest forest = getForestOrFail(forestName);
        MarkLogicNode node = getNodeOrFail(forest.getHostname());
        LOG.info("Unmounting Forest {} on node {}", forestName, node.getHostName());

        node.unmount(forest);
        LOG.info("Finished unmounting Forest {}", forestName);
    }

    @Override
    public void mountForest(String forestName) {
        LOG.info("Mounting Forest {}", forestName);

        Forest forest = getForestOrFail(forestName);
        MarkLogicNode node = getNodeOrFail(forest.getHostname());

        node.mount(forest);
        LOG.info("Finished mounting Forest {}", forestName);
    }

    private List<Forest> getReplicasForMaster(String forestName) {
        List<Forest> result = new LinkedList<Forest>();
        for (Forest forest : this) {
            if (forestName.equals(forest.getMaster())) {
                result.add(forest);
            }
        }

        return result;
    }

    private void sleepSome() {
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void moveForest(String primaryForestName, String hostName) {
        LOG.info("Moving forest {} to host {}", primaryForestName, hostName);

        Forest primaryForest = getForestOrFail(primaryForestName);
        List<Forest> replicaForests = getReplicasForMaster(primaryForestName);

        if (replicaForests.size() == 0) {
            enableForest(primaryForest.getName(), false);
            sleepSome();
            primaryForest.awaitStatus("unmounted");
            sleepSome();

            unmountForest(primaryForest.getName());
            sleepSome();

            setForestHost(primaryForest.getName(), hostName);
            sleepSome();

            mountForest(primaryForest.getName());
            sleepSome();

            enableForest(primaryForest.getName(), true);
            primaryForest.awaitStatus("open", "sync replicating");
        } else if (replicaForests.size() == 1) {
            Forest replicaForest = replicaForests.get(0);

            primaryForest.awaitStatus("open");
            replicaForest.awaitStatus("sync replicating");
            sleepSome();

            enableForest(primaryForest.getName(), false);
            primaryForest.awaitStatus("unmounted");
            replicaForest.awaitStatus("open");

            sleepSome();

            unmountForest(primaryForest.getName());
            sleepSome();

            setForestHost(primaryForest.getName(), hostName);
            sleepSome();

            mountForest(primaryForest.getName());
            sleepSome();

            enableForest(primaryForest.getName(), true);
            primaryForest.awaitStatus("sync replicating");
            replicaForest.awaitStatus("open");

            enableForest(replicaForest.getName(), false);
            sleepSome();

            enableForest(replicaForest.getName(), true);
            sleepSome();

            primaryForest.awaitStatus("open");
            replicaForest.awaitStatus("sync replicating");
        } else {
            throw new RuntimeException();//todo:
        }
    }

}
