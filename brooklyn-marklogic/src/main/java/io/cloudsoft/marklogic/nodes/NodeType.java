package io.cloudsoft.marklogic.nodes;

public enum NodeType {

    /**
     * Evaluator nodes evaluate XQuery programs, XDBC requests, WebDAV requests and other server requests.
     * If requests need forest data the e-node will communicate with one or more {@link #D_NODE d-nodes}.
     */
    E_NODE,

    /**
     * Data nodes store content in {@link io.cloudsoft.marklogic.forests.Forest forests}. They are responsible
     * for maintaining data (transactional integrity, optimisation, index maintenance) and service
     * {@link #E_NODE e-nodes} with content.
     */
    D_NODE,

    /**
     * Joint {@link #E_NODE e-} and {@link #D_NODE d-nodes}. They will contain a combination of appservers and
     * forests. In single host configurations the host will always be both an e-node and a d-node. Shared duties
     * are also possible in clusters, though in general they will be split.
     */
    E_D_NODE
}
