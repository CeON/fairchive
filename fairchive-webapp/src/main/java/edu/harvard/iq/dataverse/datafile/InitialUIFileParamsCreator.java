package edu.harvard.iq.dataverse.datafile;

import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;

import edu.harvard.iq.dataverse.license.TermsOfUseFactory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseRepository;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;

/**
 * Creates initial {@link FileParams} values for files being added to a dataset.
 *
 * <p>This helper is used by file upload/replacement workflows to prepopulate
 * UI-level file metadata before the files are finally saved.</p>
 *
 * <p>Currently, it initializes the file terms of use. When configured to do so,
 * it attempts to reuse terms already present on dataset files and/or files
 * already uploaded in the current operation, but only if all examined files
 * have matching terms. If no common terms can be determined, it falls back to
 * terms created from the first active license.</p>
 *
 * <p>The class is responsible only for choosing initial default values; it does
 * not take part in persisting files or enforcing final validation/business 
 * rules for file metadata.</p>
 */
@Stateless
public class InitialUIFileParamsCreator {

    private LicenseRepository licenseRepository;
    private TermsOfUseFactory termsOfUseFactory;
    private SettingsServiceBean settingsService;
    

    InitialUIFileParamsCreator() {}
    
    @Inject
    public InitialUIFileParamsCreator(LicenseRepository licenseRepository, TermsOfUseFactory termsOfUseFactory,
            SettingsServiceBean settingsService) {
        this.licenseRepository = licenseRepository;
        this.termsOfUseFactory = termsOfUseFactory;
        this.settingsService = settingsService;
    }

    public FileParams createInitialFileParams(Dataset dataset, List<DataFile> newFiles) {
        return new FileParams()
                .withTerms(initialTermsOfUse(dataset, newFiles));
    }


    private FileTermsOfUse initialTermsOfUse(Dataset dataset, List<DataFile> newFiles) {

        if (!settingsService.isTrueForKey(Key.FillDefaultTermsOfUseUsingExistingFiles)) {
            return termsOfUseFactory.createTermsOfUseFromLicense(licenseRepository.findFirstActive());
        }

        return pickFirstTermsOfUse(dataset, newFiles)
                .filter(t -> allTermsMatch(dataset, newFiles, t))
                .orElseGet(() -> termsOfUseFactory.createTermsOfUseFromLicense(licenseRepository.findFirstActive()));
    }


    private boolean allTermsMatch(Dataset dataset, List<DataFile> newFiles, FileTermsOfUse t) {
        return allTermsMatch(newFiles, t) && allTermsMatch(dataset, t);
    }
    private boolean allTermsMatch(List<DataFile> newFiles, FileTermsOfUse t) {
        return newFiles.stream()
                .map(f -> f.getLatestFileMetadata().getTermsOfUse())
                .allMatch(t::isSameAs);
    }
    private boolean allTermsMatch(Dataset dataset, FileTermsOfUse t) {
        return dataset.getLatestVersion().getFileMetadatas().stream()
                .map(fm -> fm.getTermsOfUse())
                .allMatch(t::isSameAs);
    }

    private Optional<FileTermsOfUse> pickFirstTermsOfUse(Dataset dataset, List<DataFile> newFiles) {
        if (!dataset.getLatestVersion().getFileMetadatas().isEmpty()) {
            return Optional.of(dataset.getLatestVersion().getFileMetadatas().get(0).getTermsOfUse());
        } else if (!newFiles.isEmpty()) {
            return Optional.of(newFiles.get(0).getLatestFileMetadata().getTermsOfUse());
        }
        return Optional.empty();
    }
}
