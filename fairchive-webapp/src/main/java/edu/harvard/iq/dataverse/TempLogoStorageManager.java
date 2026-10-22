package edu.harvard.iq.dataverse;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.list;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

@Stateless
public class TempLogoStorageManager {

	private final Path baseDir;

	public TempLogoStorageManager() {
		this(Paths.get("../docroot/logos/temp"));
	}

	public TempLogoStorageManager(final Path baseDir) {
		this.baseDir = baseDir;
		
	}

	public Storage createStorageFor(final Dataverse dataverse) {
		try {
			final String dirName = dataverse.isNew() 
					? UUID.randomUUID().toString() 
					: dataverse.getId().toString();
			final Path tempRoot = createDirectories(this.baseDir);
			return new Storage(createTempDirectory(tempRoot, dirName));
		} catch (final IOException e) {
			throw new RuntimeException("Error creating temp directory", e);
		}
	}

	public final static class Storage implements Closeable {
		
		private final static String error = "Error deleting temp directory";

		private final Path dir;

		private Storage(final Path dir) {
			this.dir = dir;
		}
		
		public void saveFrom(final String name, final InputStream in) 
				throws IOException {
			copy(in, this.dir.resolve(name), REPLACE_EXISTING);
		}
		
		public void copyIfExists(final String name, final Path target) 
				throws IOException {
			final Path source = this.dir.resolve(name);
			if(exists(source)) {
				copy(source, target, REPLACE_EXISTING);
			}
		}

		@Override
		public void close() throws IOException {
			try {
				list(this.dir).forEach(file -> {
					try {
						deleteIfExists(file);
					} catch (final IOException e) {
						throw new RuntimeException(error, e);
					}
				});
				deleteIfExists(this.dir);
			} catch (final IOException e) {
				throw new RuntimeException(error, e);
			}
		}

		@Override
		public String toString() {
			return this.dir.toString();
		}
		
		public String getName() {
			return this.dir.getFileName().toString();
		}
	}
}
