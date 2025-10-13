package edu.harvard.iq.dataverse.settings;

import javax.enterprise.inject.Produces;

import static edu.harvard.iq.dataverse.settings.FileSettingLocations.PathType.DIRECT;
import static edu.harvard.iq.dataverse.settings.FileSettingLocations.PathType.PROPERTY;
import static edu.harvard.iq.dataverse.settings.FileSettingLocations.SettingLocationType.CLASSPATH;
import static edu.harvard.iq.dataverse.settings.FileSettingLocations.SettingLocationType.FILESYSTEM;
import static java.lang.System.getProperty;

public class FileSettingLocationsFactory {

    // -------------------- LOGIC --------------------

    /**
     * Returns setting locations used in production. That is (at least):
     * <p>
     * 1) Properties file in classpath: {@code /config/dataverse.default.properties }<br/>
     * 2) External properties file: {@code ${user.home}/.dataverse/dataverse.properties }<br/>
     */
    @Produces
    public FileSettingLocations buildSettingLocations() {
        return new FileSettingLocations()
                .addLocation(1, CLASSPATH,"/config/dataverse.default.properties", 
                        DIRECT, false)
                .addLocation(2,FILESYSTEM,
                        getProperty("user.home").concat("/.dataverse/dataverse.properties"), 
                        DIRECT, true)
                .addLocation(3, FILESYSTEM, ":SamlPropertiesPath", PROPERTY, false)
                .addFallbackLocation(3, CLASSPATH, "/config/saml.properties", DIRECT);
    }
}
