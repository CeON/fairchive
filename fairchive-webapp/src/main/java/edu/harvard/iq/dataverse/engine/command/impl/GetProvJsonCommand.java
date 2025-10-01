package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import javax.json.Json;
import javax.json.JsonObject;

import java.io.IOException;
import java.io.InputStream;

@SuppressWarnings("serial")
@RequiredPermissions(Permission.EditDataset)
public class GetProvJsonCommand extends AbstractCommand<JsonObject> {

    private final DataFile dataFile;

    public GetProvJsonCommand(DataverseRequest aRequest, DataFile dataFile) {
        super(aRequest, dataFile);
        this.dataFile = dataFile;
    }

    @Override
    public JsonObject execute(final CommandContext ctxt) {
        try {
            final StorageIO<DataFile> storageIO = ctxt.dataAccess()
                    .getStorageIO(dataFile);
            try (final InputStream inputStream = storageIO
                    .getAuxFileAsInputStream("prov-json.json")) {
                return inputStream != null
                        ? Json.createReader(inputStream).readObject()
                        : null;
            }
        } catch (final IOException ex) {
            String error = "Exception caught in DataAccess.getStorageIO(dataFile) getting file. Error: "
                    + ex;
            throw new IllegalCommandException(error, this);
        }
    }
}
