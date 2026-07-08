package edu.harvard.iq.dataverse.api.imports;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;

import org.dspace.xoai.model.oaipmh.MetadataFormat;

/**
 * Import types supported for harvesting.
 */
public enum HarvestImporterType {
	DDI {
		protected boolean matches(final MetadataFormat format) {
			// FIXME: should match on namespace instead of prefix
			final String prefix = format.getMetadataPrefix();
			return "ddi".equalsIgnoreCase(prefix) 
					|| prefix.toLowerCase().startsWith("oai_ddi");
		}
	},
	DUBLIN_CORE {
		protected boolean matches(final MetadataFormat format) {
			// FIXME: should match on namespace instead of prefix
			final String prefix = format.getMetadataPrefix();
			return "dc".equalsIgnoreCase(prefix) || "oai_dc".equals(prefix);
		}
	},
	DATAVERSE_JSON {
		protected boolean matches(final MetadataFormat format) {
			return "dataverse_json".equals(format.getMetadataPrefix());
		}
	};

	protected abstract boolean matches(final MetadataFormat metadataFormat);
	
    public static  Optional<HarvestImporterType> resolve(final MetadataFormat format) {
    	return stream(values()).filter(type -> type.matches(format)).findFirst();
    }
    
    public static List<MetadataFormat> filterSupported(final List<MetadataFormat> formats) {
        return formats.stream()
                .filter(format -> stream(values()).anyMatch(type -> type.matches(format)))
                .collect(toList());
    }
}
