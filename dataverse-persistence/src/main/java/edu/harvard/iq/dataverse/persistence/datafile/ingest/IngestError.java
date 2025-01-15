package edu.harvard.iq.dataverse.persistence.datafile.ingest;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;

import java.util.List;

/**
 * Enum representing error keys, which translates to bundle key with prefix.
 */
public enum IngestError {
    NOPLUGIN,
    STATS_OR_SIGNATURE_FAILURE,
    DB_FAIL_WITH_TAB_PRODUCED,
    DB_FAIL,
    PLUGIN_RAW_FILES,
    UNZIP_FAIL,
    UNZIP_SIZE_FAIL,
    UNZIP_FILE_LIMIT_FAIL,
    WRONG_HEADER,
    CSV_INVALID_HEADER,
    CSV_LINE_MISMATCH,
    CSV_RECORD_MISMATCH,
    EXCEL_PARSE,
    EXCEL_UNKNOWN_VARIABLE_NAME,
    EXCEL_AMBIGUOUS_INDEX_POSITION,
    EXCEL_NO_ROWS,
    EXCEL_ONLY_ONE_ROW,
    EXCEL_READ_FAIL,
    EXCEL_MISMATCH,
    EXCEL_LINE_COUNT,
    EXCEL_NUMERIC_PARSE,
    RTAB_FAIL,
    RTAB_MISMATCH,
    RTAB_BOOLEAN_FAIL,
    RTAB_UNREDABLE_BOOLEAN,
    RTAB_VARQNTY_MISSING,
    RTAB_VARQNTY_ZERO,
    GENERAL_TOO_MANY_VARIABLES,
    UNKNOWN_ERROR;

    // -------------------- LOGIC --------------------

    public String getErrorMessage(final List<String> arguments) {
        return getStringFromBundle("ingest.error.".concat(name()), arguments.toArray());
    }

}
