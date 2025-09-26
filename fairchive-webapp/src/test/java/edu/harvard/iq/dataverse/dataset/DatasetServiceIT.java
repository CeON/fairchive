package edu.harvard.iq.dataverse.dataset;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import io.vavr.Value;

@Transactional(TransactionMode.ROLLBACK)
public class DatasetServiceIT extends WebappArquillianDeployment {

    @Inject
    private DatasetService datasetService;

    @Inject
    private DataverseSession dataverseSession;

    @Inject
    private AuthenticationServiceBean authenticationServiceBean;

    @Inject
    private DatasetRepository datasetRepo;

    @BeforeEach
    public void setUp() {
        dataverseSession.logIn(authenticationServiceBean.getAdminUser());
    }

    @Test
    public void removeDatasetThumbnail() {
        // given
        Dataset datasetWithFiles = find(52L);
        datasetWithFiles.setThumbnailFile(datasetWithFiles.getFiles().get(0));
        Dataset datasetWithThumbnail = this.datasetRepo.save(datasetWithFiles);

        // when
        datasetService.removeDatasetThumbnail(datasetWithThumbnail);

        // then
        Dataset updatedDataset = this.datasetRepo.findById(52L).get();
        assertThat(updatedDataset.getThumbnailFile()).isNull();
    }

    @Test
    public void changeDatasetThumbnail() {
        // given
        Dataset datasetWithFiles = find(52L);

        // when
        datasetService.changeDatasetThumbnail(datasetWithFiles, datasetWithFiles.getFiles().get(0));
        Dataset updatedDataset = find(52L);

        // then
        assertThat(updatedDataset.getThumbnailFile()).isEqualTo(datasetWithFiles.getFiles().get(0));
    }

    @Test
    public void shouldSetDatasetEmbargoDate() {
        // given
        Dataset draftDataset = find(66L);
        Date embargoDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));

        // when
        datasetService.setDatasetEmbargoDate(draftDataset, embargoDate);

        // then
        Dataset dbDataset = find(66L);
        assertThat(dbDataset.getEmbargoDate().isDefined()).isTrue();
        assertThat(dbDataset.getEmbargoDate().get()).isEqualTo(embargoDate);
    }

    @Test
    public void shouldLiftDatasetEmbargoDate() {
        // given
        Dataset draftDataset = find(66L);
        Date embargoDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
        draftDataset.setEmbargoDate(embargoDate);
        this.datasetRepo.save(draftDataset);

        // when
        datasetService.liftDatasetEmbargoDate(draftDataset);

        // then
        Dataset dbDataset = find(66L);
        assertThat(dbDataset.getEmbargoDate().isEmpty()).isTrue();
    }

    @Test
    public void updateLastChangeForExporterTime() {
        // given
        Dataset dataset = find(52L);
        Date lastChangeForExporterTime = dataset.getLastChangeForExporterTime().getOrNull();

        // when
        datasetService.updateLastChangeForExporterTime(dataset);

        // then
        assertThat(dataset.getLastChangeForExporterTime()).isNotEqualTo(lastChangeForExporterTime);
    }

    @Test
    public void updateAllLastChangeForExporterTime() {
        // when
        datasetService.updateAllLastChangeForExporterTime();

        // then
        List<Date> updatedTimeList = this.datasetRepo.findAll().stream()
                .filter(d -> !d.isHarvested())
                .map(Dataset::getLastChangeForExporterTime)
                .map(Value::getOrNull)
                .collect(Collectors.toList());
        Date first = updatedTimeList.get(0);
        assertThat(first).isNotNull();
        assertThat(updatedTimeList).allMatch(first::equals);
    }
    
    @Test
    public void shouldIndexAfterEmbargo( ) {
        List<Dataset> forIndexAfterEmbargo = this.datasetRepo.findNotIndexedAfterEmbargo();
        assertThat(forIndexAfterEmbargo).isNotEmpty();
        assertThat(forIndexAfterEmbargo.size()).isEqualTo(1);
        Dataset foundDataset = forIndexAfterEmbargo.get(0);
        assertThat(foundDataset.getId()).isEqualTo(101);
    }
    
    private Dataset find(long id) {
        return this.datasetRepo.findById(52L).get();
    }
}
