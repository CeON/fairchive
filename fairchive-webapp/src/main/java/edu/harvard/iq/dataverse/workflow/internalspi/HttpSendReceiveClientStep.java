package edu.harvard.iq.dataverse.workflow.internalspi;

import static edu.harvard.iq.dataverse.workflow.execution.WorkflowContext.TriggerType.PostPublishDataset;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
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
import org.apache.http.impl.client.HttpClients;
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

    private static final Logger logger = Logger.getLogger(HttpSendReceiveClientStep.class.getName());

    private final WorkflowStepParams params;
    private final DatasetVersionServiceBean versionsService;
    private final CitationFactory citationFactory;

    public HttpSendReceiveClientStep(WorkflowStepParams params, 
    		                         DatasetVersionServiceBean versionsService,
                                     CitationFactory citationFactory) {
        this.params = params;
        this.versionsService = versionsService;
        this.citationFactory = citationFactory;
    }

    @Override
    public WorkflowStepResult run(WorkflowExecutionStepContext context) {
    	try (CloseableHttpClient client = HttpClients.createDefault()) {
            // build method
        	HttpUriRequest request = buildRequest(false, context);
            // execute
        	try (CloseableHttpResponse response = client.execute(request)) {
	        	int responseStatus = response.getStatusLine().getStatusCode();
	            if (responseStatus >= 200 && responseStatus < 300) {
	                // HTTP OK range
	                return new Pending();
	            } else {
	            	String responseBody = response.getEntity() != null
	            	        ? EntityUtils.toString(response.getEntity())
	            	        : "";
	                return new Failure("Error communicating with server. Server response: " + 
	            	        responseBody + " (" + responseStatus + ").");
	            }
        	}

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error communicating with remote server: " + 
            		ex.getMessage(), ex);
            return new Failure("Error executing request: " + ex.getLocalizedMessage(), 
            		"Cannot communicate with remote server.");
        }
    }

    @Override
    public WorkflowStepResult resume(WorkflowExecutionStepContext context, 
    		Map<String, String> internalData, String externalData) {
        Pattern pat = Pattern.compile(params.get("expectedResponse"));
        String response = externalData.trim();
        if (pat.matcher(response).matches()) {
            return new Success();
        } else {
            logger.log(Level.WARNING, "Remote system returned a bad reposonse: {0}", externalData);
            return new Failure("Response from remote server did not match expected one (response:" + response + ")");
        }
    }

    @Override
    public void rollback(WorkflowExecutionStepContext context, Failure reason) {
    	try(CloseableHttpClient client = HttpClients.createDefault()) {
            // build method
        	HttpUriRequest request = buildRequest(true, context);

            // execute
        	try(CloseableHttpResponse response = client.execute(request)) {
	        	int responseStatus = response.getStatusLine().getStatusCode();
	            if (responseStatus < 200 || responseStatus >= 300) {
	                // out of HTTP OK range
	            	String responseBody = response.getEntity() != null
	            	        ? EntityUtils.toString(response.getEntity())
	            	        : "";
	                logger.log(Level.WARNING, 
	                		"Bad response from remote server while rolling back step: {0}", 
	                		responseBody);
	            }
        	}
        } catch (Exception ex) {
            logger.log(Level.WARNING, "IO error rolling back step: " + ex.getMessage(), ex);
        }
    }

    HttpUriRequest buildRequest(boolean rollback, WorkflowExecutionStepContext ctxt) 
    		throws Exception {
        String methodName = params.getOrDefault("method" + (rollback ? "-rollback" : ""), "GET").
        		trim().toUpperCase();
        
        Map<String, String> templateParams = buildTemplateParams(ctxt);
        
        String urlKey = rollback ? "rollbackUrl" : "url";
        String url = process(params.get(urlKey), templateParams);
        
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
                request = new HttpDelete();
                break;
            default:
                throw new IllegalStateException("Unsupported HTTP method: '" + methodName + "'");
        }

        request.setHeader("Content-Type", params.getOrDefault("contentType", "text/plain"));

        String bodyKey = (rollback ? "rollbackBody" : "body");
        if (params.containsKey(bodyKey) && request instanceof HttpEntityEnclosingRequestBase) {
        	String body = process(params.get(bodyKey), buildTemplateParams(ctxt));
            StringEntity entity = new StringEntity(body, "UTF-8");
            ((HttpEntityEnclosingRequestBase) request).setEntity(entity);
        }

        return request;
    }

	private Map<String, String> buildTemplateParams(WorkflowExecutionStepContext ctxt) {
		Map<String, String> templateParams = new HashMap<>();
        templateParams.put("invocationId", ctxt.getInvocationId());
        templateParams.putAll(versionsService.withDatasetVersion(ctxt,
                datasetVersion -> {
                    Map<String, String> params = new HashMap<>();
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
    

    String process(String template, Map<String, String> values) {
        String curValue = template;
        for (Map.Entry<String, String> ent : values.entrySet()) {
            String val = ent.getValue();
            if (val == null) {
                val = "";
            }
            String varRef = "${" + ent.getKey() + "}";
            while (curValue.contains(varRef)) {
                curValue = curValue.replace(varRef, val);
            }
        }

        return curValue;
    }

}
