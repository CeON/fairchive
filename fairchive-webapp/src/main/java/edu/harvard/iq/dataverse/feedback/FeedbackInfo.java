package edu.harvard.iq.dataverse.feedback;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.persistence.DvObject;

import java.util.Objects;

import javax.mail.internet.InternetAddress;

/**
 * Context information for feedback provided by user.
 * @param <T> type of the resource getting the feedback
 */
public class FeedbackInfo<T extends DvObject> {
    private T feedbackTarget;
    private FeedbackRecipient recipient;
    private String userEmail;
    private String systemEmail;
    private String messageSubject;
    private String userMessage;
    private String dataverseSiteUrl;
    private String installationBrandName;
    private String supportTeamName;

    // -------------------- GETTERS --------------------

    public T getFeedbackTarget() {
        return feedbackTarget;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getSystemEmail() {
        return systemEmail;
    }

    public String getMessageSubject() {
        return messageSubject;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getDataverseSiteUrl() {
        return dataverseSiteUrl;
    }

    public String getInstallationBrandName() {
        return installationBrandName;
    }

    public String getSupportTeamName() {
        return supportTeamName;
    }

    public FeedbackRecipient getRecipient() {
        return recipient;
    }

    // -------------------- LOGIC --------------------

    public FeedbackInfo<T> withFeedbackTarget(T feedbackTarget) {
        this.feedbackTarget = feedbackTarget;
        return this;
    }

    public FeedbackInfo<T> withRecipient(FeedbackRecipient recipient) {
        this.recipient = recipient;
        return this;
    }

    public FeedbackInfo<T> withUserEmail(DataverseSession dataverseSession, String fallbackEmail) {
        if (isLoggedIn(dataverseSession)) {
            this.userEmail = loggedInUserEmail(dataverseSession);
        } else {
            this.userEmail = fallbackEmail;
        }
        return this;
    }

    public FeedbackInfo<T> withUserEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    public FeedbackInfo<T> withSystemEmail(InternetAddress systemAddress) {
        this.systemEmail = systemAddress.getAddress();
        return this;
    }

    public FeedbackInfo<T> withMessageSubject(String messageSubject) {
        this.messageSubject = messageSubject;
        return this;
    }

    public FeedbackInfo<T> withUserMessage(String userMessage) {
        this.userMessage = userMessage;
        return this;
    }

    public FeedbackInfo<T> withDataverseSiteUrl(String dataverseSiteUrl) {
        this.dataverseSiteUrl = dataverseSiteUrl;
        return this;
    }

    public FeedbackInfo<T> withInstallationBrandName(String installationBrandName) {
        this.installationBrandName = installationBrandName;
        return this;
    }

    public FeedbackInfo<T> withSupportTeamName(String supportTeamName) {
        this.supportTeamName = supportTeamName;
        return this;
    }

    // -------------------- PRIVATE --------------------

    private static boolean isLoggedIn(DataverseSession dataverseSession) {
        if (dataverseSession != null) {
            return dataverseSession.isUserLoggedIn();
        }
        return false;
    }

    private static String loggedInUserEmail(DataverseSession dataverseSession) {
        return dataverseSession.getUser().getDisplayInfo().getEmailAddress();
    }


    public void setFeedbackTarget(T feedbackTarget) {
        this.feedbackTarget = feedbackTarget;
    }

    public void setRecipient(FeedbackRecipient recipient) {
        this.recipient = recipient;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public void setSystemEmail(String systemEmail) {
        this.systemEmail = systemEmail;
    }

    public void setMessageSubject(String messageSubject) {
        this.messageSubject = messageSubject;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public void setDataverseSiteUrl(String dataverseSiteUrl) {
        this.dataverseSiteUrl = dataverseSiteUrl;
    }

    public void setInstallationBrandName(String installationBrandName) {
        this.installationBrandName = installationBrandName;
    }

    public void setSupportTeamName(String supportTeamName) {
        this.supportTeamName = supportTeamName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataverseSiteUrl, feedbackTarget, installationBrandName, messageSubject, recipient,
                supportTeamName, systemEmail, userEmail, userMessage);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FeedbackInfo<?> other = (FeedbackInfo<?>) obj;
        return Objects.equals(dataverseSiteUrl, other.dataverseSiteUrl)
                && Objects.equals(feedbackTarget, other.feedbackTarget)
                && Objects.equals(installationBrandName, other.installationBrandName)
                && Objects.equals(messageSubject, other.messageSubject)
                && recipient == other.recipient
                && Objects.equals(supportTeamName, other.supportTeamName)
                && Objects.equals(systemEmail, other.systemEmail)
                && Objects.equals(userEmail, other.userEmail)
                && Objects.equals(userMessage, other.userMessage);
    }

    @Override
    public String toString() {
        return "FeedbackInfo [feedbackTarget=" + feedbackTarget + ", recipient=" + recipient + ", userEmail="
                + userEmail + ", systemEmail=" + systemEmail + ", messageSubject=" + messageSubject + ", userMessage="
                + userMessage + ", dataverseSiteUrl=" + dataverseSiteUrl + ", installationBrandName="
                + installationBrandName + ", supportTeamName=" + supportTeamName + "]";
    }
}
