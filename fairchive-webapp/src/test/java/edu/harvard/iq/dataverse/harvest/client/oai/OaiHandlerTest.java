package edu.harvard.iq.dataverse.harvest.client.oai;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import java.util.List;

import org.dspace.xoai.model.oaipmh.MetadataFormat;
import org.dspace.xoai.model.oaipmh.Set;
import org.dspace.xoai.serviceprovider.ServiceProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class OaiHandlerTest {

    @Mock
    private ServiceProvider serviceProvider;

    private final List<MetadataFormat> formats = asList(
            new MetadataFormat()
                    .withMetadataPrefix("oai_dc")
                    .withMetadataNamespace("http://www.openarchives.org/OAI/2.0/oai_dc/")
                    .withSchema("http://www.openarchives.org/OAI/2.0/oai_dc.xsd"),
            new MetadataFormat()
                    .withMetadataPrefix("oai_datacite")
                    .withMetadataNamespace("http://datacite.org/schema/kernel-3")
                    .withSchema("http://schema.datacite.org/meta/kernel-3.1/metadata.xsd"));
    
    private final List<Set> sets = asList(
    		new Set()
    			.withSpec("spec1"), 
    		new Set()
    			.withSpec("spec2"));
    
    
    @BeforeEach
    void setUp() throws Exception {
    	when(this.serviceProvider.listMetadataFormats()).thenReturn(this.formats.iterator());
    	when(this.serviceProvider.listSets()).thenReturn(this.sets.iterator());
    }
    
    @Test
    void constructor_throwsExteptions_forIncompleteArguments() throws Exception {
    	
    	assertThatThrownBy(() -> new OaiHandler((String)null))
    		.isInstanceOf(OaiHandlerException.class);
    	assertThatThrownBy(() -> new OaiHandler(""))
    		.isInstanceOf(OaiHandlerException.class);
    	
    	assertThatThrownBy(() -> new OaiHandler((HarvestingClient)null))
    		.isInstanceOf(NullPointerException.class);
    	
    	HarvestingClient client = new HarvestingClient();
    	
    	assertThatThrownBy(() -> new OaiHandler(client))
    		.isInstanceOf(OaiHandlerException.class);
    	
    	client.setHarvestingUrl("");
    	
    	assertThatThrownBy(() -> new OaiHandler(client))
			.isInstanceOf(OaiHandlerException.class);
    	
    	client.setHarvestingUrl("http://oai-server.org/oai");
    	
    	assertThatThrownBy(() -> new OaiHandler(client))
			.isInstanceOf(OaiHandlerException.class);
    	
        client.setMetadataPrefix("");
        
    	assertThatThrownBy(() -> new OaiHandler(client))
			.isInstanceOf(OaiHandlerException.class);
    	
    	client.setMetadataPrefix("oai_dc");
    	 
     	assertThat(new OaiHandler(client).getBaseOaiUrl())
     		.isEqualTo(client.getHarvestingUrl()); // does not throw
     	
     	client.setHarvestingSet("default");
     	
     	assertThat(new OaiHandler(client).getMetadataPrefix())
 		.isEqualTo(client.getMetadataPrefix()); // does not throw
    }
    
    @Test
    void withFetchedMetadataFormat() throws Exception {

        HarvestingClient client = new HarvestingClient();
        client.setMetadataPrefix("oai_dc");
        client.setHarvestingSet("default");
        client.setHarvestingUrl("http://oai-server.org/oai");
        client.setId(1L);
        client.setName("OAI DC client");
        OaiHandler handler = new OaiHandler(client)
        		.withServiceProvider(this.serviceProvider)
        		.withFetchedMetadataFormat();

        assertThat(handler.getMetadataFormat()).isEqualTo(this.formats.get(0));
    }

    @Test
    void withFetchedMetadataFormat__no_matching_format_found() throws Exception {
    	
        HarvestingClient client = new HarvestingClient();
        client.setMetadataPrefix("oai_ddi");
        client.setHarvestingSet("default");
        client.setHarvestingUrl("http://oai-server.org/oai");
        client.setId(1L);
        client.setName("OAI DDI client");
        OaiHandler handler = new OaiHandler(client)
        		.withServiceProvider(this.serviceProvider);

        assertThatThrownBy(handler::withFetchedMetadataFormat).isInstanceOf(OaiHandlerException.class);
    }
    
    @Test
    void listSets() throws Exception {
    	
    	HarvestingClient client = new HarvestingClient();
        client.setMetadataPrefix("oai_dc");
        client.setHarvestingSet("default");
        client.setHarvestingUrl("http://oai-server.org/oai");
        client.setId(1L);
        client.setName("OAI DC client");
        OaiHandler handler = new OaiHandler(client)
        		.withServiceProvider(this.serviceProvider);
        
        assertThat(handler.listSets()).containsExactly("spec1", "spec2");
    }
}
