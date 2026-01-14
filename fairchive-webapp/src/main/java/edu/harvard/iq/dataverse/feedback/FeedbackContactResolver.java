package edu.harvard.iq.dataverse.feedback;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseContact;

/**
 * Service for resolving point of contact for dataverses and datasets.
 *
 * @author Krzysztof Mądry, Rafał Ścipień
 */
@SuppressWarnings("serial")
@ApplicationScoped
public class FeedbackContactResolver implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackContactResolver.class);

    /**
     * Returns points of contacts for the given dataverse.
     * Points of contact for dataverse are emails located in
     * {@link Dataverse#getDataverseContacts()}.
     */
    public List<FeedbackContact> resolveDataverseContact(Dataverse dataverse) {
        List<FeedbackContact> dataverseContacts = new ArrayList<>();
        for (DataverseContact dc : dataverse.getDataverseContacts()) {
            FeedbackContact dataverseContact = new FeedbackContact(dc.getContactEmail());
            dataverseContacts.add(dataverseContact);
        }
        return dataverseContacts;
    }

    /**
     * Returns points of contacts for the given dataset.
     * Points of contact for dataset are taken from dataset metadata. That is
     * from {@link DatasetFieldConstant#datasetContact} fields.
     */
    public List<FeedbackContact> resolveDatasetContact(Dataset dataset) {
        List<FeedbackContact> datasetContacts = new ArrayList<>();
        for (DatasetField dsf : dataset.getLatestVersion().getDatasetFields()) {
            if (dsf.getTypeName().equals(DatasetFieldConstant.datasetContact)) {
                String contactName = null;
                String contactEmail = null;

                for (DatasetField subField : dsf.getDatasetFieldsChildren()) {
                    if (subField.getTypeName().equals(DatasetFieldConstant.datasetContactName)) {
                        contactName = subField.getValue();
                        logger.debug("contactName: {}", contactName);
                    }
                    if (subField.getTypeName().equals(DatasetFieldConstant.datasetContactEmail)) {
                        contactEmail = subField.getValue();
                        logger.debug("contactEmail: {}", contactEmail);
                    }

                }

                if (contactEmail != null) {
                    FeedbackContact datasetContact = new FeedbackContact(contactName, contactEmail);
                    datasetContacts.add(datasetContact);
                }

            }
        }
        return datasetContacts;
    }
}
