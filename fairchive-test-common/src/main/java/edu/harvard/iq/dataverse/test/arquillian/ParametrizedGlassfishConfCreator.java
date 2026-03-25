package edu.harvard.iq.dataverse.test.arquillian;

import static com.google.common.io.Resources.getResource;
import static java.lang.System.getProperty;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.Files.newInputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 * Class responsible for parsing glassfish configuration.
 */
public class ParametrizedGlassfishConfCreator {

	public final Path newResourcePath = Paths.get(getProperty("java.io.tmpdir"), 
			"dataverse", "glassfish-resources.xml");

	private Path propertiesPath = Paths.get(getProperty("user.home"), ".dataverse", 
			"glassfish.properties");
	private Path tempPath = Paths.get(getProperty("java.io.tmpdir"), "dataverse");

	// -------------------- LOGIC --------------------

	/**
	 * Reads glassfish-resources template, reads values from properties file and
	 * creates new version of glassfish-resources with new values in temporary
	 * location.
	 */
	public void createTempGlassfishResources() {
		try {
			final Document document = replaceGlassfishXmlValues(loadProperties());
			createDirectories(this.tempPath);
			createGlassfishResources(document, newResourcePath);
		} catch (final Exception ex) {
			throw new IllegalStateException("There was a problem with parsing xml file", ex);
		}
	}

	public void cleanTempGlassfishResource() {
		try {
			deleteIfExists(this.newResourcePath);
		} catch (final IOException e) {
			throw new RuntimeException("Unable to delete temporary glassfish resource", e);
		}
	}

	// -------------------- PRIVATE --------------------
	
	private Properties loadProperties() throws IOException, FileNotFoundException {
		final Properties properties = new Properties();
		try (final InputStream in = openPropertiesFile()) {
				properties.load(in);
			}
		return properties;
	}
	
	private InputStream openPropertiesFile() throws IOException {
		return exists(this.propertiesPath)
				? newInputStream(this.propertiesPath)
				: getClass().getResourceAsStream("/glassfish.properties");
	}

	private void createGlassfishResources(final Document document, final Path savePath) 
			throws IOException {

		XMLWriter xmlWriter = null;
		try {
			xmlWriter = new XMLWriter(newBufferedWriter(savePath));
			xmlWriter.write(document);
		} finally {
			if (xmlWriter != null) {
				xmlWriter.close();
			}
		}
	}

	private Document replaceGlassfishXmlValues(Properties properties) 
			throws DocumentException {
		Document document = readGlassfishResources();

		List<Node> list = document.selectNodes("/resources/jdbc-connection-pool/child::*");

		list.forEach(node -> {
			Element element = (Element) node;
			String propertyName = element.attribute(0).getValue();

			switch (propertyName) {
			case "password":
				element.attribute(1).setValue(properties.getProperty("db.password"));
				break;
			case "PortNumber":
				element.attribute(1).setValue(properties.getProperty("db.portnumber"));
				break;
			case "User":
				element.attribute(1).setValue(properties.getProperty("db.user"));
				break;
			case "databaseName":
				element.attribute(1).setValue(properties.getProperty("db.databasename"));
				break;
			case "ServerName":
				element.attribute(1).setValue(properties.getProperty("db.host"));
				break;
			}
		});
		return document;
	}

	private Document readGlassfishResources() throws DocumentException {
		return new SAXReader().read(getResource("glassfish-resources.xml"));
	}

}
