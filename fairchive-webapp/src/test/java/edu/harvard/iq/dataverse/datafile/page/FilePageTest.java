package edu.harvard.iq.dataverse.datafile.page;

import static edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState.DRAFT;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState.RELEASED;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import edu.harvard.iq.dataverse.externaltools.ExternalToolHandler;
import edu.harvard.iq.dataverse.externaltools.ExternalToolServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.Type;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalToolRepository;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class FilePageTest {
	
	@Mock
	private ExternalToolRepository externalToolsRepository;
	
    private ExternalToolServiceBean externalTools;
    private ExternalToolHandler externalToolHandler;
    private FileDownloadHelper fileDownloadHelper;

	private FilePage page;
	private DataFile file;
	private ExternalTool viewer;
	
	
	@BeforeEach
	void setUp() {
		
		initFile();
		initViewer();
		
		this.externalToolHandler = new ExternalToolHandler(null);
		this.externalTools = new ExternalToolServiceBean(this.externalToolsRepository);
		this.fileDownloadHelper = new FileDownloadHelper(null, null, null, null, null, this.externalTools);
		
		this.page = new FilePage(null,
				null, null,
				null, this.externalTools, null,
				null, this.fileDownloadHelper,
				null, null, null,
				null, null, this.externalToolHandler, 
				null, null, null);
		
		this.page.setFileId(1L);
		this.page.setVersion("1.0");
		this.page.setFile(this.file);
		this.page.setFileMetadata(this.file.getFileMetadata());
	}
	
	private void initFile() {
		
		// publicly accessible file
		this.file = new DataFile();
		this.file.setContentType("text/plain");
		this.file.setId(1L);
		this.file.setFilesize(2000);
		FileMetadata meta = new FileMetadata();
		meta.setId(1L);
		meta.setDataFile(this.file);
		meta.setTermsOfUse(new FileTermsOfUse());
		this.file.setFileMetadatas(singletonList(meta));
		Dataset set = new Dataset();
		set.setId(1L);
		DatasetVersion version = new DatasetVersion();
		version.setId(2L);
		version.setVersionState(RELEASED);
		set.getVersions().add(version);
		version.setDataset(set);
		meta.setDatasetVersion(version);
		this.file.setOwner(set);
	}
	
	private void initViewer() {
		
		this.viewer = new ExternalTool("viewer", "viewer", Type.PREVIEW, 
				"http://viwer.com", "{}", "image/png");
		
		when(this.externalToolsRepository.findAll()).thenReturn(singletonList(this.viewer));
	}
	
	
	@Test
	void displayPreviewTab() {
		
		// no viewer for text/plain is available
		assertThat(this.page.displayPreviewTab()).isFalse();
		
		this.file.setContentType("image/png");
		assertThat(this.file.getFileMetadata().isFilePubliclyAccessible()).isTrue();
		assertThat(this.page.displayPreviewTab()).isTrue();
		
		// restrict access
		this.file.getFileMetadata().getDatasetVersion().setVersionState(DRAFT);
		assertThat(this.file.getFileMetadata().isFilePubliclyAccessible()).isFalse();
		assertThat(this.fileDownloadHelper.canUserDownloadFile(this.file.getFileMetadata())).isTrue();
		assertThat(this.page.displayPreviewTab()).isFalse();
		
		this.viewer.setTrusted(true);
		assertThat(this.file.getFileMetadata().isFilePubliclyAccessible()).isFalse();
		assertThat(this.fileDownloadHelper.canUserDownloadFile(this.file.getFileMetadata())).isTrue();
		assertThat(this.page.displayPreviewTab()).isTrue();
	}
	
	
	@Test
	void isFileSizeUnderLimit() {
		
		// no viewer for text/plain is available
		assertThat(this.page.isFileSizeUnderLimit()).isFalse();
		
		this.file.setContentType("image/png");
		assertThat(this.page.isFileSizeUnderLimit()).isTrue();
		
		this.viewer.setFileSizeLimit(5000L);
		assertThat(this.page.isFileSizeUnderLimit()).isTrue();
		
		this.viewer.setFileSizeLimit(1000L);
		assertThat(this.page.isFileSizeUnderLimit()).isFalse();
	}
}
