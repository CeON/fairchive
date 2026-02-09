package edu.harvard.iq.dataverse.datafile;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.AntivirusScannerEnabled;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.AntivirusScannerMaxFileSize;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.AntivirusScannerMaxFileSizeForExecutables;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.AntivirusScannerSocketAddress;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.AntivirusScannerSocketPort;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.AntivirusScannerSocketTimeout;
import static java.nio.file.Files.write;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class AntivirFileScannerTest {

	private final static long MAX_FILE_SIZE = 10;
	private final static long MAX_EXEC_FILE_SIZE = 5;
	// https://www.eicar.org/download-anti-malware-testfile/
	private final static String EICAR = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";

	@Mock
	SettingsServiceBean settings;

	@InjectMocks
	AntivirFileScanner scanner;

	@TempDir
	Path dir;

	@BeforeEach
	void setUp() {
		when(settings.isTrueForKey(eq(AntivirusScannerEnabled))).thenReturn(true);
		when(settings.getValueForKey(eq(AntivirusScannerSocketAddress))).thenReturn("localhost");
		when(settings.getValueForKeyAsInt(eq(AntivirusScannerSocketPort))).thenReturn(3310);
		when(settings.getValueForKeyAsInt(eq(AntivirusScannerSocketTimeout))).thenReturn(10000);

		when(settings.getValueForKeyAsLong(eq(AntivirusScannerMaxFileSizeForExecutables)))
				.thenReturn(MAX_EXEC_FILE_SIZE);
		when(settings.getValueForKeyAsLong(eq(AntivirusScannerMaxFileSize))).thenReturn(MAX_FILE_SIZE);
	}

	@Disabled("Requires ClamD listening on localhost:3310")
	@Test
	void scaningCleanFile_reports_NotInfected() throws Exception {
		
		Path file = this.dir.resolve("file");
		write(file, "Hello.".getBytes());
		
		AntivirScannerResponse response = this.scanner.scan(file);
		
		assertThat(response.isFileInfected()).isFalse();
		assertThat(response.getMessage()).contains("OK");
	}
	
	@Disabled("Requires ClamD listening on localhost:3310")
	@Test
	void scaningEICARFile_reports_Infected() throws Exception {
		
		Path file = this.dir.resolve("file");
		write(file, EICAR.getBytes());	
		
		AntivirScannerResponse response = this.scanner.scan(file);
		
		assertThat(response.isFileInfected()).isTrue();
		assertThat(response.getMessage()).contains("FOUND");
	}

	@Test
	void isFileOverSizeLimit() throws Exception {

		Path smallFile = this.dir.resolve("small");
		write(smallFile, new byte[1]);

		assertThat(this.scanner.isFileOverSizeLimit(smallFile, "application/x-ms-installer")).isFalse();
		assertThat(this.scanner.isFileOverSizeLimit(smallFile, "text/plain")).isFalse();
		
		Path mediumFile = this.dir.resolve("meduim");
		write(mediumFile, new byte[8]);
		
		assertThat(this.scanner.isFileOverSizeLimit(mediumFile, "application/x-ms-installer")).isTrue();
		assertThat(this.scanner.isFileOverSizeLimit(mediumFile, "text/plain")).isFalse();
		
		Path largeFile = this.dir.resolve("large");
		write(largeFile, new byte[20]);
		
		assertThat(this.scanner.isFileOverSizeLimit(largeFile, "application/x-ms-installer")).isTrue();
		assertThat(this.scanner.isFileOverSizeLimit(largeFile, "text/plain")).isTrue();
	}
}
