package edu.harvard.iq.dataverse.common.files.mime;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.List;


public final class MimeTypes {

	public final static String TSV = "text/tsv";
	public final static String TAB_SEPARATED_VALUES = "text/tab-separated-values";
	public final static String CSV = "text/csv";
	public final static String COMMA_SEPARATED_VALUES = "text/comma-separated-values";
	public final static String PLAIN_TEXT = "text/plain";
	public final static String FIXED_FIELD = "text/x-fixed-field";
	public final static String GRAPHML = "text/xml-graphml";
	public final static String STATA_SYNTAX = "text/x-stata-syntax";
	public final static String SPSS_CCARD = "text/x-spss-syntax";
	public final static String SAS_SYNTAX = "text/x-sas-syntax";
	// http://en.wikipedia.org/wiki/Shapefile
	public final static String SHAPEFILE = "application/zipped-shapefile";

	public final static String APPLICATION_FITS = "application/fits";
	public final static String APPLICATION_FITS_GZIPPED = "application/fits-gzipped";
	public final static String SPSS_SAV = "application/x-spss-sav";
	public final static String SPSS_POR = "application/x-spss-por";
	public final static String R_SYNTAX = "application/x-r-syntax";
	public final static String SAS_SYSTEM = "application/x-sas-system";
	public final static String DOCUMENT_PDF = "application/pdf";
	public final static String SAS_TRANSPORT = "application/x-sas-transport";
	public final static String GEO_SHAPE = "application/zipped-shapefile";
	public final static String UNDETERMINED_DEFAULT = "application/octet-stream";
	public final static String UNDETERMINED_BINARY = "application/binary";
	public final static String ZIP = "application/zip";
	public final static String STATA = "application/x-stata";
	public final static String STATA13 = "application/x-stata-13";
	public final static String STATA14 = "application/x-stata-14";
	public final static String STATA15 = "application/x-stata-15";
	public final static String RDATA = "application/x-rlang-transport";
	public final static String DOCUMENT_MSWORD = "application/msword";
	public final static String DOCUMENT_MSEXCEL = "application/vnd.ms-excel";
	public final static String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	public final static String DOCUMENT_MSWORD_OPENXML = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	
	public final static String IMAGE_FITS = "image/fits";
	
	public final static String DATAVERSE_PACKAGE = "application/vnd.dataverse.file-package";

	private final static List<String> ingestable = asList(CSV, COMMA_SEPARATED_VALUES, 
			TSV, TAB_SEPARATED_VALUES, STATA, STATA13, STATA14, STATA15, RDATA, 
			XLSX, SPSS_SAV, SPSS_POR);
	
	private final static List<String> selectivelyIngestable = asList(CSV, COMMA_SEPARATED_VALUES, 
			TSV, TAB_SEPARATED_VALUES, XLSX);
	
	private final static List<String> tabular = asList(SAS_TRANSPORT, SAS_SYSTEM, 
			STATA, STATA13, STATA14, STATA15, RDATA, XLSX, SPSS_SAV, SPSS_POR, 
			FIXED_FIELD, CSV, COMMA_SEPARATED_VALUES, TSV, TAB_SEPARATED_VALUES);
	
	private final static List<String> code = asList(R_SYNTAX, STATA_SYNTAX, 
			SAS_SYNTAX, SPSS_CCARD);
	
	private final static List<String> documents = asList(PLAIN_TEXT, DOCUMENT_PDF, 
			DOCUMENT_MSWORD, DOCUMENT_MSEXCEL, DOCUMENT_MSWORD_OPENXML);
	
	private final static List<String> astro = asList(APPLICATION_FITS, 
			APPLICATION_FITS_GZIPPED, IMAGE_FITS);
	
	private final static List<String> supportsPickingEncoding = asList(SPSS_POR, 
			SPSS_SAV, CSV, COMMA_SEPARATED_VALUES);
	
	public static boolean isIngestable(final String mimeType) {
		return ingestable.contains(mimeType);
	}
	
	public static boolean isSelectivelyIngestable(final String mimeType) {
		return selectivelyIngestable.contains(mimeType);
	}
	
	public static boolean isTabular(final String mimeType) {
		return tabular.contains(mimeType);
	}
	
	public static boolean supportsPickingEncoding(final String mimeType) {
		return supportsPickingEncoding.contains(mimeType);
	}
	
    public static boolean supportsInclusionOfLabels(final String mimeType) {
        return SPSS_POR.equals(mimeType);
    }
	
	public static boolean isCode(final String mimeType) {
		return code.contains(mimeType);
	}
	
	public static boolean isDocument(final String mimeType) {
		return mimeType != null
			? documents.stream().anyMatch(mime -> mimeType.startsWith(mime))
		    : false;
	}

	public static boolean isAstro(final String mimeType) {
		return astro.contains(mimeType);
	}
	
	public static boolean isNetwork(final String mimeType) {
		return GRAPHML.equals(mimeType);
	}
	
	public static boolean isGeoShape(final String mimeType) {
		return GEO_SHAPE.equals(mimeType);
	}
	
	public static boolean isPDF(final String mimeType) {
		return DOCUMENT_PDF.equals(mimeType);
	}
	
	public static boolean isDataversePackage(final String mimeType) {
		return DATAVERSE_PACKAGE.equals(mimeType);
	}
	
	public static boolean isVideo(final String mimeType) {
		return mimeType != null ? mimeType.startsWith("video/") : false;
	}
	
	public static boolean isAudio(final String mimeType) {
		return mimeType != null ? mimeType.startsWith("audio/") : false;
	}
	
	public static boolean isImage(final String mimeType) {
		return mimeType != null ? mimeType.startsWith("image/") : false;
	}
	
	public static boolean isUndetermined(final String mimeType) {
		return isEmpty(mimeType) 
				|| UNDETERMINED_BINARY.equals(mimeType) 
				|| UNDETERMINED_DEFAULT.equals(mimeType);
	}
	
    public static String fileExtensionOf(final String fileType) {
        
    	if (SPSS_SAV.equalsIgnoreCase(fileType)) {
            return ".sav";
        } else if (SPSS_POR.equalsIgnoreCase(fileType)) {
            return ".por";
        } else if (STATA.equalsIgnoreCase(fileType)) {
            return ".dta";
        } else if (RDATA.equalsIgnoreCase(fileType)) {
            return ".RData";
        } else if (CSV.equalsIgnoreCase(fileType)) {
            return ".csv";
        } else if (TSV.equalsIgnoreCase(fileType)) {
            return ".tsv";
        } else if (XLSX.equalsIgnoreCase(fileType)) {
            return ".xlsx";
        } else {
        	return EMPTY;
        }
    }
}
