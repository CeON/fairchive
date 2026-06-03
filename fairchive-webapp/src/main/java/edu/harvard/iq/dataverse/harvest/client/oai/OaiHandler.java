package edu.harvard.iq.dataverse.harvest.client.oai;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.dspace.xoai.model.oaipmh.Granularity;
import org.dspace.xoai.model.oaipmh.Header;
import org.dspace.xoai.model.oaipmh.MetadataFormat;
import org.dspace.xoai.serviceprovider.ServiceProvider;
import org.dspace.xoai.serviceprovider.client.HttpOAIClient;
import org.dspace.xoai.serviceprovider.exceptions.BadArgumentException;
import org.dspace.xoai.serviceprovider.exceptions.IdDoesNotExistException;
import org.dspace.xoai.serviceprovider.exceptions.InvalidOAIResponse;
import org.dspace.xoai.serviceprovider.exceptions.NoSetHierarchyException;
import org.dspace.xoai.serviceprovider.model.Context;
import org.dspace.xoai.serviceprovider.parameters.ListIdentifiersParameters;
import org.xml.sax.SAXException;

import edu.harvard.iq.dataverse.api.imports.HarvestImporterType;
import edu.harvard.iq.dataverse.harvest.client.FastGetRecord;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;

public class OaiHandler {

    private final String baseOaiUrl; 
    private String metadataPrefix; 
    private MetadataFormat metadataFormat;
    private String setName;
    private Date fromDate;
    private ServiceProvider serviceProvider;
    private HarvestingClient harvestingClient;

    public OaiHandler(final String baseOaiUrl) throws OaiHandlerException {
        if (isEmpty(baseOaiUrl)) {
            throw new OaiHandlerException("Empty OAI url");
        }
        this.baseOaiUrl = baseOaiUrl;
    }

    public OaiHandler(final HarvestingClient client) 
            throws OaiHandlerException {
        this(client.getHarvestingUrl());

        this.metadataPrefix = client.getMetadataPrefix();
        if (isEmpty(this.metadataPrefix)) {
            throw new OaiHandlerException("Empty metadata prefix");
        }

        if (!isEmpty(client.getHarvestingSet())) {
            try {
                this.setName = URLEncoder.encode(client.getHarvestingSet(), "UTF-8");
            } catch (final UnsupportedEncodingException e) {
                throw new OaiHandlerException(e);
            }
        }

        this.fromDate = client.getLastNonEmptyHarvestTime();
        this.harvestingClient = client;
    }

    public String getBaseOaiUrl() {
        return this.baseOaiUrl;
    }

    public String getMetadataPrefix() {
        return this.metadataPrefix;
    }

    public MetadataFormat getMetadataFormat() {
        return this.metadataFormat;
    }

    public HarvestingClient getHarvestingClient() {
        return this.harvestingClient;
    }

    private ServiceProvider getServiceProvider() {
        if (this.serviceProvider == null) {
            final Context context = new Context();
            context.withBaseUrl(this.baseOaiUrl);
            context.withGranularity(Granularity.Second);
            context.withOAIClient(new HttpOAIClient(this.baseOaiUrl));
            this.serviceProvider = new ServiceProvider(context);
        } 
        return this.serviceProvider;
    }

    OaiHandler withServiceProvider(final ServiceProvider provider) {
        this.serviceProvider = provider;
        return this;
    }

    /**
     * Fetches all available metadata formats from the remote and searches for the set metadata prefix.
     */
    public OaiHandler withFetchedMetadataFormat() 
            throws OaiHandlerException {
    	if (this.metadataPrefix == null) {
            throw new OaiHandlerException("Can't fetch metadata format, prefix not set.");
        }
        try {
			this.metadataFormat = listMetadataFormats().stream()
			        .filter(format -> this.metadataPrefix.equals(format.getMetadataPrefix()))
			        .findFirst()
			        .orElseThrow(() -> new OaiHandlerException(
			        		"Couldn't find metadata format with prefix: "
			        			.concat(this.metadataPrefix)));
	        return this;
		} catch (final IdDoesNotExistException e) {
			throw new OaiHandlerException(e);
		} 

    }
    
	public HarvestImporterType resolveImportType() {
		return HarvestImporterType.resolve(this.metadataFormat).
				orElseThrow(() -> new IllegalArgumentException(
						"Unsupported import metadata format: " + 
								this.metadataFormat.getMetadataPrefix()));
	}

    public List<String> listSets() throws OaiHandlerException {
        try {
            final List<String> result = new ArrayList<>();
            getServiceProvider().listSets().forEachRemaining(set -> {
                if (isNotEmpty(set.getSpec())) {
                    result.add(set.getSpec());
                }
            });
            return result;
        } catch (final NoSetHierarchyException e) {
            return emptyList();
        } catch (final InvalidOAIResponse e) {
            throw new OaiHandlerException(e);
        }
    }

    public List<MetadataFormat> listMetadataFormats()
            throws OaiHandlerException, IdDoesNotExistException {
        try {
            final List<MetadataFormat> result = new ArrayList<>();
            getServiceProvider().listMetadataFormats().forEachRemaining(format -> {
                if (isNotEmpty(format.getMetadataPrefix())) {
                    result.add(format);
                }
            });
            return result;
        } catch (final InvalidOAIResponse e) {
            throw new OaiHandlerException(e);
        }
    }

    public Iterator<Header> listIdentifiers() throws OaiHandlerException {
        try {
            return getServiceProvider().listIdentifiers(buildListIdentifiersParams());
        } catch (final BadArgumentException | InvalidOAIResponse e) {
            throw new OaiHandlerException(e);
        }

    }

	public FastGetRecord getRecord(final String identifier) throws OaiHandlerException {
		if (isEmpty(this.metadataPrefix)) {
			throw new OaiHandlerException("Empty metadata prefix");
		}
		try {
			return new FastGetRecord(this.baseOaiUrl, identifier, this.metadataPrefix);
		} catch (final ParserConfigurationException | SAXException | 
				TransformerException | IOException e) {
			throw new OaiHandlerException(e);
		}
	}

    private ListIdentifiersParameters buildListIdentifiersParams() 
            throws OaiHandlerException {

        if (isEmpty(this.metadataPrefix)) {
            throw new OaiHandlerException("Empty metadata prefix");
        }
        
        final ListIdentifiersParameters result = ListIdentifiersParameters.request();
        result.withMetadataPrefix(this.metadataPrefix);
        if (this.fromDate != null) {
            result.withFrom(this.fromDate);
        }
        if (isNotEmpty(this.setName)) {
            result.withSetSpec(this.setName);
        }
        return result;
    }
}
