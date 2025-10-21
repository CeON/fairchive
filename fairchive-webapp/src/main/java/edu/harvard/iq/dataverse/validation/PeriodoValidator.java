package edu.harvard.iq.dataverse.validation;

import static edu.harvard.iq.dataverse.validation.ValidationResult.invalid;
import static edu.harvard.iq.dataverse.validation.ValidationResult.ok;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.persistence.dataset.PeriodoDictionary;

@Stateless
public class PeriodoValidator {

    private final static String prefix = "http://n2t.net/ark:/99152/p";

    public ValidationResult validate(final String periodUrl) {
        if (isNotBlank(periodUrl) 
                && periodUrl.startsWith(prefix)
                && periodUrl.substring(prefix.length()).matches("[a-z0-9]{10}")) {
               return  PeriodoDictionary.getByUrl(periodUrl).isPresent()
                       ? ok()
                       : invalid("periodo.invalidId");
        } else {
                return invalid("periodo.invalid.format");
        }
    }
}
