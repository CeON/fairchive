package edu.harvard.iq.dataverse.search.periodo;

import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.parseLong;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
        return new PeriodoImporter().importNames(json).iterator();
    }
    
    public static Iterator<Period> readPeriods(final InputStream json, final InputStream tsv) 
            throws Exception {
        return new PeriodoImporter().importNames(json, tsv).iterator();
    }

    private List<Period> importNames(final InputStream json) throws Exception {
        try {
            log.info("Reading Perio.do json");
            final long begin = currentTimeMillis();
            final List<Period> result = readJson(json);
            final long readtime = currentTimeMillis();
            log.info("Perio.do read in {} seconds.", (readtime - begin) / 1000);
            return result;
        } catch (final Exception e) {
            log.warn("Importing Perio.do failed.", e);
            throw e;
        }
    }
    
    private List<Period> importNames(final InputStream json, final InputStream tsv) 
            throws Exception {
        try {
            log.info("Reading Perio.do json");
            final long begin = currentTimeMillis();
            final List<Period> result = readJson(json);
            final Map<String, Translations> translations = readTsv(tsv);
            for(final Period period : result) {
                period.setTextEn(translations.getOrDefault(period.getId(), Translations.none).en);
                period.setTextPl(translations.getOrDefault(period.getId(), Translations.none).pl);
            }
            final long readtime = currentTimeMillis();
            log.info("Perio.do read in {} seconds.", (readtime - begin) / 1000);
            return result;
        } catch (final Exception e) {
            log.warn("Importing Perio.do failed.", e);
            throw e;
        }
    }
    
    private List<Period> readJson(final InputStream in) throws Exception {
        try {
            final JSONObject json = new JSONObject(new JSONTokener(new InputStreamReader(in, UTF_8)));
            return parseAuthorities(json.getJSONObject("authorities"));
        } catch(final JSONException e) {
            if(e.getMessage().startsWith("End of input at character 0")) {
                return emptyList();
            } else {
                throw e;
            }
        }
    }
    
    private List<Period> parseAuthorities(final JSONObject json) {
        final List<Period> result = new ArrayList<Period>(10000);
        
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
    
    private void parsePeriods(final JSONObject json,
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
    
    private Map<String, Translations> readTsv(final InputStream tsv) 
            throws Exception {
        
        final HashMap<String, Translations> result = new HashMap<>();
        
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(tsv, "UTF-8"))) {    
            String line = reader.readLine(); // skip headers row
            while ((line = reader.readLine()) != null) {
                final String[] values = line.split("\t");
                final String id = values[0].startsWith(Period.base) 
                        ? values[0].substring(Period.base.length()) 
                        : values[0];     
                result.put(id, new Translations(values[1], values[2]));
            }
        }
        return result;
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
