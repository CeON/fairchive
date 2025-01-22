package edu.harvard.iq.dataverse.feedback;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.util.List;

import edu.harvard.iq.dataverse.persistence.DvObject;

/**
 * Recipient options available to send feedback to.
 */
public enum FeedbackRecipient {
    SYSTEM_SUPPORT, DATAVERSE_CONTACT, DATASET_CONTACT;
    

    private final static List<FeedbackRecipient> system = unmodifiableList(
            asList(SYSTEM_SUPPORT));
    private final static List<FeedbackRecipient> dataverse = unmodifiableList(
            asList(SYSTEM_SUPPORT, DATAVERSE_CONTACT));
    private final static List<FeedbackRecipient> dataset = unmodifiableList(
            asList(SYSTEM_SUPPORT, DATAVERSE_CONTACT, DATASET_CONTACT));
    
    public static List<FeedbackRecipient> possibleRecipientsFor(
            final DvObject object) {
        if (object == null) {
            return system;
        } else if (object.isInstanceofDataverse()) {
            return dataverse;
        } else {
            return dataset;
        }
    }
    
    public static FeedbackRecipient defaultRecipientFor(final DvObject object) {
        if (object == null) {
            return SYSTEM_SUPPORT;
        } else if (object.isInstanceofDataverse()) {
            return DATAVERSE_CONTACT;
        } else {
            return DATASET_CONTACT;
        }
    }
}
