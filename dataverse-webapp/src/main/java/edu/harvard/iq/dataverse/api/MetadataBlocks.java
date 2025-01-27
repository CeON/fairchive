package edu.harvard.iq.dataverse.api;

import static java.util.stream.Collectors.toList;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO;
import edu.harvard.iq.dataverse.dataverse.MetadataBlockTsvCreator;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlockRepository;

/**
 * Api bean for managing metadata blocks.
 *
 * @author michael
 */
@Path("metadatablocks")
public class MetadataBlocks extends AbstractApiBean {

    @Inject
    private MetadataBlockRepository metadataBlockRepo;

    // -------------------- LOGIC --------------------

    @GET
    @Produces("application/json")
    public Response list() {
        MetadataBlockDTO.Converter converter = new MetadataBlockDTO.Converter();
        return allowCors(ok(this.metadataBlockRepo.findAll().stream()
                .map(converter::convertMinimal)
                .collect(toList())));
    }

    @GET
    @Path("{identifier}")
    @Produces("application/json")
    public Response getBlock(@PathParam("identifier") String identifier) {
        Optional<MetadataBlock> b = this.metadataBlockRepo.findByName(identifier);

        return allowCors(b.isPresent()
                ? ok(new MetadataBlockDTO.Converter().convert(b.get()))
                : notFound(String.format("Can't find metadata block '%s'", identifier)));
    }

    @Path("/tsv/{identifier}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response createBlockTsv(@PathParam("identifier") String identifier) {
        Optional<MetadataBlock> block = this.metadataBlockRepo.findByName(identifier);
        if (block.isPresent()) {
        StreamingOutput tsvStreamer = output -> new MetadataBlockTsvCreator().createTsv(block.get(), output);
        String fileName = identifier + ".tsv";

        return Response.ok(tsvStreamer, MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=" + fileName)
                .build();
        } else {
            return notFound(String.format("Can't find metadata block '%s'", identifier));
        }
    }
}
