package io.cloudsoft.marklogic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Resource {

    // TODO Should contain id, meta, name and related-views.

    @JsonProperty
    private String id;

    public String getId() {
        return id;
    }

}
