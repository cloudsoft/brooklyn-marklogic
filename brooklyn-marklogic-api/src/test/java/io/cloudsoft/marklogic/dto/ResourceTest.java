package io.cloudsoft.marklogic.dto;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class ResourceTest extends MarshallingTest {

    @Test
    public void testUnmarshalForestCounts() {
        Resource counts = unmarshalFile("json/forest-counts.json", ForestCounts.class);
        assertEquals(counts.getId(), "12339959031384385639");
    }

}
