package edu.harvard.iq.dataverse.dataset.tab;

import static edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.RestrictType.NOT_FOR_REDISTRIBUTION;
import static edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType.LICENSE_BASED;
import static edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType.RESTRICTED;
import static edu.harvard.iq.dataverse.persistence.datafile.license.License.CCO_LICENSE_NAME;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.arquillian.transaction.api.annotation.TransactionMode.ROLLBACK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import java.util.Date;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.behavior.Behavior;
import javax.inject.Inject;

import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.primefaces.event.SelectEvent;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.datafile.file.FileMetadataService;
import edu.harvard.iq.dataverse.datafile.page.FileDownloadHelper;
import edu.harvard.iq.dataverse.datafile.page.FileDownloadRequestHelper;
import edu.harvard.iq.dataverse.dataset.EmbargoAccessService;
import edu.harvard.iq.dataverse.dataverse.DataverseService;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetResult;
import edu.harvard.iq.dataverse.externaltools.ExternalToolServiceBean;
import edu.harvard.iq.dataverse.guestbook.GuestbookResponseDialog;
import edu.harvard.iq.dataverse.guestbook.GuestbookResponseServiceBean;
import edu.harvard.iq.dataverse.license.TermsOfUseFormMapper;
import edu.harvard.iq.dataverse.mail.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseRepository;
import edu.harvard.iq.dataverse.persistence.datafile.license.TermsOfUseForm;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.validation.DatasetFieldValidationService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
@Transactional(ROLLBACK)
public class DatasetFilesTabIT extends WebappArquillianDeployment {

    DatasetFilesTabStubbed filesTab;

    @Inject
    DatasetRepository datasetRepo;
    @Mock
    FileDownloadHelper fileDownloadHelper;
    @Inject
    DataFileServiceBean datafileService;
    @Inject
    PermissionServiceBean permissionService;
    @Mock
    PermissionsWrapper permissionsWrapper;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject
    DataverseSession session;
    @Inject
    GuestbookResponseServiceBean guestbookResponseService;
    @Inject
    EmbargoAccessService embargoAccess;
    @Inject
    SettingsServiceBean settingsService;
    @Inject
    SystemConfig config;;
    @Inject
    EjbDataverseEngine commandEngine;
    @Inject
    ExternalToolServiceBean externalToolService;
    @Inject
    TermsOfUseFormMapper termsOfUseFormMapper;
    @Mock
    FileDownloadRequestHelper fileDownloadRequestHelper;
    @Mock
    GuestbookResponseDialog guestbookResponseDialog;
    @Inject
    ImageThumbConverter imageThumbConverter;
    @Inject
    FileMetadataService fileMetadataService;

    DatasetFilesTabFacade datasetFilesTabFacade;
    @Inject
    ConfirmEmailServiceBean confirmEmailService;
    @Inject
    DatasetFieldValidationService fieldValidationService;
    @Inject
    DatasetDao datasetDao;
    @Inject
    DatasetVersionRepository datasetVersionRepository;
    @Inject
    AuthenticatedUserRepository userRepo;
    @Inject
    DataverseService dataverseService;

    @Inject
    LicenseRepository licenseRepo;
    @Mock
    UIComponent ui;
    @Mock
    Behavior behavior;

    Dataset dataset;

    @BeforeEach
    public void setUp() {
        this.dataset = this.datasetRepo.getById(52L);
        this.datasetFilesTabFacade = new DatasetFilesTabFacade(
                datasetVersionRepository, fileDownloadHelper, datasetDao);
        this.filesTab = new DatasetFilesTabStubbed(fileDownloadHelper,
                datafileService,
                permissionService, permissionsWrapper,
                dvRequestService, session,
                guestbookResponseService, embargoAccess,
                settingsService, commandEngine,
                externalToolService, termsOfUseFormMapper,
                fileDownloadRequestHelper, 
                guestbookResponseDialog, imageThumbConverter,
                fileMetadataService, datasetFilesTabFacade,
                confirmEmailService, fieldValidationService, config);
        this.filesTab.init(this.dataset.getLatestVersion());
        this.filesTab.setFileLabelSearchTerm("");

        AuthenticatedUser user = this.userRepo.getById(2L);
        this.session.logIn(user);

        when(this.permissionsWrapper.canViewUnpublishedDataset(any()))
                .thenReturn(TRUE);
    }

    @Test
    public void updateLicenseOfSigneFile_forUnreleasedDatasetVersion_works() {
        // select single file
        FileMetadata fileMeta = fileMatadatasOfLatestVersion().get(0);
        assertNotForDistributionStateOf(fileMeta);

        this.filesTab.onRowSelectByCheckbox(
                new SelectEvent<FileMetadata>(this.ui, this.behavior, fileMeta));
        // trigger license change for selected file
        this.filesTab.saveTermsOfUse(licenseApacheForm());

        assertThatUpdateSucceded();
        
        fileMeta = this.dataset.getLatestVersion().getFileMetadatas().get(0);
        assertApacheLicenseStateOf(fileMeta);
        
        fileMeta = fileMatadatasOfLatestVersion().get(0);
        assertApacheLicenseStateOf(fileMeta);
    }

    @Test
    public void updateLicenseOfSigneFile_forReleasedDatasetVersion_works() {
        publishDatasetUnderTest();
        // select single file
        FileMetadata fileMeta = fileMatadatasOfLatestVersion().get(0);
        assertNotForDistributionStateOf(fileMeta);

        this.filesTab.onRowSelectByCheckbox(
                new SelectEvent<FileMetadata>(this.ui, this.behavior, fileMeta));
        // trigger license change for selected file
        this.filesTab.saveTermsOfUse(licenseApacheForm());

        assertThatUpdateSucceded();
        
        fileMeta = this.dataset.getLatestVersion().getFileMetadatas().get(0);
        assertApacheLicenseStateOf(fileMeta);
        
        fileMeta = fileMatadatasOfLatestVersion().get(0);
        assertApacheLicenseStateOf(fileMeta);
    }

    @Test
    public void updateLicenseOfAllFiles_forUnreleasedDatasetVersion_works() {
        List<FileMetadata> files = fileMatadatasOfLatestVersion();
        assertThat(files.size()).isEqualTo(3);
        assertNotForDistributionStateOf(files.get(0));
        assertCC0LicenseStateOf(files.get(1));
        assertCC0LicenseStateOf(files.get(2));
        // select all files
        this.filesTab.getFileMetadatasSearch().load(0, 10, emptyMap(),  emptyMap());
        this.filesTab.selectAllFiles();
        // trigger license change for all files
        this.filesTab.saveTermsOfUse(licenseApacheForm());

        assertThatUpdateSucceded();
        
        files = this.dataset.getLatestVersion().getFileMetadatas();
        assertThat(files.size()).isEqualTo(3);
        assertApacheLicenseStateOf(files.get(0));
        assertApacheLicenseStateOf(files.get(1));
        assertApacheLicenseStateOf(files.get(2));
        
        files = fileMatadatasOfLatestVersion();
        assertThat(files.size()).isEqualTo(3);
        assertApacheLicenseStateOf(files.get(0));
        assertApacheLicenseStateOf(files.get(1));
        assertApacheLicenseStateOf(files.get(2));
    }

    @Test
    public void bulkUpdateLicenseOfAllFiles_forReleasedDatasetVersion_works() {
        publishDatasetUnderTest();
        List<FileMetadata> files = fileMatadatasOfLatestVersion();
        assertThat(files.size()).isEqualTo(3);
        assertNotForDistributionStateOf(files.get(0));
        assertCC0LicenseStateOf(files.get(1));
        assertCC0LicenseStateOf(files.get(2));
        // select all files
        this.filesTab.getFileMetadatasSearch().load(0, 10, emptyMap(),  emptyMap());
        this.filesTab.selectAllFiles();
        // trigger license change for selected file
        this.filesTab.saveTermsOfUse(licenseApacheForm());

        assertThatUpdateSucceded();
        
        files = this.dataset.getLatestVersion().getFileMetadatas();
        assertThat(files.size()).isEqualTo(3);
        assertApacheLicenseStateOf(files.get(0));
        assertApacheLicenseStateOf(files.get(1));
        assertApacheLicenseStateOf(files.get(2));
        
        files = fileMatadatasOfLatestVersion();
        assertThat(files.size()).isEqualTo(3);
        assertApacheLicenseStateOf(files.get(0));
        assertApacheLicenseStateOf(files.get(1));
        assertApacheLicenseStateOf(files.get(2));
    }
    
    @Test
    public void bulkUpdateLicenseOfAllFiles_thenSingleUpdate_forReleasedDatasetVersion_works() {
        publishDatasetUnderTest();
        List<FileMetadata> files = fileMatadatasOfLatestVersion();
        assertThat(files.size()).isEqualTo(3);
        assertNotForDistributionStateOf(files.get(0));
        assertCC0LicenseStateOf(files.get(1));
        assertCC0LicenseStateOf(files.get(2));
        // select all files
        this.filesTab.getFileMetadatasSearch().load(0, 10, emptyMap(),  emptyMap());
        this.filesTab.selectAllFiles();
        // trigger license change for selected file
        this.filesTab.saveTermsOfUse(licenseApacheForm());

        assertThatUpdateSucceded();
        
        files = this.dataset.getLatestVersion().getFileMetadatas();
        assertThat(files.size()).isEqualTo(3);
        assertApacheLicenseStateOf(files.get(0));
        assertApacheLicenseStateOf(files.get(1));
        assertApacheLicenseStateOf(files.get(2));
        
        files = fileMatadatasOfLatestVersion();
        assertThat(files.size()).isEqualTo(3);
        assertApacheLicenseStateOf(files.get(0));
        assertApacheLicenseStateOf(files.get(1));
        assertApacheLicenseStateOf(files.get(2));
        
        this.filesTab.updateFailed = false;
        this.filesTab.bannerMessagePrinted = false;
        // select single file
        FileMetadata fileMeta = files.get(0);

        this.filesTab.onRowSelectByCheckbox(
                new SelectEvent<FileMetadata>(this.ui, this.behavior, fileMeta));
        // trigger license change for selected file
        this.filesTab.saveTermsOfUse(licenseCC0Form());

        assertThatUpdateSucceded();
        
        fileMeta = this.dataset.getLatestVersion().getFileMetadatas().get(0);
        assertCC0LicenseStateOf(fileMeta);
        
        fileMeta = fileMatadatasOfLatestVersion().get(0);
        assertCC0LicenseStateOf(fileMeta);
    }
    
    private static TermsOfUseForm licenseApacheForm() {
        final TermsOfUseForm form = new TermsOfUseForm();
        form.setTypeWithLicenseId(LICENSE_BASED.name().concat(":8"));
        return form;
    }
    
    private static TermsOfUseForm licenseCC0Form() {
        final TermsOfUseForm form = new TermsOfUseForm();
        form.setTypeWithLicenseId(LICENSE_BASED.name().concat(":1"));
        return form;
    }
    
    private List<FileMetadata> fileMatadatasOfLatestVersion() {
        
        return this.datasetRepo.getById(52L).getLatestVersion().getFileMetadatas();
    }

    private void publishDatasetUnderTest() {
        final Dataverse owner = this.dataset.getOwner();
        final Dataverse root = owner.getOwner();

        this.dataverseService.publishDataverse(root);
        this.dataverseService.publishDataverse(owner);

        dataset.setGlobalIdCreateTime(new Date());

        final PublishDatasetResult result = commandEngine.submit(
                new PublishDatasetCommand(this.dataset,
                        dvRequestService.getDataverseRequest(), false));

        assertThat(result.isCompleted()).isTrue();
        assertThat(root.isReleased()).isTrue();
        assertThat(owner.isReleased()).isTrue();
        assertThat(this.dataset.isReleased()).isTrue();
    }

    private void assertNotForDistributionStateOf(final FileMetadata fileMeta) {
        FileTermsOfUse terms = fileMeta.getTermsOfUse();
        assertThat(terms.getLicense()).isNull();
        assertThat(terms.getRestrictCustomText()).isNull();
        assertThat(terms.getRestrictType()).isEqualTo(NOT_FOR_REDISTRIBUTION);
        assertThat(terms.getTermsOfUseType()).isEqualTo(RESTRICTED);
        assertThat(terms.isAllRightsReserved()).isFalse();
    }

    private void assertCC0LicenseStateOf(final FileMetadata fileMeta) {
        FileTermsOfUse terms = fileMeta.getTermsOfUse();
        assertThat(terms.getLicense().getId()).isEqualTo(1);
        assertThat(terms.getLicense().getName()).isEqualTo(CCO_LICENSE_NAME);
        assertThat(terms.getRestrictCustomText()).isNull();
        assertThat(terms.getRestrictType()).isNull();
        assertThat(terms.getTermsOfUseType()).isEqualTo(LICENSE_BASED);
        assertThat(terms.isAllRightsReserved()).isFalse();
    }

    private void assertApacheLicenseStateOf(final FileMetadata fileMeta) {
        FileTermsOfUse terms = fileMeta.getTermsOfUse();
        assertThat(terms.getLicense().getId()).isEqualTo(8);
        assertThat(terms.getLicense().getName()).isEqualTo("Apache Software License 2.0");
        assertThat(terms.getRestrictCustomText()).isNull();
        assertThat(terms.getRestrictType()).isNull();
        assertThat(terms.getTermsOfUseType()).isEqualTo(LICENSE_BASED);
        assertThat(terms.isAllRightsReserved()).isFalse();
    }

    private void assertThatUpdateSucceded() {
        assertThat(this.filesTab.updateFailed).isFalse();
        assertThat(this.filesTab.bannerMessagePrinted).isTrue();
    }

    @SuppressWarnings("serial")
    private static class DatasetFilesTabStubbed extends DatasetFilesTab {

        boolean updateFailed = false;
        boolean bannerMessagePrinted = false;

        public DatasetFilesTabStubbed(FileDownloadHelper fileDownloadHelper,
                DataFileServiceBean datafileService,
                PermissionServiceBean permissionService,
                PermissionsWrapper permissionsWrapper,
                DataverseRequestServiceBean dvRequestService,
                DataverseSession session,
                GuestbookResponseServiceBean guestbookResponseService,
                EmbargoAccessService embargoAccess,
                SettingsServiceBean settingsService,
                EjbDataverseEngine commandEngine,
                ExternalToolServiceBean externalToolService,
                TermsOfUseFormMapper termsOfUseFormMapper,
                FileDownloadRequestHelper fileDownloadRequestHelper,
                GuestbookResponseDialog guestbookResponseDialog,
                ImageThumbConverter imageThumbConverter,
                FileMetadataService fileMetadataService,
                DatasetFilesTabFacade datasetFilesTabFacade,
                ConfirmEmailServiceBean confirmEmailService,
                DatasetFieldValidationService fieldValidationService,
                SystemConfig config) {
            super(fileDownloadHelper, datafileService, permissionService,
                    permissionsWrapper,
                    dvRequestService, session, guestbookResponseService,
                    embargoAccess,
                    settingsService, commandEngine, externalToolService,
                    termsOfUseFormMapper,
                    fileDownloadRequestHelper, 
                    guestbookResponseDialog,
                    imageThumbConverter, fileMetadataService, datasetFilesTabFacade,
                    confirmEmailService, fieldValidationService, config);
        }

        @Override
        protected void populateDatasetUpdateFailureMessage() {
            this.updateFailed = true;
        }

        @Override
        protected void printBannerMessage() {
            this.bannerMessagePrinted = true;
        }
    }
}
