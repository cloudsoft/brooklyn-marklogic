package io.cloudsoft.marklogic.groups;

import brooklyn.entity.Entity;
import brooklyn.entity.group.DynamicClusterImpl;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.DependentConfiguration;
import brooklyn.policy.ha.MemberFailureDetectionPolicy;
import brooklyn.util.MutableMap;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import io.cloudsoft.marklogic.nodes.NodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * TODO:
 * Currently the MarkLogicGroupImpl doesn't create the actual Groups. The 2 groups to create (E-Nodes, D-Nodes)
 * are created from the driver while configuring the initial host. This needs to be moved to the MarkLogicGroup so that
 * many groups can be created, not only the ones hardcoded in the driver.
 */
public class MarkLogicGroupImpl extends DynamicClusterImpl implements MarkLogicGroup {

    private static final Logger LOG = LoggerFactory.getLogger(MarkLogicGroupImpl.class);
    private MemberFailureDetectionPolicy policy;

    @Override
    public void init() {
        super.init();

        policy = new MemberFailureDetectionPolicy();
        addPolicy(policy);
    }

    public boolean isUp() {
        return getAttribute(SERVICE_UP);
    }

    /**
     * When creating the node, if it's the first node then set and record it as the master.
     * For subsequent nodes, configure its INITIAL_HOST_ADDRESS using {@link DependentConfiguration#attributeWhenReady(Entity, AttributeSensor)}
     * so that those nodes will block their launch until the master node's hostname is available.
     */
    @Override
    protected Entity createNode(Map flags) {
        return addChild(BasicEntitySpec.newInstance(MarkLogicNode.class)
                .configure(MarkLogicNode.GROUP, getGroupName())
                .configure(MarkLogicNode.CLUSTER, getConfig(MarkLogicGroup.CLUSTER)));
    }

    @Override
    public MarkLogicNode getAnyStartedMember() {
        for (Entity member : getMembers()) {
            if (member instanceof MarkLogicNode) {
                MarkLogicNode node = (MarkLogicNode) member;
                if (node.isUp()) {
                    return node;
                }
            }
        }

        return null;
    }


    @Override
    public MarkLogicNode getAnyOtherStartedMember(String... hostNames) {
        for (Entity member : getMembers()) {
            if (member instanceof MarkLogicNode) {
                MarkLogicNode node = (MarkLogicNode) member;
                if (node.isUp()) {
                    boolean excluded = false;
                    for (String hostName : hostNames) {
                        if (hostName.endsWith(node.getHostName())) {
                            excluded = true;
                            break;
                        }

                    }

                    if (!excluded) {
                        return node;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public String getGroupName() {
        return getConfig(GROUP_NAME);
    }

    @Override
    public NodeType getNodeType() {
        return getConfig(NODE_TYPE);
    }
}
