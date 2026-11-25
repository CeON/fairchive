package edu.harvard.iq.dataverse.mail;

import static edu.harvard.iq.dataverse.common.BrandingUtil.getSupportTeamEmailAddress;
import static edu.harvard.iq.dataverse.common.BrandingUtil.getSupportTeamName;
import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundleWithLocale;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.ASSIGNROLE;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.CHECKSUMFAIL;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.CHECKSUMIMPORT;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.CONFIRMEMAIL;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.CREATEACC;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.CREATEDS;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.CREATEDV;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.FILESYSTEMIMPORT;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.GRANTFILEACCESS;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.GRANTFILEACCESSINFO;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.MAPLAYERDELETEFAILED;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.MAPLAYERUPDATED;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.PUBLISHEDDS;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.PUBLISHEDDV;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.REJECTFILEACCESS;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.REJECTFILEACCESSINFO;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.REQUESTFILEACCESS;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.RETURNEDDS;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.RETURNEDDV;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.REVOKEROLE;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.SUBMITTEDDS;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.SUBMITTEDDV;
import static java.util.Arrays.stream;
import static java.util.logging.Logger.getLogger;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.StringUtils;
import org.simplejavamail.email.Recipient;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.GenericDao;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.common.BrandingUtil;
import edu.harvard.iq.dataverse.common.RoleTranslationUtil;
import edu.harvard.iq.dataverse.mail.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.notification.NotificationParameter;
import edu.harvard.iq.dataverse.notification.dto.EmailNotificationDto;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.util.MailUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;

/**
 * Class takes care of creating text templates for emails.
 */
@Stateless
public class MailMessageCreator {

    private SystemConfig systemConfig;
    private PermissionServiceBean permissionService;
    private DataverseDao dataverseDao;
    private ConfirmEmailServiceBean confirmEmailService;
    private GenericDao genericDao;
    private AuthenticatedUserRepository authenticatedUserRepository;

    private static final Logger logger = getLogger(MailMessageCreator.class.getCanonicalName());

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated /* JEE requirement */
    public MailMessageCreator() { }

    @Inject
    public MailMessageCreator(final SystemConfig systemConfig, 
    						  final PermissionServiceBean permissionService,
                              final DataverseDao dataverseDao, 
                              final ConfirmEmailServiceBean confirmEmailService,
                              final GenericDao genericDao, 
                              final AuthenticatedUserRepository authenticatedUserRepository) {
        this.systemConfig = systemConfig;
        this.permissionService = permissionService;
        this.dataverseDao = dataverseDao;
        this.confirmEmailService = confirmEmailService;
        this.genericDao = genericDao;
        this.authenticatedUserRepository = authenticatedUserRepository;
    }

    // -------------------- LOGIC --------------------

    /**
     * Creates footer for email message.
     */
    public String createMailFooterMessage(final String closingLineKey, 
    		final Locale messageLocale, final String rootDataverseName, 
    		final InternetAddress systemAddress) {
        return getStringFromBundleWithLocale(closingLineKey, messageLocale,
                        getSupportTeamEmailAddress(systemAddress),
                        getSupportTeamName(systemAddress, rootDataverseName, messageLocale));
    }

    /**
     * Divides recipientsEmails into multiple recipients with their own emails.
     *
     * @param recipientsEmails - comma separated emails.
     * @param recipientsName   - common name for all recipients, usually it is blank since it is only visible in email header.
     */
    public List<Recipient> createRecipients(final String recipientsEmails, 
    		final String recipientsName) {
        return stream(recipientsEmails.split(","))
                .map(recipient -> new Recipient(recipientsName, recipient, Message.RecipientType.TO))
                .collect(toList());
    }

    /**
     * Retrives message and subject template based on {@link NotificationObjectType} and {@link NotificationType}.
     *
     * @return message and subject or blank tuple if notificationType didn't match any template.
     */
    public Tuple2<String, String> getMessageAndSubject(final EmailNotificationDto notificationDto, 
    		final String systemEmail) {
        final Lazy<String> rootDataverseName = Lazy.of(() -> dataverseDao.findRootDataverse().getName());

        if (notificationDto.getNotificationObjectType() == NotificationObjectType.DATAVERSE) {
            Dataverse dataverse = dataverseDao.find(notificationDto.getDvObjectId());
            String message = dataverseMessage(notificationDto, dataverse);
            String subject = getSubjectText(notificationDto, rootDataverseName.get());

            return Tuple.of(message, subject);
        }

        if (notificationDto.getNotificationObjectType() == NotificationObjectType.DATASET) {
            Dataset dataset = genericDao.find(notificationDto.getDvObjectId(), Dataset.class);
            String message = datasetMessage(notificationDto, dataset);
            String subject = getSubjectText(notificationDto, rootDataverseName.get());

            return Tuple.of(message, subject);
        }

        if (notificationDto.getNotificationObjectType() == NotificationObjectType.DATASET_VERSION) {
            DatasetVersion datasetVersion = genericDao.find(notificationDto.getDvObjectId(), DatasetVersion.class);
            String message = datasetVersionMessage(notificationDto, datasetVersion);
            String subject = getSubjectText(notificationDto, rootDataverseName.get());

            return subject.isEmpty()
                    ? Tuple.of(message,
                             getSubjectTextForDatasetVersion(notificationDto, rootDataverseName.get(), datasetVersion))
                    : Tuple.of(message, subject);
        }
        
        if (notificationDto.getNotificationObjectType() == NotificationObjectType.DATAVERSE_VERSION) {
            DataverseVersion dataverseVersion = genericDao.find(notificationDto.getDvObjectId(), DataverseVersion.class);
            String message = dataverseVersionMessage(notificationDto, dataverseVersion);
            String subject = getSubjectText(notificationDto, rootDataverseName.get());

            return subject.isEmpty()
                    ? Tuple.of(message,
                             getSubjectTextForDataverseVersion(notificationDto, rootDataverseName.get(), dataverseVersion))
                    : Tuple.of(message, subject);
        }

        if (notificationDto.getNotificationObjectType() == NotificationObjectType.AUTHENTICATED_USER) {
            String message = authenticatedUserMessage(notificationDto, rootDataverseName.get(),
                                                      MailUtil.parseSystemAddress(systemEmail));
            String subject = getSubjectText(notificationDto, rootDataverseName.get());

            return Tuple.of(message, subject);
        }

        if (notificationDto.getNotificationObjectType() == NotificationObjectType.DATAFILE) {
            DataFile dataFile = genericDao.find(notificationDto.getDvObjectId(), DataFile.class);
            String message = dataFileMessage(notificationDto, dataFile);
            String subject = getSubjectText(notificationDto, rootDataverseName.get());

            return Tuple.of(message, subject);
        }

        if (notificationDto.getNotificationObjectType() == NotificationObjectType.FILEMETADATA) {
            FileMetadata fileMetadata = genericDao.find(notificationDto.getDvObjectId(), FileMetadata.class);
            String message = fileMetadataMessage(notificationDto, fileMetadata);
            String subject = getSubjectText(notificationDto, rootDataverseName.get());

            return Tuple.of(message, subject);
        }

        return Tuple.of(StringUtils.EMPTY, StringUtils.EMPTY);
    }

    // -------------------- PRIVATE --------------------

    private String dataverseMessage(EmailNotificationDto notificationDto, Dataverse dataverse) {
        Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();
        String messageText = getStringFromBundleWithLocale("notification.email.greeting",
                notificationsEmailLanguage);

        switch (notificationDto.getNotificationType()) {
            case ASSIGNROLE:
                String joinedRoleNames = permissionService.getRolesOfUser(notificationDto.getNotificationReceiver(), dataverse).stream()
                        .map(RoleAssignment::getRole)
                        .map(DataverseRole::getAlias)
                        .map(RoleTranslationUtil::getLocaleNameFromAlias)
                        .collect(joining("/"));

                messageText += getStringFromBundleWithLocale("notification.email.dataverse.assignRole",
                        notificationsEmailLanguage,
                        joinedRoleNames, dataverse.getDisplayName(), getDataverseLink(dataverse));

                if (joinedRoleNames.contains(BuiltInRole.FILE_DOWNLOADER.getAlias())) {
                    String pattern = getStringFromBundleWithLocale(
                            "notification.access.granted.fileDownloader.additionalDataverse",
                            notificationsEmailLanguage);
                    messageText += MessageFormat.format(pattern, " ");
                }

                return messageText;
            case REVOKEROLE:
                messageText += getStringFromBundleWithLocale("notification.email.dataverse.revokeRole",
                        notificationsEmailLanguage,
                        dataverse.getDisplayName(), getDataverseLink(dataverse));
                return messageText;
            case CREATEDV:
                Dataverse parentDataverse = dataverse.getOwner();
                String dataverseCreatedMessage = getStringFromBundleWithLocale("notification.email.createDataverse",
                        notificationsEmailLanguage, dataverse.getDisplayName(), getDataverseLink(dataverse),
                        parentDataverse != null ? parentDataverse.getDisplayName() : "",
                        parentDataverse != null ? getDataverseLink(parentDataverse) : "",
                        systemConfig.getGuidesBaseUrl(notificationsEmailLanguage), systemConfig.getGuidesVersion());
                logger.fine(dataverseCreatedMessage);
                return messageText + dataverseCreatedMessage;
        }

        return StringUtils.EMPTY;
    }

    private String datasetMessage(final EmailNotificationDto notificationDto, 
    		final Dataset dataset) {

        Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();
        String messageText = getStringFromBundleWithLocale("notification.email.greeting",
                notificationsEmailLanguage);
        String pattern;

        switch (notificationDto.getNotificationType()) {
            case ASSIGNROLE:

                String joinedRoleNames = permissionService.getRolesOfUser(notificationDto.getNotificationReceiver(), dataset).stream()
                        .map(RoleAssignment::getRole)
                        .map(DataverseRole::getAlias)
                        .map(RoleTranslationUtil::getLocaleNameFromAlias)
                        .collect(joining("/"));

                messageText += getStringFromBundleWithLocale("notification.email.dataset.assignRole",
                        notificationsEmailLanguage,
                        joinedRoleNames, dataset.getDisplayName(), getDatasetLink(dataset));

                if (joinedRoleNames.contains(BuiltInRole.FILE_DOWNLOADER.getAlias())) {
                    pattern = getStringFromBundleWithLocale(
                            "notification.access.granted.fileDownloader.additionalDataverse", notificationsEmailLanguage);
                    messageText += MessageFormat.format(pattern, " ");
                }
                return messageText;
            case REVOKEROLE:
                messageText += getStringFromBundleWithLocale("notification.email.dataset.revokeRole",
                        notificationsEmailLanguage,
                        dataset.getDisplayName(), getDatasetLink(dataset));
                return messageText;
            case GRANTFILEACCESS:
                pattern = getStringFromBundleWithLocale("notification.email.grantFileAccess",
                        notificationsEmailLanguage);
                messageText += MessageFormat.format(pattern,
                        dataset.getDisplayName(), getDatasetLink(dataset));
                return messageText;
            case REJECTFILEACCESS:
                pattern = getStringFromBundleWithLocale("notification.email.rejectFileAccess",
                        notificationsEmailLanguage);
                messageText += MessageFormat.format(pattern,
                        dataset.getDisplayName(), getDatasetLink(dataset));
                return messageText;
            case CHECKSUMFAIL:
                String checksumFailMsg = getStringFromBundleWithLocale("notification.checksumfail",
                        notificationsEmailLanguage, dataset.getGlobalId().toString());
                logger.fine("checksumFailMsg: " + checksumFailMsg);
                return messageText + checksumFailMsg;
            case GRANTFILEACCESSINFO:
                pattern = getStringFromBundleWithLocale("notification.email.grant.file.access.info.text", notificationsEmailLanguage);
                messageText += MessageFormat.format(pattern,
                        notificationDto.getParameter(NotificationParameter.GRANTED_BY.key()), dataset.getDisplayName(), getDatasetLink(dataset),
                        getNumberOfUsersRequestingForFileAccess(dataset), getDatasetManageFilePermissionsLink(dataset));
                return messageText;
            case REJECTFILEACCESSINFO:
                pattern = getStringFromBundleWithLocale("notification.email.reject.file.access.info.text", notificationsEmailLanguage);
                messageText += MessageFormat.format(pattern,
                        notificationDto.getParameter(NotificationParameter.REJECTED_BY.key()), dataset.getDisplayName(), getDatasetLink(dataset),
                        getNumberOfUsersRequestingForFileAccess(dataset), getDatasetManageFilePermissionsLink(dataset));
                return messageText;
        }

        return StringUtils.EMPTY;
    }

    private String datasetVersionMessage(final EmailNotificationDto notificationDto, 
    		final DatasetVersion version) {

        final Locale notificationsEmailLanguage = notificationDto.
        		getNotificationReceiver().getNotificationsLanguage();
        final String greetingsText = getStringFromBundleWithLocale("notification.email.greeting", notificationsEmailLanguage);

        final Function<String, String> addMessageIfNotEmpty = key -> {
            final String message = notificationDto.getParameter(NotificationParameter.MESSAGE.key());
            return isNotBlank(message)
                    ? getStringFromBundleWithLocale(key, notificationsEmailLanguage, message)
                    : EMPTY;
        };

        switch (notificationDto.getNotificationType()) {
            case CREATEDS:
                String datasetCreatedMessage = getStringFromBundleWithLocale("notification.email.createDataset",
                        notificationsEmailLanguage,
                        version.getDataset().getDisplayName(),
                        getDatasetLink(version.getDataset()),
                        version.getDataset().getOwner().getDisplayName(),
                        getDataverseLink(version.getDataset().getOwner()),
                        systemConfig.getGuidesBaseUrl(notificationsEmailLanguage),
                        systemConfig.getGuidesVersion());

                return greetingsText + datasetCreatedMessage;
            case MAPLAYERUPDATED:
                return greetingsText + getStringFromBundleWithLocale("notification.email.worldMap.added",
                        notificationsEmailLanguage,
                        version.getDataset().getDisplayName(),
                        getDatasetLink(version.getDataset()));
            case PUBLISHEDDS:
                return greetingsText + getStringFromBundleWithLocale("notification.email.wasPublished",
                        notificationsEmailLanguage,
                        version.getDataset().getDisplayName(),
                        getDatasetLink(version.getDataset()),
                        version.getDataset().getOwner().getDisplayName(),
                        getDataverseLink(version.getDataset().getOwner()));
            case SUBMITTEDDS:
                final AuthenticatedUser requestor = findRequestor(notificationDto.
                		getParameter(NotificationParameter.REQUESTOR_ID.key()));

                final String requestorName = requestor != null 
                		? requestor.getFirstName() + SPACE + requestor.getLastName() 
                		: EMPTY;
                final String requestorEmail = requestor != null 
                		? requestor.getEmail() 
                		: EMPTY;

                return greetingsText +
                        getStringFromBundleWithLocale("notification.email.wasSubmittedForReview",
                                notificationsEmailLanguage,
                                version.getDataset().getDisplayName(),
                                getDatasetDraftLink(version.getDataset()),
                                version.getDataset().getOwner().getDisplayName(),
                                getDataverseLink(version.getDataset().getOwner()),
                                requestorName, requestorEmail) +
                        addMessageIfNotEmpty.apply("notification.email.submitted.additional.message");
            case RETURNEDDS:
                return greetingsText
                        + getStringFromBundleWithLocale("notification.email.wasReturnedByReviewer",
                                notificationsEmailLanguage,
                                version.getDataset().getDisplayName(),
                                getDatasetDraftLink(version.getDataset()),
                                version.getDataset().getOwner().getDisplayName(),
                                getDataverseLink(version.getDataset().getOwner()))
                        + addMessageIfNotEmpty.apply("notification.email.returned.additional.message")
                        + getStringFromBundleWithLocale("notification.email.returned.replyTo",
                            notificationsEmailLanguage, notificationDto.getParameter(NotificationParameter.REPLY_TO.key()));
            case FILESYSTEMIMPORT:
                return greetingsText +
                        getStringFromBundleWithLocale("notification.mail.import.filesystem", 
                        		                      notificationsEmailLanguage,
                                                      systemConfig.getDataverseSiteUrl(),
                                                      version.getDataset().getGlobalId().toString(),
                                                      version.getDataset().getDisplayName());

            case CHECKSUMIMPORT:
                return greetingsText + 
                		getStringFromBundleWithLocale("notification.import.checksum", 
                		                              notificationsEmailLanguage,
                                                      version.getDataset().getGlobalId().toString(),
                                                      version.getDataset().getDisplayName());

        }
        return StringUtils.EMPTY;
    }
    
    private String dataverseVersionMessage(final EmailNotificationDto notificationDto, 
    		final DataverseVersion version) {

        final Locale notificationsEmailLanguage = notificationDto.
        		getNotificationReceiver().getNotificationsLanguage();
        final String greetingsText = getStringFromBundleWithLocale(
        		"notification.email.greeting", notificationsEmailLanguage);

        final Function<String, String> addMessageIfNotEmpty = key -> {
            final String message = notificationDto.getParameter(NotificationParameter.MESSAGE.key());
            return isNotBlank(message)
                    ? getStringFromBundleWithLocale(key, notificationsEmailLanguage, message)
                    : EMPTY;
        };

        switch (notificationDto.getNotificationType()) {
            case CREATEDV:
            	final Dataverse parentDataverse = version.getDataverse().getOwner();
                final String datasetCreatedMessage = 
                		getStringFromBundleWithLocale("notification.email.createDataverse",
                        notificationsEmailLanguage, version.getDataverse().getDisplayName(), 
                        getDataverseLink(version.getDataverse()),
                        parentDataverse != null ? parentDataverse.getDisplayName() : EMPTY,
                        parentDataverse != null ? getDataverseLink(parentDataverse) : EMPTY,
                        systemConfig.getGuidesBaseUrl(notificationsEmailLanguage), 
                        systemConfig.getGuidesVersion());
                return greetingsText + datasetCreatedMessage;
            case PUBLISHEDDV:
                return greetingsText + 
                		getStringFromBundleWithLocale("notification.email.wasPublished.dataverse",
                        notificationsEmailLanguage,
                        version.getDataverse().getDisplayName(),
                        getDataverseLink(version.getDataverse()),
                        version.getDataverse().getOwner().getDisplayName(),
                        getDataverseLink(version.getDataverse().getOwner()));
            case SUBMITTEDDV:
                final AuthenticatedUser requestor = findRequestor(
                		notificationDto.getParameter(NotificationParameter.REQUESTOR_ID.key()));
                final String requestorName = requestor != null 
                		? requestor.getFirstName() + SPACE + requestor.getLastName() 
                		: EMPTY;
                final String requestorEmail = requestor != null 
                		? requestor.getEmail() 
                		: EMPTY;

                return greetingsText +
                        getStringFromBundleWithLocale("notification.email.wasSubmittedForReview.dataverse",
                                notificationsEmailLanguage,
                                version.getDataverse().getDisplayName(),
                                getDataverseLink(version.getDataverse()),
                                version.getDataverse().getOwner().getDisplayName(),
                                getDataverseLink(version.getDataverse().getOwner()),
                                requestorName, requestorEmail) +
                        addMessageIfNotEmpty.apply("notification.email.submitted.additional.message.dataverse");
            case RETURNEDDV:
                return greetingsText
                        + getStringFromBundleWithLocale("notification.email.wasReturnedByReviewer.dataverse",
                                notificationsEmailLanguage,
                                version.getDataverse().getDisplayName(),
                                getDataverseLink(version.getDataverse()),
                                version.getDataverse().getOwner().getDisplayName(),
                                getDataverseLink(version.getDataverse().getOwner()))
                        + addMessageIfNotEmpty.apply("notification.email.returned.additional.message.dataverse")
                        + getStringFromBundleWithLocale("notification.email.returned.replyTo.dataverse",
                            notificationsEmailLanguage, notificationDto.getParameter(NotificationParameter.REPLY_TO.key()));
            default : 
            	return EMPTY;
        }
    }

    private String dataFileMessage(final EmailNotificationDto notificationDto, 
    		final DataFile dataFile) {
        final Locale locale = notificationDto.getNotificationReceiver().getNotificationsLanguage();
        String messageText = getStringFromBundleWithLocale(
        		"notification.email.greeting", locale);

        if (notificationDto.getNotificationType().equals(REQUESTFILEACCESS)) {
            AuthenticatedUser requestor = findRequestor(notificationDto.getParameter(NotificationParameter.REQUESTOR_ID.key()));
            final String pattern = getStringFromBundleWithLocale(
            		"notification.email.requestFileAccess", locale);
            final String requestorName = requestor != null 
            		? requestor.getFirstName() + SPACE + requestor.getLastName() 
            		: EMPTY;
            final String requestorEmail = requestor != null 
            		? requestor.getEmail() 
            		: EMPTY;

            messageText += MessageFormat.format(pattern, dataFile.getOwner().getDisplayName(), requestorName,
                    requestorEmail, getDatasetManageFileAccessLink(dataFile));
            return messageText;
        }
        return EMPTY;
    }

    private String fileMetadataMessage(EmailNotificationDto notificationDto, FileMetadata fileMetadata) {
        Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();
        String messageText = getStringFromBundleWithLocale("notification.email.greeting",
                notificationsEmailLanguage);

        if (notificationDto.getNotificationType().equals(MAPLAYERDELETEFAILED)) {

            DatasetVersion version = fileMetadata.getDatasetVersion();
            String pattern = getStringFromBundleWithLocale("notification.email.maplayer.deletefailed.text",
                    notificationsEmailLanguage);

            messageText += MessageFormat.format(pattern, fileMetadata.getLabel(), getDatasetLink(version.getDataset()));
            return messageText;
        }
        return EMPTY;
    }

    private String authenticatedUserMessage(EmailNotificationDto notificationDto, String rootDataverseName, InternetAddress systemAddress) {
        Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();
        String messageText = getStringFromBundleWithLocale("notification.email.greeting",
                notificationsEmailLanguage);

        if (notificationDto.getNotificationType().equals(CREATEACC)) {

            String accountCreatedMessage = getStringFromBundleWithLocale("notification.email.welcome", notificationsEmailLanguage,
                    rootDataverseName,
                    systemConfig.getGuidesBaseUrl(notificationsEmailLanguage),
                    systemConfig.getGuidesVersion(),
                    BrandingUtil.getSupportTeamName(systemAddress, rootDataverseName, notificationsEmailLanguage),
                    BrandingUtil.getSupportTeamEmailAddress(systemAddress));
            String optionalConfirmEmailAddon = confirmEmailService.optionalConfirmEmailAddonMsg(notificationDto.getNotificationReceiver());
            accountCreatedMessage += optionalConfirmEmailAddon;
            logger.fine("accountCreatedMessage: " + accountCreatedMessage);
            return messageText + accountCreatedMessage;
        }

        return EMPTY;
    }

    private String getSubjectText(final EmailNotificationDto notificationDto, 
    		final String rootDataverseName) {
    	
        final Locale locale = notificationDto.getNotificationReceiver().
        		getNotificationsLanguage();

        switch (notificationDto.getNotificationType()) {
            case ASSIGNROLE:
                return getStringFromBundleWithLocale("notification.email.assign.role.subject",
                                                      locale, rootDataverseName);
            case REVOKEROLE:
                return getStringFromBundleWithLocale("notification.email.revoke.role.subject",
                                                      locale, rootDataverseName);
            case CREATEDV:
                return getStringFromBundleWithLocale("notification.email.create.dataverse.subject",
                                                      locale, rootDataverseName);
            case REQUESTFILEACCESS:
                return getStringFromBundleWithLocale("notification.email.request.file.access.subject",
                                                      locale, rootDataverseName);
            case GRANTFILEACCESS:
                return getStringFromBundleWithLocale("notification.email.grant.file.access.subject",
                                                      locale, rootDataverseName);
            case REJECTFILEACCESS:
                return getStringFromBundleWithLocale("notification.email.rejected.file.access.subject",
                                                      locale, rootDataverseName);
            case MAPLAYERUPDATED:
                return getStringFromBundleWithLocale("notification.email.update.maplayer",
                                                      locale, rootDataverseName);
            case MAPLAYERDELETEFAILED:
                return getStringFromBundleWithLocale("notification.email.maplayer.deletefailed.subject",
                                                      locale, rootDataverseName);
            case CREATEDS:
                return getStringFromBundleWithLocale("notification.email.create.dataset.subject",
                                                      locale, rootDataverseName);
            case SUBMITTEDDS:
                return getStringFromBundleWithLocale("notification.email.submit.dataset.subject",
                                                      locale, rootDataverseName);
            case SUBMITTEDDV:
                return getStringFromBundleWithLocale("notification.email.submit.dataverse.subject",
                                                      locale, rootDataverseName);
            case PUBLISHEDDS:
                return getStringFromBundleWithLocale("notification.email.publish.dataset.subject",
                                                      locale, rootDataverseName);
            case PUBLISHEDDV:
                return getStringFromBundleWithLocale("notification.email.publish.dataverse.subject",
                                                      locale, rootDataverseName); 
            case RETURNEDDS:
                return getStringFromBundleWithLocale("notification.email.returned.dataset.subject",
                                                      locale, rootDataverseName);
            case RETURNEDDV:
                return getStringFromBundleWithLocale("notification.email.returned.dataverse.subject",
                                                      locale, rootDataverseName);
            case CREATEACC:
                return getStringFromBundleWithLocale("notification.email.create.account.subject",
                                                      locale, rootDataverseName);
            case CHECKSUMFAIL:
                return getStringFromBundleWithLocale("notification.email.checksumfail.subject",
                                                      locale, rootDataverseName);
            case CHECKSUMIMPORT:
                return getStringFromBundleWithLocale("notification.email.import.checksum.subject",
                                                      locale, rootDataverseName);
            case CONFIRMEMAIL:
                return getStringFromBundleWithLocale("notification.email.verifyEmail.subject",
                                                      locale, rootDataverseName);
            case GRANTFILEACCESSINFO:
                return getStringFromBundleWithLocale("notification.email.grant.file.access.info.subject",
                                                      locale, rootDataverseName);
            case REJECTFILEACCESSINFO:
                return getStringFromBundleWithLocale("notification.email.reject.file.access.info.subject",
                                                      locale, rootDataverseName);
            default:
            	return EMPTY;
        }
    }

    private String getSubjectTextForDatasetVersion(final EmailNotificationDto notificationDto, 
    		final String rootDataverseName, final DatasetVersion version) {
        if (notificationDto.getNotificationType().equals(FILESYSTEMIMPORT)) {
            final Locale locale = notificationDto.getNotificationReceiver().getNotificationsLanguage();
            try {
                return getStringFromBundleWithLocale("notification.email.import.filesystem.subject",
                        locale, version.getDataset().getDisplayName());
            } catch (Exception e) {
                return getStringFromBundleWithLocale("notification.email.import.filesystem.subject",
                        locale, rootDataverseName);
            }
        }

        return EMPTY;
    }
    
    private String getSubjectTextForDataverseVersion(final EmailNotificationDto notificationDto, 
    		final String rootDataverseName, final DataverseVersion version) {
        return EMPTY;
    }

    private String getDatasetManageFileAccessLink(final DataFile datafile) {
        return systemConfig.getDataverseSiteUrl() + 
        		"/permissions-manage-files.xhtml?id=" + datafile.getOwner().getId();
    }

    private String getDatasetLink(final Dataset dataset) {
        return systemConfig.getDataverseSiteUrl() + 
        		"/dataset.xhtml?persistentId=" + dataset.getGlobalId();
    }

    private String getDatasetDraftLink(final Dataset dataset) {
        return systemConfig.getDataverseSiteUrl() + 
        		"/dataset.xhtml?persistentId=" + dataset.getGlobalId() + "&version=DRAFT";
    }

    private String getDatasetManageFilePermissionsLink(final Dataset dataset) {
        return systemConfig.getDataverseSiteUrl() + 
        		"/permissions-manage-files.xhtml?id=" + dataset.getId();
    }

    private String getDataverseLink(final Dataverse dataverse) {
        return systemConfig.getDataverseSiteUrl() + "/dataverse/" + dataverse.getAlias();
    }

    private String getNumberOfUsersRequestingForFileAccess(Dataset dataset) {
        long numberOfUsersRequestingForAccess = dataset.getFiles().stream()
                .filter(f -> f.getFileMetadatas().stream()
                        .anyMatch(m -> m.isFileUseRestricted()))
                .map(DataFile::getFileAccessRequesters)
                .flatMap(Collection::stream)
                .distinct()
                .count();
        return String.valueOf(numberOfUsersRequestingForAccess);
    }

    private AuthenticatedUser findRequestor(final String requestorId) {
        if (isEmpty(requestorId)) {
            return null;
        } else {
        	return authenticatedUserRepository.findById(Long.valueOf(requestorId)).orElse(null);
        }
    }
}
