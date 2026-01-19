package edu.harvard.iq.dataverse.common;

import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import edu.harvard.iq.dataverse.common.files.mime.MimeTypes;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;

public class FriendlyFileTypeUtil {

    public static String getUserFriendlyFileType(DataFile dataFile) {
        return getUserFriendlyFileTypeForDisplay(dataFile.getContentType());
    }

    public static String getUserFriendlyFileTypeForDisplay(String dataFileContentType) {
        if (dataFileContentType.equalsIgnoreCase(MimeTypes.SHAPEFILE)) {
            return "Shapefile as ZIP Archive";
        }
        if (dataFileContentType.contains(";")) {
            dataFileContentType = dataFileContentType.substring(0, dataFileContentType.indexOf(";"));
        }

        return Optional.ofNullable(BundleUtil.getStringFromNonDefaultBundle(dataFileContentType, "MimeTypeDisplay"))
                .filter(bundleName -> !bundleName.isEmpty())
                .orElse(BundleUtil.getStringFromNonDefaultBundle(MimeTypes.UNDETERMINED_DEFAULT, "MimeTypeDisplay"));
    }

    public static String getUserFriendlyFileType(DataFile dataFile, Locale locale) {
        String fileType = dataFile.getContentType();

        if (fileType.equalsIgnoreCase(MimeTypes.SHAPEFILE)) {
            return "Shapefile as ZIP Archive";
        }
        if (fileType.contains(";")) {
            fileType = fileType.substring(0, fileType.indexOf(";"));
        }

        return Optional.ofNullable(BundleUtil.getStringFromNonDefaultBundleWithLocale(fileType, "MimeTypeFacets", locale))
                .filter(bundleName -> !bundleName.isEmpty())
                .orElse(BundleUtil.getStringFromNonDefaultBundleWithLocale(MimeTypes.UNDETERMINED_DEFAULT, "MimeTypeFacets", locale));
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
