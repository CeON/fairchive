package edu.harvard.iq.dataverse;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;

@Transactional(TransactionMode.ROLLBACK)
public class DataFileServiceBeanIT extends WebappArquillianDeployment {

	@Inject
	private DataFileServiceBean fileService;
	
	@Test
	void findReplacementFile() {

		assertThat(this.fileService.findReplacementFile(53L)).isEmpty();

		DataFile file = this.fileService.find(55L).get();
		file.setPreviousDataFileId(53L);
		this.fileService.save(file);

		assertThat(this.fileService.findReplacementFile(53L).get().getId()).
			isEqualTo(55L);
	}
}
