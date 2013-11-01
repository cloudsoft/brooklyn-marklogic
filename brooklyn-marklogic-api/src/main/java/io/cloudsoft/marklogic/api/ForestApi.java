package io.cloudsoft.marklogic.api;

import io.cloudsoft.marklogic.dto.ForestCounts;

public interface ForestApi {

    /**
     * Loads server counts for the given forest.
     * @see <a href="http://docs-ea.marklogic.com/REST/GET/manage/v2/forests">
     *     http://docs-ea.marklogic.com/REST/GET/manage/v2/forests</a>
     */
    public ForestCounts getForestCounts(String forestName);
}
