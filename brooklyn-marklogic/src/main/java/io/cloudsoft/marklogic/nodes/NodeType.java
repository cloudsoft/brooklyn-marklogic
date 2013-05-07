package io.cloudsoft.marklogic.nodes;

public enum NodeType {
    //will only contain appservers
    E_NODE,
    //will only contain forests
    D_NODE,
    //can contain a combination of forests and appservers
    E_D_NODE
}
