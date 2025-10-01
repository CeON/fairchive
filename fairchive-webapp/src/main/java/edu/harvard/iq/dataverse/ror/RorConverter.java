package edu.harvard.iq.dataverse.ror;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.api.dto.RorEntryDTO;
import edu.harvard.iq.dataverse.persistence.ror.RorData;
import edu.harvard.iq.dataverse.persistence.ror.RorLabel;
import edu.harvard.iq.dataverse.search.ror.RorDto;

/**
 * Simple converter for Ror objects.
 */
@Stateless
public class RorConverter {

    private static final String ROR_URL_PREFIX = "https://ror.org/";

    // -------------------- LOGIC --------------------

    public RorData toEntity(final RorEntryDTO entry) {
        final RorData converted = new RorData();

        converted.setRorId(extractRor(entry.getId()));
        converted.setName(entry.getName());

        if (entry.getCities().length > 0) {
            converted.setCity(entry.getCities()[0].getCity());
        }

        if (entry.getLinks().length > 0) {
            converted.setWebsite(entry.getLinks()[0]);
        }

        if (entry.getCountry() != null) {
            converted.setCountryName(entry.getCountry().getCountryName());
            converted.setCountryCode(entry.getCountry().getCountryCode());
        }

        converted.getAcronyms().addAll(asList(entry.getAcronyms()));
        converted.getNameAliases().addAll(asList(entry.getAliases()));
        converted.getLabels().addAll(
                stream(entry.getLabels())
                      .map(RorConverter::rorLabelFrom)
                      .collect(toSet()));

        return converted;
    }
    
    private static RorLabel rorLabelFrom(RorEntryDTO.Label l) {
        if(l.getIso639() == null) {
            l.setIso639("en");
        }
        return new RorLabel(l.getLabel(), l.getIso639());
    }

    public RorDto toSolrDto(RorData entry) {
        final RorDto converted = new RorDto();

        converted.setRorId(entry.getRorId());
        converted.setRorUrl(ROR_URL_PREFIX.concat(entry.getRorId()));
        converted.setName(entry.getName());

        converted.setCity(entry.getCity());
        converted.setWebsite(entry.getWebsite());

        converted.setCountryName(entry.getCountryName());
        converted.setCountryCode(entry.getCountryCode());


        converted.getAcronyms().addAll(entry.getAcronyms());
        converted.getNameAliases().addAll(entry.getNameAliases());
        converted.getLabels().addAll(
                entry.getLabels()
                     .stream()
                      .map(RorLabel::getLabel)
                      .collect(toSet()));

        return converted;
    }

    // -------------------- PRIVATE --------------------

    private String extractRor(final String rorId) {
        if (isBlank(rorId) || !rorId.contains("/0")) {
            return EMPTY;
        } else {
            return rorId.substring(rorId.lastIndexOf("/") + 1);
        }
    }
}
