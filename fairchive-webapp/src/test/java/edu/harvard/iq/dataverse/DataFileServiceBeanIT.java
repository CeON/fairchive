package edu.harvard.iq.dataverse;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;

@Transactional(TransactionMode.ROLLBACK)
public class DataFileServiceBeanIT extends WebappArquillianDeployment {

	@Inject
	private DataFileServiceBean service;
	
	@Test
	void findReplacementFile() {

		assertThat(this.service.findReplacementFile(53L)).isEmpty();

		DataFile file = this.service.find(55L).get();
		file.setPreviousDataFileId(53L);
		this.service.save(file);

		assertThat(this.service.findReplacementFile(53L).get().getId()).
			isEqualTo(55L);
	}
	
	@Test
	void findAllRelatedByRootDatafileId() {
		
		assertThat(this.service.findAllRelatedByRootDatafileId(53L)).isEmpty();
		
		DataFile file = this.service.find(55L).get();
		file.setRootDataFileId(53L);
		this.service.save(file);
		
		List<DataFile> list = this.service.findAllRelatedByRootDatafileId(53L);
		assertThat(list.size()).isOne();
		assertThat(list.get(0).getId()).isEqualTo(55L);
	}
	
	@Test
	void hasReplacement() throws Exception {
		
		DataFile replacedFile = new DataFile();

		assertThat(this.service.hasReplacement(replacedFile)).isFalse();
		
		replacedFile.setId(53L);
		
		assertThat(this.service.hasReplacement(replacedFile)).isFalse();

		DataFile newFile = this.service.find(55L).get();
		newFile.setPreviousDataFileId(53L);
		this.service.save(newFile);

		assertThat(this.service.hasReplacement(replacedFile)).isTrue();
	}
	
	@Test
	void findDataFilesByFileMetadataIds() {
		
		List<Long>  ids = this.service.find(55L).get().getFileMetadatas().
				stream().map(FileMetadata::getId).collect(toList());
		
		List<DataFile> list = this.service.findDataFilesByFileMetadataIds(ids);
		
		assertThat(list.size()).isOne();
		assertThat(list.get(0).getId()).isEqualTo(55L);
	}
	
	@Test
	void findFileMetadataByDatasetVersionIdAndDataFileId() {
			
		assertThat(this.service.findFileMetadataByDatasetVersionIdAndDataFileId(36L, 55L)
				.get().getId()).isEqualTo(112L);
	}
	
}
