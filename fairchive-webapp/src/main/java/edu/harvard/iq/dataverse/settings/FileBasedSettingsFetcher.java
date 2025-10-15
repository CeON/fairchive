package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.settings.FileSettingLocations.SettingLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import static java.util.Collections.unmodifiableMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Service responsible for loading and serving application settings
 * defined in property files.
 *
 * @author madryk
 */
@Startup
@Singleton
public class FileBasedSettingsFetcher {
    private static final Logger logger = LoggerFactory.getLogger(FileBasedSettingsFetcher.class);

    private static final String BUILTIN_USERS_KEY_SETTING = "BuiltinUsers.KEY";

    private FileSettingLocations fileSettingLocations;

    private Map<String, String> settings = new HashMap<>();

    // -------------------- CONSTRUCTORS --------------------

    public FileBasedSettingsFetcher() {
        // JEE requirement
    }

    @Inject
    public FileBasedSettingsFetcher(final FileSettingLocations fileSettingLocations) {
        this.fileSettingLocations = fileSettingLocations;
    }

    // -------------------- LOGIC --------------------

    /**
     * Loads settings from files defined in {@link FileSettingLocations}<br/>
     * <br/>
     * If some setting is defined in multiple files, then
     * the last occurrence of this setting will take precedence
     * (setting from 2nd file overrides setting from 1st).
     */
    @PostConstruct
    public void loadSettings() {
        final Map<Integer, SettingLocation> fallbackLocations = this.fileSettingLocations
                .getFallbackLocations();
        for (final SettingLocation settingLocation : this.fileSettingLocations
                .getSettingLocations()) {
            Optional<Properties> properties = loadProperties(settingLocation);
            if (!properties.isPresent()) {
                final SettingLocation fallback = fallbackLocations
                        .get(settingLocation.getOrder());
                logger.warn("Cannot load properties from primary location: "
                        + settingLocation
                        + ". Fallback: " + fallback);
                properties = loadProperties(fallback);
            }
            if (!properties.isPresent() && !settingLocation.isOptional()) {
                logger.error(
                        "Cannot load properties for location: " + settingLocation);
                throw new RuntimeException("Cannot load mandatory properties");
            }
            properties.orElseGet(Properties::new)
                    .forEach((key, value) -> this.settings.put(
                            convertPropertiesKeyToSettingName((String) key),
                            sanitizeSettingValue((String) value)));
        }
    }

    /**
     * Returns setting value with the given key
     */
    public String getSetting(final String key) {
        final String setting = this.settings.get(key);
        return setting == null ? EMPTY : setting;
    }

    /**
     * Returns all defined settings
     */
    public Map<String, String> getAllSettings() {
        return unmodifiableMap(this.settings);
    }


    // -------------------- PRIVATE --------------------

    private Optional<Properties> loadProperties(final SettingLocation location) {
        if (location == null) {
            logger.warn("Null location received. Returning empty result.");
            return Optional.empty();
        }
        switch (location.getLocationType()) {
            case FILESYSTEM:
                return loadPropertiesFromFile(location);
            case CLASSPATH:
                return loadPropertiesFromClasspath(location);
            default:
                throw new RuntimeException("Not supported setting location type: " 
                        + location.getLocationType());
        }
    }

    private Optional<Properties> loadPropertiesFromClasspath(
            final SettingLocation location) {
        final Properties properties = new Properties();
        final String classpath = readPath(location);
        if (isBlank(classpath)) {
            logger.warn("Blank classpath for property file location of " + location);
            return Optional.empty();
        }
        try (final InputStream in = getClass().getResourceAsStream(classpath)) {
            if (in == null) {
                logger.error("Empty stream while trying to read properties from "
                        + classpath);
                return Optional.empty();
            }
            properties.load(in);
        } catch (final IOException e) {
            throw new RuntimeException(
                    "Unable to read properties from classpath: " + classpath, e);
        }
        return Optional.of(properties);
    }

    private Optional<Properties> loadPropertiesFromFile(
            final SettingLocation location) {
        final Properties properties = new Properties();
        final String path = readPath(location);
        if (isBlank(path)) {
            logger.warn("Blank path for property file location of " + location);
            return Optional.empty();
        }
        final File propertiesFile = new File(path);
        if (!(propertiesFile.exists() && propertiesFile.isFile())) {
            logger.error(
                    "Empty or nonexisting file encountered while trying to read properties from "
                            + path);
            return Optional.empty();
        }
        try (final InputStream in = new FileInputStream(propertiesFile)) {
            properties.load(in);
        } catch (final IOException e) {
            throw new RuntimeException("Unable to read properties from: " + path, e);
        }
        return Optional.of(properties);
    }

    private String readPath(final SettingLocation location) {
        switch (location.getPathType()) {
        case DIRECT:
            return location.getPath();
        case PROPERTY:
            return this.settings.get(location.getPath());
        default:
            throw new RuntimeException(
                    "Not supported setting path type: " + location.getPathType());
        }
    }

    private static String convertPropertiesKeyToSettingName(final String key) {
        if (key.equals(BUILTIN_USERS_KEY_SETTING)) { // For some reason this setting doesn't have ':' prefix
            return BUILTIN_USERS_KEY_SETTING;
        }
        return ":" + key;
    }

    private static String sanitizeSettingValue(final String value) {
        return isEmpty(value) ? null :  value;
    }
}
