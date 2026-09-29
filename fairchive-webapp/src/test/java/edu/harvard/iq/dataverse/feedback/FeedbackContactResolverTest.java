package edu.harvard.iq.dataverse.feedback;

import static edu.harvard.iq.dataverse.persistence.MocksFactory.create;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeDataset;
import static edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion.State.RELEASED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion;
import io.vavr.control.Option;

/**
 * @author Krzysztof Mądry, Rafał Ścipień
 */
public class FeedbackContactResolverTest {

    private FeedbackContactResolver contactResolver = new FeedbackContactResolver();

    @Test
    public void resolveDataverseContact() {
        // given
        Dataverse dataverse = new Dataverse();
        DataverseVersion version = dataverse.addVersion(RELEASED);
        version.addContact("dvContact1@librascholar.edu");
        version.addContact("dvContact2@librascholar.edu");

        // when
        List<FeedbackContact> contacts = contactResolver.resolveDataverseContact(dataverse);

        // them
        assertThat(contacts).hasSize(2)
            .extracting(FeedbackContact::getName, FeedbackContact::getEmail)
            .containsExactly(
                    tuple(Option.none(), "dvContact1@librascholar.edu"),
                    tuple(Option.none(), "dvContact2@librascholar.edu"));
    }

    @Test
    public void resolveDataverseContact__no_contact() {
        // given
        Dataverse dataverse = new Dataverse();
        DataverseVersion version = dataverse.addVersion(RELEASED);

        // when
        List<FeedbackContact> contacts = contactResolver.resolveDataverseContact(dataverse);

        // them
        assertThat(contacts).isEmpty();
    }


    @Test
    public void resolveDatasetContact() {
        // given
        Dataset dataset = makeDataset();
        
        List<DatasetField> fields = new ArrayList<>();
        fields.add(create(DatasetFieldConstant.datasetContact, "",
                        create(DatasetFieldConstant.datasetContactName, "Brady, Tom"),
                        create(DatasetFieldConstant.datasetContactEmail, "tom@brady.com")));
        fields.add(create(DatasetFieldConstant.datasetContact, "",
                        create(DatasetFieldConstant.datasetContactEmail, "jogn@doe.com")));
        
        dataset.getLatestVersion().setDatasetFields(fields);

        // when
        List<FeedbackContact> contacts = contactResolver.resolveDatasetContact(dataset);

        // them
        assertThat(contacts).hasSize(2)
            .extracting(FeedbackContact::getName, FeedbackContact::getEmail)
            .containsExactly(
                    tuple(Option.of("Brady, Tom"), "tom@brady.com"),
                    tuple(Option.none(), "jogn@doe.com"));
    }

    @Test
    public void resolveDatasetContact__no_email() {
        // given
        Dataset dataset = makeDataset();
        
        List<DatasetField> fields = new ArrayList<>();
        fields.add(create(DatasetFieldConstant.datasetContact, "",
                        create(DatasetFieldConstant.datasetContactName, "Brady, Tom")));
        
        dataset.getLatestVersion().setDatasetFields(fields);

        // when
        List<FeedbackContact> contacts = contactResolver.resolveDatasetContact(dataset);

        // them
        assertThat(contacts).isEmpty();
    }

    @Test
    public void resolveDatasetContact__no_dataset_contact_parent() {
        // given
        Dataset dataset = makeDataset();
        
        List<DatasetField> fields = new ArrayList<>();
        fields.add(create(DatasetFieldConstant.datasetContactEmail, "tom@brady.com"));
        fields.add(create(DatasetFieldConstant.datasetContactEmail, "jogn@doe.com"));
        
        dataset.getLatestVersion().setDatasetFields(fields);

        // when
        List<FeedbackContact> contacts = contactResolver.resolveDatasetContact(dataset);

        // them
        assertThat(contacts).isEmpty();
    }
}
