package io.cloudsoft.marklogic.dto;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class ForestCountsTest extends MarshallingTest {

    @Test
    public void testUnmarshalForestCounts() {
        ForestCounts counts = unmarshalFile("json/forest-counts.json", ForestCounts.class);
        assertEquals(counts.getActiveFragmentCount(), 9);
        assertEquals(counts.getDeletedFragmentCount(), 29);
        assertEquals(counts.getDirectoryCount(), 40);
        assertEquals(counts.getDocumentCount(), 100);
        assertEquals(counts.getNascentFragmentCount(), 20);
    }

}
