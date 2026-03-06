package edu.harvard.iq.dataverse.api.filters;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.BlockedApiEndpoints;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.BlockedApiKey;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.BlockedApiPolicy;
import static java.util.logging.Logger.getLogger;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

/**
 * A web filter to block API administration calls.
 *
 * @author michael
 */
public class ApiBlockingFilter implements javax.servlet.Filter {
    private static final String UNBLOCK_KEY_QUERYPARAM = "unblock-key";
    private static final Logger logger = getLogger(ApiBlockingFilter.class.getName());

    @Inject
    protected SettingsServiceBean settings;

    private final Set<String> blockedApiEndpoints = new TreeSet<>();
    private String lastEndpointList;
    private final Map<String, BlockPolicy> policies = new TreeMap<>();

    interface BlockPolicy {
        void doBlock(ServletRequest request, ServletResponse response, 
        		FilterChain chain) throws IOException, ServletException;
    }

    /**
     * A policy that allows all requests.
     */
    private static void allow(final ServletRequest request,
    		final ServletResponse response, final FilterChain chain) 
    		throws IOException, ServletException {
    	
            chain.doFilter(request, response);
    }

    /**
     * A policy that drops blocked requests.
     */
    private static void drop(final ServletRequest request, 
    		final ServletResponse response, final FilterChain chain) 
    		throws IOException, ServletException {
    	
        final HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.getWriter().println("{ status:\"error\", message:\"Endpoint blocked. Please contact administrator\"}");
        httpResponse.setStatus(SC_SERVICE_UNAVAILABLE);
        httpResponse.setContentType("application/json");
    }

    /**
     * Allow only from localhost.
     */
    private static void localhostOnly(final ServletRequest request, 
    		final ServletResponse response, final FilterChain chain) 
    		throws IOException, ServletException {
    	
        if (isFromLocalhost(request)) {
            chain.doFilter(request, response);
        } else {
            final HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.getWriter().println("{ status:\"error\", message:\"Endpoint available from localhost only. Please contact administrator\"}");
            httpResponse.setStatus(SC_SERVICE_UNAVAILABLE);
            httpResponse.setContentType("application/json");
        }
    }
    
    private static boolean isFromLocalhost(final ServletRequest request) {
    	return new DataverseRequest(null, (HttpServletRequest) request).
    			getSourceAddress().isLocalhost();
    }

    /**
     * Allow only for requests that have the {@link #UNBLOCK_KEY_QUERYPARAM} param with
     * value from {@link SettingsServiceBean.Key.BlockedApiKey}
     */
    private void unblockKey(final ServletRequest request, 
    		final ServletResponse response, final FilterChain chain) 
    		throws IOException, ServletException {
    	
        boolean block = true;

        final String masterKey = this.settings.getValueForKey(BlockedApiKey);
        if (isNotEmpty(masterKey)) {
            final String queryString = ((HttpServletRequest) request).getQueryString();
            if (queryString != null) {
                for (final String paramPair : queryString.split("&")) {
                    final String[] curPair = paramPair.split("=", -1);
                    if ((curPair.length >= 2)
                            && UNBLOCK_KEY_QUERYPARAM.equals(curPair[0])
                            && masterKey.equals(curPair[1])) {
                        block = false;
                        break;
                    }
                }
            }
        }

        if (block) {
            final HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.getWriter().println("{ status:\"error\", message:\"Endpoint available using API key only. Please contact the dataverse administrator\"}");
            httpResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            httpResponse.setContentType("application/json");
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void init(final FilterConfig chain) throws ServletException {
        updateBlockedPoints();
        this.policies.put("allow", ApiBlockingFilter::allow);
        this.policies.put("drop", ApiBlockingFilter::drop);
        this.policies.put("localhost-only", ApiBlockingFilter::localhostOnly);
        this.policies.put("unblock-key", this::unblockKey);
    }

    private void updateBlockedPoints() {
        this.blockedApiEndpoints.clear();
        final String endpointList = this.settings.getValueForKey(BlockedApiEndpoints);
        for (final String endpoint : endpointList.split(",")) {
            String endpointPrefix = canonize(endpoint);
            if (!endpointPrefix.isEmpty()) {
                endpointPrefix = endpointPrefix + '/';
                logger.log(Level.INFO, "Blocking API endpoint: {0}", endpointPrefix);
                this.blockedApiEndpoints.add(endpointPrefix);
            }
        }
        this.lastEndpointList = endpointList;
    }

    @Override
    public void doFilter(final ServletRequest request, 
    		final ServletResponse response, final FilterChain chain) 
    		throws IOException, ServletException {

        final String endpointList = this.settings.getValueForKey(BlockedApiEndpoints);
        if (!endpointList.equals(this.lastEndpointList)) {
            updateBlockedPoints();
        }

        final HttpServletRequest hsr = (HttpServletRequest) request;
        final String requestURI = hsr.getRequestURI();
        final String apiEndpoint = canonize(requestURI.substring(hsr.getServletPath().length()));
        for (final String prefix : blockedApiEndpoints) {
            if (apiEndpoint.startsWith(prefix)) {
                getBlockPolicy().doBlock(request, response, chain);
                return;
            }
        }
        try {
            chain.doFilter(request, response);
        } catch (ServletException se) {
            logger.log(Level.WARNING, "Error processing " + requestURI + ": " + se.getMessage(), se);
            final HttpServletResponse resp = (HttpServletResponse) response;
            resp.setStatus(500);
            resp.setHeader("PROCUDER", "ApiBlockingFilter");
            resp.getWriter().append("Error: ").append(se.getMessage());
        }
    }

    @Override
    public void destroy() {
    }

    private BlockPolicy getBlockPolicy() {
        final String blockPolicyName = this.settings.getValueForKey(BlockedApiPolicy);
        final BlockPolicy p = this.policies.get(blockPolicyName.trim());
        if (p != null) {
            return p;
        } else {
            logger.log(Level.WARNING, "Undefined block policy {0}. Available policies are {1}",
                       new Object[]{blockPolicyName, this.policies.keySet()});
            return ApiBlockingFilter::allow;
        }
    }

    /**
     * Creates a canonical representation of {@code in}: trimmed spaces and slashes
     *
     * @param in the raw string
     * @return {@code in} with no trailing and leading spaces and slashes.
     */
    private static String canonize(String in) {
        in = in.trim();
        if (in.startsWith("/")) {
            in = in.substring(1);
        }
        if (in.endsWith("/")) {
            in = in.substring(0, in.length() - 1);
        }
        return in;
    }

}
