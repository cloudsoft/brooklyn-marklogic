package io.cloudsoft.marklogic.api.impl;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Strings;

import io.cloudsoft.marklogic.api.ForestApi;
import io.cloudsoft.marklogic.client.HttpClient;
import io.cloudsoft.marklogic.client.Response;
import io.cloudsoft.marklogic.dto.ForestCounts;

public class ForestApiImpl implements ForestApi {

    private final HttpClient client;
    private final int port;

    /** Creates ForestApiImpl pointing at port 8002. */
    public ForestApiImpl(HttpClient client) {
        this(client, 8002);
    }

    public ForestApiImpl(HttpClient client, int port) {
        this.client = client;
        this.port = port;
    }

    @Override
    public ForestCounts getForestCounts(String forestName) {
        checkArgument(!Strings.isNullOrEmpty(forestName), "forestName");
        Response response = client.newRequest("/manage/v2/forests/"+forestName)
                .queryParam("view", "counts")
                .port(port)
                .get();
        return response.get(ForestCounts.class);
    }
}
