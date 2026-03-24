package edu.harvard.iq.dataverse.workflow.artifacts;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Stores and retrieves binary data.
 */
public interface WorkflowArtifactStorage {

    /**
     * Reads specified location for stored data.
     * @param location location within storage to read from.
     * @return {@link Optional} containing {@link InputStream} of stored data, or empty one if location was not found.
     */
    Optional<Supplier<InputStream>> read(String location);

    /**
     * Writes given data into storage.
     * @param data stored data to write.
     * @return location of stored data.
     */
    String write(Supplier<InputStream> data) throws IOException;

    /**
     * Deletes data under specified location.
     * @param location location to delete data from.
     */
    void delete(String location) throws IOException;
}
