package io.cloudsoft.marklogic.forests;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractEntity;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.entity.trait.Startable;
import brooklyn.location.Location;
import brooklyn.management.Task;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static brooklyn.entity.proxying.EntitySpecs.wrapSpec;
import static java.lang.String.format;

public class ForestsImpl extends AbstractEntity implements Forests {

    private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private static final Logger LOG = LoggerFactory.getLogger(ForestsImpl.class);
    private final Object mutex = new Object();

    @Override
    public void moveAllForestFromNode(MarkLogicNode node) {
        LOG.info("MoveAllForestsFromNode:" + node.getHostName());

        final List<Forest> forests = getBrooklynCreatedForestsOnHosts(node.getHostName());
        if (forests.isEmpty()) {
            LOG.info("There are no forests on host: " + node.getHostName());
            return;
        }

        LOG.info(format("Moving %s forests from host %s", forests.size(), node.getHostName()));
        for (Forest forest : forests) {
            List<String> nonDesiredHostNames = new LinkedList<String>();
            nonDesiredHostNames.add(node.getHostName());

            if (forest.getMaster() != null) {
                Forest master = getForest(forest.getMaster());
                nonDesiredHostNames.add(master.getHostname());
            }

            for(Forest replica : getReplicasForMaster(forest.getName())){
                nonDesiredHostNames.add(replica.getHostname());
            }

            MarkLogicNode targetNode = getGroup().getAnyOtherUpMember(nonDesiredHostNames.toArray(new String[nonDesiredHostNames.size()]));
            if (targetNode == null) {
                throw new IllegalStateException("Can't move forest: " + forest.getName() + " from node: " + node.getHostName() + ", there are no candidate nodes available");
            }

            moveForest(forest.getName(), targetNode.getHostName());
        }
    }

    @Override
    public void rebalance() {
        LOG.info("Rebalance");
    }

    @Override
    public List<Forest> asList() {
        List<Forest> result = new LinkedList<Forest>();

        for (Entity member : getChildren()) {
            if (member instanceof Forest) {
                Forest forest = (Forest) member;
                result.add(forest);
            }

        }
        return result;
    }

    private List<Forest> getBrooklynCreatedForestsOnHosts(String hostName) {
        List<Forest> forests = new LinkedList<Forest>();

        for (Forest forest : asList()) {
            if (forest.createdByBrooklyn() && hostName.equals(forest.getHostname())) {
                forests.add(forest);
            }
        }
        return forests;
    }

    public MarkLogicGroup getGroup() {
        return getConfig(GROUP);
    }

    private MarkLogicNode getNodeOrFail(String hostname) {
        MarkLogicGroup cluster = getGroup();

        for (Entity member : cluster.getMembers()) {
            if (member instanceof MarkLogicNode) {
                MarkLogicNode node = (MarkLogicNode) member;
                if (hostname.equals(node.getHostName())) {
                    return node;
                }
            }
        }

        throw new IllegalStateException(format("Can't create a forest, no node with hostname '%s' found", hostname));
    }

    private boolean forestExists(String forestName) {
        return getForest(forestName) != null;
    }

    private Forest getForest(String forestName) {
        for (Forest forest : asList()) {
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
    public void init() {
        super.init();

        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    MarkLogicNode node = getGroup().getAnyUpMember();
                    if (node == null) {
                        LOG.debug("Can't discover forests, no nodes in cluster");
                        return;
                    }

                    Set<String> forests = node.scanForests();
                    for (String forestName : forests) {
                        synchronized (mutex) {
                            if (!forestExists(forestName)) {
                                LOG.info("Discovered forest {}", forestName);

                                BasicEntitySpec<Forest, ?> spec = BasicEntitySpec.newInstance(Forest.class)
                                        .displayName(forestName)
                                        .configure(Forest.CREATED_BY_BROOKLYN, true)
                                        .configure(Forest.GROUP, getGroup())
                                        .configure(Forest.NAME, forestName);

                                Forest forest = addChild(spec);

                                Entities.invokeEffectorList(
                                        ForestsImpl.this,
                                        Lists.newArrayList(forest),
                                        Startable.START,
                                        ImmutableMap.of("locations", getLocations())).getUnchecked();
                            }
                        }
                    }
                } catch (Exception e) {
                    //    LOG.error("Failed to discover forests", e);
                }
            }
        };
        scheduler.scheduleAtFixedRate(task, 0, 5, TimeUnit.SECONDS);
    }

    @Override
    public Forest createForestWithSpec(BasicEntitySpec<Forest, ?> forestSpec) {
        String forestName = (String) forestSpec.getConfig().get(Forest.NAME);
        String hostName = (String) forestSpec.getConfig().get(Forest.HOST);

        LOG.info("Creating forest {} on host {}", forestName, hostName);

        MarkLogicNode node = getNodeOrFail(hostName);

        forestSpec = wrapSpec(forestSpec)
                .configure(Forest.GROUP, getGroup())
                .configure(Forest.CREATED_BY_BROOKLYN, true)
                .displayName(forestName);

        Forest forest;
        synchronized (mutex) {
            if (forestExists(forestName)) {
                throw new IllegalArgumentException(format("A forest with name '%s' already exists", forestName));
            }

            forest = addChild(forestSpec);
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

        BasicEntitySpec<Forest, ?> forestSpec = BasicEntitySpec.newInstance(Forest.class)
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

        node.attachReplicaForest(primaryForestName, replicaForestName);
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

        MarkLogicNode node = getGroup().getAnyUpMember();
        node.enableForest(forestName, enabled);

        if (enabled) {
            LOG.info("Finished enabling forest {}", forestName);
        } else {
            LOG.info("Finished disabling forest {}", forestName);
        }
    }

    @Override
    public void deleteForestConfiguration(String forestName) {
        LOG.info("Delete forest {} configuration", forestName);

        synchronized (mutex) {
            Forest forest = getForest(forestName);
            if (forest != null) {
                forest.clearParent();

                LOG.info("Forest {} still found after delete:", forestExists(forestName));

            } else {
                LOG.info("Forest {} not found in Brooklyn", forestName);
            }
        }

        MarkLogicNode node = getGroup().getAnyUpMember();
        node.deleteForestConfiguration(forestName);

        LOG.info("Finished deleting forest {} configuration", forestName);
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
        if (startableChildren.isEmpty())
            return;

        final Task<List<Void>> task = Entities.invokeEffectorList(
                this,
                startableChildren,
                Startable.START,
                ImmutableMap.of("locations", locations));
        task.getUnchecked();
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
        for (Forest forest : asList()) {
            if (forestName.equals(forest.getMaster())) {
                result.add(forest);
            }
        }

        return result;
    }

    @Override
    public void moveForest(String primaryForestName, String hostName) {
        Forest primaryForest = getForestOrFail(primaryForestName);
        List<Forest> replicaForests = getReplicasForMaster(primaryForestName);

        if (replicaForests.size() == 0) {
            enableForest(primaryForest.getName(), false);
            primaryForest.awaitStatus("unmounted");
            unmountForest(primaryForest.getName());
            setForestHost(primaryForest.getName(), hostName);
            mountForest(primaryForest.getName());
            enableForest(primaryForest.getName(), true);
            primaryForest.awaitStatus("open", "sync replicating");
        } else if (replicaForests.size() == 1) {
            Forest replicaForest = replicaForests.get(0);

            primaryForest.awaitStatus("open");
            replicaForest.awaitStatus("sync replicating");

            enableForest(primaryForest.getName(), false);
            primaryForest.awaitStatus("unmounted");
            replicaForest.awaitStatus("open");

            unmountForest(primaryForest.getName());
            setForestHost(primaryForest.getName(), hostName);
            mountForest(primaryForest.getName());
            enableForest(primaryForest.getName(), true);
            primaryForest.awaitStatus("sync replicating");
            replicaForest.awaitStatus("open");

            enableForest(replicaForest.getName(), false);
            enableForest(replicaForest.getName(), true);
            primaryForest.awaitStatus("open");
            replicaForest.awaitStatus("sync replicating");
        } else {
            throw new RuntimeException();//todo:
        }
    }
}
