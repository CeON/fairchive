package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.common.BrandingUtil.getContactHeader;
import static edu.harvard.iq.dataverse.common.BrandingUtil.getInstallationBrandName;
import static edu.harvard.iq.dataverse.common.BrandingUtil.getSupportTeamName;
import static edu.harvard.iq.dataverse.common.BundleUtil.getCurrentLocale;
import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundleWithLocale;
import static edu.harvard.iq.dataverse.feedback.FeedbackRecipient.SYSTEM_SUPPORT;
import static edu.harvard.iq.dataverse.feedback.FeedbackRecipient.defaultRecipientFor;
import static edu.harvard.iq.dataverse.feedback.FeedbackRecipient.possibleRecipientsFor;
import static edu.harvard.iq.dataverse.feedback.FeedbackUtil.gatherFeedback;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SystemEmail;
import static edu.harvard.iq.dataverse.util.MailUtil.parseSystemAddress;
import static javax.faces.application.FacesMessage.SEVERITY_ERROR;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Stream;

import javax.activation.DataSource;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.internet.InternetAddress;

import org.apache.commons.validator.routines.EmailValidator;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import edu.harvard.iq.dataverse.feedback.Feedback;
import edu.harvard.iq.dataverse.feedback.FeedbackInfo;
import edu.harvard.iq.dataverse.feedback.FeedbackRecipient;
import edu.harvard.iq.dataverse.mail.MailService;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.UIMessages;

@SuppressWarnings("serial")
@ViewScoped
@Named
public class SendFeedbackDialog implements java.io.Serializable {

    private final static long MAX_ATTACHMENTS_SIZE_IN_BYTES = 10000000;

    private MailService mailService;
    private SettingsServiceBean sessings;
    private DataverseDao dataverseDao;
    private SystemConfig config;
    private DataverseSession session;
    private UIMessages uiMessages;

    private String userEmail = "";
    private String userMessage = "";
    private String messageSubject = "";
    private Integer op1;
    private Integer op2;
    private Integer userSum;
    private boolean sendCopy = false;

    /**
     * Either the dataverse or the dataset that the message is pertaining to. If
     * there is no target, the feedback message is about the repo as a whole.
     */
    private DvObject feedbackTarget = null;
    /** :SystemEmail (the main support address for an installation). */
    private InternetAddress systemAddress;

    private FeedbackRecipient recipientOption;
    private String rootDataverseName;
    private List<UploadedFile> attachments = new ArrayList<>();

    @Inject
    public SendFeedbackDialog(final MailService mailService,
            final SettingsServiceBean settings,
            final DataverseDao dataverseDao, SystemConfig config,
            final DataverseSession session, final UIMessages uiMessages) {
        this.mailService = mailService;
        this.sessings = settings;
        this.dataverseDao = dataverseDao;
        this.config = config;
        this.session = session;
        this.uiMessages = uiMessages;
    }

    @PostConstruct
    public void init() {
        reset(null);
        this.systemAddress = parseSystemAddress(
                this.sessings.getValueForKey(SystemEmail));
        this.rootDataverseName = this.dataverseDao.findRootDataverse().getName();
    }

    public void reset(final DvObject object) {
        this.userEmail = "";
        this.userMessage = "";
        this.messageSubject = "";
        this.sendCopy = false;
        this.userSum = null;
        final Random random = new Random();
        this.op1 = random.nextInt(10);
        this.op2 = random.nextInt(10);
        this.feedbackTarget = object;
        this.recipientOption = defaultRecipientFor(object);
    }

    public String getUserEmail() {
        return this.userEmail;
    }

    public void setUserEmail(String uEmail) {
        userEmail = uEmail;
    }

    public Integer getOp1() {
        return this.op1;
    }

    public void setOp1(final Integer op1) {
        this.op1 = op1;
    }

    public Integer getOp2() {
        return this.op2;
    }

    public void setOp2(final Integer op2) {
        this.op2 = op2;
    }

    public Integer getUserSum() {
        return this.userSum;
    }

    public void setUserSum(final Integer userSum) {
        this.userSum = userSum;
    }

    public String getUserMessage() {
        return this.userMessage;
    }

    public void setUserMessage(final String userMessage) {
        this.userMessage = userMessage;
    }

    public String getMessageSubject() {
        return this.messageSubject;
    }

    public void setMessageSubject(final String messageSubject) {
        this.messageSubject = messageSubject;
    }

    public boolean getSendCopy() {
        return this.sendCopy;
    }

    public void setSendCopy(final boolean sendCopy) {
        this.sendCopy = sendCopy;
    }

    public FeedbackRecipient getRecipientOption() {
        return this.recipientOption;
    }

    public void setRecipientOption(final FeedbackRecipient recipientOption) {
        this.recipientOption = recipientOption;
    }

    public List<FeedbackRecipient> getRecipientOptions() {
        return possibleRecipientsFor(this.feedbackTarget);
    }

    public boolean displayOnlyOneRecipient() {
        return getRecipientOptions().size() == 1;
    }

    public boolean isLoggedIn() {
        return this.session.isUserLoggedIn();
    }

    public String loggedInUserEmail() {
        return this.session.getUserEmailAddress();
    }

    public String getRecipientOptionLabel(final FeedbackRecipient option) {
        return getStringFromBundle("contact.".concat(option.name()),
                this.dataverseDao.findRootDataverse().getName());
    }

    public String getSystemRecipientLabel() {
        return getStringFromBundle(SYSTEM_SUPPORT.name(),
                this.dataverseDao.findRootDataverse().getName());
    }

    public String getFormHeader() {
        if (this.feedbackTarget == null) {
            return getContactHeader();
        } else {
            return getStringFromBundle(this.feedbackTarget.isInstanceofDataverse()
                    ? "contact.dataverse.header"
                    : "contact.dataset.header");
        }
    }

    public List<UploadedFile> getAttachments() {
        return this.attachments;
    }

    private Stream<DataSource> getAttachmentDataSources() {
        return this.attachments.stream().map(UploadedFileDataSource::new);
    }

    public void handleFileUpload(final FileUploadEvent event) {
        this.attachments.add(event.getFile());
    }

    public boolean displayFileUpload() {
        return this.session.isUserLoggedIn();
    }

    public void validateUserSum(final FacesContext context,
            final UIComponent component,
            final Object value) throws ValidatorException {

        if (this.op1 + this.op2 != (Long) value) {
            throwValidatonError("contact.sum.invalid");
        }
    }

    public void validateUserEmail(final FacesContext context,
            final UIComponent component,
            final Object value) throws ValidatorException {

        if (!EmailValidator.getInstance().isValid((String) value)) {
            throwValidatonError("external.newAccount.emailInvalid");
        }
    }

    private static void throwValidatonError(final String key) {
        final FacesMessage msg = new FacesMessage(getStringFromBundle(key));
        msg.setSeverity(SEVERITY_ERROR);
        throw new ValidatorException(msg);
    }

    private boolean combinedAttachmensSizeBelowThreshold() {
        return this.attachments.stream().mapToLong(UploadedFile::getSize)
                .sum() < MAX_ATTACHMENTS_SIZE_IN_BYTES;
    }

    public void sendMessage() {

        if (combinedAttachmensSizeBelowThreshold()) {
            final String installationBrandName = getInstallationBrandName(
                    this.rootDataverseName);
            final String supportTeamName = getSupportTeamName(this.systemAddress,
                    this.rootDataverseName);
            final List<Feedback> feedbacks = gatherFeedback(new FeedbackInfo<>()
                    .withFeedbackTarget(this.feedbackTarget)
                    .withRecipient(this.recipientOption)
                    .withUserEmail(this.session, this.userEmail)
                    .withSystemEmail(this.systemAddress)
                    .withMessageSubject(this.messageSubject)
                    .withUserMessage(this.userMessage)
                    .withDataverseSiteUrl(this.config.getDataverseSiteUrl())
                    .withInstallationBrandName(installationBrandName)
                    .withSupportTeamName(supportTeamName));
            if (feedbacks.isEmpty()) {
                this.uiMessages
                        .addErrorMessage(
                                getStringFromBundle("contact.send.failure"));
            }
            for (final Feedback feedback : feedbacks) {
                this.mailService.sendMailAsync(feedback.getFromEmail(),
                        feedback.getToEmail(),
                        feedback.getSubject(), feedback.getBody(),
                        getAttachmentDataSources());
            }
            if (this.sendCopy) {
                sendCopy(feedbacks.get(0));
            }
            this.uiMessages
                    .addSuccessMessage(getStringFromBundle("contact.send.success"));
        } else {
            this.attachments.clear();
            this.uiMessages.addErrorMessage(
                    getStringFromBundle("contact.attachments.maxSizeExceeded"));
        }
    }

    private void sendCopy(final Feedback feedback) {
        final Locale locale = this.session.getUserLocaleOr(getCurrentLocale());
        final String content = prepareHeader(locale)
                + getStringFromBundleWithLocale("contact.copy.message.template",
                        locale, userMessage)
                + this.mailService.getFooterMailMessage(null, locale);

        final String mail = this.session.getUserEmailOr(this.userEmail);
        this.mailService.sendMailAsync(null, mail,
                getStringFromBundleWithLocale("contact.copy.message.subject", locale,
                        feedback.getSubject()),
                content, getAttachmentDataSources());
    }

    private String prepareHeader(final Locale locale) {
        if (this.feedbackTarget == null) {
            return prepareGenericHeader(locale);
        } else if (this.feedbackTarget.isInstanceofDataverse()) {
            return prepareHeaderForDataverse(locale);
        } else if (this.feedbackTarget.isInstanceofDataset()) {
            return prepareHeaderForDataset(locale);
        } else {
            return prepareHeaderForDataFile(locale);
        }
    }

    private String prepareGenericHeader(final Locale locale) {
        return getStringFromBundleWithLocale("contact.copy.message.header.general",
                locale, this.rootDataverseName);
    }

    private String prepareHeaderForDataverse(final Locale locale) {
        final String siteUrl = this.config.getDataverseSiteUrl();
        final Dataverse dataverse = (Dataverse) this.feedbackTarget;
        final String url = siteUrl + "/dataverse/" + dataverse.getAlias();
        return getStringFromBundleWithLocale("contact.copy.message.header.dataverse",
                locale, this.rootDataverseName, dataverse.getName(), url);
    }

    private String prepareHeaderForDataset(final Locale locale) {
        final String siteUrl = this.config.getDataverseSiteUrl();
        final Dataset dataset = (Dataset) this.feedbackTarget;
        final String url = siteUrl + "/dataset.xhtml?persistentId="
                + dataset.getGlobalId().asString();
        return getStringFromBundleWithLocale("contact.copy.message.header.dataset",
                locale, this.rootDataverseName, dataset.getDisplayName(), url);
    }

    private String prepareHeaderForDataFile(final Locale locale) {
        final String siteUrl = this.config.getDataverseSiteUrl();
        final DataFile datafile = (DataFile) this.feedbackTarget;
        final String url = siteUrl + "/dataset.xhtml?persistentId="
                + datafile.getFileMetadata().getDatasetVersion().getDataset()
                        .getGlobalId().asString();
        return getStringFromBundleWithLocale("contact.copy.message.header.dataset",
                locale, this.rootDataverseName, datafile.getDisplayName(), url);
    }

    // --------------------------------------------------------------------------
    private final static class UploadedFileDataSource implements DataSource {

        private final UploadedFile uploadedFile;

        public UploadedFileDataSource(final UploadedFile uploadedFile) {
            this.uploadedFile = uploadedFile;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return this.uploadedFile.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new IOException("Writing to uploaded file not supported.");
        }

        @Override
        public String getContentType() {
            return this.uploadedFile.getContentType();
        }

        @Override
        public String getName() {
            return this.uploadedFile.getFileName();
        }
    }
}