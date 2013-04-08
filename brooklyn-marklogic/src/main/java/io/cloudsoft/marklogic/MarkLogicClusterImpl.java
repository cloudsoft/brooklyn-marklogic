package io.cloudsoft.marklogic;

import static brooklyn.event.basic.DependentConfiguration.attributeWhenReady;

import java.util.Map;

import brooklyn.entity.Entity;
import brooklyn.entity.group.DynamicClusterImpl;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.DependentConfiguration;

public class MarkLogicClusterImpl extends DynamicClusterImpl implements MarkLogicCluster {

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

            MarkLogicNode result = getEntityManager().createEntity(BasicEntitySpec.newInstance(MarkLogicNode.class)
                    .parent(this)
                    .configure(MarkLogicNode.IS_MASTER, isMaster)
                    .configure(MarkLogicNode.MASTER_ADDRESS, isMaster ? null : attributeWhenReady(master, MarkLogicNode.HOSTNAME))
                    //very nasty hack to wait for the service up from the master to start a client; we need to master to be fully started before we proceed with the client.
                    .configure(MarkLogicNode.IS_BACKUP_EBS, isMaster ? null : attributeWhenReady(master, MarkLogicNode.SERVICE_UP)));
             if (isMaster) {
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
