package edu.harvard.iq.dataverse.datafile;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.AntivirusScannerEnabled;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.AntivirusScannerMaxFileSize;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.AntivirusScannerMaxFileSizeForExecutables;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.AntivirusScannerSocketAddress;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.AntivirusScannerSocketPort;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.AntivirusScannerSocketTimeout;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.size;
import static java.util.Arrays.asList;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Collection;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MediaTypeRegistry;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

@Stateless
public class AntivirFileScanner {

	private static final Collection<String> EXECUTABLE_SUBTYPES = asList("x-msdownload", "x-ms-installer",
			"java-archive");

	private static final int CHUNK_SIZE = 2048;

	private static final byte[] INSTREAM = new byte[] { 'z', 'I', 'N', 'S', 'T', 'R', 'E', 'A', 'M', 0 };

	private SettingsServiceBean settings;

	// -------------------- CONSTRUCTORS --------------------

	@Deprecated
	public AntivirFileScanner() {
	}

	@Inject
	public AntivirFileScanner(final SettingsServiceBean settings) {
		this.settings = settings;
	}

	// -------------------- LOGIC --------------------

	public boolean isEnabled() {
		return this.settings.isTrueForKey(AntivirusScannerEnabled);
	}

	public boolean isFileOverSizeLimit(final Path file, final String contentType) throws IOException {
		final SettingsServiceBean.Key key = isExecutable(contentType) ? AntivirusScannerMaxFileSizeForExecutables
				: AntivirusScannerMaxFileSize;
		return size(file) > this.settings.getValueForKeyAsLong(key);
	}

	public AntivirScannerResponse scan(final Path file) throws IOException {
		try (final InputStream fileInput = newInputStream(file)) {
			return scan(fileInput);
		}
	}

	public AntivirScannerResponse scan(final InputStream fileInput) throws IOException {
		if (isEnabled()) {
			try (final Socket socket = openSocket()) {
				int bytesRead = CHUNK_SIZE;
				final byte[] buffer = new byte[CHUNK_SIZE];

				final DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				out.write(INSTREAM);
				while (bytesRead == CHUNK_SIZE) {
					bytesRead = fileInput.read(buffer);
					if (bytesRead > 0) {
						out.writeInt(bytesRead);
						out.write(buffer, 0, bytesRead);
					}
				}
				out.writeInt(0);
				out.flush();

				bytesRead = socket.getInputStream().read(buffer);
				final String message = new String(buffer, 0, bytesRead);
				final boolean infected = message.contains("FOUND");
				return new AntivirScannerResponse(infected, message);
			}
		} else {
			return new AntivirScannerResponse(false, "DISABLED");
		}
	}

	private Socket openSocket() throws IOException {
		Socket socket = new Socket(this.settings.getValueForKey(AntivirusScannerSocketAddress),
				this.settings.getValueForKeyAsInt(AntivirusScannerSocketPort));
		socket.setSoTimeout(this.settings.getValueForKeyAsInt(AntivirusScannerSocketTimeout));
		return socket;
	}

	private static boolean isExecutable(final String contentType) {
		MediaType mediaType = MediaType.parse(contentType);
		while (mediaType != null && !mediaType.equals(MediaType.OCTET_STREAM)) {
			if (EXECUTABLE_SUBTYPES.contains(mediaType.getSubtype())) {
				return true;
			}
			mediaType = MediaTypeRegistry.getDefaultRegistry().getSupertype(mediaType);
		}
		return false;
	}
}
