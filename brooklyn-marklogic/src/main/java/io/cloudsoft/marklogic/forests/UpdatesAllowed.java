package io.cloudsoft.marklogic.forests;

import static java.lang.String.format;

/**
 * specifies which operations are allowed on this forest
 */
public enum UpdatesAllowed {
    //All specifies that read, insert, update, and delete operations are allowed on the forest.
    ALL("all"),

    // delete-only specifies that read and delete operations are allowed on the forest, but insert and update
    // operations are not allowed. Updates to delete-only forest that specify the forest ID of one or more forests
    // that allow updates will result in the document being moved to one of the specified forests.
    DELETE_ONLY("delete-only"),

    //read-only specifies that read operations are allowed on the forest,but insert,update,and delete operations
    // are not allowed.A transaction attempting to make changes to fragments in the forest will throw an exception.
    READ_ONLY("read-only"),

    //flash-backup puts the forest in read-only mode without throwing exceptions on insert,update,or delete transactions,
    //allowing the transactions to retry.
    FLASH_BACKUPS("flash-backup");

    private String name;

    private UpdatesAllowed(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static UpdatesAllowed get(String s) {
        if (ALL.name.equals(s)) {
            return ALL;
        }

        if (DELETE_ONLY.name.equals(s)) {
            return DELETE_ONLY;
        }

        if (READ_ONLY.name.equals(s)) {
            return READ_ONLY;
        }

        if (FLASH_BACKUPS.name.equals(s)) {
            return FLASH_BACKUPS;
        }

        throw new IllegalArgumentException(
                format("UpdatesAllowed value: '%s' should be one of %s,%s,%s,%s", s, ALL.name, DELETE_ONLY.name, READ_ONLY.name, FLASH_BACKUPS.name));
    }
}


