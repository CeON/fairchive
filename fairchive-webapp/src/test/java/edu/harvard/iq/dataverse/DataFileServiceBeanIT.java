package edu.harvard.iq.dataverse;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;

@Transactional(TransactionMode.ROLLBACK)
public class DataFileServiceBeanIT extends WebappArquillianDeployment {

	@Inject
	private DataFileServiceBean srvice;
	
	@Test
	void findReplacementFile() {

		assertThat(this.srvice.findReplacementFile(53L)).isEmpty();

		DataFile file = this.srvice.find(55L).get();
		file.setPreviousDataFileId(53L);
		this.srvice.save(file);

		assertThat(this.srvice.findReplacementFile(53L).get().getId()).
			isEqualTo(55L);
	}
	
	@Test
	void findAllRelatedByRootDatafileId() {
		
		assertThat(this.srvice.findAllRelatedByRootDatafileId(53L)).isEmpty();
		
		DataFile file = this.srvice.find(55L).get();
		file.setRootDataFileId(53L);
		this.srvice.save(file);
		
		List<DataFile> list = this.srvice.findAllRelatedByRootDatafileId(53L);
		assertThat(list.size()).isOne();
		assertThat(list.get(0).getId()).isEqualTo(55L);
	}
	
	@Test
	void hasReplacement() throws Exception {
		
		DataFile replacedFile = new DataFile();

		assertThat(this.srvice.hasReplacement(replacedFile)).isFalse();
		
		replacedFile.setId(53L);
		
		assertThat(this.srvice.hasReplacement(replacedFile)).isFalse();

		DataFile newFile = this.srvice.find(55L).get();
		newFile.setPreviousDataFileId(53L);
		this.srvice.save(newFile);

		assertThat(this.srvice.hasReplacement(replacedFile)).isTrue();
	}
}
