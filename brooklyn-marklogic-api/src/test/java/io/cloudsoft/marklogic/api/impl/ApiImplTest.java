package io.cloudsoft.marklogic.api.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.entity.ContentType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

public abstract class ApiImplTest {

    protected MockWebServer server;
    protected String baseUrl;
    protected AdminApiImpl adminApi;
    protected ForestApiImpl forestApi;

    @BeforeMethod(alwaysRun=true)
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.play();
        baseUrl = "http://" + server.getHostName();
        adminApi = new MarkLogicApiImpl(baseUrl, "username", "password").getAdminApi(server.getPort());
        forestApi = new MarkLogicApiImpl(baseUrl, "username", "password").getForestApi(server.getPort());
    }

    @AfterMethod(alwaysRun=true)
    public void tearDown() throws Exception {
        if (server != null) server.shutdown();
    }

    protected void enqueueResponseWithFileAsBody(String filename, int responseCode) {
        try {
            MockResponse response = new MockResponse().setResponseCode(responseCode);
            response.addHeader("content-type: " + ContentType.APPLICATION_JSON.getMimeType());
            InputStream resource = Resources.getResource(filename).openStream();
            response.setBody(ByteStreams.toByteArray(resource));
            resource.close();
            server.enqueue(response);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
