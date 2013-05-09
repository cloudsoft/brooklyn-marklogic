package io.cloudsoft.marklogic.forests;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractEntity;
import brooklyn.entity.proxying.BasicEntitySpec;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class ForestsImpl extends AbstractEntity implements Forests {

    private static final Logger LOG = LoggerFactory.getLogger(Forests.class);


    public MarkLogicGroup getGroup() {
        return getConfig(GROUP);
    }

    private MarkLogicNode getNode(String hostname) {
        MarkLogicGroup cluster = getGroup();

        for (Entity member : cluster.getMembers()) {
            if (member instanceof MarkLogicNode) {
                MarkLogicNode node = (MarkLogicNode)member;
                if(hostname.equals(node.getHostName())){
                    return node;
                }
            }
        }

        throw new IllegalStateException(format("Can't create a forest, no node with hostname '%s' found", hostname));
    }

    @Override
    public Forest createForest(
            String name,
            String hostname,
            String dataDir,
            String largeDataDir,
            String fastDataDir,
            String updatesAllowedStr,
            String rebalancerEnabled,
            String failoverEnabled) {

        LOG.info(format("Creating forest name '%s' dataDir '%s' largeDataDir '%s' fastDataDir '%s' updatesAllowed '%s' rebalancerEnabled '%s' failoverEnabled '%s'",
                name, dataDir, largeDataDir, fastDataDir, updatesAllowedStr, rebalancerEnabled, failoverEnabled));

        UpdatesAllowed updatesAllowed = UpdatesAllowed.get(updatesAllowedStr);

        MarkLogicNode node = getNode(hostname);

        LOG.info("Creating forest on host:"+hostname);

        Forest forest = addChild(BasicEntitySpec.newInstance(Forest.class)
                .displayName(name)
                .configure(Forest.NAME, name)
                .configure(Forest.HOST, hostname)
                .configure(Forest.DATA_DIR, dataDir)
                .configure(Forest.LARGE_DATA_DIR, largeDataDir)
                .configure(Forest.FAST_DATA_DIR, fastDataDir)
                .configure(Forest.UPDATES_ALLOWED, updatesAllowed)
                .configure(Forest.REBALANCER_ENABLED, true)
                .configure(Forest.FAILOVER_ENABLED, true)
        );

        node.createForest(forest);
        return forest;
    }
}
