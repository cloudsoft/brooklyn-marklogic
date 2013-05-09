package io.cloudsoft.marklogic.forests;

import brooklyn.entity.basic.AbstractEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class ForestsImpl extends AbstractEntity implements Forests {

    private static final Logger LOG = LoggerFactory.getLogger(Forests.class);

    @Override
    public void createForest(
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

//        if (getNodeType() == NodeType.E_NODE) {
//            throw new IllegalStateException("Can't create a forest on an e-node");
//        }
//
//        //todo: it probably is better not to create the entity yet; if we are automatically going to sync our internal
//        //structure to what is running in the marklogic hosts, then eventually the create forest in marklogic will result in a
//        //in a forest entity in the marklogic node.
//
//        //todo: do we want to have the marklogicnode as parent of the forest, or should we insert a group entity in between.
//
//        UpdatesAllowed updatesAllowed = UpdatesAllowed.get(updatesAllowedStr);
//
//        Forest forest = getEntityManager().createEntity(BasicEntitySpec.newInstance(Forest.class)
//                .parent(this)
//                .configure(Forest.NAME, name)
//                        // .configure(Forest.HOST, host)
//                .configure(Forest.DATA_DIR, dataDir)
//                .configure(Forest.LARGE_DATA_DIR, largeDataDir)
//                .configure(Forest.FAST_DATA_DIR, fastDataDir)
//                .configure(Forest.UPDATES_ALLOWED, updatesAllowed)
//                .configure(Forest.REBALANCER_ENABLED, true)//rebalancerEnabled)
//                .configure(Forest.FAILOVER_ENABLED, true)//failoverEnabled)
//        );
//
//        getDriver().createForest(forest);
    }
}
