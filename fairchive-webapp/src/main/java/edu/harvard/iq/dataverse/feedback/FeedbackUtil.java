package edu.harvard.iq.dataverse.feedback;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.logging.Logger;

public class FeedbackUtil {

    private static final Logger logger = Logger.getLogger(FeedbackUtil.class.getCanonicalName());

    private static final String NO_DATASET_CONTACT_INTRO = getStringFromBundle("contact.context.dataset.noContact");

    @SuppressWarnings("unchecked")
    public static <T extends DvObject> List<Feedback> gatherFeedback(FeedbackInfo<T> feedbackInfo) {
        if (feedbackInfo.getFeedbackTarget() == null) {
            return Lists.newArrayList(getFeedbackToRepoSupport(feedbackInfo));
        }

        feedbackInfo.withMessageSubject(getStringFromBundle("contact.context.subject.dvobject", 
                feedbackInfo.getInstallationBrandName(), feedbackInfo.getMessageSubject()));

        if (feedbackInfo.getFeedbackTarget().isInstanceofDataverse()) {
            return getFeedbacksToDataverse((FeedbackInfo<Dataverse>) feedbackInfo);
        } else if (feedbackInfo.getFeedbackTarget().isInstanceofDataset()) {
            return getFeedbacksToDataset((FeedbackInfo<Dataset>) feedbackInfo);
        } else {
            return getFeedbacksToDataFile((FeedbackInfo<DataFile>) feedbackInfo);
        }
    }

    private static List<Feedback> getFeedbacksToDataverse(FeedbackInfo<Dataverse> feedbackInfo) {
        List<FeedbackContact> dataverseContacts = resolveContactsForDataverse(feedbackInfo);

        String dataverseContextEnding = getStringFromBundle("contact.context.dataverse.ending",
                feedbackInfo.getSupportTeamName(),
                feedbackInfo.getSystemEmail(),
                feedbackInfo.getDataverseSiteUrl(),
                feedbackInfo.getFeedbackTarget().getAlias(),
                feedbackInfo.getSupportTeamName(),
                feedbackInfo.getSystemEmail());

        if (dataverseContacts.isEmpty()) {
            String dataverseContextIntroError = getStringFromBundle("contact.context.dataverse.noContact");
            return Lists.newArrayList(new Feedback(feedbackInfo.getUserEmail(), feedbackInfo.getSystemEmail(),
                    feedbackInfo.getMessageSubject(),
                    dataverseContextIntroError + feedbackInfo.getUserMessage() + dataverseContextEnding));
        }

        String placeHolderIfDataverseContactsGetNames = "";
        String dataverseContextIntro = getStringFromBundle("contact.context.dataverse.intro",
                placeHolderIfDataverseContactsGetNames,
                feedbackInfo.getUserEmail(),
                feedbackInfo.getInstallationBrandName(),
                feedbackInfo.getFeedbackTarget().getAlias());
        String body = dataverseContextIntro + feedbackInfo.getUserMessage() 
                        + dataverseContextEnding;

        return dataverseContacts.stream()
                .map(dataverseContact ->
                        new Feedback(feedbackInfo.getUserEmail(), dataverseContact.getEmail(), 
                                feedbackInfo.getMessageSubject(), body))
                .collect(toList());
    }

    private static List<Feedback> getFeedbacksToDataset(FeedbackInfo<Dataset> feedbackInfo) {
        List<FeedbackContact> recipients = resolveContactsForDataset(feedbackInfo);
        String datasetPid = feedbackInfo.getFeedbackTarget().getGlobalId().toString();

        String datasetContextEnding = getStringFromBundle("contact.context.dataset.ending",
                feedbackInfo.getSupportTeamName(),
                feedbackInfo.getSystemEmail(),
                feedbackInfo.getDataverseSiteUrl(),
                datasetPid,
                feedbackInfo.getSupportTeamName(),
                feedbackInfo.getSystemEmail());

        if (recipients.isEmpty()) {
            return Lists.newArrayList(new Feedback(feedbackInfo.getUserEmail(), 
                    feedbackInfo.getSystemEmail(), feedbackInfo.getMessageSubject(),
                    NO_DATASET_CONTACT_INTRO + feedbackInfo.getUserMessage() + datasetContextEnding));
        }

        String datasetTitle = feedbackInfo.getFeedbackTarget().getLatestVersion().getParsedTitle();

        return recipients.stream().map(datasetContact -> {
            String datasetContextIntro = getStringFromBundle("contact.context.dataset.intro",
                    getGreeting(datasetContact),
                    feedbackInfo.getUserEmail(),
                    feedbackInfo.getInstallationBrandName(),
                    datasetTitle,
                    datasetPid);
            String body = datasetContextIntro + feedbackInfo.getUserMessage()
                                + datasetContextEnding;
            return new Feedback(feedbackInfo.getUserEmail(), datasetContact.getEmail(), 
                    feedbackInfo.getMessageSubject(), body);
        }).collect(toList());
    }

    private static List<Feedback> getFeedbacksToDataFile(FeedbackInfo<DataFile> feedbackInfo) {
        List<FeedbackContact> datasetContacts = resolveContactsForDataFile(feedbackInfo);

        DataFile dataFile = feedbackInfo.getFeedbackTarget();
        String fileContextEnding = getStringFromBundle("contact.context.file.ending",
                feedbackInfo.getSupportTeamName(),
                feedbackInfo.getSystemEmail(),
                feedbackInfo.getDataverseSiteUrl(),
                dataFile.getId().toString(),
                feedbackInfo.getSupportTeamName(),
                feedbackInfo.getSystemEmail());

        if (datasetContacts.isEmpty()) {
            return Lists.newArrayList(new Feedback(feedbackInfo.getUserEmail(), 
                    feedbackInfo.getSystemEmail(), feedbackInfo.getMessageSubject(), 
                    NO_DATASET_CONTACT_INTRO + feedbackInfo.getUserMessage() + fileContextEnding));
        }

        String filename = dataFile.getFileMetadatas().get(0).getLabel();
        String datasetTitle = dataFile.getOwner().getLatestVersion().getParsedTitle();
        String datasetPid = dataFile.getOwner().getGlobalId().toString();

        return datasetContacts.stream().map(datasetContact -> {
            String fileContextIntro = getStringFromBundle("contact.context.file.intro",
                    getGreeting(datasetContact),
                    feedbackInfo.getUserEmail(),
                    feedbackInfo.getInstallationBrandName(),
                    filename,
                    datasetTitle,
                    datasetPid);
            String body = fileContextIntro + feedbackInfo.getUserMessage() 
                            + fileContextEnding;

            return new Feedback(feedbackInfo.getUserEmail(), datasetContact.getEmail(), 
                    feedbackInfo.getMessageSubject(), body);
        }).collect(toList());
    }

    private static Feedback getFeedbackToRepoSupport(FeedbackInfo<?> feedbackInfo) {
        String noDvObjectContextIntro = getStringFromBundle("contact.context.support.intro", 
                feedbackInfo.getSupportTeamName(), feedbackInfo.getUserEmail());
        String noDvObjectContextEnding = getStringFromBundle("contact.context.support.ending", "");
        return new Feedback(
                feedbackInfo.getUserEmail(),
                feedbackInfo.getSystemEmail(),
                getStringFromBundle("contact.context.subject.support", 
                        feedbackInfo.getInstallationBrandName(), feedbackInfo.getMessageSubject()),
                noDvObjectContextIntro + feedbackInfo.getUserMessage() + noDvObjectContextEnding);
    }

    public static List<FeedbackContact> resolveContactsForDataverse(FeedbackInfo<Dataverse> feedbackInfo) {
        if (feedbackInfo.getRecipient() == null || feedbackInfo.getRecipient() == FeedbackRecipient.DATAVERSE_CONTACT) {
            return FeedbackContact.fromDataverse(feedbackInfo.getFeedbackTarget());
        } else if (feedbackInfo.getRecipient() == FeedbackRecipient.SYSTEM_SUPPORT) {
            return Lists.newArrayList(new FeedbackContact(feedbackInfo.getSystemEmail()));
        }

        return emptyList();
    }

    public static List<FeedbackContact> resolveContactsForDataset(FeedbackInfo<Dataset> feedbackInfo) {
        if (feedbackInfo.getRecipient() == null || feedbackInfo.getRecipient() == FeedbackRecipient.DATASET_CONTACT) {
            return FeedbackContact.fromDataset(feedbackInfo.getFeedbackTarget());
        } else if (feedbackInfo.getRecipient() == FeedbackRecipient.DATAVERSE_CONTACT) {
            return FeedbackContact.fromDataverse(feedbackInfo.getFeedbackTarget().getOwner());
        } else if (feedbackInfo.getRecipient() == FeedbackRecipient.SYSTEM_SUPPORT) {
            return Lists.newArrayList(new FeedbackContact(feedbackInfo.getSystemEmail()));
        }

        return emptyList();
    }

    public static List<FeedbackContact> resolveContactsForDataFile(FeedbackInfo<DataFile> feedbackInfo) {
        if (feedbackInfo.getRecipient() == null || feedbackInfo.getRecipient() == FeedbackRecipient.DATASET_CONTACT) {
            return FeedbackContact.fromDataset(feedbackInfo.getFeedbackTarget().getOwner());
        } else if (feedbackInfo.getRecipient() == FeedbackRecipient.DATAVERSE_CONTACT) {
            return FeedbackContact.fromDataverse(feedbackInfo.getFeedbackTarget().getOwner().getOwner());
        } else if (feedbackInfo.getRecipient() == FeedbackRecipient.SYSTEM_SUPPORT) {
            return Lists.newArrayList(new FeedbackContact(feedbackInfo.getSystemEmail()));
        }

        return emptyList();
    }


    /**
     * When contacts are people we suggest that they be stored as "Simpson,
     * Homer" so the idea of this method is that it returns "Homer Simpson", if
     * it can.
     * <p>
     * Contacts don't necessarily have to be people, however. They can be
     * organizations. We ran into similar trouble (but for authors) when
     * implementing Schema.org JSON-LD support. See getJsonLd on DatasetVersion.
     * Some day it might be nice to store whether an author or a contact is a
     * person or an organization.
     */
    private static String getGreeting(FeedbackContact dvObjectContact) {
        logger.fine("dvObjectContact: " + dvObjectContact);
        try {
            return dvObjectContact.getName().map(name -> {
                logger.fine("dvObjectContact name: " + name);
                String[] lastFirstParts = name.split(",");
                String last = lastFirstParts[0];
                String first = lastFirstParts[1];
                return getStringFromBundle("contact.context.dataset.greeting.helloFirstLast", first.trim(), last.trim());
            }).getOrElse(() -> getStringFromBundle("contact.context.dataset.greeting.organization"));
        } catch (Exception ex) {
            logger.warning("problem in getGreeting: " + ex);
            return getStringFromBundle("contact.context.dataset.greeting.organization");
        }
    }
}
