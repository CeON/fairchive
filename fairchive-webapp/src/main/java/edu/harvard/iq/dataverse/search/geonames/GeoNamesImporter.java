package edu.harvard.iq.dataverse.search.geonames;

import static java.lang.System.currentTimeMillis;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;

final class GeoNamesImporter {
    // this class operates on large number of data do the code is lower-level
    // for performance reasons

    // strings repeat often so deduplicating them saves a lot of memory
    private final StringDeduplicator cache = new StringDeduplicator();

    private static final Logger log = getLogger(GeoNamesImporter.class);
    private static final String SEP = " - ";

    /**
     * Consumes unzipped text files published on
     * https://download.geonames.org/export/dump/
     */
    static Iterator<GeoName> readNames(final InputStream in) throws Exception {
        return new GeoNamesImporter().importNames(in).iterator();
    }

    private Stream<GeoName> importNames(final InputStream in) throws Exception {
        try {
            log.info("Reading geo names");
            final long begin = currentTimeMillis();
            final Map<String, List<GeoName>> map = read(in);
            final long readtime = currentTimeMillis();
            log.info("Geo names read in {} seconds.", (readtime - begin) / 1000);

            int index = 1;
            for (final String countryCode : map.keySet()) {
                log.info("Processing geo names from {} - {}/{}.",
                        countryCode, index, map.size());
                final long processingStart = currentTimeMillis();
                final List<GeoName> geoNames = map.get(countryCode);
                process(geoNames);
                log.info("Processed geo names from {} in {} seconds.",
                        countryCode, (currentTimeMillis() - processingStart) / 1000);
                ++index;
            }
            log.info("Finished processing geo names in {} seconds.",
                    +(currentTimeMillis() - begin) / 1000);
            log.info("Cache size: " + this.cache.size());
            return map.values().stream().flatMap(List::stream);
        } catch (final Exception e) {
            log.warn("Importing geo names failed.", e);
            throw e;
        }
    }

    private void process(final List<GeoName> geoNames) {

        Map<String, List<GeoName>> tier1 = processTier1(geoNames);
        Map<String, Map<String, List<GeoName>>> tier2 = processTier2(geoNames, tier1);
        tier1 = null; // it can be garbage-collected now
        Map<String, Map<String, Map<String, List<GeoName>>>> tier3 = processTier3(
                geoNames, tier2);
        tier2 = null; // it can be garbage-collected now
        processTier4(geoNames, tier3);
    }

    private Map<String, List<GeoName>> processTier1(final List<GeoName> geoNames) {
        final HashMap<String, List<GeoName>> result = new HashMap<>();
        // reusing a builder to reduce garbage in comparison with + operator
        final StringBuilder builder = new StringBuilder(40);

        for (final GeoName gn : geoNames) {
            if (gn.isTier1()) {
                builder.setLength(0);
                builder.append(gn.getCountryCode()).append(SEP).append(gn.getName());
                gn.setHierarchy(builder.toString());

                result.computeIfAbsent(gn.getAdmin1Code(), k -> new ArrayList<>())
                        .add(gn);
            }
        }
        return result;
    }

    private Map<String, Map<String, List<GeoName>>> processTier2(
            final List<GeoName> names,
            final Map<String, List<GeoName>> tier1) {
        final Map<String, Map<String, List<GeoName>>> result = new HashMap<>();
        // reusing a builder to reduce garbage in comparison with + operator
        final StringBuilder builder = new StringBuilder(40);

        for (final GeoName gn : names) {
            if (gn.isTier2()) {
                final List<GeoName> list = tier1.get(gn.getAdmin1Code());
                if (list != null) {
                    final GeoName tr = find(list, GeoName::isAdm1);
                    if (tr != null) {
                        builder.setLength(0);
                        builder.append(tr.getHierarchy()).append(SEP)
                                .append(gn.getName());
                        gn.setHierarchy(builder.toString());

                        result.computeIfAbsent(gn.getAdmin1Code(),
                                k -> new HashMap<>())
                                .computeIfAbsent(gn.getAdmin2Code(),
                                        k -> new ArrayList<>())
                                .add(gn);
                    }
                }
            }
        }
        return result;
    }

    private Map<String, Map<String, Map<String, List<GeoName>>>> processTier3(
            final List<GeoName> names,
            final Map<String, Map<String, List<GeoName>>> tier2) {
        final Map<String, Map<String, Map<String, List<GeoName>>>> result = new HashMap<>();
        // reusing a builder to reduce garbage in comparison with + operator
        final StringBuilder builder = new StringBuilder(40);

        for (final GeoName gn : names) {
            if (gn.isTier3() && tier2.containsKey(gn.getAdmin1Code())) {
                final List<GeoName> list = tier2.get(gn.getAdmin1Code())
                        .get(gn.getAdmin2Code());
                if (list != null) {
                    final GeoName tr = find(list, GeoName::isAdm2);
                    if (tr != null) {
                        builder.setLength(0);
                        builder.append(tr.getHierarchy()).append(SEP)
                                .append(gn.getName());
                        gn.setHierarchy(builder.toString());

                        result.computeIfAbsent(gn.getAdmin1Code(),
                                k -> new HashMap<>())
                                .computeIfAbsent(gn.getAdmin2Code(),
                                        k -> new HashMap<>())
                                .computeIfAbsent(gn.getAdmin3Code(),
                                        k -> new ArrayList<>())
                                .add(gn);
                    }
                }
            }
        }
        return result;
    }

    private void processTier4(final List<GeoName> names,
            final Map<String, Map<String, Map<String, List<GeoName>>>> tier3) {
        // reusing a builder to reduce garbage in comparison with + operator
        final StringBuilder builder = new StringBuilder(40);
        for (final GeoName gn : names) {
            if (gn.isTier4()
                    && tier3.containsKey(gn.getAdmin1Code())
                    && tier3.get(gn.getAdmin1Code()).containsKey(gn.getAdmin2Code())) {
                final List<GeoName> list = tier3.get(gn.getAdmin1Code())
                        .get(gn.getAdmin2Code()).get(gn.getAdmin3Code());
                if (list != null) {
                    final GeoName tr = find(list, GeoName::isAdm3);
                    if (tr != null) {
                        builder.setLength(0);
                        builder.append(tr.getHierarchy()).append(SEP)
                                .append(gn.getName());
                        gn.setHierarchy(builder.toString());
                    }
                }
            }
        }
    }

    private Map<String, List<GeoName>> read(final InputStream in) throws Exception {
        return new BufferedReader(new InputStreamReader(in, "utf8"))
                .lines()
                .map(line -> line.split("\t"))
                .map(cells -> {
                    final GeoName result = new GeoName();
                    result.setId(cells[0].trim());
                    result.setName(cells[1].trim());
                    result.setAlternateNames(cells[3].trim().replaceAll(",", ", "));
                    result.setFeatureCode(cache.deduplicate(cells[7].trim()));
                    result.setCountryCode(cache.deduplicate(cells[8].trim()));
                    result.setAdmin1Code(cache.deduplicateOrNull(cells[10]));
                    result.setAdmin2Code(cache.deduplicateOrNull(cells[11]));
                    result.setAdmin3Code(cache.deduplicateOrNull(cells[12]));
                    result.setAdmin4Code(cache.deduplicateOrNull(cells[13]));
                    result.setHierarchy(result.getCountryCode());
                    // fix/mask dictionary errors
                    if (result.isAdm3() && result.getAdmin2Code() == null) {
                        result.setAdmin2Code("-1");
                    }
                    return result;
                })
                .collect(groupingBy(GeoName::getCountryCode));
    }

    private static GeoName find(final List<GeoName> list,
            final Predicate<GeoName> predicate) {
        // this methods is called frequently, avoiding streams and optionals
        // reduces a lot of garbage objects
        for (final GeoName gn : list) {
            if (predicate.test(gn)) {
                return gn;
            }
        }
        return null;
    }

    @SuppressWarnings("serial")
    private final static class StringDeduplicator extends HashMap<String, String> {

        private final Function<String, String> identity = identity();

        String deduplicate(final String s) {
            return computeIfAbsent(s, identity);
        }

        String deduplicateOrNull(final String s) {
            return isBlank(s) ? null : computeIfAbsent(s, identity);
        }
    }
}
