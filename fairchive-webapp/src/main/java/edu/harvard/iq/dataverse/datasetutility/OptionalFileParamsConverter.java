package edu.harvard.iq.dataverse.datasetutility;

import javax.ejb.Stateless;
import javax.inject.Inject;

import edu.harvard.iq.dataverse.api.dto.FileTermsOfUseDTO;
import edu.harvard.iq.dataverse.datafile.FileParams;
import edu.harvard.iq.dataverse.license.TermsOfUseFactory;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseRepository;

@Stateless
public class OptionalFileParamsConverter {

    private LicenseRepository licenseRepository;
    private TermsOfUseFactory termsOfUseFactory;

    OptionalFileParamsConverter() {}

    @Inject
    public OptionalFileParamsConverter(LicenseRepository licenseRepository, TermsOfUseFactory termsOfUseFactory) {
        this.licenseRepository = licenseRepository;
        this.termsOfUseFactory = termsOfUseFactory;
    }

    public FileParams toFileParams(OptionalFileParams optParams) {
        FileParams params = new FileParams();
        
        if (optParams.hasDescription()) {
            params.withDescription(optParams.getDescription());
        }
        if (optParams.hasCategories()) {
            params.withCategories(optParams.getCategories());
        }
        params.withTerms(convertTerms(optParams.getFileTermsOfUseDTO()));
        
        return params;
    }

    private FileTermsOfUse convertTerms(FileTermsOfUseDTO termsDTO) {
        if(termsDTO.getTermsType().equals(FileTermsOfUse.TermsOfUseType.LICENSE_BASED.toString())) {
            License license = licenseRepository.findActiveOrderedByPosition()
                    .stream()
                    .filter(l -> l.getName().equals(termsDTO.getLicense()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("There is no active license with name: " + termsDTO.getLicense()));

            return termsOfUseFactory.createTermsOfUseFromLicense(license);
        }

        if(termsDTO.getTermsType().equals(FileTermsOfUse.TermsOfUseType.ALL_RIGHTS_RESERVED.toString())) {
            return termsOfUseFactory.createAllRightsReservedTermsOfUse();
        }

        if(termsDTO.getTermsType().equals(FileTermsOfUse.TermsOfUseType.RESTRICTED.toString())) {
            if(!termsDTO.getAccessConditions().equals(FileTermsOfUse.RestrictType.CUSTOM.toString())) {
                return termsOfUseFactory.createRestrictedTermsOfUse(FileTermsOfUse.RestrictType.valueOf(termsDTO.getAccessConditions()));
            } else {
                return termsOfUseFactory.createRestrictedCustomTermsOfUse(termsDTO.getAccessConditionsCustomText());
            }
        }
        throw new RuntimeException("Unknown terms type");
    }
}
