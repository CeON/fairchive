package edu.harvard.iq.dataverse.search.periodo;

import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.parseLong;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

final class PeriodoImporter {

    private static final Logger log = getLogger(PeriodoImporter.class);

    /**
     * Consumes JSON files published on https://perio.do
     */
    public static Iterator<Period> readPeriods(final InputStream json) throws Exception {
        try {
            log.info("Reading Perio.do json");
            final long from = currentTimeMillis();
            final List<Period> result = readJson(json);
            logExecutionDuration(from);
            return result.iterator();
        } catch (final Exception e) {
            log.warn("Importing Perio.do failed.", e);
            throw e;
        }
    }
    
    /**
     * @param json file downloaded from https://perio.do
     * @param translatons a file in TSV format in form of 
     *          - header row
     *          - {period url}\t{english text}\t{polish text}.... (rest of columns ignored)
     */
	public static Iterator<Period> readPeriods(final InputStream json, 
			final InputStream translations)
			throws Exception {
		if (translations != null) {
			try {
				log.info("Reading Perio.do json");
				final long from = currentTimeMillis();
				final List<Period> result = readJson(json);
				supplementWithTranslations(result, translations);
				logExecutionDuration(from);
				return result.iterator();
			} catch (final Exception e) {
				log.warn("Importing Perio.do failed.", e);
				throw e;
			}
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
			try {
				log.info("Reading Perio.do json");
				final long from = currentTimeMillis();
				final List<Period> result = readJson(json);
				supplementWithTranslations(result, translations);
				removeExclusions(result, exclusions);
				logExecutionDuration(from);
				return result.iterator();
			} catch (final Exception e) {
				log.warn("Importing Perio.do failed.", e);
				throw e;
			}
		} else {
			return readPeriods(json, translations);
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
        	final JsonElement json = new JsonParser().parse(reader(in));
        	return json.isJsonObject()
        			? parseAuthorities(json.getAsJsonObject().get("authorities").getAsJsonObject())
        			: emptyList();
        } catch(final JsonParseException  e) {
            if(e.getMessage().startsWith("End of input at character 0")) {
                return emptyList();
            } else {
                throw e;
            }
        }
    }
    
    private static ArrayList<Period> parseAuthorities(final JsonObject json) {
        final ArrayList<Period> result = new ArrayList<Period>(10000);
        
        for (final Map.Entry<String, JsonElement> entry : json.entrySet()) {
        	final JsonObject authority = entry.getValue().getAsJsonObject();
            final String title = getAutorityTitle(authority);
            parsePeriods(authority.getAsJsonObject("periods"), title, result);
        }
        return result;
    }
    
	private static String getAutorityTitle(final JsonObject authority) {
		final JsonObject source = authority.getAsJsonObject("source");
		if (source.has("title") && !source.get("title").isJsonNull()) {
			return source.get("title").getAsString();
		} else {
			if (source.has("partOf") && source.get("partOf").isJsonObject()) {
				final JsonObject partOf = source.getAsJsonObject("partOf");
				return partOf.has("title") ? partOf.get("title").getAsString() : "Unknown";
			} else {
				return source.has("url") ? source.get("url").getAsString() : "Unknown";
			}
		}
	}
    
	private static void parsePeriods(final JsonObject json, 
			final String authorityTitle, final List<Period> result) {
		for (final Map.Entry<String, JsonElement> entry : json.entrySet()) {
			final JsonObject period = entry.getValue().getAsJsonObject();

			final String id = period.get("id").getAsString();
			final String label = period.get("label").getAsString();
			final long start = getStartYear(period);
			final long stop = getStopYear(period);
			final Collection<String> locations = parseLocations(
					period.has("spatialCoverage") ? period.getAsJsonArray("spatialCoverage") : null);
			final String coverageDescription = period.has("spatialCoverageDescription")
					? period.get("spatialCoverageDescription").getAsString()
					: "";

			result.add(new Period(id, label, start, stop, authorityTitle, 
					coverageDescription, locations));
		}
	}
    
    private static long getStartYear(final JsonObject period) {
        final JsonObject in = period.getAsJsonObject("start").getAsJsonObject("in");
        return parseLong(
                in.has("year") ? in.get("year").getAsString()
                        : in.get("earliestYear").getAsString()
        );
    }
    
    private static long getStopYear(final JsonObject period) {
        final JsonObject stop = period.getAsJsonObject("stop");

        if (stop.has("in") && stop.get("in").isJsonObject()) {
            final JsonObject in = stop.getAsJsonObject("in");
            final String year = in.has("year")
                    ? in.get("year").getAsString()
                    : in.get("latestYear").getAsString();

            final int index = year.lastIndexOf('-');
            return parseLong(index > 0 ? year.substring(index + 1) : year);
        } else {
            final String label = stop.get("label").getAsString();
            if ("present".equals(label)) {
                return MAX_VALUE;
            } else {
                throw new RuntimeException("Unknown label value '" + label +
                        "', for period '" + period.get("id").getAsString() + "'.");
            }
        }
    }
    
    private static Collection<String> parseLocations(final JsonArray json) {
        if (json == null) {
            return emptyList();
        } else if(json.size() == 0) {
            return emptyList();
        } else if (json.size() == 1) {
            return singletonList(json.get(0).getAsJsonObject().get("label").getAsString());
        } else {
            final ArrayList<String> result = new ArrayList<>(json.size());
            for (final JsonElement el : json) {
                result.add(el.getAsJsonObject().get("label").getAsString());
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
    	return new InputStreamReader(in, UTF_8);
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
