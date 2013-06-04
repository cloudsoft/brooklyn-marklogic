package io.cloudsoft.marklogic.forests;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractEntity;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.entity.trait.Startable;
import brooklyn.location.Location;
import brooklyn.management.Task;
import com.google.common.collect.ImmutableMap;
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

    public MarkLogicGroup getGroup() {
        return getConfig(GROUP);
    }

    private MarkLogicNode getNode(String hostname) {
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
        for (Entity member : getChildren()) {
            if (member instanceof Forest) {
                Forest forest = (Forest) member;
                if (forestName.equals(forest.getName())) {
                    return forest;
                }
            }
        }

        return null;
    }

    @Override
    public void init() {
        super.init();

        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    MarkLogicNode node = getGroup().getAnyStartedMember();
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
                                        .configure(Forest.GROUP, getGroup())
                                        .configure(Forest.NAME, forestName);

                                Forest forest = addChild(spec);
                                forest.start(new LinkedList<Location>());
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Failed to discover forests", e);
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


        MarkLogicNode node = getNode(hostName);

        forestSpec = wrapSpec(forestSpec).configure(Forest.GROUP, getGroup())
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

        MarkLogicNode node = getGroup().getAnyStartedMember();
        node.attachReplicaForest(primaryForestName, replicaForestName);

        LOG.info("Finished attaching replica-forest {} to primary-forest {}", replicaForestName, primaryForestName);
    }

    @Override
    public void enableForest(String forestName, boolean enabled) {
        if (enabled) {
            LOG.info("Enabling forest {}", forestName);
        } else {
            LOG.info("Disabling forest {}", forestName);
        }

        MarkLogicNode node = getGroup().getAnyStartedMember();
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

        MarkLogicNode node = getGroup().getAnyStartedMember();
        node.deleteForestConfiguration(forestName);

        LOG.info("Finished deleting forest {} configuration", forestName);
    }

    @Override
    public void setForestHost(String forestName, String newHostName) {
        LOG.info("Setting Forest {} host {}", forestName, newHostName);

        Forest forest = getForest(forestName);
        if (forest == null) {
            throw new IllegalArgumentException(format("Can't change the host of forest, forest %s is not found", forestName));
        }

        MarkLogicNode node = getNode(newHostName);
        if (node == null) {
            throw new IllegalArgumentException(format("Can't change the host of forest, host %s is not found", newHostName));
        }

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
        LinkedList result = new LinkedList();
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
        LOG.info("Unmounting Forest {}", forestName);


        Forest forest = getForest(forestName);
        if (forest == null) {
            throw new IllegalArgumentException(format("Can't unmount unknown forest %s", forestName));
        }

        MarkLogicNode node = getNode(forest.getHostname());
        if (node == null) {
            throw new IllegalArgumentException(format("Can't unmount forest %s, its host %s is not found", forest.getName(), forest.getHostname()));
        }

        node.unmount(forest);
        LOG.info("Finished unmounting Forest {}", forestName);
    }

    @Override
    public void mountForest(String forestName) {
        LOG.info("Mounting Forest {}", forestName);


        Forest forest = getForest(forestName);
        if (forest == null) {
            throw new IllegalArgumentException(format("Can't mount unknown forest %s", forestName));
        }

        MarkLogicNode node = getNode(forest.getHostname());
        if (node == null) {
            throw new IllegalArgumentException(format("Can't mount forest %s, its host %s is not found", forest.getName(), forest.getHostname()));
        }

        node.mount(forest);
        LOG.info("Finished mounting Forest {}", forestName);
    }

    @Override
    public void moveForest(String primaryForestName, String hostName, String replicaForestName) {
        Forest primaryForest = getForest(primaryForestName);
        if(primaryForest == null){
            throw new IllegalArgumentException("Can't move unknown forest: "+primaryForest) ;
        }

        Forest replicaForest = getForest(replicaForestName);
        if(replicaForest == null){
            throw new IllegalArgumentException("Can't move unknown forest: "+primaryForest) ;
        }

        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("sync replicating");

        enableForest(primaryForest.getName(), false);
        primaryForest.awaitStatus("unmounted");
        replicaForest.awaitStatus("open");

        unmountForest(primaryForest.getName());
        setForestHost(primaryForest.getName(),hostName);
        mountForest(primaryForest.getName());
        enableForest(primaryForest.getName(), true);
        primaryForest.awaitStatus("sync replicating");
        replicaForest.awaitStatus("open");

        enableForest(replicaForest.getName(), false);
        enableForest(replicaForest.getName(), true);
        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("sync replicating");
    }
}
