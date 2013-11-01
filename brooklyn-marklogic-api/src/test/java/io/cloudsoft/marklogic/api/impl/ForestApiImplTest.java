package io.cloudsoft.marklogic.api.impl;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.mockwebserver.RecordedRequest;

import io.cloudsoft.marklogic.dto.ForestCounts;

public class ForestApiImplTest extends ApiImplTest {

    @Test
    public void testGetForestCounts() throws InterruptedException {
        enqueueResponseWithFileAsBody("json/forest-counts.json", 200);
        ForestCounts counts = forestApi.getForestCounts("irrelevant");
        assertEquals(counts.getActiveFragmentCount(), 9);
        RecordedRequest request = server.takeRequest();
        assertEquals(request.getPath(), "/manage/v2/forests/irrelevant?view=counts");
        assertEquals(request.getMethod(), "GET");
        assertEquals(server.getRequestCount(), 1);
    }

    @Test(groups = {"localhost"})
    public void testGetForestCountsFromLocalhost() {
        MarkLogicApiImpl local = new MarkLogicApiImpl("http://localhost", "admin", "password");
        System.out.println(local.getForestApi().getForestCounts("Documents"));
    }

}
