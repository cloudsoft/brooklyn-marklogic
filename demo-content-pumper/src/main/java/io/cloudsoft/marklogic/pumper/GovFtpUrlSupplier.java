package io.cloudsoft.marklogic.pumper;

import java.util.Collection;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

/**
 * Supplies data from the official US government FTP site.
 */
public class GovFtpUrlSupplier implements Supplier<Collection<String>> {

    private static final String DATA_FTP_ROOT = "ftp://ftp.ncbi.nlm.nih.gov/pubchem/Compound/CURRENT-Full/XML/";

    @Override
    public Collection<String> get() {
        return ImmutableList.of(
                DATA_FTP_ROOT + "Compound_000000001_000025000.xml.gz",
                DATA_FTP_ROOT + "Compound_000025001_000050000.xml.gz");
    }

}
