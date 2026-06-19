package edu.harvard.iq.dataverse.persistence;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getLogger;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Pattern;
/**
 * @author skraffmiller
 */
@SuppressWarnings("serial")
public class GlobalId implements Serializable {

    public static final String DOI_PROTOCOL = "doi";
    public static final String HDL_PROTOCOL = "hdl";
    public static final String URL_PROTOCOL = "url";
    public static final String HDL_RESOLVER_URL = "https://hdl.handle.net/";
    public static final String DOI_RESOLVER_URL = "https://doi.org/";
    public static final String URL_RESOLVER_URL = "https://";
    private static final String PID_ALLOWED_CHARACTERS_PATTERN = "^[A-Za-z0-9._/:\\\\-]*";
    
    private static final Logger logger = getLogger(GlobalId.class.getName());
    
    private String protocol;
    private String authority;
    private String identifier;

    public static Optional<GlobalId> parse(final String identifierString) {
        try {
            return Optional.of(new GlobalId(identifierString));
        } catch (final IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    /**
     * @param identifier The string to be parsed
     * @throws IllegalArgumentException if the passed string cannot be parsed.
     * @Thorws NullPointerException i identifier is null
     */
    public GlobalId(final String identifier) {
        // set the protocol, authority, and identifier via parsePersistentId
        if (!parsePersistentId(identifier)) {
            throw new IllegalArgumentException("Failed to parse identifier: ".concat(identifier));
        }
    }

    public GlobalId(final String protocol, final String authority, final String identifier) {
        this.protocol = protocol;
        this.authority = authority;
        this.identifier = identifier;
    }

    public GlobalId(final DvObject dvObject) {
        this.authority = dvObject.getAuthority();
        this.protocol = dvObject.getProtocol();
        this.identifier = dvObject.getIdentifier();
    }

    /**
     * Tests whether {@code this} instance has all the data required for a
     * global id.
     *
     * @return {@code true} iff all the fields are non-empty; {@code false} otherwise.
     */
    public boolean isComplete() {
        return isNotBlank(this.protocol) &&
                isNotBlank(this.authority) &&
                isNotBlank(this.identifier);
    }

    public String getProtocol() {
        return this.protocol;
    }

    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    public String getAuthority() {
        return this.authority;
    }

    public void setAuthority(final String authority) {
        this.authority = authority;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(final String identifier) {
        this.identifier = identifier;
    }

    public String toString() {
        return asString();
    }

    /**
     * Returns {@code this}' string representation. Differs from {@link #toString}
     * which can also contain debug data, if needed.
     *
     * @return The string representation of this global id.
     */
    public String asString() {
        if(this.protocol == null || this.authority == null || this.identifier == null) {
            return "";
        } else if(URL_PROTOCOL.equals(this.protocol)) {
        	return this.protocol + ':' + this.identifier;
        } else {
            return this.protocol + ':' + this.authority + '/' + this.identifier;
        }
    }

    public URL toURL() {
		if (this.identifier != null) {
			try {
				if (DOI_PROTOCOL.equals(this.protocol)) {
					return new URL(DOI_RESOLVER_URL + this.authority + '/' + this.identifier);
				} else if (HDL_PROTOCOL.equals(this.protocol)) {
					return new URL(HDL_RESOLVER_URL + this.authority + '/' + this.identifier);
				} else if(URL_PROTOCOL.equals(this.protocol)) {
					return new URL(this.identifier);
				}
			} catch (final MalformedURLException ex) {
				logger.log(SEVERE, null, ex);
			}
		}
		return null;
    }


    /**
     * Parse a Persistent Id and set the protocol, authority, and identifier
     * <p>
     * Example 1: doi:10.5072/FK2/BYM3IW
     * protocol: doi
     * authority: 10.5072
     * identifier: FK2/BYM3IW
     * <p>
     * Example 2: hdl:1902.1/111012
     * protocol: hdl
     * authority: 1902.1
     * identifier: 111012
     *
     * @param identifierString
     * @param separator        the string that separates the authority from the identifier.
     * @param destination      the global id that will contain the parsed data.
     * @return {@code destination}, after its fields have been updated, or
     * {@code null} if parsing failed.
     */
    private boolean parsePersistentId(final String identifierString) {

        final int index1 = identifierString.indexOf(':');
        if (index1 > 0) { // ':' found with one or more characters before it
        	if(identifierString.startsWith(URL_PROTOCOL)) {
        		this.protocol = URL_PROTOCOL;
        		this.authority = "";
        		this.identifier = identifierString.substring(index1 +1);
        		return !testforNullTerminator(this.identifier);
        	} else {
	            final int index2 = identifierString.indexOf('/', index1 + 1);
	            if (index2 > 0 && (index2 + 1) < identifierString.length()) { // '/' found with one or more characters
	                // between ':'
	                this.protocol = identifierString.substring(0, index1); // and '/' and there are characters after '/'
	                if (!DOI_PROTOCOL.equals(this.protocol) 
	                		&& !HDL_PROTOCOL.equals(this.protocol)) {
	                    return false;
	                }
	                //Strip any whitespace, ; and ' from authority (should finding them cause a failure instead?)
	                this.authority = formatIdentifierString(identifierString.substring(index1 + 1, index2));
	                if (testforNullTerminator(this.authority)) {
	                    return false;
	                }
	                if (this.protocol.equals(DOI_PROTOCOL) && !this.checkDOIAuthority(this.authority)) {
	                    return false;
	                }
	                // Passed all checks
	                //Strip any whitespace, ; and ' from identifier (should finding them cause a failure instead?)
	                this.identifier = formatIdentifierString(identifierString.substring(index2 + 1));
	                return !testforNullTerminator(this.identifier);
	            } else {
	                logger.log(INFO, "Error parsing identifier: {0}: '':<authority>/<identifier>'' not found in string", 
	                		identifierString);
	                return false;
	            }
        	}
        } else {
            logger.log(INFO, "Error parsing identifier: {0}: ''<protocol>:'' not found in string", 
            		identifierString);
            return false;
        }
    }

    private static String formatIdentifierString(final String str) {
    	return str.replaceAll("\\s+|'|;", "");

        /*
        < 	(%3C)
> 	(%3E)
{ 	(%7B)
} 	(%7D)
^ 	(%5E)
[ 	(%5B)
] 	(%5D)
` 	(%60)
| 	(%7C)
\ 	(%5C)
+
        */
        // http://www.doi.org/doi_handbook/2_Numbering.html
    }

    private static boolean testforNullTerminator(final String str) {
    	return str != null ? str.indexOf('\u0000') > 0 : false;
    }

	private boolean checkDOIAuthority(final String doiAuthority) {
		return doiAuthority != null ? doiAuthority.startsWith("10.") : false;

	}

    /**
     * Verifies that the pid only contains allowed characters.
     *
     * @param pidParam
     * @return true if pid only contains allowed characters false if pid
     * contains characters not specified in the allowed characters regex.
     */
    public static boolean verifyImportCharacters(final String pidParam) {
    	return Pattern.matches(PID_ALLOWED_CHARACTERS_PATTERN, pidParam);
    }
}
