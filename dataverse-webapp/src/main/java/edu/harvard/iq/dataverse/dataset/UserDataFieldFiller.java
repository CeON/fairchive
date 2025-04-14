package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
public class UserDataFieldFiller {

    private DatasetFieldServiceBean fieldService;

    private Clock clock = Clock.systemDefaultZone();

    // -------------------- CONSTRUCTORS --------------------

    public UserDataFieldFiller() { }

    @Inject
    public UserDataFieldFiller(DatasetFieldServiceBean fieldService) {
        this.fieldService = fieldService;
    }

    // -------------------- LOGIC --------------------

    public void fillUserDataInDatasetFields(List<DatasetField> datasetFields, AuthenticatedUser user) {

        String userFullName = user.getLastName() + ", " + user.getFirstName();
        String todayDate = LocalDate.now(clock).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        for (DatasetField dsf : datasetFields) {
            if (dsf.getTypeName().equals(DatasetFieldConstant.depositor) && dsf.isEmpty()) {
                dsf.setFieldValue(userFullName);
            }
            if (dsf.getTypeName().equals(DatasetFieldConstant.dateOfDeposit) && dsf.isEmpty()) {
                dsf.setFieldValue(todayDate);
            }

            if (dsf.getTypeName().equals(DatasetFieldConstant.datasetContact) && dsf.isEmpty()) {
                fillUserDataInContactField(dsf, user, userFullName);
            }

            if (dsf.getTypeName().equals(DatasetFieldConstant.author) && dsf.isEmpty()) {
                fillUserDataInAuthorField(dsf, user, userFullName);
            }
        }

    }

    protected void fillUserDataInContactField(DatasetField contactField, AuthenticatedUser user, String userFullName) {
        contactField.streamChildrenByTypeName(DatasetFieldConstant.datasetContactName)
            .forEach(f -> f.setFieldValue(userFullName));

        contactField.streamChildrenByTypeName(DatasetFieldConstant.datasetContactAffiliation)
            .forEach(f -> f.setFieldValue(user.getAffiliation()));

        contactField.streamChildrenByTypeName(DatasetFieldConstant.datasetContactEmail)
            .forEach(f -> f.setFieldValue(user.getEmail()));

    }

    protected void fillUserDataInAuthorField(DatasetField authorField, AuthenticatedUser user, String userFullName) {

        for (DatasetField subField : authorField.getDatasetFieldsChildren()) {
            if (subField.getTypeName().equals(DatasetFieldConstant.authorName)) {
                subField.setFieldValue(userFullName);
            }
            if (subField.getTypeName().equals(DatasetFieldConstant.authorAffiliation)) {
                subField.setFieldValue(user.getAffiliation());
            }
            if (subField.getTypeName().equals(DatasetFieldConstant.authorAffiliationIdentifier)) {
                subField.setFieldValue(user.getAffiliationROR());
            }
            if (user.getOrcid() != null) {
                if (subField.getTypeName().equals(DatasetFieldConstant.authorIdValue)) {
                    subField.setFieldValue(user.getOrcid());
                }
                if (subField.getTypeName().equals(DatasetFieldConstant.authorIdType)) {
                    DatasetFieldType authorIdTypeDatasetField = fieldService.findByName(DatasetFieldConstant.authorIdType);
                    ControlledVocabularyValue vocabValue = fieldService.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(authorIdTypeDatasetField, "ORCID", true);
                    subField.setSingleControlledVocabularyValue(vocabValue);
                }
            }
        }

    }

    // -------------------- SETTERS --------------------

    public void setClock(Clock clock) {
        this.clock = clock;
    }
}
