package io.cloudsoft.marklogic.api;

public interface AdminApi {

    /**
     * @return The timestamp of the last server restart.
     * @see <a href="http://docs-ea.marklogic.com/REST/GET/admin/v1/timestamp">
     *     http://docs-ea.marklogic.com/REST/GET/admin/v1/timestamp</a>
     */
    public long getServerTimestamp();

    /**
     * @return True if the server responded 200 to an is-available check.
     * @see <a href="http://docs-ea.marklogic.com/REST/HEAD/admin/v1/timestamp">
     *     http://docs-ea.marklogic.com/REST/HEAD/admin/v1/timestamp</a>
     */
    public boolean isServerUp();

    /**
     * Removes the server from its cluster, dropping it from the cluster.
     * Notes from MarkLogic documentation:
     * <ul>
     *   <li>The host must not be a bootstrap host for database replication.</li>
     *   <li>The host must not have any forests configured.</li>
     *   <li>The host must not serve as a failover host for shared disk failover.</li>
     * </ul>
     * @return True if the operation succeeded
     * @see <a href="http://docs-ea.marklogic.com/REST/DELETE/admin/v1/host-config">
     *     http://docs-ea.marklogic.com/REST/DELETE/admin/v1/host-config</a>
     */
    public boolean removeNodeFromCluster();

}
