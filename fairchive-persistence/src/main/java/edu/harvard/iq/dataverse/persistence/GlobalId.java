package edu.harvard.iq.dataverse.persistence;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getLogger;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.startsWithIgnoreCase;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
/**
 * @author skraffmiller
 */
@SuppressWarnings("serial")
public class GlobalId implements Serializable {

    public static final String DOI_PROTOCOL = "doi";
    public static final String HDL_PROTOCOL = "hdl";
    public static final String HTTP_PROTOCOL = "http";
    public static final String HTTPS_PROTOCOL = "https";
    public static final String HDL_RESOLVER_URL = "https://hdl.handle.net/";
    public static final String HDL_RESOLVER_URL2 = "http://hdl.handle.net/";
    public static final String DOI_RESOLVER_URL = "https://doi.org/";
    public static final String DOI_RESOLVER_URL2 = "http://doi.org/";
    public static final String HTTPS_RESOLVER_URL = "https://";
    public static final String HTTP_RESOLVER_URL = "http://";
    private static final String PID_ALLOWED_CHARACTERS_PATTERN = "^[A-Za-z0-9._/:\\\\-]*";
    
    private static final Logger logger = getLogger(GlobalId.class.getName());
    
    private final String protocol;
    private final String authority;
    private final String identifier;
    
    public static GlobalId fromHDLUrl(final String url) {
		final int lastSlashIndex = url.lastIndexOf('/');
		final int authorityOffset = url.startsWith(HDL_RESOLVER_URL)
			? HDL_RESOLVER_URL.length() : HDL_RESOLVER_URL2.length();
		return new GlobalId(HDL_PROTOCOL, 
				url.substring(authorityOffset, lastSlashIndex),
				url.substring(lastSlashIndex + 1));
    }
    
    public static GlobalId fromDOIUrl(final String url) {
 		final int lastSlashIndex = url.lastIndexOf('/');
 		final int authorityOffset = url.startsWith(DOI_RESOLVER_URL)
 			? DOI_RESOLVER_URL.length() : DOI_RESOLVER_URL2.length();
 		
 		final String authority = url.substring(authorityOffset, lastSlashIndex);
 		final String identifier = url.substring(lastSlashIndex + 1);
 		
 		// sometimes DOIs come in form of
 		// https://doi.org/DOI: 10.19195/2353-8546.8.13 or
 		// https://doi.org/DOI : 10.19195/2353-8546.8.13
 		// https://doi.org/1https://doi.org/0.14746/eip.2023.2.8
 		// even after fixing, these links lead to nowhere so it is 
 		// better to let the caller know that it needs to find another identifier
 		if(startsWithIgnoreCase(authority, "DOI") || authority.startsWith("1https")) {
 			throw new IllegalArgumentException("Bloken DOI url: ".concat(url));
 		}
 		return new GlobalId(DOI_PROTOCOL, authority, identifier);
   }
    
	public static GlobalId fromHttpUrl(final String url) throws RuntimeException {
		try {
			final URL u = new URL(url);
			return new GlobalId(HTTP_PROTOCOL, u.getHost(), u.getPath());
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
	}
		
	public static GlobalId fromHttpsUrl(final String url) throws RuntimeException {
		try {
			final URL u = new URL(url);
			return new GlobalId(HTTPS_PROTOCOL, u.getHost(), u.getPath());
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
	}

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
    public GlobalId(final String identifierString) {
        final int index1 = identifierString.indexOf(':');
        if (index1 > 0) { // ':' found with one or more characters before it
        	if(identifierString.startsWith(HTTP_PROTOCOL)) {
        		try {
	        		final URL url = new URL(identifierString.substring(HTTP_PROTOCOL.length() +1));
	        		this.protocol = HTTP_PROTOCOL;
	        		this.authority = url.getHost();
	        		this.identifier = url.getPath();
	        		if(conainsNullTerminator(this.identifier)) {
	        			throw createException(identifierString);
	        		}
        		} catch (final MalformedURLException e) {
        			throw new IllegalArgumentException(e);
        		}
        	} else if(identifierString.startsWith(HTTPS_PROTOCOL)) {
        		try {
	        		final URL url = new URL(identifierString.substring(HTTPS_PROTOCOL.length() +1));
	        		this.protocol = HTTPS_PROTOCOL;
	        		this.authority = url.getHost();
	        		this.identifier = url.getPath();
	        		if(conainsNullTerminator(this.identifier)) {
	        			throw createException(identifierString);
	        		}
        		} catch (final MalformedURLException e) {
        			throw new IllegalArgumentException(e);
        		}
        	} else {
	            final int index2 = identifierString.indexOf('/', index1 + 1);
	            if (index2 > 0 && (index2 + 1) < identifierString.length()) { // '/' found with one or more characters
	                // between ':'
	                this.protocol = identifierString.substring(0, index1); // and '/' and there are characters after '/'
	                if (!DOI_PROTOCOL.equals(this.protocol) 
	                		&& !HDL_PROTOCOL.equals(this.protocol)) {
	                	throw createException(identifierString);
	                }
	                //Strip any whitespace, ; and ' from authority (should finding them cause a failure instead?)
	                this.authority = formatIdentifierString(identifierString.substring(index1 + 1, index2));
	                if (conainsNullTerminator(this.authority)) {
	                	throw createException(identifierString);
	                }
	                if (this.protocol.equals(DOI_PROTOCOL) && !this.checkDOIAuthority(this.authority)) {
	                	throw createException(identifierString);
	                }
	                // Passed all checks
	                //Strip any whitespace, ; and ' from identifier (should finding them cause a failure instead?)
	                this.identifier = formatIdentifierString(identifierString.substring(index2 + 1));
	                if(conainsNullTerminator(this.identifier)) {
	                	throw createException(identifierString);
	                }
	            } else {
	            	throw createException(identifierString);
	            }
        	}
        } else {
        	throw createException(identifierString);
        }
    }
    
    private static IllegalArgumentException createException(final String identifier) {
    	return new IllegalArgumentException("Failed to parse identifier: ".concat(identifier));
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

	public String getAuthority() {
		return this.authority;
	}

    public String getIdentifier() {
		return this.identifier;
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
        } else if(HTTP_PROTOCOL.equals(this.protocol) || HTTPS_PROTOCOL.equals(this.protocol)) {
        	return this.protocol + ':' + this.protocol + "://" + this.authority + this.identifier;
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
				} else if(HTTP_PROTOCOL.equals(this.protocol) || HTTPS_PROTOCOL.equals(this.protocol)) {
					return new URL(this.protocol, this.authority, this.identifier);
				}
			} catch (final MalformedURLException ex) {
				logger.log(SEVERE, null, ex);
			}
		}
		return null;
    }
    
    public String getStoragePath() {
    	return this.authority.replace(':', '_') + '/' + 
    			stripLeadingSlashes(this.identifier.replace(':', '_'));
    }
    
    private static String stripLeadingSlashes(final String s) {
    	int index = 0;
    	while(s.charAt(index) == '/') {
    		++index;
    	}
    	return s.substring(index);
    }

    public static boolean isDOI(final String id) {
    	return id.startsWith(DOI_RESOLVER_URL) || id.startsWith(DOI_RESOLVER_URL2);
    }
    
    public static boolean isHDL(final String id) {
    	return id.startsWith(HDL_RESOLVER_URL) || id.startsWith(HDL_RESOLVER_URL2);
    }
    
    public static boolean isHTTP(final String id) {
    	return id.startsWith(HTTP_RESOLVER_URL);
    }
    
    public static boolean isHTTPS(final String id) {
    	return id.startsWith(HTTPS_RESOLVER_URL);
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

    private static boolean conainsNullTerminator(final String str) {
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
