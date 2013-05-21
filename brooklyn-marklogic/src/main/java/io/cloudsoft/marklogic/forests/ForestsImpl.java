package io.cloudsoft.marklogic.forests;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractEntity;
import brooklyn.entity.proxying.BasicEntitySpec;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
        for (Entity member : getChildren()) {
            if (member instanceof Forest) {
                Forest forest = (Forest) member;
                if (forestName.equals(forest.getName())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void init() {
        super.init();

        Runnable task = new Runnable() {
            @Override
            public void run() {
                MarkLogicGroup cluster = getGroup();
                Collection<Entity> markLogicNodes = cluster.getMembers();
                if (markLogicNodes.isEmpty()) return;
                for (Entity member : markLogicNodes) {
                    if (member instanceof MarkLogicNode) {
                        MarkLogicNode node = (MarkLogicNode) member;
                        if (node.isUp()) {
                            Set<String> forests = node.scanForests();
                            for (String forestName : forests) {
                                synchronized (mutex) {
                                    if (!forestExists(forestName)) {
                                        addChild(BasicEntitySpec.newInstance(Forest.class)
                                                .displayName(forestName)
                                                .configure(Forest.NAME, forestName)
                                        );
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };
        scheduler.scheduleAtFixedRate(task, 0, 5, TimeUnit.SECONDS);
    }

    @Override
    public Forest createForest(BasicEntitySpec<Forest, ?> forestSpec) {
        String forestName = (String) forestSpec.getConfig().get(Forest.NAME);
        String hostName = (String) forestSpec.getConfig().get(Forest.HOST);

        LOG.info("Creating forest {} on host {}", forestName, hostName);


        MarkLogicNode node = getNode(hostName);

        Forest forest;
        synchronized (mutex) {
            if (forestExists(forestName)) {
                throw new IllegalArgumentException(format("A forest with name '%s' already exists", forestName));
            }

            forest = addChild(forestSpec);
        }

        node.createForest(forest);

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
                .displayName(forestName)
                .configure(Forest.NAME, forestName)
                .configure(Forest.HOST, hostname)
                .configure(Forest.DATA_DIR, dataDir)
                .configure(Forest.LARGE_DATA_DIR, largeDataDir)
                .configure(Forest.FAST_DATA_DIR, fastDataDir)
                .configure(Forest.UPDATES_ALLOWED, UpdatesAllowed.get(updatesAllowedStr))
                .configure(Forest.REBALANCER_ENABLED, rebalancerEnabled)
                .configure(Forest.FAILOVER_ENABLED, failoverEnabled);
        return createForest(forestSpec);
    }
}
