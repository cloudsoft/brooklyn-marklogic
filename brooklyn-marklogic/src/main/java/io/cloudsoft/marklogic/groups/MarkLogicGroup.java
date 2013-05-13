package io.cloudsoft.marklogic.groups;

import brooklyn.config.ConfigKey;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;
import io.cloudsoft.marklogic.clusters.MarkLogicCluster;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import io.cloudsoft.marklogic.nodes.NodeType;

/**
 * Creates a cluster of MarkLogic nodes
 */
@ImplementedBy(MarkLogicGroupImpl.class)
public interface MarkLogicGroup extends DynamicCluster {

    @SetFromFlag("name")
    ConfigKey<String> GROUP_NAME = new BasicConfigKey<String>(
            String.class, "marklogic.group.name",
            "The name of the database", "Default");

    @SetFromFlag("name")
    ConfigKey<NodeType> NODE_TYPE = new BasicConfigKey<NodeType>(
            NodeType.class, "marklogic.group.type",
            "The type of nodes in this group", NodeType.E_D_NODE);


    @SetFromFlag("cluster")
    ConfigKey<MarkLogicCluster> CLUSTER = new BasicConfigKey<MarkLogicCluster>(
            MarkLogicCluster.class, "marklogic.group.cluster",
            "The cluster this group belongs to", null);

    String getGroupName();

    NodeType getNodeType();

    boolean isUp();

    MarkLogicNode getAnyStartedMember();
}
