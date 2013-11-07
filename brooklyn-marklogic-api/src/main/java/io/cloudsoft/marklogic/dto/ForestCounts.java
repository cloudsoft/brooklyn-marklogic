package io.cloudsoft.marklogic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.google.common.base.Objects;

@JsonRootName("forest-counts")
public class ForestCounts extends Resource {

    // TODO: Add stand counts
    // TODO: Better way of parsing?

    private static class Count {
        @JsonProperty
        private String units;
        @JsonProperty
        private long value;
    }
    private static class CountsProperties {
        @JsonProperty("active-fragment-count")
        private Count activeFragmentCount;
        @JsonProperty("deleted-fragment-count")
        private Count deletedFragmentCount;
        @JsonProperty("directory-count")
        private Count directoryCount;
        @JsonProperty("document-count")
        private Count documentCount;
        @JsonProperty("nascent-fragment-count")
        private Count nascentFragmentCount;

        private CountsProperties() {}
    }

    @JsonProperty("count-properties")
    private CountsProperties counts;

    private ForestCounts() {}

    public long getActiveFragmentCount() {
        if (counts == null || counts.activeFragmentCount == null)
            return -1;
        else
            return counts.activeFragmentCount.value;
    }

    public long getDeletedFragmentCount() {
        if (counts == null || counts.deletedFragmentCount == null)
            return -1;
        else
            return counts.deletedFragmentCount.value;
    }

    public long getDirectoryCount() {
        if (counts == null || counts.directoryCount == null)
            return -1;
        else
            return counts.directoryCount.value;
    }

    public long getDocumentCount() {
        if (counts == null || counts.documentCount == null)
            return -1;
        else
            return counts.documentCount.value;
    }

    public long getNascentFragmentCount() {
        if (counts == null || counts.nascentFragmentCount == null)
            return -1;
        else
            return counts.nascentFragmentCount.value;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("activeFragmentCount", getActiveFragmentCount())
                .add("deletedFragmentCount", getDeletedFragmentCount())
                .add("nascentFragmentCount", getNascentFragmentCount())
                .add("directoryCount", getDirectoryCount())
                .add("documentCount", getDocumentCount())
                .toString();
    }

}
