package edu.harvard.iq.dataverse.datafile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.harvard.iq.dataverse.license.TermsOfUseFactory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseRepository;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;

@ExtendWith(MockitoExtension.class)
public class InitialUIFileParamsCreatorTest {

    @InjectMocks
    private InitialUIFileParamsCreator fileParamsCreator;
    @Mock
    private LicenseRepository licenseRepository;
    @Mock
    private TermsOfUseFactory termsOfUseFactory;
    @Mock
    private SettingsServiceBean settingsService;

    private License activeLicense = new License();
    private FileTermsOfUse activeLicenseTerms = new FileTermsOfUse();

    private License license1 = new License();
    private FileTermsOfUse license1Terms = new FileTermsOfUse();

    private License license2 = new License();
    private FileTermsOfUse license2Terms = new FileTermsOfUse();

    @BeforeEach
    void beforeEach() {
        activeLicense.setId(3L);
        activeLicense.setName("First active license");
        activeLicenseTerms.setLicense(activeLicense);

        license1.setId(1L);
        license1.setName("License 1");
        license1Terms.setLicense(license1);

        license2.setId(2L);
        license2.setName("License 2");
        license2Terms.setLicense(license2);

        when(settingsService.isTrueForKey(Key.FillDefaultTermsOfUseUsingExistingFiles)).thenReturn(true);
    }
    
    @Test
    void createInitialFileParams__no_dataset_file_or_new_files() {
        // given
        when(licenseRepository.findFirstActive()).thenReturn(activeLicense);
        when(termsOfUseFactory.createTermsOfUseFromLicense(activeLicense)).thenReturn(activeLicenseTerms);

        // when
        FileParams fileParams = fileParamsCreator.createInitialFileParams(new Dataset(), new ArrayList<>());
        // then
        assertThat(fileParams.getDescription()).isNull();
        assertThat(fileParams.getCategories()).isEmpty();
        assertThat(fileParams.getTermsOfUse())
            .extracting(FileTermsOfUse::getTermsOfUseType, tou -> tou.getLicense().getId())
            .containsExactly(TermsOfUseType.LICENSE_BASED, 3L);
    }

    @Test
    void createInitialFileParams__dataset_with_same_terms_for_files() {
        // given
        List<FileMetadata> fileMetadatas = new ArrayList<>();
        fileMetadatas.add(createFileMetadataWithTerms(license1Terms));
        fileMetadatas.add(createFileMetadataWithTerms(license1Terms));

        Dataset dataset = new Dataset();
        dataset.getLatestVersion().setFileMetadatas(fileMetadatas);

        // when
        FileParams fileParams = fileParamsCreator.createInitialFileParams(dataset, new ArrayList<>());
        // then
        assertThat(fileParams.getDescription()).isNull();
        assertThat(fileParams.getCategories()).isEmpty();
        assertThat(fileParams.getTermsOfUse())
            .extracting(FileTermsOfUse::getTermsOfUseType, tou -> tou.getLicense().getId())
            .containsExactly(TermsOfUseType.LICENSE_BASED, 1L);
    }

    @Test
    void createInitialFileParams__dataset_with_different_terms_for_files() {
        // given
        when(licenseRepository.findFirstActive()).thenReturn(activeLicense);
        when(termsOfUseFactory.createTermsOfUseFromLicense(activeLicense)).thenReturn(activeLicenseTerms);
        
        List<FileMetadata> fileMetadatas = new ArrayList<>();
        fileMetadatas.add(createFileMetadataWithTerms(license1Terms));
        fileMetadatas.add(createFileMetadataWithTerms(license2Terms));

        Dataset dataset = new Dataset();
        dataset.getLatestVersion().setFileMetadatas(fileMetadatas);

        // when
        FileParams fileParams = fileParamsCreator.createInitialFileParams(dataset, new ArrayList<>());
        // then
        assertThat(fileParams.getDescription()).isNull();
        assertThat(fileParams.getCategories()).isEmpty();
        assertThat(fileParams.getTermsOfUse())
            .extracting(FileTermsOfUse::getTermsOfUseType, tou -> tou.getLicense().getId())
            .containsExactly(TermsOfUseType.LICENSE_BASED, 3L);
    }

    @Test
    void createInitialFileParams__only_new_files_with_same_terms() {
        // given
        List<DataFile> newFiles = new ArrayList<>();
        newFiles.add(createDataFileWithTerms(license1Terms));
        newFiles.add(createDataFileWithTerms(license1Terms));
        
        // when
        FileParams fileParams = fileParamsCreator.createInitialFileParams(new Dataset(), newFiles);
        // then
        assertThat(fileParams.getDescription()).isNull();
        assertThat(fileParams.getCategories()).isEmpty();
        assertThat(fileParams.getTermsOfUse())
            .extracting(FileTermsOfUse::getTermsOfUseType, tou -> tou.getLicense().getId())
            .containsExactly(TermsOfUseType.LICENSE_BASED, 1L);
    }

    @Test
    void createInitialFileParams__only_new_files_with_different_terms() {
        // given
        when(licenseRepository.findFirstActive()).thenReturn(activeLicense);
        when(termsOfUseFactory.createTermsOfUseFromLicense(activeLicense)).thenReturn(activeLicenseTerms);

        List<DataFile> newFiles = new ArrayList<>();
        newFiles.add(createDataFileWithTerms(license1Terms));
        newFiles.add(createDataFileWithTerms(license2Terms));
        
        // when
        FileParams fileParams = fileParamsCreator.createInitialFileParams(new Dataset(), newFiles);
        // then
        assertThat(fileParams.getDescription()).isNull();
        assertThat(fileParams.getCategories()).isEmpty();
        assertThat(fileParams.getTermsOfUse())
            .extracting(FileTermsOfUse::getTermsOfUseType, tou -> tou.getLicense().getId())
            .containsExactly(TermsOfUseType.LICENSE_BASED, 3L);
    }

    @Test
    void createInitialFileParams__dataset_file_and_new_file_same_terms() {
        // given
        Dataset dataset = new Dataset();
        dataset.getLatestVersion().setFileMetadatas(Collections.singletonList(createFileMetadataWithTerms(license1Terms)));

        // when
        FileParams fileParams = fileParamsCreator.createInitialFileParams(dataset,
                Collections.singletonList(createDataFileWithTerms(license1Terms)));

        // then
        assertThat(fileParams.getDescription()).isNull();
        assertThat(fileParams.getCategories()).isEmpty();
        assertThat(fileParams.getTermsOfUse())
            .extracting(FileTermsOfUse::getTermsOfUseType, tou -> tou.getLicense().getId())
            .containsExactly(TermsOfUseType.LICENSE_BASED, 1L);
    }

    @Test
    void createInitialFileParams__dataset_file_and_new_file_different_terms() {
        // given
        when(licenseRepository.findFirstActive()).thenReturn(activeLicense);
        when(termsOfUseFactory.createTermsOfUseFromLicense(activeLicense)).thenReturn(activeLicenseTerms);

        Dataset dataset = new Dataset();
        dataset.getLatestVersion().setFileMetadatas(Collections.singletonList(createFileMetadataWithTerms(license1Terms)));

        // when
        FileParams fileParams = fileParamsCreator.createInitialFileParams(dataset,
                Collections.singletonList(createDataFileWithTerms(license2Terms)));

        // then
        assertThat(fileParams.getDescription()).isNull();
        assertThat(fileParams.getCategories()).isEmpty();
        assertThat(fileParams.getTermsOfUse())
            .extracting(FileTermsOfUse::getTermsOfUseType, tou -> tou.getLicense().getId())
            .containsExactly(TermsOfUseType.LICENSE_BASED, 3L);
    }

    private DataFile createDataFileWithTerms(FileTermsOfUse terms) {
        DataFile dataFile = new DataFile();
        FileMetadata fm = createFileMetadataWithTerms(terms);
        dataFile.setFileMetadatas(Collections.singletonList(fm));
        return dataFile;
    }

    private FileMetadata createFileMetadataWithTerms(FileTermsOfUse terms) {
        FileMetadata fm = new FileMetadata();
        fm.setTermsOfUse(terms);
        return fm;
    }
}
