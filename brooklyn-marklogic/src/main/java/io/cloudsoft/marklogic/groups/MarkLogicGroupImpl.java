package io.cloudsoft.marklogic.groups;

import brooklyn.entity.Entity;
import brooklyn.entity.group.DynamicClusterImpl;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.DependentConfiguration;
import brooklyn.location.Location;
import io.cloudsoft.marklogic.clusters.MarkLogicCluster;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import io.cloudsoft.marklogic.nodes.NodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

/**
 * TODO:
 * Currently the MarkLogicGroupImpl doesn't create the actual Groups. The 2 groups to create (E-Nodes, D-Nodes)
 * are created from the driver while configuring the initial host. This needs to be moved to the MarkLogicGroup so that
 * many groups can be created, not only the ones hardcoded in the driver.
 */
public class MarkLogicGroupImpl extends DynamicClusterImpl implements MarkLogicGroup {

    private static final Logger LOG = LoggerFactory.getLogger(MarkLogicGroupImpl.class);

    @Override
    public Iterator<MarkLogicNode> iterator() {
        return FluentIterable.from(getMembers())
                .filter(MarkLogicNode.class)
                .iterator();
    }

    /**
     * When creating the node, if it's the first node then set and record it as the master.
     * For subsequent nodes, configure its INITIAL_HOST_ADDRESS using {@link DependentConfiguration#attributeWhenReady(Entity, AttributeSensor)}
     * so that those nodes will block their launch until the master node's hostname is available.
     */
    @Override
    protected Entity createNode(Location location, Map flags) {
        return addChild(EntitySpec.create(MarkLogicNode.class)
                .configure(MarkLogicNode.GROUP, getGroupName())
                .configure(MarkLogicNode.NODE_TYPE, getNodeType())
                .configure(MarkLogicNode.CLUSTER, getConfig(MarkLogicGroup.CLUSTER)));
    }

    @Override
    public List<MarkLogicNode> getAllUpMembers() {
        ImmutableList.Builder<MarkLogicNode> result = ImmutableList.builder();
        for (MarkLogicNode node : this) {
            if (node.isUp()) {
                result.add(node);
            }
        }
        return result.build();
    }

    @Override
    public MarkLogicNode getAnyUpMember() {
        for (MarkLogicNode node : this) {
            if (node.isUp()) {
                return node;
            }
        }
        return null;
    }

    @Override
    public MarkLogicNode getAnyOtherUpMember(String... hostNames) {
        for (MarkLogicNode node : this) {
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

        return null;
    }

    @Override
    public MarkLogicCluster getCluster() {
        return getConfig(CLUSTER);
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
    public boolean isUp() {
        return getAttribute(SERVICE_UP);
    }

}
