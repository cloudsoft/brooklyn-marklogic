package io.cloudsoft.marklogic.groups;

import brooklyn.entity.Entity;
import brooklyn.entity.group.DynamicClusterImpl;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.DependentConfiguration;
import io.cloudsoft.marklogic.MarkLogicNode;
import io.cloudsoft.marklogic.NodeType;

import java.util.Map;

import static brooklyn.event.basic.DependentConfiguration.attributeWhenReady;

public class MarkLogicGroupImpl extends DynamicClusterImpl implements MarkLogicGroup {

    private final Object mutex = new Object();

    /**
     * When creating the node, if it's the first node then set and record it as the master.
     * For subsequent nodes, configure its MASTER_ADDRESS using {@link DependentConfiguration#attributeWhenReady(Entity, AttributeSensor)}
     * so that those nodes will block their launch until the master node's hostname is available.
     */
    @Override
    protected Entity createNode(Map flags) {
        synchronized (mutex) {
            MarkLogicNode master = getMaster();
            boolean isMaster = master == null;

            MarkLogicNode result;
            if (isMaster) {
                result = addChild(BasicEntitySpec.newInstance(MarkLogicNode.class)
                        .configure(MarkLogicNode.IS_MASTER, true));
                setAttribute(MASTER_NODE, result);
            } else {
                result = addChild((BasicEntitySpec.newInstance(MarkLogicNode.class)
                        .configure(MarkLogicNode.IS_MASTER, false)
                        .configure(MarkLogicNode.MASTER_ADDRESS,  attributeWhenReady(master, MarkLogicNode.HOSTNAME))
                                //very nasty hack to wait for the service up from the master to start a client; we need to master to be fully started before we proceed with the client.
                        .configure(MarkLogicNode.IS_BACKUP_EBS, attributeWhenReady(master, MarkLogicNode.SERVICE_UP))));
            }
            return result;
        }
    }

    /**
     * Returns the master, or null if the cluster does not have a master.
     */
    private MarkLogicNode getMaster() {
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
