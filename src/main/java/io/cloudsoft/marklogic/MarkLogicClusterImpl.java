package io.cloudsoft.marklogic;

import static brooklyn.event.basic.DependentConfiguration.attributeWhenReady;

import java.util.Map;

import brooklyn.entity.Entity;
import brooklyn.entity.group.DynamicClusterImpl;
import brooklyn.entity.proxying.BasicEntitySpec;

public class MarkLogicClusterImpl extends DynamicClusterImpl implements MarkLogicCluster {

	private final Object mutex = new Object();
	
	@Override
    protected Entity createNode(Map flags) {
    	synchronized (mutex) {
    		MarkLogicNode master = getMaster();
    		MarkLogicNode result = getEntityManager().createEntity(BasicEntitySpec.newInstance(MarkLogicNode.class)
            		.parent(this)
            		.configure(MarkLogicNode.IS_MASTER, master == null)
            		.configure(MarkLogicNode.MASTER_ADDRESS, master == null ? null : attributeWhenReady(master, MarkLogicNode.HOSTNAME)));
            if (master == null) {
            	setAttribute(MASTER_NODE, result);
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
}
