package io.cloudsoft.marklogic.api.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.xml.bind.DatatypeConverter;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

public class AdminApiImplTest {

    private final String timestamp = "2013-10-04T15:11:43.825453+01:00";
    private MockWebServer server;
    private String baseUrl;
    private AdminApiImpl adminApi;

    @BeforeMethod(alwaysRun=true)
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.play();
        baseUrl = "http://" + server.getHostName();
        adminApi = new MarkLogicApiImpl(baseUrl, "username", "password").getAdminApi(server.getPort());
    }

    @AfterMethod(alwaysRun=true)
    public void tearDown() throws Exception {
        if (server != null) server.shutdown();
    }

    private void enqueueTimestampGet(boolean includeBody) {
        MockResponse response = new MockResponse().setResponseCode(200);
        if (includeBody) {
            response.addHeader("content-type: text/plain");
            response.setBody(timestamp);
        }
        server.enqueue(response);
    }

    @Test
    public void testIsServerUpFalseWhenNoServerExists() throws Exception {
        server.shutdown();
        server = null;
        assertFalse(adminApi.isServerUp(), "Expected no MarkLogic server at host");
    }

    @Test
    public void testIsServerUpTrueOn200() {
        enqueueTimestampGet(true);
        assertTrue(adminApi.isServerUp());
    }

    @Test
    public void testGetServerTimestamp() {
        enqueueTimestampGet(true);
        assertEquals(adminApi.getServerTimestamp(), 1380895903825L);
    }

}
