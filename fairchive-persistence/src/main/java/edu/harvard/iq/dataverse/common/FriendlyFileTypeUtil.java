package edu.harvard.iq.dataverse.common;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromNonDefaultBundleWithLocale;
import static edu.harvard.iq.dataverse.common.files.mime.MimeTypes.UNDETERMINED_DEFAULT;

import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import edu.harvard.iq.dataverse.common.files.mime.MimeTypes;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;

public class FriendlyFileTypeUtil {

    public static String getUserFriendlyFileType(final DataFile dataFile) {
        return getUserFriendlyFileTypeForDisplay(dataFile.getContentType());
    }

    public static String getUserFriendlyFileTypeForDisplay(final String contentType) {
        return getUserFriendlyFileType(contentType, Locale.ENGLISH, "MimeTypeDisplay");
    }

    public static String getUserFriendlyFileType(final DataFile dataFile, 
    		final Locale locale) {
        return getUserFriendlyFileType(dataFile.getContentType(), locale, 
        		"MimeTypeFacets");
    }
    
    private static String getUserFriendlyFileType(String contentType, 
    		final Locale locale, final String bundle) {
        
        if (contentType.contains(";")) {
        	contentType = contentType.substring(0, contentType.indexOf(";"));
        }

        return Optional.ofNullable(getStringFromNonDefaultBundleWithLocale(contentType, bundle, locale))
                .filter(name -> !name.isEmpty())
                .orElse(getStringFromNonDefaultBundleWithLocale(UNDETERMINED_DEFAULT, bundle, locale));
    }
    
    
    public static String getUserFriendlyOriginalType(DataFile dataFile) {
        if (!dataFile.isTabularData()) {
            return null;
        }

        String fileType = dataFile.getOriginalFileFormat();

        if (StringUtils.isNotEmpty(fileType)) {
            if (fileType.contains(";")) {
                fileType = fileType.substring(0, fileType.indexOf(";"));
            }

            return Optional.ofNullable(BundleUtil.getStringFromNonDefaultBundle(fileType, "MimeTypeDisplay"))
                    .filter(bundleName -> !bundleName.isEmpty())
                    .orElse(fileType);
        }

        return BundleUtil.getStringFromNonDefaultBundle(MimeTypes.UNDETERMINED_DEFAULT, "MimeTypeDisplay");
    }
}
