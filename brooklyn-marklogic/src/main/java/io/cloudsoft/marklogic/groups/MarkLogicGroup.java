package io.cloudsoft.marklogic.groups;

import brooklyn.config.ConfigKey;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;
import io.cloudsoft.marklogic.clusters.MarkLogicCluster;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import io.cloudsoft.marklogic.nodes.NodeType;

import java.util.List;

import static brooklyn.entity.basic.ConfigKeys.newConfigKey;
import static brooklyn.entity.basic.ConfigKeys.newStringConfigKey;

/**
 * A group is a set of hosts with uniform HTTP, WebDAV and XDBC configurations.
 * They simplify cluster management.
 */
@ImplementedBy(MarkLogicGroupImpl.class)
public interface MarkLogicGroup extends DynamicCluster, Iterable<MarkLogicNode> {

    ConfigKey<String> GROUP_NAME = newStringConfigKey(
            "marklogic.group.name",
            "The name of the database",
            "Default");

    ConfigKey<NodeType> NODE_TYPE = newConfigKey(
            NodeType.class,
            "marklogic.group.type",
            "The type of nodes in this group",
            NodeType.E_D_NODE);

    ConfigKey<MarkLogicCluster> CLUSTER = newConfigKey(
            MarkLogicCluster.class,
            "marklogic.group.cluster",
            "The cluster this group belongs to");

    String getGroupName();

    MarkLogicCluster getCluster();

    NodeType getNodeType();

    boolean isUp();

    MarkLogicNode getAnyUpMember();

    List<MarkLogicNode> getAllUpMembers();

    MarkLogicNode getAnyOtherUpMember(String... hostName);
}
