package edu.harvard.iq.dataverse.persistence.dataset;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.parseLong;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import com.github.openjson.JSONTokener;

public final class PeriodoDictionary {

    private static final String base;
    private static final List<Period> periods = new ArrayList<>(8800);
    private static final Set<String> locations = new TreeSet<>();

    static {
        try (final Reader in = new InputStreamReader(PeriodoDictionary.class
                .getResourceAsStream("/periodo-dataset.json"), UTF_8)) {
            final JSONObject json = new JSONObject(new JSONTokener(in));
            final JSONArray context = json.getJSONArray("@context");
            base = context.getJSONObject(1).getString("@base");
            parseAuthorities(json.getJSONObject("authorities"));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Period> find(final String query) {
        return stream(query).collect(toList());
    }

    public static Optional<Period> getByUrl(final String url) {
        return url.startsWith(base) ? stream(url).findAny() : empty();
    }

    public static List<String> locations() {
        return new ArrayList<String>(locations);
    }

    private static void parseAuthorities(final JSONObject json) {
        for (final String autorityName : json.keySet()) {
            final JSONObject authority = json.getJSONObject(autorityName);
            final String title = getAutorityTitle(authority);
            parsePeriods(authority.getJSONObject("periods"), title);
        }
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

    private static void parsePeriods(final JSONObject json,
            final String autorityTile) {
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
            periods.add(new Period(id, label, start, stop, autorityTile,
                    coverageDescription, locations));
        }
    }

    private static Collection<String> parseLocations(final JSONArray json) {
        // most locations contain 0 or 1 elemetns
        if (json == null) {
            return emptyList();
        } else if (json.length() == 0) {
            return emptyList();
        } else if (json.length() == 1) {
            return singletonList(getLabel(json.getJSONObject(0)));
        } else {
            final ArrayList<String> result = new ArrayList<>(json.length());
            for (int i = 0; i < json.length(); ++i) {
                result.add(getLabel(json.getJSONObject(i)));
            }
            return result;
        }
    }

    private static String getLabel(final JSONObject json) {
        // locations often repeat, so interning them reduces size of the dictionary in
        // memory
        final String label = json.getString("label").intern();
        locations.add(label);
        return label;
    }

    private static Stream<Period> stream(final String query) {
        final String sanitizedQuery = sanitizeQuery(query);
        if (sanitizedQuery.isEmpty()) {
            return Stream.empty();
        } else {
            return periods.stream().filter(period -> period.matches(sanitizedQuery));
        }
    }

    private static String sanitizeQuery(final String query) {
        final int indexOfLastSlash = query.lastIndexOf('/');
        return indexOfLastSlash > -1 ? query.substring(indexOfLastSlash + 1).trim()
                : query.trim();
    }

    public static final class Period {
        private final String id;
        private final String label;
        private final long start;
        private final long stop;
        private final String authorityTitle;
        private final String coverageName;
        private final Collection<String> locations;

        private Period(final String id, final String label,
                final long start, final long stop, final String authorityTitle,
                final String coverageName, final Collection<String> locations) {
            this.id = id;
            this.label = label;
            this.start = start;
            this.stop = stop;
            this.authorityTitle = authorityTitle;
            this.coverageName = coverageName;
            this.locations = locations;
        }

        private boolean matches(final String query) {
            return containsIgnoreCase(this.id, query) ||
                    containsIgnoreCase(this.label, query) ||
                    containsIgnoreCase(this.coverageName, query) ||
                    containsIgnoreCase(this.authorityTitle, query) ||
                    this.locations.stream()
                            .anyMatch(location -> containsIgnoreCase(location, query));
        }

        public String getValue() {
            return base.concat(this.id);
        }

        @Override
        public String toString() {
            return getValue();
        }

        public String getDetails(final String separator) {
            final StringBuilder result = new StringBuilder(80);
            result.append(getStringFromBundle("periodo.label")).append(": ")
                    .append(this.label).append(separator);
            result.append(getStringFromBundle("periodo.location")).append(": ");
            String coma = "";
            for (final String location : this.locations) {
                result.append(coma).append(location);
                coma = ", ";
            }
            result.append(separator);
            result.append(getStringFromBundle("periodo.location.desc")).append(": ")
                    .append(this.coverageName).append(separator);
            result.append(getStringFromBundle("periodo.start")).append(": ")
                    .append(this.start).append(separator);
            result.append(getStringFromBundle("periodo.end")).append(": ");
            if (this.stop == MAX_VALUE) {
                result.append(getStringFromBundle("periodo.present"));
            } else {
                result.append(this.stop);
            }
            result.append(separator);
            result.append(getStringFromBundle("periodo.authority")).append(": ")
                    .append(this.authorityTitle);
            return result.toString();
        }

        public String getDetails() {
            return getDetails(" ");
        }

        public String getId() {
            return this.id;
        }

        public String getLabel() {
            return this.label;
        }

        public long getStart() {
            return this.start;
        }

        public long getStop() {
            return this.stop;
        }

        public String getAuthorityTitle() {
            return this.authorityTitle;
        }

        public String getCoverageName() {
            return this.coverageName;
        }

        public Collection<String> getLocations() {
            return this.locations;
        }
    }
}