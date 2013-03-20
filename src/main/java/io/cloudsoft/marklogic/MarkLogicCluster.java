package io.cloudsoft.marklogic;

import brooklyn.entity.group.DynamicCluster;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensor;

/**
 * Creates a cluster of MarkLogic nodes (where the first node is designated the master).
 */
public interface MarkLogicCluster extends DynamicCluster {

	public static final AttributeSensor<MarkLogicNode> MASTER_NODE = new BasicAttributeSensor<MarkLogicNode>(
			MarkLogicNode.class, "marklogic.cluster.masterNode", "Master node, or null if no master");
}
