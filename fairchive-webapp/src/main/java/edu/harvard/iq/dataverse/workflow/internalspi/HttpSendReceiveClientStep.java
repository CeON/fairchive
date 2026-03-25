package edu.harvard.iq.dataverse.workflow.internalspi;

import static edu.harvard.iq.dataverse.workflow.execution.WorkflowContext.TriggerType.PostPublishDataset;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.http.impl.client.HttpClients.createDefault;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.workflow.execution.WorkflowExecutionStepContext;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.Pending;
import edu.harvard.iq.dataverse.workflow.step.Success;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepParams;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

/**
 * A workflow step that sends a HTTP request, and then pauses, waiting for a response.
 *
 * @author michael
 */
public class HttpSendReceiveClientStep implements WorkflowStep {

    private static final Logger logger = getLogger(HttpSendReceiveClientStep.class.getName());

    private final WorkflowStepParams params;
    private final DatasetVersionServiceBean versionsService;
    private final CitationFactory citationFactory;

    public HttpSendReceiveClientStep(final WorkflowStepParams params, 
    		                         final DatasetVersionServiceBean versionsService,
                                     final CitationFactory citationFactory) {
    	
        this.params = params;
        this.versionsService = versionsService;
        this.citationFactory = citationFactory;
    }

    @Override
    public WorkflowStepResult run(final WorkflowExecutionStepContext context) {
    	
    	try (final CloseableHttpClient client = createDefault()) {
        	final HttpUriRequest request = buildRequest(false, context);
        	try (final CloseableHttpResponse response = client.execute(request)) {
	        	final int responseStatus = response.getStatusLine().getStatusCode();
	            if (responseStatus >= 200 && responseStatus < 300) {
	                // HTTP OK range
	                return new Pending();
	            } else {
	            	final String responseBody = response.getEntity() != null
	            	        ? EntityUtils.toString(response.getEntity())
	            	        : EMPTY;
	                return new Failure("Error communicating with server. Server response: " + 
	            	        responseBody + " (" + responseStatus + ").");
	            }
        	}

        } catch (final Exception e) {
            logger.log(SEVERE, "Error communicating with remote server", e);
            return new Failure("Error executing request: " + e.getLocalizedMessage(), 
            		"Cannot communicate with remote server.");
        }
    }

    @Override
    public WorkflowStepResult resume(final WorkflowExecutionStepContext context, 
    		final Map<String, String> internalData, final String externalData) {
    	
        final Pattern pat = Pattern.compile(params.get("expectedResponse"));
        final String response = externalData.trim();
        if (pat.matcher(response).matches()) {
            return new Success();
        } else {
            logger.log(WARNING, "Remote system returned a bad reposonse: {0}", externalData);
            return new Failure("Response from remote server did not match expected one (response:" + response + ')');
        }
    }

    @Override
    public void rollback(final WorkflowExecutionStepContext context, 
    		final Failure reason) {
    	
    	try(final CloseableHttpClient client = createDefault()) {
        	final HttpUriRequest request = buildRequest(true, context);
        	try(final CloseableHttpResponse response = client.execute(request)) {
	        	final int responseStatus = response.getStatusLine().getStatusCode();
	            if (responseStatus < 200 || responseStatus >= 300) {
	                // out of HTTP OK range
	            	final String responseBody = response.getEntity() != null
	            	        ? EntityUtils.toString(response.getEntity())
	            	        : EMPTY;
	                logger.log(WARNING, 
	                		"Bad response from remote server while rolling back step: {0}", 
	                		responseBody);
	            }
        	}
        } catch (final Exception e) {
            logger.log(WARNING, "IO error rolling back step", e);
        }
    }

    HttpUriRequest buildRequest(final boolean rollback, final WorkflowExecutionStepContext ctxt) 
    		throws Exception {
    	
        final String methodName = params.getOrDefault("method" + (rollback ? "-rollback" : ""), "GET").
        		trim().toUpperCase();
        
        final Map<String, String> templateParams = buildTemplateParams(ctxt);
        
        final String urlKey = rollback ? "rollbackUrl" : "url";
        final String url = process(params.get(urlKey), templateParams);
        
        HttpUriRequest request = null;
        switch (methodName) {
            case "GET":
                request = new HttpGet(url);
                break;
            case "POST":
                request = new HttpPost(url);
                break;
            case "PUT":
                request = new HttpPut(url);
                break;
            case "DELETE":
                request = new HttpDelete(url);
                break;
            default:
                throw new IllegalStateException("Unsupported HTTP method: '" + methodName + '\'');
        }

        request.setHeader("Content-Type", params.getOrDefault("contentType", "text/plain"));

        final String bodyKey = rollback ? "rollbackBody" : "body";
        if (params.containsKey(bodyKey) && request instanceof HttpEntityEnclosingRequestBase) {
        	final String body = process(params.get(bodyKey), templateParams);
            final StringEntity entity = new StringEntity(body, "UTF-8");
            ((HttpEntityEnclosingRequestBase) request).setEntity(entity);
        }

        return request;
    }

	private Map<String, String> buildTemplateParams(final WorkflowExecutionStepContext ctxt) {
		
		final Map<String, String> templateParams = new HashMap<>();
        templateParams.put("invocationId", ctxt.getInvocationId());
        templateParams.putAll(versionsService.withDatasetVersion(ctxt,
                datasetVersion -> {
                    final Map<String, String> params = new HashMap<>();
                    params.put("dataset.id", Long.toString(datasetVersion.getDataset().getId()));
                    params.put("dataset.identifier", datasetVersion.getDataset().getIdentifier());
                    params.put("dataset.globalId", datasetVersion.getDataset().getGlobalId().toString());
                    params.put("dataset.displayName", datasetVersion.getDataset().getDisplayName());
                    params.put("dataset.citation",
                            citationFactory.create(datasetVersion.getDataset().getLatestVersion())
                                    .toString(false));

                    return params;
                }
        ).orElseGet(Collections::emptyMap));
        templateParams.put("minorVersion", Long.toString(ctxt.getMinorVersionNumber()));
        templateParams.put("majorVersion", Long.toString(ctxt.getVersionNumber()));
        templateParams.put("releaseStatus", (ctxt.getType() == PostPublishDataset) ? "done" : "in-progress");
		return templateParams;
	}
    

    String process(final String template, final Map<String, String> values) {
    	
        String result = template;
        for (final Map.Entry<String, String> ent : values.entrySet()) {
            String val = ent.getValue();
            if (val == null) {
                val = EMPTY;
            }
            final String varRef = "${" + ent.getKey() + '}';
            while (result.contains(varRef)) {
                result = result.replace(varRef, val);
            }
        }
        return result;
    }
}
