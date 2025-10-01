package edu.harvard.iq.dataverse.mocks;

import edu.harvard.iq.dataverse.validation.PasswordValidatorServiceBean;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@SuppressWarnings("serial")
public class MockPasswordValidatorServiceBean extends PasswordValidatorServiceBean {

    @Override
    public List<String> validate(String password, Date passwordModificationTime, boolean isHumanReadable) {
        return Collections.emptyList();
    }

}
