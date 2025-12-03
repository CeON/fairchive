package edu.harvard.iq.dataverse.search.periodo;

import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.parseLong;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;
import com.github.openjson.JSONTokener;

final class PeriodoImporter {

    private static final Logger log = getLogger(PeriodoImporter.class);

    /**
     * Consumes JSON files published on https://perio.do
     */
    public static Iterator<Period> readPeriods(final InputStream json) 
            throws Exception {
        return importNames(json).iterator();
    }
    
    /**
     * @param json file downloaded from https://perio.do
     * @param translatons a file in TSV format in form of 
     *          - header row
     *          - {period url}\t{english text}\t{polish text}.... (rest of columns ignored)
     */
    public static Iterator<Period> readPeriods(final InputStream json, 
    		final InputStream translations) throws Exception {
		if (translations != null) {
			return importNames(json, translations).iterator();
		} else {
			return readPeriods(json);
		}
    }
    
    /**
     * @param json file downloaded from https://perio.do
     * @param translatons a file in TSV format in form of 
     *          - header row
     *          - {period url}\t{english text}\t{polish text}.... (rest of columns ignored)
     * @param exclusions a file in CSV format
     * 			- header row
     * 			- {period url} ... ((rest of columns ignored)
     */
    public static Iterator<Period> readPeriods(final InputStream json, 
    		final InputStream translations, final InputStream exclusions) 
    				throws Exception {
		if (exclusions != null) {
			return importNames(json, translations, exclusions).iterator();
		} else {
			return readPeriods(json, translations);
		}
    }

    private static List<Period> importNames(final InputStream json) throws Exception {
        try {
            log.info("Reading Perio.do json");
            final long from = currentTimeMillis();
            final List<Period> result = readJson(json);
            logExecutionDuration(from);
            return result;
        } catch (final Exception e) {
            log.warn("Importing Perio.do failed.", e);
            throw e;
        }
    }
    
    private static List<Period> importNames(final InputStream json, 
    		final InputStream translations) throws Exception {
        try {
            log.info("Reading Perio.do json");
            final long from = currentTimeMillis();
            final List<Period> result = readJson(json);
            supplementWithTranslations(result, translations);
            logExecutionDuration(from);
            return result;
        } catch (final Exception e) {
            log.warn("Importing Perio.do failed.", e);
            throw e;
        }
    }
    
    private static List<Period> importNames(final InputStream json, 
    		final InputStream translations, final InputStream exclusions) 
    				throws Exception {
        try {
            log.info("Reading Perio.do json");
            final long from = currentTimeMillis();
            final List<Period> result = readJson(json);
            supplementWithTranslations(result, translations);
            removeExclusions(result, exclusions);
            logExecutionDuration(from);
            return result;
        } catch (final Exception e) {
            log.warn("Importing Perio.do failed.", e);
            throw e;
        }
    }
    
	private static void supplementWithTranslations(final List<Period> result, 
			final InputStream translations) throws Exception {
		final Map<String, Translations> translationsMap = readTSV(translations);
		for(final Period period : result) {
		    period.setTextEn(translationsMap.getOrDefault(period.getId(), Translations.none).en);
		    period.setTextPl(translationsMap.getOrDefault(period.getId(), Translations.none).pl);
		}
	}

	private static void removeExclusions(final List<Period> result, 
			final InputStream exclusions) throws Exception {
		final HashSet<String> exclusionsSet = readCSV(exclusions);
		result.removeIf(p -> exclusionsSet.contains(p.getId()));
	}
    
    private static List<Period> readJson(final InputStream in) throws Exception {
        try {
            final JSONObject json = new JSONObject(new JSONTokener(reader(in)));
            return parseAuthorities(json.getJSONObject("authorities"));
        } catch(final JSONException e) {
            if(e.getMessage().startsWith("End of input at character 0")) {
                return emptyList();
            } else {
                throw e;
            }
        }
    }
    
    private static ArrayList<Period> parseAuthorities(final JSONObject json) {
        final ArrayList<Period> result = new ArrayList<Period>(10000);
        
        for (final String autorityName : json.keySet()) {
            final JSONObject authority = json.getJSONObject(autorityName);
            final String title = getAutorityTitle(authority);
            parsePeriods(authority.getJSONObject("periods"), title, result);
        }
        return result;
    }
    
    private static String getAutorityTitle(final JSONObject authority) {
        final String title = authority.getJSONObject("source").optString("title",
                null);
        if (title != null) {
            return title;
        } else {
            final JSONObject partOf = authority.getJSONObject("source")
                    .optJSONObject("partOf");
            if (partOf != null) {
                return partOf.optString("title", "Unknown");
            } else {
                return authority.getJSONObject("source").optString("url", "Unknown");
            }
        }
    }
    
    private static void parsePeriods(final JSONObject json,
            final String autorityTile, final List<Period> result) {
        for (final String periodName : json.keySet()) {
            final JSONObject period = json.getJSONObject(periodName);
            final String id = period.getString("id");
            final String label = period.getString("label");
            final long start = getStartYear(period);
            final long stop = getStopYear(period);
            final Collection<String> locations = parseLocations(
                    period.optJSONArray("spatialCoverage"));
            final String coverageDescription = period
                    .optString("spatialCoverageDescription", "");
            result.add(new Period(id, label, start, stop, autorityTile,
                    coverageDescription, locations));
        }
    }
    
    private static long getStartYear(final JSONObject period) {
        final JSONObject in = period.getJSONObject("start").optJSONObject("in");
        return parseLong(in.optString("year", in.optString("earliestYear")));
    }

    private static long getStopYear(final JSONObject period) {
        final JSONObject stop = period.getJSONObject("stop");
        final JSONObject in = stop.optJSONObject("in");
        if (in != null) {
            final String year = in.optString("year", in.optString("latestYear"));
            // sometimes year is in the form of range (eg. "0040-0050") then take
            // last
            final int index = year.lastIndexOf('-');
            return parseLong(index > 0 ? year.substring(index + 1) : year);
        } else {
            if (stop.getString("label").equals("present")) {
                return MAX_VALUE;
            } else {
                throw new RuntimeException("Unknown label value '"
                        + stop.getString("label")
                        + ", for period '"
                        + period.getString("id") + "'.");
            }
        }
    }
    
    private static Collection<String> parseLocations(final JSONArray json) {
        // most locations contain 0 or 1 elemetns
        if (json == null) {
            return emptyList();
        } else if (json.length() == 0) {
            return emptyList();
        } else if (json.length() == 1) {
            return singletonList(json.getJSONObject(0).getString("label"));
        } else {
            final ArrayList<String> result = new ArrayList<>(json.length());
            for (int i = 0; i < json.length(); ++i) {
                result.add(json.getJSONObject(i).getString("label"));
            }
            return result;
        }
    }
    
    private static HashMap<String, Translations> readTSV(final InputStream tsv) 
            throws Exception {
        
        final HashMap<String, Translations> result = new HashMap<>();
        
        try (final BufferedReader reader = new BufferedReader(reader(tsv))) {    
            String line = reader.readLine(); // skip headers row
            while ((line = reader.readLine()) != null) {
                final String[] values = line.split("\t");
                final String id = values[0].startsWith(Period.base) 
                        ? values[0].substring(Period.base.length()).trim()
                        : values[0].trim();     
                result.put(id, new Translations(values[1], values[2]));
            }
        }
        return result;
    }
    
    private static HashSet<String> readCSV(final InputStream csv) 
            throws Exception {
        
        final HashSet<String> result = new HashSet<>();
        
        try (final BufferedReader reader = new BufferedReader(reader(csv))) {    
            String line = reader.readLine(); // skip headers row
            while ((line = reader.readLine()) != null) {
                final String[] values = line.split(",");
                final String id = values[0].startsWith(Period.base) 
                        ? values[0].substring(Period.base.length()).trim()
                        : values[0].trim();     
                result.add(id);
            }
        }
        return result;
    }
    
    private static Reader reader(final InputStream in) throws IOException{
    	return new InputStreamReader(in, "UTF-8");
    }
    
	private static void logExecutionDuration(final long begin) {
		log.info("Perio.do read in {} seconds.", (currentTimeMillis() - begin) / 1000);
	}
    
    private final static class Translations {
        
        final static Translations none = new Translations(null, null);
        final String en;
        final String pl;
        
        Translations(final String en, final String pl) {
            this.en = en;
            this.pl = pl;
        }
    }
}
