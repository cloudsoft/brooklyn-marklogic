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
import com.google.mockwebserver.RecordedRequest;

public class AdminApiImplTest extends ApiImplTest {

    private final String timestamp = "2013-10-04T15:11:43.825453+01:00";

    private void enqueueTimestampGet() {
        MockResponse response = new MockResponse().setResponseCode(200);
        response.addHeader("content-type: text/plain");
        response.setBody(timestamp);
        server.enqueue(response);
    }

    @Test
    public void testIsServerUpFalseWhenNoServerExists() throws Exception {
        server.shutdown();
        server = null;
        assertFalse(adminApi.isServerUp(), "Expected no MarkLogic server at host");
    }

    @Test
    public void testIsServerUpTrueOn200() throws InterruptedException {
        enqueueTimestampGet();
        assertTrue(adminApi.isServerUp());
        RecordedRequest request = server.takeRequest();
        assertEquals(request.getPath(), "/admin/v1/timestamp");
        assertEquals(request.getMethod(), "HEAD");
        assertEquals(server.getRequestCount(), 1);
    }

    @Test
    public void testGetServerTimestamp() throws InterruptedException {
        enqueueTimestampGet();
        assertEquals(adminApi.getServerTimestamp(), 1380895903825L);
        RecordedRequest request = server.takeRequest();
        assertEquals(request.getPath(), "/admin/v1/timestamp");
        assertEquals(request.getMethod(), "GET");
        assertEquals(server.getRequestCount(), 1);
    }

}
