package edu.harvard.iq.dataverse.persistence.geonames;

import static java.lang.Integer.parseInt;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.ejb.Singleton;
import javax.transaction.Transactional;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Singleton
public class GeoNameRepository extends JpaRepository<Integer, GeoName> {

    public GeoNameRepository() {
        super(GeoName.class);
    }

    public List<GeoName> find(final String text) {
        final String trimmedText = text.trim();
        if (trimmedText.isEmpty()) {
            return emptyList();
        } else {
            return this.em.createQuery("SELECT gn FROM GeoName gn " +
                    "WHERE LOWER(gn.name) LIKE LOWER(CONCAT('%', :text, '%')) " +
                    "OR LOWER(gn.alternateNames) LIKE LOWER(CONCAT('%', :text, '%')) ",
                    GeoName.class)
                    .setParameter("text", trimmedText)
                    .getResultList();
        }
    }

    public void deleteAll() {
        this.em.createNativeQuery("TRUNCATE TABLE geoname CONTINUE IDENTITY RESTRICT")
                .executeUpdate();
    }

    public void importNames(final InputStream in) throws Exception {
        final List<GeoName> names = read(in);
        List<GeoName> tier1 = processTier1(names);
        List<GeoName> tier2 = processTier2(names, tier1);
        List<GeoName> tier3 = processTier3(names, tier2);
        processTier4(names, tier3);
        
        names.forEach(this::store);
    }
    
    private List<GeoName> processTier1(final List<GeoName> names) {
        return names.stream()
                .filter(GeoName::isTier1)
                .peek(gn -> gn.setHierarchy(gn.getCountryCode() + " - " + gn.getName()))
                .collect(toList());
    }
    
    private List<GeoName> processTier2(final List<GeoName> names,
            final List<GeoName> tier1) {
        return names.stream()
                .filter(GeoName::isTier2)
                .peek(gn -> gn.setHierarchy(
                        findByAdmin1Code(tier1, gn.getAdmin1Code()).getHierarchy()
                                + " - " + gn.getName()))
                .collect(toList());
    }

    private static GeoName findByAdmin1Code(final List<GeoName> tier1,
            final String code) {
        return tier1.stream()
                .filter(GeoName::isAdm1)
                .filter(gm -> gm.getAdmin1Code().equals(code))
                .findFirst()
                .get();
    }

    private List<GeoName> processTier3(final List<GeoName> names,
            final List<GeoName> tier2) {
        return names.stream()
                .filter(GeoName::isTier3)
                .peek(gn -> gn.setHierarchy(findByAdmin12Code(tier2,
                        gn.getAdmin1Code(), gn.getAdmin2Code()).getHierarchy() + " - "
                        + gn.getName()))
                .collect(toList());
    }

    private static GeoName findByAdmin12Code(final List<GeoName> tier2,
            final String admin1Code, final String admin2Code) {
        return tier2.stream()
                .filter(GeoName::isAdm2)
                .filter(gm -> gm.getAdmin1Code().equals(admin1Code)
                        && gm.getAdmin2Code().equals(admin2Code))
                .findFirst()
                .get();
    }
    
    private List<GeoName> processTier4(final List<GeoName> names,
            final List<GeoName> tier3) {
        return names.stream()
                .filter(GeoName::isTier4)
                .peek(gn -> gn.setHierarchy(findByAdmin123Code(tier3,
                        gn.getAdmin1Code(), gn.getAdmin2Code(), gn.getAdmin3Code()).getHierarchy() + " - "
                        + gn.getName()))
                .collect(toList());
    }

    private static GeoName findByAdmin123Code(final List<GeoName> tier3,
            final String admin1Code, final String admin2Code,
            final String admin3Code) {
        return tier3.stream()
                .filter(GeoName::isAdm3)
                .filter(gm -> gm.getAdmin1Code().equals(admin1Code)
                        && gm.getAdmin2Code().equals(admin2Code)
                        && gm.getAdmin3Code().equals(admin3Code))
                .findFirst()
                .get();
    }
    

    private List<GeoName> read(final InputStream in) throws Exception {
        return new BufferedReader(new InputStreamReader(in, "utf8"))
                .lines()
                .map(line -> line.split("\t"))
                .map(cells -> {
                    final GeoName result = new GeoName();
                    result.setId(parseInt(cells[0].trim()));
                    result.setName(cells[1].trim());
                    result.setAlternateNames(cells[3].trim());
                    result.setFeatureCode(cells[7].trim());
                    result.setCountryCode(cells[8].trim());
                    result.setAdmin1Code(trimToNull(cells[10].trim()));
                    result.setAdmin2Code(trimToNull(cells[11]));
                    result.setAdmin3Code(trimToNull(cells[12].trim()));
                    result.setAdmin4Code(trimToNull(cells[13].trim()));
                    result.setHierarchy(result.getCountryCode());
                    return result;
                })
                .collect(toList());
    }

    @Transactional
    private void store(final GeoName gn) {
        save(gn);
    }
}