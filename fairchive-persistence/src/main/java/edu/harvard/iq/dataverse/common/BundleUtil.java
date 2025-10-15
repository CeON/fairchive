package edu.harvard.iq.dataverse.common;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;

import javax.faces.context.FacesContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public class BundleUtil {

    private static final Logger logger = Logger.getLogger(BundleUtil.class.getCanonicalName());

    private static final String DEFAULT_BUNDLE_FILE = "Bundle";

    private static final String EXTENSION_SUFFIX = "extension";

    private static final Set<String> INTERNAL_BUNDLE_NAMES = Sets.newHashSet(
            DEFAULT_BUNDLE_FILE, "BuiltInRoles", "MimeTypeDisplay", "MimeTypeFacets", "ValidationMessages");

    /**
     * This map is CRUCIAL for the performance of all methods from
     * the class, especially in parts for search, as the original
     * {@link ResourceBundle#getBundle(String)} could be very
     * inefficient.
     */
    private static ConcurrentMap<String, ResourceBundle> bundleCache = new ConcurrentHashMap<>();

    private static final ResourceBundle EMPTY_BUNDLE = new ResourceBundle() {
        @Override protected Object handleGetObject(String key) { return null; }
        @Override public Enumeration<String> getKeys() { return null; }
    };

    // -------------------- LOGIC --------------------

    public static boolean hasKeyInBundle(String key) {
        return hasKeyInPropertyFile(key, DEFAULT_BUNDLE_FILE, getCurrentLocale());
    }

    public static String getStringFromBundle(String key, Object ... arguments) {
        return getStringFromBundleWithLocale(key, getCurrentLocale(), arguments);
    }

    public static String getStringFromBundleWithLocale(String key, Locale locale, Object... arguments) {
        return getStringFromPropertyFile(key, DEFAULT_BUNDLE_FILE, locale)
                .map(m -> MessageFormat.format(m, arguments))
                .orElse(StringUtils.EMPTY);
    }

    public static boolean hasKeyInNonDefaultBundle(String key, String bundleName) {
        return hasKeyInPropertyFile(key, bundleName, getCurrentLocale());
    }

    public static String getStringFromNonDefaultBundle(String key, String bundleName, Object... arguments) {
        return getStringFromNonDefaultBundleWithLocale(key, bundleName, getCurrentLocale(), arguments);
    }

    public static String getStringFromNonDefaultBundleWithLocale(String key, String bundleName, Locale locale, Object... arguments) {
        return getStringFromPropertyFile(key, bundleName, locale)
                .map(m -> MessageFormat.format(m, arguments))
                .orElse(StringUtils.EMPTY);
    }

    public static String getStringFromClasspathBundle(String key, String bundleName, Object... arguments) {
        return getStringFromPropertyFile(key, bundleName, getCurrentLocale())
                .map(m -> MessageFormat.format(m, arguments))
                .orElse(StringUtils.EMPTY);
    }

    public static Locale getCurrentLocale() {
        if (FacesContext.getCurrentInstance() == null) {
            return new Locale("en");
        } else if (FacesContext.getCurrentInstance().getViewRoot() == null) {
            return FacesContext.getCurrentInstance().getExternalContext().getRequestLocale();
        } else if ("en_US".equals(FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage())) {
            return new Locale("en");
        }

        return FacesContext.getCurrentInstance().getViewRoot().getLocale();
    }

    // -------------------- PRIVATE --------------------

    private static boolean hasKeyInPropertyFile(String bundleKey, String bundleName, Locale locale) {

        if (shouldCheckForExternalBundle(bundleName) && hasKeyInExternalBundle(bundleKey, bundleName, locale)) {
            return true;
        }
        ResourceBundle resourceBundle = getCachedBundle(bundleName + "_" + EXTENSION_SUFFIX, locale);
        if (!EMPTY_BUNDLE.equals(resourceBundle) && resourceBundle.containsKey(bundleKey)) {
            return true;
        }
        resourceBundle = getCachedBundle(bundleName, locale);
        return !EMPTY_BUNDLE.equals(resourceBundle) && resourceBundle.containsKey(bundleKey);
    }

    /**
     * Gets display name for specified bundle key. If it is external bundle,
     * method tries to access external directory (jvm property - dataverse.lang.directory)
     * where bundles are kept and return the display name.
     * <p>
     * If it is default bundle or default metadata block #{@link DefaultMetadataBlocks#METADATA_BLOCK_NAMES}
     * method tries to get the name from default bundles otherwise it returns empty string.
     */
    private static Optional<String> getStringFromPropertyFile(String bundleKey, String bundleName, Locale locale) {
        Optional<String> resolvedValue = Optional.empty();

        if (shouldCheckForExternalBundle(bundleName)) {
            resolvedValue = getStringFromExternalBundle(bundleKey, bundleName, locale);
            if (resolvedValue.isPresent()) {
                return resolvedValue;
            }
        }

        resolvedValue = getStringFromInternalBundle(bundleKey, bundleName + "_" + EXTENSION_SUFFIX, locale);
        if (resolvedValue.isPresent()) {
            return resolvedValue;
        }

        return getStringFromInternalBundle(bundleKey, bundleName, locale);
    }

    private static Optional<String> getStringFromInternalBundle(String bundleKey, String bundleName, Locale locale) {

        Optional<String> displayNameFromExtensionBundle = getStringFromInternalBundle(bundleKey, bundleName, EXTENSION_SUFFIX, locale);
        return displayNameFromExtensionBundle.isPresent() ?
                    displayNameFromExtensionBundle : getStringFromInternalBundle(bundleKey, bundleName, "", locale);
    }

    private static Optional<String> getStringFromInternalBundle(String bundleKey, String bundleName, String extension, Locale locale) {

        ResourceBundle resourceBundle = getCachedBundle(bundleName, locale);

        try {
            return !EMPTY_BUNDLE.equals(resourceBundle)
                    ? Optional.of(resourceBundle.getString(bundleKey))
                    : Optional.empty();
        } catch (Exception ex) {
            logger.finest("Could not find key \"" + bundleKey + "\" in bundle file: " + bundleName);
            return Optional.empty();
        }
    }

    private static ResourceBundle getCachedBundle(String bundleName, Locale locale) {
        String cacheKey = bundleName + "_" + locale.getLanguage();
        ResourceBundle resourceBundle = bundleCache.get(cacheKey);
        if (resourceBundle == null) {
            try {
                resourceBundle = ResourceBundle.getBundle(bundleName, locale);
            } catch (MissingResourceException mre) {
                resourceBundle = EMPTY_BUNDLE;
            }
            bundleCache.putIfAbsent(cacheKey, resourceBundle);
        }
        return resourceBundle;
    }

    private static ResourceBundle getCachedExternalBundle(String bundleName, Locale locale) {
        String key = bundleName + "_ext_" + locale.getLanguage();

        ResourceBundle resourceBundle = bundleCache.get(key);
        if (resourceBundle == null) {
            try {
                URL customBundlesDir = Paths.get(System.getProperty("dataverse.lang.directory")).toUri().toURL();
                URLClassLoader externalBundleDirURL = new URLClassLoader(new URL[]{customBundlesDir});
                resourceBundle = ResourceBundle.getBundle(bundleName, locale, externalBundleDirURL);
            } catch (MalformedURLException | MissingResourceException ex) {
                resourceBundle = EMPTY_BUNDLE;
            }
            bundleCache.putIfAbsent(key, resourceBundle);
        }
        return resourceBundle;
    }

    private static boolean shouldCheckForExternalBundle(String bundleName) {
        return (!DefaultMetadataBlocks.METADATA_BLOCK_NAMES.contains(bundleName) && !INTERNAL_BUNDLE_NAMES.contains(bundleName))
                && System.getProperty("dataverse.lang.directory") != null;
    }

    private static boolean hasKeyInExternalBundle(String bundleKey, String bundleName, Locale locale) {
        ResourceBundle resourceBundle = getCachedExternalBundle(bundleName, locale);

        return !EMPTY_BUNDLE.equals(resourceBundle) && resourceBundle.containsKey(bundleKey);
    }

    // IMPORTANT: this method is nearly exact copy of getStringFromInternalBundle(…), however
    // any attempt in extracting common code from these two and pass differing parts as lambdas
    // would cause great decrease in performance of WHOLE dataverse app.
    private static Optional<String> getStringFromExternalBundle(String bundleKey, String bundleName, Locale locale) {
        ResourceBundle resourceBundle = getCachedExternalBundle(bundleName, locale);

        try {
            return !EMPTY_BUNDLE.equals(resourceBundle)
                    ? Optional.of(resourceBundle.getString(bundleKey))
                    : Optional.empty();
        } catch (Exception ex) {
            logger.finest("Could not find key \"" + bundleKey + "\" in bundle file: " + bundleName);
            return Optional.empty();
        }
    }
}

