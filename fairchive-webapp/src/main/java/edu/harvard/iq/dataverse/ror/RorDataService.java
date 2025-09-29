package edu.harvard.iq.dataverse.ror;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import edu.harvard.iq.dataverse.api.dto.RorEntryDTO;
import edu.harvard.iq.dataverse.interceptors.SuperuserRequired;
import edu.harvard.iq.dataverse.persistence.ror.RorData;
import edu.harvard.iq.dataverse.util.FileUtil;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.inject.Inject;

import static javax.ejb.TransactionManagementType.BEAN;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Stateless
@TransactionManagement(BEAN) // We don't want to start txs automatically here
public class RorDataService {

    private static final Logger logger = getLogger(RorDataService.class);

    private RorConverter converter;

    private RorTransactionsService transactionsService;

    private static int DEFAULT_BATCH_SIZE_FOR_TX = 100;
    private int batchSizeForTx = DEFAULT_BATCH_SIZE_FOR_TX;

    // -------------------- CONSTRUCTORS --------------------

    public RorDataService() {
    }

    @Inject
    public RorDataService(final RorConverter rorConverter, 
            final RorTransactionsService rorTransactionsService) {
        this.converter = rorConverter;
        this.transactionsService = rorTransactionsService;
    }

    public RorDataService(RorConverter rorConverter, 
            final RorTransactionsService rorTransactionsService, 
            final int transactionBatchSize) {
        this.converter = rorConverter;
        this.transactionsService = rorTransactionsService;
        batchSizeForTx = transactionBatchSize;
    }

    // -------------------- LOGIC --------------------

    /**
     * Method dedicated for refreshing ror data, dropping old data and then adding fresh entries.
     */
    @SuperuserRequired
    public UpdateResult refreshRorData(final File file, 
            final FormDataContentDisposition header) {
        
        final File processed = selectFileToProcess(file, header);
        final UpdateResult updateResult = new UpdateResult();
        
        try (final FileReader fileReader = new FileReader(processed);
            final JsonReader jsonReader = new JsonReader(fileReader)) {
            jsonReader.beginArray();

            final Gson gson = new Gson();
            int count = 0;
            final Set<RorData> toSave = new HashSet<>();
            while (jsonReader.hasNext()) {
                count++;
                final RorEntryDTO rorEntry = gson.fromJson(jsonReader, RorEntryDTO.class);
                updateResult.update(rorEntry);

                if (count == 1) {
                    transactionsService.truncateAll();
                }

                if (count % batchSizeForTx == 0) {
                    transactionsService.saveMany(toSave);
                    updateResult.getSavedRorData().addAll(toSave);
                    toSave.clear();
                }
                toSave.add(converter.toEntity(rorEntry));
            }
            transactionsService.saveMany(toSave);
            updateResult.getSavedRorData().addAll(toSave);

            jsonReader.endArray();
        } catch (IOException ioe) {
            logger.warn("Exception while processing input file", ioe);
        } finally {
            processed.delete();
        }
        return updateResult;
    }

    // -------------------- PRIVATE --------------------

    private File selectFileToProcess(final File file, 
            final FormDataContentDisposition header) {
        if (hasExtension(header.getFileName(), ".zip")) {
            return decompressJson(file);
        } else if (hasExtension(header.getFileName(), ".json")) {
            return file;
        } else {
            throw new IllegalArgumentException("No valid file uploaded (only .json or zipped .json");
        }
    }

    private File decompressJson(final File zipped) {
        File decompressed = null;
        try (final ZipInputStream in = new ZipInputStream(new FileInputStream(zipped))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (!entry.isDirectory() && hasExtension(entry.getName(), ".json")) {
                    break;
                }
                in.closeEntry();
            }
            if (entry != null) {
                decompressed = FileUtil.inputStreamToFile(in, 8192);
                in.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            zipped.delete();
        }

        return decompressed;
    }

    private boolean hasExtension(final String fileName, final String extension) {
        return isNotBlank(fileName) && fileName.toLowerCase().endsWith(extension);
    }

    // -------------------- INNER CLASSES --------------------

    public static class UpdateResult {
        private Integer total = 0;
        private SortedMap<String, Integer> stats = new TreeMap<>();
        private List<RorData> savedRorData = new ArrayList<>();

        // -------------------- GETTERS --------------------

        public Integer getTotal() {
            return total;
        }

        public SortedMap<String, Integer> getStats() {
            return stats;
        }

        public List<RorData> getSavedRorData() {
            return savedRorData;
        }

        // -------------------- LOGIC --------------------

        public void update(RorEntryDTO entryDTO) {
            String countryName = entryDTO.getCountry().getCountryName();
            Integer countryTotal = stats.get(countryName);
            stats.put(countryName, countryTotal != null ? countryTotal + 1 : 1);
            total++;
        }
    }
}
