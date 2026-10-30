package edu.harvard.iq.dataverse.notification;

public enum NotificationObjectType {

    DATAVERSE("Dataverse"),
    DATASET("Dataset"),
    DATASET_VERSION("DatasetVersion"),
    DATAFILE("DataFile"),
    AUTHENTICATED_USER("AuthenticatedUser"),
    FILEMETADATA("FileMetadata"),
    DATAVERSE_VERSION("DataVerseVersion");

    private String objectTypeDescription;

    NotificationObjectType(final String objectType) {
        this.objectTypeDescription = objectType;
    }

    public String getObjectTypeDescription() {
        return this.objectTypeDescription;
    }
}
