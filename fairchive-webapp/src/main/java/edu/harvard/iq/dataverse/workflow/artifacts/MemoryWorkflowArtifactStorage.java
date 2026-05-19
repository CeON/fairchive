package edu.harvard.iq.dataverse.workflow.artifacts;

import static java.util.Optional.ofNullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import javax.enterprise.inject.Vetoed;

import org.apache.commons.io.IOUtils;

/**
 * Simple in-memory implementation of {@link WorkflowArtifactStorage}.
 * Meant mainly for usage in tests.
 */
@Vetoed
public class MemoryWorkflowArtifactStorage implements WorkflowArtifactStorage {

    private static final Map<String, byte[]> storage = new HashMap<>();

    @Override
    public Optional<Supplier<InputStream>> read(String location) {
        return ofNullable(storage.get(location))
                .map(bytes -> () -> new ByteArrayInputStream(bytes));
    }

    @Override
    public String write(Supplier<InputStream> data) throws IOException {
        String location = UUID.randomUUID().toString();
        try (InputStream in = data.get()) {
            storage.put(location, IOUtils.toByteArray(in));
        }
        return location;
    }

    @Override
    public void delete(String location) {
        storage.remove(location);
    }
}
