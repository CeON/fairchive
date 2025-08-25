package edu.harvard.iq.dataverse.settings;

import static java.nio.file.Files.copy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.harvard.iq.dataverse.settings.FileSettingLocations.PathType;
import edu.harvard.iq.dataverse.settings.FileSettingLocations.SettingLocationType;

public class FileBasedSettingsFetcherTest {

    @Test
    public void loadSettings__PROPERTIES_FROM_CLASSPATH() {

        // given
        FileSettingLocations settingLocations = new FileSettingLocations()
                .addLocation(1, SettingLocationType.CLASSPATH,
                        "/test1.properties", PathType.DIRECT, false);

        FileBasedSettingsFetcher settingsFetcher = new FileBasedSettingsFetcher(settingLocations);

        // when
        settingsFetcher.loadSettings();

        // then
        assertThat(settingsFetcher.getSetting("BuiltinUsers.KEY")).isEqualTo("keyval1");
        assertThat(settingsFetcher.getSetting(":SomeSetting")).isEqualTo("someval1");
        assertThat(settingsFetcher.getSetting(":Test1Setting")).isEqualTo("testval1");
        assertThat(settingsFetcher.getAllSettings())
                .containsKeys("BuiltinUsers.KEY", ":SomeSetting", ":Test1Setting");
    }

    @Test
    public void loadSettings__NOT_EXISTING_PROPERTIES_FROM_CLASSPATH() {

        // given
        FileSettingLocations settingLocations = new FileSettingLocations()
                .addLocation(1, SettingLocationType.CLASSPATH,
                        "/notexisting.properties", PathType.DIRECT, false);

        FileBasedSettingsFetcher settingsFetcher = new FileBasedSettingsFetcher(settingLocations);

        // when & then
        assertThatThrownBy(settingsFetcher::loadSettings).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void loadSettings__NOT_EXISTING_OPTIONAL_PROPERTIES_FROM_CLASSPATH() {

        // given
        FileSettingLocations settingLocations = new FileSettingLocations()
                .addLocation(1, SettingLocationType.CLASSPATH,
                        "/notexisting.properties", PathType.DIRECT, true);

        FileBasedSettingsFetcher settingsFetcher = new FileBasedSettingsFetcher(settingLocations);

        // when
        settingsFetcher.loadSettings();

        // then
        assertThat(settingsFetcher.getAllSettings()).hasSize(0);
    }


    @Test
    public void loadSettings__PROPERTIES_FROM_FILESYSTEM(@TempDir Path tempDir) 
            throws IOException {
        // given
        Path filesystemPropertiesFile = tempDir.resolve("filesystem.properties");
        try(final InputStream in = getClass().getResourceAsStream("/test1.properties")) {
            copy(in, filesystemPropertiesFile);
        }

        FileSettingLocations settingLocations = new FileSettingLocations()
                .addLocation(1, SettingLocationType.FILESYSTEM,
                        filesystemPropertiesFile.toAbsolutePath().toString(), PathType.DIRECT, false);

        FileBasedSettingsFetcher settingsFetcher = new FileBasedSettingsFetcher(settingLocations);

        // when
        settingsFetcher.loadSettings();

        // then
        assertThat(settingsFetcher.getSetting("BuiltinUsers.KEY")).isEqualTo("keyval1");
        assertThat(settingsFetcher.getSetting(":SomeSetting")).isEqualTo("someval1");
        assertThat(settingsFetcher.getSetting(":Test1Setting")).isEqualTo("testval1");
        assertThat(settingsFetcher.getAllSettings())
                .containsKeys("BuiltinUsers.KEY", ":SomeSetting", ":Test1Setting");
    }

    @Test
    public void loadSettings__NOT_EXISTING_PROPERTIES_FROM_FILESYSTEM() {

        // given
        FileSettingLocations settingLocations = new FileSettingLocations()
                .addLocation(1, SettingLocationType.FILESYSTEM,
                        "notexisting.properties", PathType.DIRECT, false);

        FileBasedSettingsFetcher settingsFetcher = new FileBasedSettingsFetcher(settingLocations);

        // when & then
        assertThatThrownBy(settingsFetcher::loadSettings)
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void loadSettings__NOT_EXISTING_OPTIONAL_PROPERTIES_FROM_FILESYSTEM() {

        // given
        FileSettingLocations settingLocations = new FileSettingLocations()
                .addLocation(1, SettingLocationType.FILESYSTEM,
                        "notexisting.properties", PathType.DIRECT, true);

        FileBasedSettingsFetcher settingsFetcher = new FileBasedSettingsFetcher(settingLocations);

        // when
        settingsFetcher.loadSettings();

        // then
        assertThat(settingsFetcher.getAllSettings()).hasSize(0);
    }

    @Test
    public void loadSettings__MULTIPLE_PROPERTIES() {

        // given
        FileSettingLocations settingLocations = new FileSettingLocations()
                .addLocation(1, SettingLocationType.CLASSPATH,
                        "/test1.properties", PathType.DIRECT, false)
                .addLocation(2, SettingLocationType.CLASSPATH,
                        "/test2.properties", PathType.DIRECT, false)
                .addLocation(3, SettingLocationType.CLASSPATH,
                        "/test3.properties", PathType.DIRECT, true);

        FileBasedSettingsFetcher settingsFetcher = new FileBasedSettingsFetcher(settingLocations);

        // when
        settingsFetcher.loadSettings();

        // then
        assertThat(settingsFetcher.getSetting("BuiltinUsers.KEY")).isEqualTo("keyval2");
        assertThat(settingsFetcher.getSetting(":SomeSetting")).isEqualTo("someval2");
        assertThat(settingsFetcher.getSetting(":Test1Setting")).isEqualTo("testval1");
        assertThat(settingsFetcher.getSetting(":Test2Setting")).isEqualTo("testval2");
        assertThat(settingsFetcher.getAllSettings())
                .containsKeys("BuiltinUsers.KEY", ":SomeSetting", ":Test1Setting", ":Test2Setting");
    }

    @Test
    public void loadSettings__PROPERTIES_WITH_PROPERTY_TYPE_PATH(@TempDir Path tempDir) 
            throws IOException {
        // given
        Path filesystemPropertiesFile = tempDir.resolve("filesystem.properties");
        Path secondPropertiesFile = tempDir.resolve("second.properties");
        String value = secondPropertiesFile.toAbsolutePath().toString();
        value = value.replace("\\", "\\\\"); // make it work under Windows
        
        Files.write(filesystemPropertiesFile, ("props.path=" + value).getBytes());
        try(final InputStream in = getClass().getResourceAsStream("/test1.properties")) {
            Files.copy(in, secondPropertiesFile);
        }
        FileSettingLocations settingLocations = new FileSettingLocations()
                .addLocation(1, SettingLocationType.FILESYSTEM,
                        filesystemPropertiesFile.toAbsolutePath().toString(), PathType.DIRECT, false)
                .addLocation(2, SettingLocationType.FILESYSTEM,
                        ":props.path", PathType.PROPERTY, false);
        FileBasedSettingsFetcher settingsFetcher = new FileBasedSettingsFetcher(settingLocations);

        // when
        settingsFetcher.loadSettings();

        // then
        assertThat(settingsFetcher.getSetting(":SomeSetting")).isEqualTo("someval1");
        assertThat(settingsFetcher.getAllSettings())
                .containsKeys("BuiltinUsers.KEY", ":SomeSetting", ":Test1Setting", ":props.path");
    }

    @Test
    public void loadSettings__FALLBACK_PROPERTIES() {

        // given
        FileSettingLocations settingLocations = new FileSettingLocations()
                .addLocation(1, SettingLocationType.CLASSPATH,
                        "qwerty.properties", PathType.DIRECT, true)
                .addFallbackLocation(1, SettingLocationType.CLASSPATH,
                        "/test1.properties", PathType.DIRECT);

        FileBasedSettingsFetcher settingsFetcher = new FileBasedSettingsFetcher(settingLocations);

        // when
        settingsFetcher.loadSettings();

        // then
        assertThat(settingsFetcher.getSetting(":Test1Setting")).isEqualTo("testval1");
        assertThat(settingsFetcher.getAllSettings())
                .containsKeys("BuiltinUsers.KEY", ":SomeSetting", ":Test1Setting");
    }
}
