package edu.harvard.iq.dataverse.citation;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundleWithLocale;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.city;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.country;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.dateOfCollection;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.dateOfCollectionEnd;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.dateOfCollectionStart;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.distributorName;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.geographicCoverage;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.grantNumber;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.grantNumberAgency;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.kindOfData;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.language;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.otherGeographicCoverage;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.otherIdValue;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.producer;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.producerAffiliation;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.producerName;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.productionDate;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.productionPlace;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.series;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.seriesName;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.state;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ejb.Stateless;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.harvard.iq.dataverse.common.DateUtil;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestStyle;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import io.vavr.Tuple;
import io.vavr.Tuple2;

@Stateless
public class CitationDataExtractor {

    private static final Logger logger = LoggerFactory.getLogger(CitationDataExtractor.class);

    // -------------------- LOGIC --------------------

    public CitationData create(final DatasetVersion version, final Locale locale) {
        final CitationData result = new CitationData();
        extractAndWriteCommonValues(version, result, locale);
        result.setDirect(false)
                .setPersistentId(extractPID(version, version.getDataset(), false)) // Global Id: always part of citation for local datasets & some harvested
                .setPidOfDataset(extractDatasetPID(version));
        return result;
    }

    public CitationData create(final FileMetadata metadata, final boolean direct, 
    		final Locale locale) {
        final CitationData result = new CitationData();
        final DatasetVersion version = metadata.getDatasetVersion();
        extractAndWriteCommonValues(version, result, locale);
        final DataFile dataFile = metadata.getDataFile();
        result.setDirect(direct)
                .setFileTitle(metadata.getLabel())
                .setPersistentId(extractPID(version, dataFile, direct)) // Global Id of datafile (if published & isDirect==true) or dataset as appropriate
                .setPidOfDataset(extractDatasetPID(version))
                .setPidOfFile(extractFilePID(version, dataFile, direct));
        return result;
    }

    // -------------------- PRIVATE --------------------

    private void extractAndWriteCommonValues(final DatasetVersion version, 
    		final CitationData citation, final Locale locale) {
        final Date citationDate = extractCitationDate(version);

        citation.getAuthors().addAll(extractAuthors(version));
        citation.setYear(new SimpleDateFormat("yyyy").format(citationDate))
                .setTitle(version.getParsedTitle());

        if (!version.getDataset().isHarvested()) {
            citation.getProducers().addAll(extractProducers(version));
            citation.getDistributors().addAll(getDatasetFieldValuesByTypeName(version, distributorName));
            citation.getFunders().addAll(getUniqueGrantAgencyValues(version));
            citation.getKindsOfData().addAll(version.extractFieldValues(kindOfData));
            citation.getDatesOfCollection().addAll(getDatesOfCollection(version));
            citation.getLanguages().addAll(version.extractFieldValues(language));
            citation.getSpatialCoverages().addAll(extractSpatialCoverages(version));
            citation.getKeywords().addAll(version.getKeywords());
            citation.getOtherIds().addAll(getDatasetFieldValuesByTypeName(version, otherIdValue));

            citation.setDate(citationDate)
                    .setProductionPlace(extractField(version, productionPlace))
                    .setProductionDate(extractProductionDate(version))
                    .setReleaseYear(extractReleaseYear(version))
                    .setRootDataverseName(version.getRootDataverseNameForCitation())
                    .setSeriesTitle(getSeriesTitle(version))
                    .setPublisher(extractPublisher(version))
                    .setVersion(extractVersion(version,locale));
        }
    }

    private List<String> getDatasetFieldValuesByTypeName(final DatasetVersion version, 
    		final String datasetFieldTypeName) {
        return version.streamDatasetFieldsByTypeName(datasetFieldTypeName)
                .map(DatasetField::getValue)
                .collect(toList());
    }

    private String extractField(final DatasetVersion version, final String typeName) {
        return version.getDatasetFieldByTypeName(typeName)
                .map(DatasetField::getValue)
                .orElse(null);
    }

    private List<String> getUniqueGrantAgencyValues(final DatasetVersion version) {
        // Since only grant agency names are returned, use distinct() to avoid repeats
        // (e.g. if there are two grants from the same agency)
        return version.getCompoundChildFieldValues(grantNumber,
                singletonList(grantNumberAgency)).stream()
                .distinct()
                .collect(toList());
    }

    private List<String> getDatesOfCollection(final DatasetVersion version) {
        return version.extractFieldWithSubfields(dateOfCollection,
                asList(dateOfCollectionStart, dateOfCollectionEnd))
                .stream()
                .map(e -> Tuple.of(e.get(dateOfCollectionStart), e.get(dateOfCollectionEnd)))
                .filter(t -> t._1 != null && !t._1.isEmptyForDisplay() && t._2 != null && !t._2.isEmptyForDisplay())
                .map(t -> t._1.getValue() + "/" + t._2.getValue())
                .collect(toList());
    }

    public List<String> extractSpatialCoverages(final DatasetVersion version) {
        final List<String> subfields = asList(country, state, city, otherGeographicCoverage);
        return version.extractFieldWithSubfields(geographicCoverage, subfields).stream()
                .map(s -> subfields.stream()
                        .map(s::get)
                        .filter(v -> v != null && !v.isEmptyForDisplay())
                        .map(DatasetField::getValue)
                        .collect(joining(",")))
                .filter(StringUtils::isNotEmpty)
                .collect(toList());
    }

    private String extractProductionDate(final DatasetVersion version) {
        return version.getDatasetFieldByTypeName(productionDate)
                .map(DatasetField::getValue)
                .map(date -> {
                    final Matcher yearMatcher = Pattern.compile("\\d{4}").matcher(date);
                    return yearMatcher.find() ? yearMatcher.group(): EMPTY;
                })
                .orElse(EMPTY);
    }

    private List<CitationData.Producer> extractProducers(final DatasetVersion version) {
        return getProducers(version).stream()
                .map(p -> new CitationData.Producer(p._1, p._2))
                .collect(toList());
    }

    private List<Tuple2<String, String>> getProducers(final DatasetVersion version) {
        return version.extractFieldWithSubfields(producer, asList(producerName, producerAffiliation))
                .stream()
                .filter(e -> {
                    final DatasetField name = e.get(producerName);
                    return name != null && !name.isEmptyForDisplay();
                })
                .map(e -> Tuple.of(e.get(producerName), e.get(producerAffiliation)))
                .map(t -> Tuple.of(t._1.getValue(), defaultString(t._2.getValue())))
                .collect(toList());
    }

    private String extractReleaseYear(final DatasetVersion version) {
        return version.getReleaseTime() != null
                ? new SimpleDateFormat("yyyy").format(version.getReleaseTime())
                : EMPTY;
    }

    private List<String> extractAuthors(final DatasetVersion version) {
        return version.getDatasetAuthors().stream()
                .filter(a -> !a.isEmpty())
                .map(a -> a.getName().getDisplayValue().trim())
                .collect(toList());
    }

    private GlobalId extractPID(final DatasetVersion version, 
    		final DvObject dvObject, final boolean direct) {
        if (shouldCreateGlobalId(version)) {
            if (!direct && isNotEmpty(version.getDataset().getIdentifier())) {
                return new GlobalId(version.getDataset());
            } else if (direct && isNotEmpty(dvObject.getIdentifier())) {
                return new GlobalId(dvObject);
            }
        }
        return null;
    }

    private GlobalId extractDatasetPID(final DatasetVersion version) {
        return shouldCreateGlobalId(version) && isNotEmpty(version.getDataset().getIdentifier())
                ? new GlobalId(version.getDataset()) : null;
    }

    private GlobalId extractFilePID(final DatasetVersion version, 
    		final DataFile datafile, final boolean direct) {
        return shouldCreateGlobalId(version) && !direct && isNotEmpty(datafile.getIdentifier())
                ? new GlobalId(datafile) : null;
    }

    private boolean shouldCreateGlobalId(final DatasetVersion version) {
        final HarvestStyle harvestStyle = Optional.ofNullable(version.getDataset().getHarvestedFrom())
                .map(HarvestingClient::getHarvestStyle)
                .orElse(null);

        return !version.getDataset().isHarvested()
                || HarvestStyle.VDC.equals(harvestStyle)
                || HarvestStyle.ICPSR.equals(harvestStyle)
                || HarvestStyle.DATAVERSE.equals(harvestStyle)
                || HarvestStyle.DOI.equals(harvestStyle);
    }

    private Date extractCitationDate(final DatasetVersion version) {
        Date citationDate = null;
        if (!version.getDataset().isHarvested()) {
            citationDate = getCitationDate(version);
            if (citationDate == null) {
                citationDate = version.getDataset().getPublicationDate() != null
                        ? version.getDataset().getPublicationDate()
                        : version.getLastUpdateTime(); // for drafts
            }
        } else {
            try {
                citationDate = DateUtil.parseDateTimeFormatAsDate(version.getProductionDate());
                if (citationDate == null) {
                    citationDate = version.getDataset().getPublicationDate();
                }
            } catch (DateTimeParseException pe) {
                logger.warn(String.format("Error parsing date [%s]", version.getProductionDate()), pe);
            }
        }
        if (citationDate == null) {
            logger.warn("Unable to find citation date for datasetversion: {}", version.getId());
            citationDate = new Date(); // As a last resort, pick the current date
        }
        return citationDate;
    }

    private Date getCitationDate(final DatasetVersion version) {
        final DatasetFieldType citationDateType = version.getDataset().getCitationDateDatasetFieldType();
        final DatasetField citationDate = citationDateType != null
                ? version.getDatasetFieldByTypeName(citationDateType.getName()).orElse(null) : null;
        if (citationDate != null && FieldType.DATE.equals(citationDate.getDatasetFieldType().getFieldType())) {
            try {
                return new SimpleDateFormat("yyyy").parse(citationDate.getValue());
            } catch (final ParseException ex) {
                logger.warn("Date parsing exception: ", ex);
            }
        }
        return null;
    }

    private String getSeriesTitle(final DatasetVersion version) {
     return version.getCompoundChildFieldValues(series, singletonList(seriesName))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private String extractPublisher(final DatasetVersion version) {
        return version.getRootDataverseNameForCitation();
    }

    private String extractVersion(final DatasetVersion version, final Locale locale) {
        if (version.isDraft()) {
            return  getStringFromBundleWithLocale("draftversion", locale);
        } else if (version.getVersionNumber() != null) {
            return "V" + version.getVersionNumber()
                    + (version.isDeaccessioned()
                    ? ", " + getStringFromBundleWithLocale("deaccessionedversion", locale)
                    : EMPTY);
        } else {
        	return EMPTY;
        }
    }
}
