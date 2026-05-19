package edu.harvard.iq.dataverse.persistence.workflow;

import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.util.function.Supplier;


public class WorkflowArtifactSource {

    private final String name;
    private final String encoding;
    private final Supplier<InputStream> dataSupplier;

    public WorkflowArtifactSource(String name, String encoding, Supplier<InputStream> dataSupplier) {
        this.name = requireNonNull(name);
        this.encoding = requireNonNull(encoding);
        this.dataSupplier = requireNonNull(dataSupplier);
    }

    public String getName() {
        return name;
    }

    public String getEncoding() {
        return encoding;
    }

    public Supplier<InputStream> getDataSupplier() {
        return dataSupplier;
    }
}
