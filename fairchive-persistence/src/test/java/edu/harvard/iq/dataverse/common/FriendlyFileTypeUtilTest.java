package edu.harvard.iq.dataverse.common;

import static edu.harvard.iq.dataverse.common.FriendlyFileTypeUtil.getUserFriendlyFileType;
import static edu.harvard.iq.dataverse.common.FriendlyFileTypeUtil.getUserFriendlyFileTypeForDisplay;
import static edu.harvard.iq.dataverse.common.files.mime.MimeTypes.CSV;
import static edu.harvard.iq.dataverse.common.files.mime.MimeTypes.DOCUMENT_PDF;
import static edu.harvard.iq.dataverse.common.files.mime.MimeTypes.SHAPEFILE;
import static edu.harvard.iq.dataverse.common.files.mime.MimeTypes.UNDETERMINED_DEFAULT;
import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;

public class FriendlyFileTypeUtilTest {

	
	@Test
	void fiendlyNameOfFileType() {
		
		assertThat(getUserFriendlyFileTypeForDisplay(CSV)).isEqualTo("Comma Separated Values");
		assertThat(getUserFriendlyFileTypeForDisplay(DOCUMENT_PDF.concat(";comething"))).isEqualTo("Adobe PDF");
		assertThat(getUserFriendlyFileTypeForDisplay(UNDETERMINED_DEFAULT)).isEqualTo("Unknown");
		assertThat(getUserFriendlyFileTypeForDisplay(SHAPEFILE)).isEqualTo("Shapefile as ZIP Archive");
		assertThat(getUserFriendlyFileTypeForDisplay("unknown/mime")).isEqualTo("Unknown");
		
		assertThat(getUserFriendlyFileType(new DataFile(CSV), ENGLISH)).isEqualTo("Data");
		assertThat(getUserFriendlyFileType(new DataFile(DOCUMENT_PDF.concat(";comething")), ENGLISH)).isEqualTo("Document");
		assertThat(getUserFriendlyFileType(new DataFile(UNDETERMINED_DEFAULT), ENGLISH)).isEqualTo("Unknown");
		assertThat(getUserFriendlyFileType(new DataFile(SHAPEFILE), ENGLISH)).isEqualTo("Shape");
		assertThat(getUserFriendlyFileType(new DataFile("unknown/mime"), ENGLISH)).isEqualTo("Unknown");
	}
}
