package edu.harvard.iq.dataverse.search;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldTypeRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.search.response.SolrSearchResult;

public final class CSVResultPrinter {

    private final DatasetRepository datasetRepo;
    private final List<DatasetFieldType> exportedFields;

    private final static CSVFormat format = CSVFormat.DEFAULT.builder().build();

    // -------------------------------------------------------------------------
    public CSVResultPrinter(final DatasetRepository datasetRepo,
            final DatasetFieldTypeRepository datasetFieldTypeRepo) {

        this.datasetRepo = datasetRepo;
        this.exportedFields = datasetFieldTypeRepo.findAll().stream()
                .filter(DatasetFieldType::isExportToFile)
                .sorted(comparing(DatasetFieldType::getTitle)).collect(toList());
    }

    // -------------------------------------------------------------------------
    public StreamedContent print(final List<SolrSearchResult> results) {

        try {
            final ByteArrayOutputStream content = new ByteArrayOutputStream(4000);

            try (final CSVPrinter printer = newPrinter(content)) {
                printHeaders(printer);
                printer.println();

                for (final SolrSearchResult result : results) {
                    printer.print(result.getId());
                    printer.print(result.getName());
                    printer.print(result.getTitle());
                    if (result.isDataset()) {
                        printMetadata(printer, result);
                    }
                    printer.println();
                }
            }

            return DefaultStreamedContent.builder().name("searchResults.csv")
                    .contentLength(content.size()).contentEncoding("utf-8")
                    .contentType("text/csv")
                    .stream(() -> new ByteArrayInputStream(content.toByteArray()))
                    .build();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------------------------
    private void printHeaders(final CSVPrinter printer) throws IOException {

        printer.print("Id");
        printer.print("Name");
        printer.print("Title");
        for (final DatasetFieldType type : this.exportedFields) {
            printer.print(type.getTitle());
        }
    }

    // -------------------------------------------------------------------------
    private void printMetadata(final CSVPrinter printer, final SolrSearchResult result)
            throws IOException {

        final List<DatasetField> fields = getAllFieldsOfLatestVersionOf(
                result.getEntityId());

        for (final DatasetFieldType type : this.exportedFields) {
            printer.print(get(fields, type));
        }
    }

    // -------------------------------------------------------------------------
    private List<DatasetField> getAllFieldsOfLatestVersionOf(final Long datasetId) {

        return this.datasetRepo.getById(datasetId).getLatestVersion()
                .getDatasetFieldsAll();
    }

    // -------------------------------------------------------------------------
    private static String get(final List<DatasetField> fields,
            final DatasetFieldType type) {

        return fields.stream().filter(f -> f.isOfType(type)).findAny()
                .map(DatasetField::getDisplayValue).orElse(null);
    }

    // -------------------------------------------------------------------------
    private CSVPrinter newPrinter(final OutputStream output) throws IOException {

        return new CSVPrinter(new OutputStreamWriter(output, "utf-8"), format);
    }
}
