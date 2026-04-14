package edu.harvard.iq.dataverse.common;

import static java.lang.Runtime.getRuntime;
import static java.text.MessageFormat.format;
import static java.util.Collections.emptyEnumeration;
import static java.util.Locale.ENGLISH;
import static java.util.ResourceBundle.getBundle;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Logger.getLogger;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;

public class BundleUtil {

	private static final Logger logger = getLogger(BundleUtil.class.getCanonicalName());

	private static final String DEFAULT_BUNDLE_FILE = "Bundle";
	private static final String EXTENSION_SUFFIX = "extension";
	private static final String DEFAULT_BUNDLE_FILE_EXTENTION = 
			DEFAULT_BUNDLE_FILE + '_' + EXTENSION_SUFFIX;

	/**
	 * This map is CRUCIAL for the performance of all methods from the class,
	 * especially in parts for search, as the original
	 * {@link ResourceBundle#getBundle(String)} could be very inefficient.
	 */
	private final static ConcurrentMap<String, ResourceBundle> bundleCache = new ConcurrentHashMap<>();
	private final static String loaderPath = System.getProperty("dataverse.lang.directory");
	private final static URLClassLoader externalLoader;

	private static final ResourceBundle EMPTY_BUNDLE = new ResourceBundle() {
		@Override
		protected Object handleGetObject(final String key) {
			return null;
		}

		@Override
		public Enumeration<String> getKeys() {
			return emptyEnumeration();
		}
	};
	
	static {
		try {
			if(loaderPath != null) {
				externalLoader = new URLClassLoader(new URL[] { Paths.get(loaderPath).toUri().toURL() });
				getRuntime().addShutdownHook(new Thread(() -> {
		            try {
		                externalLoader.close();
		            } catch (IOException ignored) {}
			    }));
				
			} else {
				externalLoader = null;
			}
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	// -------------------- LOGIC --------------------

	public static boolean hasKeyInBundle(final String key) {
		return hasKeyInPropertyFile(key, DEFAULT_BUNDLE_FILE, getCurrentLocale());
	}

	public static String getStringFromBundle(final String key, 
			final Object... arguments) {
		return getStringFromBundleWithLocale(key, getCurrentLocale(), arguments);
	}

	public static String getStringFromBundleWithLocale(final String key, 
			final Locale locale, final Object... arguments) {
		return getStringFromPropertyFile(DEFAULT_BUNDLE_FILE, locale, key, arguments);
	}

	public static boolean hasKeyInNonDefaultBundle(final String key, 
			final String bundleName) {
		return hasKeyInPropertyFile(key, bundleName, getCurrentLocale());
	}

	public static String getStringFromNonDefaultBundle(final String key, 
			final String bundleName, final Object... arguments) {
		return getStringFromNonDefaultBundleWithLocale(key, bundleName, 
				getCurrentLocale(), arguments);
	}

	public static String getStringFromNonDefaultBundleWithLocale(final String key, 
			final String bundleName, final Locale locale, final Object... arguments) {
		return getStringFromPropertyFile(bundleName, locale, key, arguments);
	}

	public static String getStringFromClasspathBundle(final String key, 
			final String bundleName, final Object... arguments) {
		return getStringFromPropertyFile(bundleName, getCurrentLocale(), key, arguments);
	}

	public static Locale getCurrentLocale() {
		final FacesContext context = FacesContext.getCurrentInstance();
		if (context == null) {
			return ENGLISH;
		} else if (context.getViewRoot() == null) {
			return context.getExternalContext().getRequestLocale();
		} else if ("en".equals(context.getViewRoot().getLocale().getLanguage())) {
			return ENGLISH;
		} else {
			return context.getViewRoot().getLocale();
		}
	}

	// -------------------- PRIVATE --------------------

	private static boolean hasKeyInPropertyFile(final String key, 
			final String bundleName, final Locale locale) {

		if (shouldCheckForExternalBundle(bundleName) && 
				hasKeyInExternalBundle(key, bundleName, locale)) {
			return true;
		}
		ResourceBundle bundle = getCachedBundle(extendedBundleName(bundleName), locale);
		if (isNotEmpty(bundle) && bundle.containsKey(key)) {
			return true;
		}
		bundle = getCachedBundle(bundleName, locale);
		return isNotEmpty(bundle) && bundle.containsKey(key);
	}

	/**
	 * Gets display name for specified bundle key. If it is external bundle, method
	 * tries to access external directory (jvm property - dataverse.lang.directory)
	 * where bundles are kept and return the display name.
	 * <p>
	 * If it is default bundle or default metadata block
	 * #{@link DefaultMetadataBlocks#METADATA_BLOCK_NAMES} method tries to get the
	 * name from default bundles otherwise it returns empty string.
	 */
	private static String getStringFromPropertyFile(final String bundleName, 
			final Locale locale, final String key, final Object... arguments) {
		String result = null;

		if (shouldCheckForExternalBundle(bundleName)) {
			result = getStringFromExternalBundle(key, bundleName, locale);
			if (result != null) {
				return maybeFormat(result, arguments);
			}
		}

		result = getStringFromInternalBundle(key, extendedBundleName(bundleName), locale);
		if (result != null) {
			return maybeFormat(result, arguments);
		}
		
		result = getStringFromInternalBundle(key, bundleName, locale);
		if (result != null) {
			return maybeFormat(result, arguments);
		} else {
			return EMPTY;
		}
	}

	private static String getStringFromInternalBundle(final String key, 
			final String bundleName, final Locale locale) {

		final ResourceBundle bundle = getCachedBundle(bundleName, locale);

		try {
			return isNotEmpty(bundle)? bundle.getString(key) : null;
		} catch (final Exception ex) {
			if (logger.isLoggable(FINEST)) {
				logger.finest("Could not find key \"" + key + 
						"\" in bundle file: " + bundleName);
			}
			return null;
		}
	}

	private static ResourceBundle getCachedBundle(final String bundleName, 
			final Locale locale) {
		final String cacheKey = bundleName + '_' + locale.getLanguage();
		ResourceBundle bundle = bundleCache.get(cacheKey);
		if (bundle == null) {
			try {
				bundle = getBundle(bundleName, locale);
			} catch (final MissingResourceException mre) {
				bundle = EMPTY_BUNDLE;
			}
			bundleCache.putIfAbsent(cacheKey, bundle);
		}
		return bundle;
	}

	private static ResourceBundle getCachedExternalBundle(final String bundleName, 
			final Locale locale) {
		final String key = bundleName + "_ext_" + locale.getLanguage();
		ResourceBundle bundle = bundleCache.get(key);
		if (bundle == null) {
			try {
				bundle = getBundle(bundleName, locale, externalLoader);
			} catch (final MissingResourceException ex) {
				bundle = EMPTY_BUNDLE;
			}
			bundleCache.putIfAbsent(key, bundle);
		}
		return bundle;
	}

	private static boolean shouldCheckForExternalBundle(final String bundleName) {
		return isNotMetadataBundle(bundleName)
				&& ! isInternal(bundleName)
				&& loaderPath != null;
	}

	private static boolean hasKeyInExternalBundle(final String bundleKey, 
			final String bundleName, final Locale locale) {
		final ResourceBundle bundle = getCachedExternalBundle(bundleName, locale);

		return isNotEmpty(bundle) && bundle.containsKey(bundleKey);
	}

	// IMPORTANT: this method is nearly exact copy of
	// getStringFromInternalBundle(…), however
	// any attempt in extracting common code from these two and pass differing parts
	// as lambdas
	// would cause great decrease in performance of WHOLE dataverse app.
	private static String getStringFromExternalBundle(final String key, 
			final String bundleName, final Locale locale) {
		final ResourceBundle bundle = getCachedExternalBundle(bundleName, locale);

		try {
			return isNotEmpty(bundle) ? bundle.getString(key) : null;
		} catch (final Exception ex) {
			if (logger.isLoggable(FINEST)) {
				logger.finest("Could not find key \"" + key + 
						"\" in bundle file: " + bundleName);
			}
			return null;
		}
	}
	
	private static boolean isNotEmpty(final ResourceBundle bundle) {
		return bundle != EMPTY_BUNDLE;
	}
	
	private static boolean isInternal(final String bundleName) {
		switch(bundleName) {
		case DEFAULT_BUNDLE_FILE:
		case "BuiltInRoles":
		case "MimeTypeDisplay":
		case "MimeTypeFacets":
		case "ValidationMessages": return false;
		default: return true;
		}
	}
	
	private static boolean isNotMetadataBundle(final String bundleName) {
		return !DefaultMetadataBlocks.METADATA_BLOCK_NAMES.contains(bundleName);
	}
	
	private static String maybeFormat(final String str, final Object... arguments) {
		return arguments.length == 0 ? str : format(str, arguments);
	}
	
	private static String extendedBundleName(final String bundleName) {
		return bundleName == DEFAULT_BUNDLE_FILE
				? DEFAULT_BUNDLE_FILE_EXTENTION
				: bundleName + '_' + EXTENSION_SUFFIX;
	}
}
