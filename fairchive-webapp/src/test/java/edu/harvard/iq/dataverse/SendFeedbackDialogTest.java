package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.feedback.FeedbackRecipient.DATASET_CONTACT;
import static edu.harvard.iq.dataverse.feedback.FeedbackRecipient.DATAVERSE_CONTACT;
import static edu.harvard.iq.dataverse.feedback.FeedbackRecipient.SYSTEM_SUPPORT;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SiteUrl;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SystemEmail;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.activation.DataSource;
import javax.faces.component.UIComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import org.simplejavamail.email.Email;
import org.simplejavamail.mailer.Mailer;

import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.mail.EmailContent;
import edu.harvard.iq.dataverse.mail.MailMessageCreator;
import edu.harvard.iq.dataverse.mail.MailService;
import edu.harvard.iq.dataverse.mail.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.UIMessages;

@MockitoSettings(strictness = LENIENT)
@ExtendWith(MockitoExtension.class)
public class SendFeedbackDialogTest {
    @Mock
    private SettingsServiceBean settingsService;
    @Mock
    private DataverseDao dataverseDao;
    @Mock
    private UIMessages messages;

    private MailService mailService;
    @Mock
    private ActionLogServiceBean logSvc;
    @InjectMocks
    private SystemConfig systemConfig;
    @InjectMocks
    private DataverseSession dataverseSession;
    @Mock
    private PermissionServiceBean permissionService;
    @Mock
    private ConfirmEmailServiceBean confirmEmailService;
    @Mock
    private GenericDao genericDao;
    @Mock
    private AuthenticatedUserRepository authenticatedUserRepository;
    private MailMessageCreator mailMessageCreator;
    @Mock
    private Mailer mailSender;
    @Mock
    private UIComponent component;

    private SendFeedbackDialog dialog;

    private DataverseContact contact1 = new DataverseContact();

    @Captor
    ArgumentCaptor<Email> emailCaptor;

    private final Dataverse dataverse = new Dataverse();
    private final Dataset dataset = new Dataset();
    private final DataFile dataFile = new DataFile();
    private final AuthenticatedUser authenticatedUser = new AuthenticatedUser();

    private final static String SYSTEM_EMAIL = "system@dv.com";
    private final static String GUEST_USER_EMAIL = "guest@dv.com";
    private final static String USER_EMAIL = "user1@dv.com";
    private final static String DATAVERSE_CONTACT_EMAIL = "contact1@dv.com";
    private final static String DATASET_CONTACT_EMAIL = "contact2@dv.com";
    private final static String URL = "http://dv.org";
    private final static FakeUploadedFile attachment1 = new FakeUploadedFile(
            "file1.txt", 1000000);
    private final static FakeUploadedFile attachment2 = new FakeUploadedFile(
            "file2.txt", 3000000);
    private final static FakeUploadedFile attachment3 = new FakeUploadedFile(
            "file3.txt", 8000000);

    @BeforeEach
    public void setUp() {
        setUpAuthenticatedUser();
        setUpDatavereContact();
        setUpDataverse();
        setUpDataset();
        setUpDataFile();
        setUpDataverseDao();
        setUpSettingsService();
        setUpMailMessageCreator();
        setUpMailService();
        setUpSendFeedbackDialog();
    }

    private void setUpAuthenticatedUser() {
        this.authenticatedUser.setEmail(USER_EMAIL);
    }

    private void setUpDataverse() {
        this.dataverse.setId(1L);
        this.dataverse.setName("Dataverse1");
    }

    private void setUpDataset() {
        this.dataset.setId(2L);

        final DatasetField email = new DatasetField();
        email.setDatasetFieldType(new DatasetFieldType());
        email.getDatasetFieldType()
                .setName(DatasetFieldConstant.datasetContactEmail);
        email.setValue(DATASET_CONTACT_EMAIL);

        final DatasetField contact = new DatasetField();
        contact.setDatasetFieldType(new DatasetFieldType());
        contact.getDatasetFieldType().setName(DatasetFieldConstant.datasetContact);
        contact.getChildren().add(email);

        final DatasetVersion version = new DatasetVersion();
        version.getDatasetFields().add(contact);
        version.setDataset(this.dataset);

        this.dataset.setVersions(singletonList(version));
    }

    private void setUpDataFile() {

        final FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setLabel("file.txt");
        fileMetadata.setDatasetVersion(this.dataset.getLatestVersion());
        this.dataFile.setFileMetadatas(singletonList(fileMetadata));
        this.dataFile.setOwner(this.dataset);
        this.dataFile.setId(3L);
    }

    private void setUpDatavereContact() {
        this.contact1.setContactEmail(DATAVERSE_CONTACT_EMAIL);
    }

    private void setUpDataverseDao() {
        this.dataverse.setName("root dataverse");
        when(this.dataverseDao.findRootDataverse()).thenReturn(this.dataverse);
    }

    private void setUpSettingsService() {
        when(this.settingsService.getValueForKey(eq(SystemEmail)))
                .thenReturn(SYSTEM_EMAIL);
        when(this.settingsService.getValueForKey(eq(SiteUrl))).thenReturn(URL);
    }

    private void setUpMailService() {
        this.mailService = new TestMailService(this.dataverseDao,
                this.settingsService,
                this.mailMessageCreator, this.mailSender);
    }

    private void setUpMailMessageCreator() {

        this.mailMessageCreator = new MailMessageCreator(this.systemConfig,
                this.permissionService,
                this.dataverseDao, this.confirmEmailService,
                this.genericDao, this.authenticatedUserRepository);
    }

    private void setUpSendFeedbackDialog() {

        this.dialog = new SendFeedbackDialog(this.mailService, this.settingsService,
                this.dataverseDao, this.systemConfig, this.dataverseSession,
                this.messages);

        this.dialog.init();
    }

    private FileUploadEvent newEvent(final UploadedFile file) {
        return new FileUploadEvent(this.component, file);
    }

    @AfterEach
    public void resetMocks() {
        reset(this.messages);
        reset(this.mailSender);
    }

    @Test
    public void emailGetsSentToSystem_whenUserIsNotLoggedIn_andDataAreProvided()
            throws Exception {
        assertThat(this.dialog.getRecipientOptions())
                .containsExactly(SYSTEM_SUPPORT);
        assertThat(this.dialog.getFormHeader())
                .isEqualTo("Contact the Repository's Support");

        assertThat(this.dialog.isLoggedIn()).isFalse();
        assertThat(this.dialog.loggedInUserEmail()).isNull();
        assertThat(this.dialog.getAttachments()).isEmpty();
        assertThat(this.dialog.displayFileUpload()).isFalse();
        assertThat(this.dialog.displayOnlyOneRecipient()).isTrue();

        this.dialog.setUserEmail(GUEST_USER_EMAIL);
        this.dialog.setMessageSubject("abc subject");
        this.dialog.setUserMessage("abc message");
        this.dialog.setUserSum(this.dialog.getOp1() + this.dialog.getOp2());

        this.dialog.sendMessage();

        verify(this.mailSender, times(1)).sendMail(this.emailCaptor.capture());
        final Email email = this.emailCaptor.getValue();
        assertThat(email.getFromRecipient().getAddress()).isEqualTo(SYSTEM_EMAIL);
        assertThat(email.getReplyToRecipient().getAddress())
                .isEqualTo(GUEST_USER_EMAIL);
        assertThat(email.getSubject())
                .isEqualTo("root dataverse support request: abc subject");
        assertThat(email.getPlainText())
                .contains("The following message was sent from guest@dv.com");
        assertThat(email.getPlainText()).contains("abc message");
        assertThat(email.getAttachments()).isEmpty();

        verify(this.messages, times(1))
                .addSuccessMessage(eq("The message has been sent."));
        verify(this.messages, never()).addErrorMessage(anyString());
    }

    @Test
    public void emailGetsSentToSystem_whenUserIsLoggedIn_andDataAreProvided()
            throws Exception {
        assertThat(this.dialog.getRecipientOptions())
                .containsExactly(SYSTEM_SUPPORT);
        assertThat(this.dialog.getFormHeader())
                .isEqualTo("Contact the Repository's Support");

        this.dataverseSession.logIn(this.authenticatedUser);

        assertThat(this.dialog.isLoggedIn()).isTrue();
        assertThat(this.dialog.loggedInUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(this.dialog.getAttachments()).isEmpty();
        assertThat(this.dialog.displayFileUpload()).isTrue();
        assertThat(this.dialog.displayOnlyOneRecipient()).isTrue();

        this.dialog.setMessageSubject("abc subject");
        this.dialog.setUserMessage("abc message");
        this.dialog.setUserSum(this.dialog.getOp1() + this.dialog.getOp2());

        this.dialog.sendMessage();
        // verify email
        verify(this.mailSender, times(1)).sendMail(this.emailCaptor.capture());
        final Email email = this.emailCaptor.getValue();
        assertThat(email.getFromRecipient().getAddress()).isEqualTo(SYSTEM_EMAIL);
        assertThat(email.getReplyToRecipient().getAddress()).isEqualTo(USER_EMAIL);
        assertThat(email.getSubject())
                .isEqualTo("root dataverse support request: abc subject");
        assertThat(email.getPlainText())
                .contains("The following message was sent from user1@dv.com");
        assertThat(email.getPlainText()).contains("abc message");
        assertThat(email.getAttachments()).isEmpty();

        verify(this.messages, times(1))
                .addSuccessMessage(eq("The message has been sent."));
        verify(this.messages, never()).addErrorMessage(anyString());
    }

    @Test
    public void emailGetsSentToSystemAndUser_whenUserIsLoggedIn_andDataAreProvided_withAttachments()
            throws Exception {
        assertThat(this.dialog.getRecipientOptions())
                .containsExactly(SYSTEM_SUPPORT);
        assertThat(this.dialog.getFormHeader())
                .isEqualTo("Contact the Repository's Support");

        this.dataverseSession.logIn(this.authenticatedUser);

        assertThat(this.dialog.isLoggedIn()).isTrue();
        assertThat(this.dialog.loggedInUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(this.dialog.getAttachments()).isEmpty();
        assertThat(this.dialog.displayFileUpload()).isTrue();
        assertThat(this.dialog.displayOnlyOneRecipient()).isTrue();

        this.dialog.setMessageSubject("abc subject");
        this.dialog.setUserMessage("abc message");
        this.dialog.setUserSum(this.dialog.getOp1() + this.dialog.getOp2());
        this.dialog.setSendCopy(true);

        this.dialog.handleFileUpload(newEvent(attachment1));
        this.dialog.handleFileUpload(newEvent(attachment2));

        assertThat(this.dialog.getAttachments()).containsExactly(attachment1,
                attachment2);

        this.dialog.sendMessage();
        // verify email
        verify(this.mailSender, times(2)).sendMail(this.emailCaptor.capture());
        final Email email = this.emailCaptor.getAllValues().get(0);
        assertThat(email.getFromRecipient().getAddress()).isEqualTo(SYSTEM_EMAIL);
        assertThat(email.getReplyToRecipient().getAddress()).isEqualTo(USER_EMAIL);
        assertThat(email.getSubject())
                .isEqualTo("root dataverse support request: abc subject");
        assertThat(email.getPlainText())
                .contains("The following message was sent from user1@dv.com");
        assertThat(email.getPlainText()).contains("abc message");
        assertThat(email.getAttachments().size()).isEqualTo(2);
        assertThat(email.getAttachments().get(0).getDataSource().getName())
                .isEqualTo(attachment1.getFileName());
        assertThat(email.getAttachments().get(0).getDataSource().getName())
                .isEqualTo(attachment1.getFileName());
        // verify copy
        final Email emailCopy = this.emailCaptor.getAllValues().get(1);
        assertThat(emailCopy.getFromRecipient().getAddress())
                .isEqualTo(SYSTEM_EMAIL);
        assertThat(emailCopy.getSubject())
                .isEqualTo("root dataverse support request: abc subject");
        assertThat(emailCopy.getPlainText()).contains(
                "You receive this email because you have asked for a copy of the message. If you haven’t, someone probably entered your email address by mistake. In that case, please ignore this message.");
        assertThat(emailCopy.getPlainText()).contains("abc message");
        assertThat(emailCopy.getPlainText())
                .contains("You may contact us for support at system@dv.com.");
        assertThat(emailCopy.getAttachments().size()).isEqualTo(2);
        assertThat(emailCopy.getAttachments().get(0).getDataSource().getName())
                .isEqualTo(attachment1.getFileName());
        assertThat(emailCopy.getAttachments().get(0).getDataSource().getName())
                .isEqualTo(attachment1.getFileName());

        verify(this.messages, times(1))
                .addSuccessMessage(eq("The message has been sent."));
        verify(this.messages, never()).addErrorMessage(anyString());
    }

    @Test
    public void emailGetsSentToDataverseContact_whenUserIsLoggedIn_andDataAreProvided()
            throws Exception {
        this.dataverse.setDataverseContacts(singletonList(this.contact1));
        this.dialog.reset(this.dataverse);
        assertThat(this.dialog.getRecipientOptions())
                .containsExactly(SYSTEM_SUPPORT, DATAVERSE_CONTACT);
        assertThat(this.dialog.getFormHeader()).isEqualTo("Send E-mail");

        this.dataverseSession.logIn(this.authenticatedUser);

        assertThat(this.dialog.isLoggedIn()).isTrue();
        assertThat(this.dialog.loggedInUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(this.dialog.getAttachments()).isEmpty();
        assertThat(this.dialog.displayFileUpload()).isTrue();
        assertThat(this.dialog.displayOnlyOneRecipient()).isFalse();

        this.dialog.setMessageSubject("abc subject");
        this.dialog.setUserMessage("abc message");
        this.dialog.setUserSum(this.dialog.getOp1() + this.dialog.getOp2());
        this.dialog.setRecipientOption(DATAVERSE_CONTACT);

        this.dialog.sendMessage();
        // verify email
        verify(this.mailSender, times(1)).sendMail(this.emailCaptor.capture());
        final Email email = this.emailCaptor.getValue();
        assertThat(email.getFromRecipient().getAddress()).isEqualTo(SYSTEM_EMAIL);
        assertThat(email.getReplyToRecipient().getAddress()).isEqualTo(USER_EMAIL);
        assertThat(email.getSubject())
                .isEqualTo("root dataverse contact: abc subject");
        assertThat(email.getPlainText())
                .contains(
                        "You have just been sent the following message from user1@dv.com via the root dataverse hosted dataverse named \"null\"");
        assertThat(email.getPlainText()).contains("abc message");
        assertThat(email.getAttachments()).isEmpty();

        verify(this.messages, times(1))
                .addSuccessMessage(eq("The message has been sent."));
        verify(this.messages, never()).addErrorMessage(anyString());
    }

    @Test
    public void emailGetsSentToDataverseConctactAndUser_whenUserIsLoggedIn_andDataAreProvided()
            throws Exception {
        this.dialog.reset(this.dataverse);
        assertThat(this.dialog.getRecipientOptions())
                .containsExactly(SYSTEM_SUPPORT, DATAVERSE_CONTACT);
        assertThat(this.dialog.getFormHeader()).isEqualTo("Send E-mail");

        this.dataverseSession.logIn(this.authenticatedUser);

        assertThat(this.dialog.isLoggedIn()).isTrue();
        assertThat(this.dialog.loggedInUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(this.dialog.getAttachments()).isEmpty();
        assertThat(this.dialog.displayFileUpload()).isTrue();
        assertThat(this.dialog.displayOnlyOneRecipient()).isFalse();

        this.dialog.setMessageSubject("abc subject");
        this.dialog.setUserMessage("abc message");
        this.dialog.setUserSum(this.dialog.getOp1() + this.dialog.getOp2());
        this.dialog.setRecipientOption(DATAVERSE_CONTACT);
        this.dialog.setSendCopy(true);

        this.dialog.sendMessage();
        // verify email
        verify(this.mailSender, times(2)).sendMail(this.emailCaptor.capture());
        final Email email = this.emailCaptor.getAllValues().get(0);
        assertThat(email.getFromRecipient().getAddress()).isEqualTo(SYSTEM_EMAIL);
        assertThat(email.getReplyToRecipient().getAddress()).isEqualTo(USER_EMAIL);
        assertThat(email.getSubject())
                .isEqualTo("root dataverse contact: abc subject");
        assertThat(email.getPlainText()).contains("abc message");
        assertThat(email.getAttachments()).isEmpty();
        // verify copy
        final Email emailCopy = this.emailCaptor.getAllValues().get(1);
        assertThat(emailCopy.getFromRecipient().getAddress())
                .isEqualTo(SYSTEM_EMAIL);
        assertThat(emailCopy.getSubject())
                .isEqualTo("root dataverse contact: abc subject");
        assertThat(emailCopy.getPlainText()).contains(
                "You receive this email because you have asked for a copy of the message. If you haven’t, someone probably entered your email address by mistake. In that case, please ignore this message.");
        assertThat(emailCopy.getPlainText()).contains("abc message");
        assertThat(emailCopy.getPlainText())
                .contains("You may contact us for support at system@dv.com.");
        assertThat(emailCopy.getAttachments()).isEmpty();

        verify(this.messages, times(1))
                .addSuccessMessage(eq("The message has been sent."));
        verify(this.messages, never()).addErrorMessage(anyString());
    }

    @Test
    public void emailGetsSentToDatasetContact_whenUserIsLoggedIn_andDataAreProvided()
            throws Exception {
        this.dataverse.setDataverseContacts(singletonList(this.contact1));
        this.dialog.reset(this.dataset);
        assertThat(this.dialog.getRecipientOptions())
                .containsExactly(SYSTEM_SUPPORT, DATAVERSE_CONTACT, DATASET_CONTACT);
        assertThat(this.dialog.getFormHeader()).isEqualTo("Send E-mail");

        this.dataverseSession.logIn(this.authenticatedUser);

        assertThat(this.dialog.isLoggedIn()).isTrue();
        assertThat(this.dialog.loggedInUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(this.dialog.getAttachments()).isEmpty();
        assertThat(this.dialog.displayFileUpload()).isTrue();
        assertThat(this.dialog.displayOnlyOneRecipient()).isFalse();

        this.dialog.setMessageSubject("abc subject");
        this.dialog.setUserMessage("abc message");
        this.dialog.setUserSum(this.dialog.getOp1() + this.dialog.getOp2());
        this.dialog.setRecipientOption(DATASET_CONTACT);

        this.dialog.sendMessage();
        // verify email
        verify(this.mailSender, times(1)).sendMail(this.emailCaptor.capture());
        final Email email = this.emailCaptor.getValue();
        assertThat(email.getFromRecipient().getAddress()).isEqualTo(SYSTEM_EMAIL);
        assertThat(email.getReplyToRecipient().getAddress()).isEqualTo(USER_EMAIL);
        assertThat(email.getSubject())
                .isEqualTo("root dataverse contact: abc subject");
        assertThat(email.getPlainText()).contains("abc message");
        assertThat(email.getPlainText()).contains(
                "You have just been sent the following message from user1@dv.com via the root dataverse hosted dataset titled \"\"");
        assertThat(email.getAttachments()).isEmpty();

        verify(this.messages, times(1))
                .addSuccessMessage(eq("The message has been sent."));
        verify(this.messages, never()).addErrorMessage(anyString());
    }

    @Test
    public void emailGetsSentToDatasetConctactAndUser_whenUserIsLoggedIn_andDataAreProvided()
            throws Exception {
        this.dialog.reset(this.dataset);
        assertThat(this.dialog.getRecipientOptions())
                .containsExactly(SYSTEM_SUPPORT, DATAVERSE_CONTACT, DATASET_CONTACT);
        assertThat(this.dialog.getFormHeader()).isEqualTo("Send E-mail");

        this.dataverseSession.logIn(this.authenticatedUser);

        assertThat(this.dialog.isLoggedIn()).isTrue();
        assertThat(this.dialog.loggedInUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(this.dialog.getAttachments()).isEmpty();
        assertThat(this.dialog.displayFileUpload()).isTrue();
        assertThat(this.dialog.displayOnlyOneRecipient()).isFalse();

        this.dialog.setMessageSubject("abc subject");
        this.dialog.setUserMessage("abc message");
        this.dialog.setUserSum(this.dialog.getOp1() + this.dialog.getOp2());
        this.dialog.setRecipientOption(DATASET_CONTACT);
        this.dialog.setSendCopy(true);

        this.dialog.sendMessage();
        // verify email
        verify(this.mailSender, times(2)).sendMail(this.emailCaptor.capture());
        final Email email = this.emailCaptor.getAllValues().get(0);
        assertThat(email.getFromRecipient().getAddress()).isEqualTo(SYSTEM_EMAIL);
        assertThat(email.getReplyToRecipient().getAddress()).isEqualTo(USER_EMAIL);
        assertThat(email.getSubject())
                .isEqualTo("root dataverse contact: abc subject");
        assertThat(email.getPlainText()).contains("abc message");
        assertThat(email.getAttachments()).isEmpty();
        // verify copy
        final Email emailCopy = this.emailCaptor.getAllValues().get(1);
        assertThat(emailCopy.getFromRecipient().getAddress())
                .isEqualTo(SYSTEM_EMAIL);
        assertThat(emailCopy.getSubject())
                .isEqualTo("root dataverse contact: abc subject");
        assertThat(emailCopy.getPlainText()).contains(
                "You receive this email because you have asked for a copy of the message. If you haven’t, someone probably entered your email address by mistake. In that case, please ignore this message.");
        assertThat(emailCopy.getPlainText()).contains("abc message");
        assertThat(emailCopy.getPlainText())
                .contains("You may contact us for support at system@dv.com.");
        assertThat(emailCopy.getAttachments()).isEmpty();

        verify(this.messages, times(1))
                .addSuccessMessage(eq("The message has been sent."));
        verify(this.messages, never()).addErrorMessage(anyString());
    }

    @Test
    public void emailGetsSentToFileDatasetContact_whenUserIsLoggedIn_andDataAreProvided()
            throws Exception {
        this.dataverse.setDataverseContacts(singletonList(this.contact1));
        this.dialog.reset(this.dataFile);
        assertThat(this.dialog.getRecipientOptions())
                .containsExactly(SYSTEM_SUPPORT, DATAVERSE_CONTACT, DATASET_CONTACT);
        assertThat(this.dialog.getFormHeader()).isEqualTo("Send E-mail");

        this.dataverseSession.logIn(this.authenticatedUser);

        assertThat(this.dialog.isLoggedIn()).isTrue();
        assertThat(this.dialog.loggedInUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(this.dialog.getAttachments()).isEmpty();
        assertThat(this.dialog.displayFileUpload()).isTrue();
        assertThat(this.dialog.displayOnlyOneRecipient()).isFalse();

        this.dialog.setMessageSubject("abc subject");
        this.dialog.setUserMessage("abc message");
        this.dialog.setUserSum(this.dialog.getOp1() + this.dialog.getOp2());
        this.dialog.setRecipientOption(DATASET_CONTACT);

        this.dialog.sendMessage();
        // verify email
        verify(this.mailSender, times(1)).sendMail(this.emailCaptor.capture());
        final Email email = this.emailCaptor.getValue();
        assertThat(email.getFromRecipient().getAddress()).isEqualTo(SYSTEM_EMAIL);
        assertThat(email.getReplyToRecipient().getAddress()).isEqualTo(USER_EMAIL);
        assertThat(email.getSubject())
                .isEqualTo("root dataverse contact: abc subject");
        assertThat(email.getPlainText()).contains("abc message");
        assertThat(email.getPlainText()).contains(
                "You have just been sent the following message from user1@dv.com via the root dataverse hosted file named \"file.txt\" from the dataset titled \"\"");
        assertThat(email.getAttachments()).isEmpty();

        verify(this.messages, times(1))
                .addSuccessMessage(eq("The message has been sent."));
        verify(this.messages, never()).addErrorMessage(anyString());
    }

    @Test
    public void emailGetsSentToFileDatasetConctactAndUser_whenUserIsLoggedIn_andDataAreProvided()
            throws Exception {
        this.dialog.reset(this.dataFile);
        assertThat(this.dialog.getRecipientOptions())
                .containsExactly(SYSTEM_SUPPORT, DATAVERSE_CONTACT, DATASET_CONTACT);
        assertThat(this.dialog.getFormHeader()).isEqualTo("Send E-mail");

        this.dataverseSession.logIn(this.authenticatedUser);

        assertThat(this.dialog.isLoggedIn()).isTrue();
        assertThat(this.dialog.loggedInUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(this.dialog.getAttachments()).isEmpty();
        assertThat(this.dialog.displayFileUpload()).isTrue();
        assertThat(this.dialog.displayOnlyOneRecipient()).isFalse();

        this.dialog.setMessageSubject("abc subject");
        this.dialog.setUserMessage("abc message");
        this.dialog.setUserSum(this.dialog.getOp1() + this.dialog.getOp2());
        this.dialog.setRecipientOption(DATASET_CONTACT);
        this.dialog.setSendCopy(true);

        this.dialog.sendMessage();
        // verify email
        verify(this.mailSender, times(2)).sendMail(this.emailCaptor.capture());
        final Email email = this.emailCaptor.getAllValues().get(0);
        assertThat(email.getFromRecipient().getAddress()).isEqualTo(SYSTEM_EMAIL);
        assertThat(email.getReplyToRecipient().getAddress()).isEqualTo(USER_EMAIL);
        assertThat(email.getSubject())
                .isEqualTo("root dataverse contact: abc subject");
        assertThat(email.getPlainText()).contains("abc message");
        assertThat(email.getAttachments()).isEmpty();
        // verify copy
        final Email emailCopy = this.emailCaptor.getAllValues().get(1);
        assertThat(emailCopy.getFromRecipient().getAddress())
                .isEqualTo(SYSTEM_EMAIL);
        assertThat(emailCopy.getSubject())
                .isEqualTo("root dataverse contact: abc subject");
        assertThat(emailCopy.getPlainText()).contains(
                "You receive this email because you have asked for a copy of the message. If you haven’t, someone probably entered your email address by mistake. In that case, please ignore this message.");
        assertThat(emailCopy.getPlainText()).contains("abc message");
        assertThat(emailCopy.getPlainText())
                .contains("You may contact us for support at system@dv.com.");
        assertThat(emailCopy.getAttachments()).isEmpty();

        verify(this.messages, times(1))
                .addSuccessMessage(eq("The message has been sent."));
        verify(this.messages, never()).addErrorMessage(anyString());
    }

    @Test
    public void exceedingCombinedAttachmentSizeLimit_failsValidation()
            throws Exception {
        this.dataverseSession.logIn(this.authenticatedUser);

        this.dialog.handleFileUpload(newEvent(attachment2));

        assertThat(this.dialog.getAttachments()).containsExactly(attachment2);

        this.dialog.handleFileUpload(newEvent(attachment3));

        assertThat(this.dialog.getAttachments()).containsExactly(attachment2,
                attachment3);

        this.dialog.sendMessage();
        
        assertThat(this.dialog.getAttachments()).isEmpty();
        verify(this.mailSender, never()).sendMail(any());
        verify(this.messages, times(1))
                .addErrorMessage(eq("Combined size of attachments exceeded 10MB."));
    }

    @Test
    public void verifyRecipientLables() {
        assertThat(this.dialog.getRecipientOptionLabel(SYSTEM_SUPPORT))
                .isEqualTo("root dataverse repository support");
        assertThat(this.dialog.getRecipientOptionLabel(DATAVERSE_CONTACT))
                .isEqualTo("Contact person for this dataverse");
        assertThat(this.dialog.getRecipientOptionLabel(DATASET_CONTACT))
                .isEqualTo("Contact person for this dataset");
    }

    @SuppressWarnings("serial")
    private static class TestMailService extends MailService {

        public TestMailService(DataverseDao dataverseDao,
                SettingsServiceBean settingsService,
                MailMessageCreator mailMessageCreator, Mailer mailSender) {
            super(dataverseDao, settingsService, mailMessageCreator);
            setMailSender(mailSender);
        }

        @Override
        public CompletableFuture<Boolean> sendMailAsync(String recipientsEmails,
                String replyTo, EmailContent emailContent) {
            sendMail(recipientsEmails, replyTo, emailContent);
            return null;
        }

        @Override
        public CompletableFuture<Boolean> sendMailAsync(String replyEmail,
                String recipientsEmails, String subject, String messageText,
                Stream<DataSource> attachments) {
            sendMail(replyEmail, recipientsEmails, subject, messageText,
                    attachments);
            return null;
        }
    }

    private static final class FakeUploadedFile implements UploadedFile {

        private final String name;
        private final long size;

        FakeUploadedFile(final String name, final long size) {
            this.name = name;
            this.size = size;
        }

        @Override
        public String getFileName() {
            return this.name;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] getContent() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getContentType() {
            return "text/plain";
        }

        @Override
        public long getSize() {
            return this.size;
        }

        @Override
        public void write(String filePath) throws Exception {
            throw new UnsupportedOperationException();
        }

    }

}
