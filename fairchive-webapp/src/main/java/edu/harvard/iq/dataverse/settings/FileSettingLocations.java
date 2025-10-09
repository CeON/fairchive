package edu.harvard.iq.dataverse.settings;

import javax.enterprise.inject.Vetoed;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Locations of properties setting files that need to be
 * loaded on application startup.
 *
 * @author madryk
 */
@Vetoed
public class FileSettingLocations {

    public enum SettingLocationType {
        CLASSPATH,
        FILESYSTEM
    }

    public enum PathType {
        DIRECT,
        PROPERTY
    }

    private final List<SettingLocation> settingLocations = new ArrayList<>();
    private final Map<Integer, SettingLocation> fallbackLocations = new HashMap<>();

    // -------------------- GETTERS --------------------

    public List<SettingLocation> getSettingLocations() {
        return unmodifiableList(this.settingLocations);
    }

    public Map<Integer, SettingLocation> getFallbackLocations() {
        return unmodifiableMap(this.fallbackLocations);
    }

    // -------------------- LOGIC --------------------

    public FileSettingLocations addLocation(final int order,
            final SettingLocationType locationType, final String path,
            final PathType pathType, final boolean isOptional) {
        this.settingLocations.add(new SettingLocation(order, locationType,
                path, pathType, isOptional));
        return this;
    }

    public FileSettingLocations addFallbackLocation(final int order,
            final SettingLocationType locationType, final String path,
            final PathType pathType) {
        this.fallbackLocations.put(order,
                new SettingLocation(order, locationType, path, pathType, true));
        return this;
    }

    // -------------------- INNER CLASSES --------------------

    public static final class SettingLocation {
        private final int order;
        private final SettingLocationType locationType;
        private final String path;
        private final PathType pathType;
        private final boolean isOptional;

        // -------------------- CONSTRUCTORS --------------------

        private SettingLocation(final int order, final SettingLocationType locationType, 
                final String path, final PathType pathType, final boolean isOptional) {
            this.order = order;
            this.locationType = requireNonNull(locationType);
            this.path = requireNonNull(path);
            this.pathType = requireNonNull(pathType);
            this.isOptional = isOptional;
        }

        // -------------------- GETTERS --------------------

        public int getOrder() {
            return this.order;
        }

        public SettingLocationType getLocationType() {
            return this.locationType;
        }

        public String getPath() {
            return this.path;
        }

        public PathType getPathType() {
            return this.pathType;
        }

        public boolean isOptional() {
            return this.isOptional;
        }

        // -------------------- toString --------------------

        @Override
        public String toString() {
            return format("SettingLocation{order=%d, locationType=%s, path='%s', pathType=%s, isOptional=%s}",
                    this.order, this.locationType, this.path, this.pathType, this.isOptional);
        }
    }
}
