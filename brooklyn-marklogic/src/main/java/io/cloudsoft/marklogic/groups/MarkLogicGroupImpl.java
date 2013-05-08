package io.cloudsoft.marklogic.groups;

import brooklyn.entity.Entity;
import brooklyn.entity.group.DynamicClusterImpl;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.DependentConfiguration;
import io.cloudsoft.marklogic.databases.DatabasesImpl;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import io.cloudsoft.marklogic.nodes.NodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static brooklyn.event.basic.DependentConfiguration.attributeWhenReady;

/**
 *
 * TODO:
 * Currently the MarkLogicGroupImpl doesn't create the actual Groups. The 2 groups to create (E-Nodes, D-Nodes)
 * are created from the driver while configuring the master. This needs to be moved to the MarkLogicGroup so that
 * many groups can be created, not only the ones hardcoded in the driver.
 */
public class MarkLogicGroupImpl extends DynamicClusterImpl implements MarkLogicGroup {

    private static final Logger LOG = LoggerFactory.getLogger(MarkLogicGroupImpl.class);

    private final Object mutex = new Object();

    public boolean isUp(){
        return getAttribute(SERVICE_UP);
    }

    /**
     * When creating the node, if it's the first node then set and record it as the master.
     * For subsequent nodes, configure its MASTER_ADDRESS using {@link DependentConfiguration#attributeWhenReady(Entity, AttributeSensor)}
     * so that those nodes will block their launch until the master node's hostname is available.
     */
    @Override
    protected Entity createNode(Map flags) {
        //if there is a primary group set, we need to wait till it is up.
        MarkLogicGroup primaryStartupGroup = getConfig(PRIMARY_STARTUP_GROUP);
        if (primaryStartupGroup != null) {
            LOG.info("Group "+getGroupName()+" waiting for primary group "+primaryStartupGroup+" serviceUp");

            for (; ; ) {
                try {
                    if(primaryStartupGroup.isUp()){
                        break;
                    }
                    Thread.sleep(5000);
                } catch (Exception e) {
                    LOG.error("failed for cluster size wait 1", e);
                }
            }
            LOG.info("Group "+getGroupName()+" finished waiting for primary group "+primaryStartupGroup+" serviceUp");
        }

        synchronized (mutex) {
            MarkLogicNode master;
            if(primaryStartupGroup == null){
                master = getMaster();
            }else{
                master = primaryStartupGroup.getMaster();
            }
            boolean isMaster = master == null;

            MarkLogicNode result;
            if (isMaster) {
                result = addChild(BasicEntitySpec.newInstance(MarkLogicNode.class)
                        .configure(MarkLogicNode.GROUP, getGroupName())
                        .configure(MarkLogicNode.IS_MASTER, true));
                setAttribute(MASTER_NODE, result);
            } else {
                result = addChild(BasicEntitySpec.newInstance(MarkLogicNode.class)
                        .configure(MarkLogicNode.GROUP, getGroupName())
                        .configure(MarkLogicNode.IS_MASTER, false)
                        .configure(MarkLogicNode.MASTER_ADDRESS,  attributeWhenReady(master, MarkLogicNode.HOSTNAME))
                                //very nasty hack to wait for the service up from the master to start a client; we need to master to be fully started before we proceed with the client.
                        .configure(MarkLogicNode.IS_BACKUP_EBS, attributeWhenReady(master, MarkLogicNode.SERVICE_UP)));
            }
            return result;
        }
    }

    /**
     * Returns the master, or null if the cluster does not have a master.
     */
    public MarkLogicNode getMaster() {
        return getAttribute(MASTER_NODE);
    }

    @Override
    public String getGroupName() {
        return getConfig(GROUP_NAME);
    }

    @Override
    public NodeType getNodeType() {
        return getConfig(NODE_TYPE);
    }

    @Override
    public String getDisplayName() {
        return "Group:" + getGroupName();
    }
}
