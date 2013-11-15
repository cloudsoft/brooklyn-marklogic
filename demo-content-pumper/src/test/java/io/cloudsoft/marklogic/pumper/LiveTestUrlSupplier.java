package io.cloudsoft.marklogic.pumper;

import java.net.URL;
import java.util.Collection;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;

/**
 * Supplies data from src/test/resources.
 */
public class LiveTestUrlSupplier implements Supplier<Collection<String>> {

    @Override
    public Collection<String> get() {
        URL uri = Resources.getResource(LiveTestUrlSupplier.class, "test-compound.xml.gz");
        return ImmutableList.of("file://" + uri.getFile());
    }

}
