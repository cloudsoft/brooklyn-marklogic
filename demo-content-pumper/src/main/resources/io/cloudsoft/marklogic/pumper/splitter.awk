#!/usr/bin/awk -f
# Splits the given pharma XML file into many files, with fifty <PC-Compound> elements each.
BEGIN {
    fileno = 0;
    docCount = 0;
    docsPerFile = 50;
    printf "<PC-Compounds>\n" >> (filename"-"fileno)
}
{
# NR == number of records; line number.
# Lines 1 to 6 are the XML header, root element and namespace declarations.
    if (NR > 6) {
        print $0 >> (filename"-"fileno)
    }
}
{
# One PC-Compound element = one document in a file.
    if ($0 == "  </PC-Compound>") {
        docCount++
    }
    if (docCount == docsPerFile) {
# Close root element, reset docCount and zip
        printf "</PC-Compounds>\n" >> (filename"-"fileno);
        docCount = 0;
        zip(filename, fileno)
        close(filename"-"fileno)
# Start a new file
        fileno++;
        printf "<PC-Compounds>\n" >> (filename"-"fileno)
    }
}
END {
    zip(filename, fileno)
}

function zip(filename, fileno) {
# Sets `ids` to the formatted command, executes and closes the pipe
    sysstring = sprintf("zip -j %s-%d.zip %s-%d; rm %s-%d", filename, fileno, filename, fileno, filename, fileno);
    sysstring | getline ids;
    print ids;
    close(sysstring)
}
