package io.cloudsoft.marklogic;

/**
 * specifies which operations are allowed on this forest
 */
public enum UpdatesAllowed {
    //All specifies that read, insert, update, and delete operations are allowed on the forest.
    ALL,

    // delete-only specifies that read and delete operations are allowed on the forest, but insert and update
    // operations are not allowed. Updates to delete-only forest that specify the forest ID of one or more forests
    // that allow updates will result in the document being moved to one of the specified forests.
    DELETE_ONLY,

    //read-only specifies that read operations are allowed on the forest,but insert,update,and delete operations
    // are not allowed.A transaction attempting to make changes to fragments in the forest will throw an exception.
    READ_ONLY,

    //flash-backup puts the forest in read-only mode without throwing exceptions on insert,update,or delete transactions,
    //allowing the transactions to retry.
    FLAS_BACKUPS
}


