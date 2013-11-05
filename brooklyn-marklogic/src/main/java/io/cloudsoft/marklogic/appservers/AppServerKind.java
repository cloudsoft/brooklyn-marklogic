package io.cloudsoft.marklogic.appservers;

public enum AppServerKind {
    HTTP,
    WEBDAV,
    XDBC,
    ODBC,
    UNRECOGNIZED;

    public static AppServerKind fromValue(String in) {
        try {
            return valueOf(in.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return UNRECOGNIZED;
        }
    }
}
