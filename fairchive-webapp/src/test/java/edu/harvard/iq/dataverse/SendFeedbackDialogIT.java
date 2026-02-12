package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.feedback.FeedbackRecipient.DATASET_CONTACT;
import static edu.harvard.iq.dataverse.feedback.FeedbackRecipient.DATAVERSE_CONTACT;
import static edu.harvard.iq.dataverse.feedback.FeedbackRecipient.SYSTEM_SUPPORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.activation.DataSource;
import javax.faces.component.UIComponent;
import javax.inject.Inject;

import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import org.simplejavamail.email.Email;
import org.simplejavamail.mailer.Mailer;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.datafile.AntivirFileScanner;
import edu.harvard.iq.dataverse.datafile.AntivirScannerResponse;
import edu.harvard.iq.dataverse.feedback.FeedbackService;
import edu.harvard.iq.dataverse.mail.EmailContent;
import edu.harvard.iq.dataverse.mail.MailMessageCreator;
import edu.harvard.iq.dataverse.mail.MailService;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.UIMessages;

@Transactional(TransactionMode.ROLLBACK)
public class SendFeedbackDialogIT extends WebappArquillianDeployment {
    @Inject
    private SettingsServiceBean settingsService;
    @Inject
    private DataverseDao dataverseDao;
    @Inject
    private FeedbackService feedbackService;
    @Inject
    private SystemConfig systemConfig;
    @Inject
    private DataverseSession dataverseSession;
    @Inject
    private MailMessageCreator mailMessageCreator;


    private Mailer mailSender = Mockito.mock(Mailer.class);

    private UIMessages messages = Mockito.mock(UIMessages.class);
    
    private AntivirFileScanner scanner = Mockito.mock(AntivirFileScanner.class);
    

    private MailService mailService;

    private SendFeedbackDialog dialog;

    private ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
    
    @Inject
    private DataverseRepository dataverseRepository;
    @Inject
    private DatasetRepository datasetRepository;
    @Inject
    private DataFileRepository dataFileRepository;
    @Inject
    private AuthenticatedUserRepository userRepository;
    

    private final static String SYSTEM_EMAIL = "dataverseAdmin@mailinator.com";
    private final static String GUEST_USER_EMAIL = "guest@dv.com";
    private final static String USER_EMAIL = "filedownloader@mailinator.com";
    private final static FakeUploadedFile attachment1 = new FakeUploadedFile(
            "file1.txt", 1000000);
    private final static FakeUploadedFile attachment2 = new FakeUploadedFile(
            "file2.txt", 3000000);
    private final static FakeUploadedFile attachment3 = new FakeUploadedFile(
            "file3.txt", 8000000);

    @BeforeEach
    public void setUp() throws Exception {
        setUpMailService();
        setUpSendFeedbackDialog();
    }

    private void setUpMailService() throws Exception {  	
        this.mailService = new TestMailService(this.dataverseDao,
                this.settingsService,
                this.mailMessageCreator, this.mailSender);
    }

    private void setUpSendFeedbackDialog() throws Exception {
    	when(this.scanner.scan(any(InputStream.class))).
    		thenReturn(new AntivirScannerResponse(false, "OK"));

        this.dialog = new SendFeedbackDialog(this.feedbackService, this.mailService,
                this.settingsService, this.dataverseDao, this.systemConfig,
                this.dataverseSession, this.messages, this.scanner);

        this.dialog.init();
    }

    private FileUploadEvent newEvent(final UploadedFile file) {
        UIComponent component = Mockito.mock(UIComponent.class);
        return new FileUploadEvent(component, file);
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
        assertThat(this.dialog.displayOnlyOneRecipient()).isTrue();

        this.dialog.setUserEmail(GUEST_USER_EMAIL);
        this.dialog.setMessageSubject("abc subject");
        this.dialog.setUserMessage("abc message");
        this.dialog.setUserSum(this.dialog.getOp1() + this.dialog.getOp2());
        

        this.dialog.sendMessage();

        verify(this.mailSender, times(1)).sendMail(this.emailCaptor.capture());
        final Email email = this.emailCaptor.getValue();
        assertThat(email.getFromRecipient().getAddress()).isEqualTo(SYSTEM_EMAIL);
        assertThat(email.getSubject())
                .isEqualTo("Root support request: abc subject");
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

        this.dataverseSession.logIn(userRepository.findById(3L).get());

        assertThat(this.dialog.isLoggedIn()).isTrue();
        assertThat(this.dialog.loggedInUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(this.dialog.getAttachments()).isEmpty();
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
                .isEqualTo("Root support request: abc subject");
        assertThat(email.getPlainText())
                .contains("The following message was sent from filedownloader@mailinator.com");
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

        this.dataverseSession.logIn(userRepository.findById(3L).get());

        assertThat(this.dialog.isLoggedIn()).isTrue();
        assertThat(this.dialog.loggedInUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(this.dialog.getAttachments()).isEmpty();
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
                .isEqualTo("Root support request: abc subject");
        assertThat(email.getPlainText())
                .contains("The following message was sent from filedownloader@mailinator.com");
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
                .isEqualTo("Root support request: abc subject");
        assertThat(emailCopy.getPlainText()).contains(
                "You receive this email because you have asked for a copy of the message. If you haven’t, someone probably entered your email address by mistake. In that case, please ignore this message.");
        assertThat(emailCopy.getPlainText()).contains("abc message");
        assertThat(emailCopy.getPlainText())
                .contains("You may contact us for support at dataverseAdmin@mailinator.com.");
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
        this.dialog.reset(dataverseRepository.findById(19L).get());
        assertThat(this.dialog.getRecipientOptions())
                .containsExactly(SYSTEM_SUPPORT, DATAVERSE_CONTACT);
        assertThat(this.dialog.getFormHeader()).isEqualTo("Send E-mail");

        this.dataverseSession.logIn(userRepository.findById(3L).get());

        assertThat(this.dialog.isLoggedIn()).isTrue();
        assertThat(this.dialog.loggedInUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(this.dialog.getAttachments()).isEmpty();
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
                .isEqualTo("Root contact: abc subject");
        assertThat(email.getPlainText())
                .contains(
                        "You have just been sent the following message from filedownloader@mailinator.com via the Root hosted collection named \"ownmetadatablocks\"");
        assertThat(email.getPlainText()).contains("abc message");
        assertThat(email.getAttachments()).isEmpty();

        verify(this.messages, times(1))
                .addSuccessMessage(eq("The message has been sent."));
        verify(this.messages, never()).addErrorMessage(anyString());
    }

    @Test
    public void emailGetsSentToDataverseConctactAndUser_whenUserIsLoggedIn_andDataAreProvided()
            throws Exception {
        this.dialog.reset(dataverseRepository.findById(19L).get());
        assertThat(this.dialog.getRecipientOptions())
                .containsExactly(SYSTEM_SUPPORT, DATAVERSE_CONTACT);
        assertThat(this.dialog.getFormHeader()).isEqualTo("Send E-mail");

        this.dataverseSession.logIn(userRepository.findById(3L).get());

        assertThat(this.dialog.isLoggedIn()).isTrue();
        assertThat(this.dialog.loggedInUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(this.dialog.getAttachments()).isEmpty();
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
                .isEqualTo("Root contact: abc subject");
        assertThat(email.getPlainText()).contains("abc message");
        assertThat(email.getAttachments()).isEmpty();
        // verify copy
        final Email emailCopy = this.emailCaptor.getAllValues().get(1);
        assertThat(emailCopy.getFromRecipient().getAddress())
                .isEqualTo(SYSTEM_EMAIL);
        assertThat(emailCopy.getSubject())
                .isEqualTo("Root contact: abc subject");
        assertThat(emailCopy.getPlainText()).contains(
                "You receive this email because you have asked for a copy of the message. If you haven’t, someone probably entered your email address by mistake. In that case, please ignore this message.");
        assertThat(emailCopy.getPlainText()).contains("abc message");
        assertThat(emailCopy.getPlainText())
                .contains("You may contact us for support at dataverseAdmin@mailinator.com.");
        assertThat(emailCopy.getAttachments()).isEmpty();

        verify(this.messages, times(1))
                .addSuccessMessage(eq("The message has been sent."));
        verify(this.messages, never()).addErrorMessage(anyString());
    }

    @Test
    public void emailGetsSentToDatasetContact_whenUserIsLoggedIn_andDataAreProvided()
            throws Exception {
        this.dialog.reset(datasetRepository.findById(57L).get());
        assertThat(this.dialog.getRecipientOptions())
                .containsExactly(SYSTEM_SUPPORT, DATAVERSE_CONTACT, DATASET_CONTACT);
        assertThat(this.dialog.getFormHeader()).isEqualTo("Send E-mail");

        this.dataverseSession.logIn(userRepository.findById(3L).get());

        assertThat(this.dialog.isLoggedIn()).isTrue();
        assertThat(this.dialog.loggedInUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(this.dialog.getAttachments()).isEmpty();
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
                .isEqualTo("Root contact: abc subject");
        assertThat(email.getPlainText()).contains("abc message");
        assertThat(email.getPlainText()).contains(
                "You have just been sent the following message from filedownloader@mailinator.com via the Root hosted dataset titled \"Dataset with versions (ver2)\"");
        assertThat(email.getAttachments()).isEmpty();

        verify(this.messages, times(1))
                .addSuccessMessage(eq("The message has been sent."));
        verify(this.messages, never()).addErrorMessage(anyString());
    }

    @Test
    public void emailGetsSentToDatasetConctactAndUser_whenUserIsLoggedIn_andDataAreProvided()
            throws Exception {
        this.dialog.reset(datasetRepository.findById(57L).get());
        assertThat(this.dialog.getRecipientOptions())
                .containsExactly(SYSTEM_SUPPORT, DATAVERSE_CONTACT, DATASET_CONTACT);
        assertThat(this.dialog.getFormHeader()).isEqualTo("Send E-mail");

        this.dataverseSession.logIn(userRepository.findById(3L).get());

        assertThat(this.dialog.isLoggedIn()).isTrue();
        assertThat(this.dialog.loggedInUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(this.dialog.getAttachments()).isEmpty();
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
                .isEqualTo("Root contact: abc subject");
        assertThat(email.getPlainText()).contains("abc message");
        assertThat(email.getAttachments()).isEmpty();
        // verify copy
        final Email emailCopy = this.emailCaptor.getAllValues().get(1);
        assertThat(emailCopy.getFromRecipient().getAddress())
                .isEqualTo(SYSTEM_EMAIL);
        assertThat(emailCopy.getSubject())
                .isEqualTo("Root contact: abc subject");
        assertThat(emailCopy.getPlainText()).contains(
                "You receive this email because you have asked for a copy of the message. If you haven’t, someone probably entered your email address by mistake. In that case, please ignore this message.");
        assertThat(emailCopy.getPlainText()).contains("abc message");
        assertThat(emailCopy.getPlainText())
                .contains("You may contact us for support at dataverseAdmin@mailinator.com.");
        assertThat(emailCopy.getAttachments()).isEmpty();

        verify(this.messages, times(1))
                .addSuccessMessage(eq("The message has been sent."));
        verify(this.messages, never()).addErrorMessage(anyString());
    }

    @Test
    public void emailGetsSentToFileDatasetContact_whenUserIsLoggedIn_andDataAreProvided()
            throws Exception {
        this.dialog.reset(dataFileRepository.findById(55L).get());
        assertThat(this.dialog.getRecipientOptions())
                .containsExactly(SYSTEM_SUPPORT, DATAVERSE_CONTACT, DATASET_CONTACT);
        assertThat(this.dialog.getFormHeader()).isEqualTo("Send E-mail");

        this.dataverseSession.logIn(userRepository.findById(3L).get());

        assertThat(this.dialog.isLoggedIn()).isTrue();
        assertThat(this.dialog.loggedInUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(this.dialog.getAttachments()).isEmpty();
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
                .isEqualTo("Root contact: abc subject");
        assertThat(email.getPlainText()).contains("abc message");
        assertThat(email.getPlainText()).contains(
                "You have just been sent the following message from filedownloader@mailinator.com via the Root hosted file named \"testfile1.zip\" from the dataset titled \"Draft with files\"");
        assertThat(email.getAttachments()).isEmpty();

        verify(this.messages, times(1))
                .addSuccessMessage(eq("The message has been sent."));
        verify(this.messages, never()).addErrorMessage(anyString());
    }

    @Test
    public void emailGetsSentToFileDatasetConctactAndUser_whenUserIsLoggedIn_andDataAreProvided()
            throws Exception {
        this.dialog.reset(dataFileRepository.findById(55L).get());
        assertThat(this.dialog.getRecipientOptions())
                .containsExactly(SYSTEM_SUPPORT, DATAVERSE_CONTACT, DATASET_CONTACT);
        assertThat(this.dialog.getFormHeader()).isEqualTo("Send E-mail");

        this.dataverseSession.logIn(userRepository.findById(3L).get());

        assertThat(this.dialog.isLoggedIn()).isTrue();
        assertThat(this.dialog.loggedInUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(this.dialog.getAttachments()).isEmpty();
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
                .isEqualTo("Root contact: abc subject");
        assertThat(email.getPlainText()).contains("abc message");
        assertThat(email.getAttachments()).isEmpty();
        // verify copy
        final Email emailCopy = this.emailCaptor.getAllValues().get(1);
        assertThat(emailCopy.getFromRecipient().getAddress())
                .isEqualTo(SYSTEM_EMAIL);
        assertThat(emailCopy.getSubject())
                .isEqualTo("Root contact: abc subject");
        assertThat(emailCopy.getPlainText()).contains(
                "You receive this email because you have asked for a copy of the message. If you haven’t, someone probably entered your email address by mistake. In that case, please ignore this message.");
        assertThat(emailCopy.getPlainText()).contains("abc message");
        assertThat(emailCopy.getPlainText())
                .contains("You may contact us for support at dataverseAdmin@mailinator.com.");
        assertThat(emailCopy.getAttachments()).isEmpty();

        verify(this.messages, times(1))
                .addSuccessMessage(eq("The message has been sent."));
        verify(this.messages, never()).addErrorMessage(anyString());
    }

    @Test
    public void exceedingCombinedAttachmentSizeLimit_failsValidation()
            throws Exception {
        this.dataverseSession.logIn(userRepository.findById(3L).get());

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
                .isEqualTo("Root repository support");
        assertThat(this.dialog.getRecipientOptionLabel(DATAVERSE_CONTACT))
                .isEqualTo("Contact person for this collection");
        assertThat(this.dialog.getRecipientOptionLabel(DATASET_CONTACT))
                .isEqualTo("Contact person for this dataset");
    }


    @SuppressWarnings("serial")
    private static class TestMailService extends MailService {

        public TestMailService(DataverseDao dataverseDao,
                SettingsServiceBean settingsService,
                MailMessageCreator mailMessageCreator, 
                Mailer mailSender) {
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
