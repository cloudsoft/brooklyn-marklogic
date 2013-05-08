package io.cloudsoft.marklogic.groups;

import brooklyn.config.ConfigKey;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensor;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import io.cloudsoft.marklogic.nodes.NodeType;

/**
 * Creates a cluster of MarkLogic nodes (where the first node is designated the master).
 */
@ImplementedBy(MarkLogicGroupImpl.class)
public interface MarkLogicGroup extends DynamicCluster {

    //todo: we need to get rid of master. There is not really a master..
    public static final AttributeSensor<MarkLogicNode> MASTER_NODE = new BasicAttributeSensor<MarkLogicNode>(
            MarkLogicNode.class, "marklogic.cluster.masterNode", "Master node, or null if no master");

    @SetFromFlag("name")
    ConfigKey<String> GROUP_NAME = new BasicConfigKey<String>(
            String.class, "marklogic.group.name",
            "The name of the database", "Default");

    @SetFromFlag("name")
    ConfigKey<NodeType> NODE_TYPE = new BasicConfigKey<NodeType>(
            NodeType.class, "marklogic.group.type",
            "The type of nodes in this group", NodeType.E_D_NODE);


    @SetFromFlag("primary")
    ConfigKey<MarkLogicGroup> PRIMARY_STARTUP_GROUP = new BasicConfigKey<MarkLogicGroup>(
            MarkLogicGroup.class, "marklogic.group.primaryStartupGroup",
            "The MarkLogicGroup that needs to be started before this group is started", null);

    String getGroupName();

    NodeType getNodeType();

    MarkLogicNode getMaster();

    public boolean isUp();

}
