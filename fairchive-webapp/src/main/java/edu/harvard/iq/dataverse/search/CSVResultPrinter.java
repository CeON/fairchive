package edu.harvard.iq.dataverse.search;

import static java.util.Collections.emptyList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.ByteOrderMark;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlockRepository;
import edu.harvard.iq.dataverse.search.response.SolrSearchResult;

public final class CSVResultPrinter {

    private final DatasetRepository datasetRepo;
    private final List<DatasetFieldType> exportedFields;

    private final static CSVFormat format = CSVFormat.DEFAULT.builder().build();

    public CSVResultPrinter(final DatasetRepository datasetRepo,
            final MetadataBlockRepository metadataBlockRepo) {
        this.datasetRepo = datasetRepo;
        this.exportedFields = findFieldsToExportIn(metadataBlockRepo);
    }

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

    private void printHeaders(final CSVPrinter printer) throws IOException {
        printer.print("Id");
        printer.print("Name");
        printer.print("Title");
        for (final DatasetFieldType type : this.exportedFields) {
            if (type.getParentDatasetFieldType() != null) {
                printer.print(type.getMetadataBlock().getName() + "->"
                        + type.getParentDatasetFieldType().getTitle() + "->"
                        + type.getTitle());
            } else {
                printer.print(type.getMetadataBlock().getName()
                        + "->" + type.getTitle());
            }
        }
    }

    private void printMetadata(final CSVPrinter printer, final SolrSearchResult result)
            throws IOException {
        final List<DatasetField> fields = getAllFieldsOfLatestVersionOf(
                result.getEntityId());

        for (final DatasetFieldType type : this.exportedFields) {
            printer.print(getFieldValueOfType(fields, type));
        }
    }

    private List<DatasetField> getAllFieldsOfLatestVersionOf(final Long datasetId) {
        return this.datasetRepo.findById(datasetId)
                .map(Dataset::getLatestVersion)
                .map(DatasetVersion::getDatasetFieldsAll)
                .orElse(emptyList());
    }

    private static String getFieldValueOfType(final List<DatasetField> fields,
            final DatasetFieldType type) {
        for(final DatasetField field : fields) {
            for(final DatasetField childField : field.getChildren()){
                if(childField.isOfType(type)) {
                    return String.join("; ", childField.getValuesWithoutFormatting());
                }
            }
            if(field.isOfType(type)) {
                return String.join("; ", field.getValuesWithoutFormatting());
            }
        }
        return null;
    }

    private CSVPrinter newPrinter(final OutputStream output) throws IOException {
        output.write(ByteOrderMark.UTF_8.getBytes());
        return new CSVPrinter(new OutputStreamWriter(output, "utf-8"), format);
    }

    private static List<DatasetFieldType> findFieldsToExportIn(
            final MetadataBlockRepository metadataBlockRepo) {
        final ArrayList<DatasetFieldType> result = new ArrayList<>();

        for (final MetadataBlock block : metadataBlockRepo
                .findSystemMetadataBlocks()) {
            for (final DatasetFieldType type : block.getDatasetFieldTypes()) {
                if (type.isCompound()) {
                    for (final DatasetFieldType childType : type
                            .getChildDatasetFieldTypes()) {
                        if (childType.isExportToFile()) {
                            result.add(childType);
                        }
                    }
                } else if (type.isExportToFile()) {
                    result.add(type);
                }
            }
        }
        return result;
    }
}
