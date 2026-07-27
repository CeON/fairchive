package edu.harvard.iq.dataverse.dataverse.template;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.faces.event.AbortProcessingException;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;

import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.dataset.DatasetFieldsInitializer;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsOfType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataset.Template;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.util.UIMessages;

@SuppressWarnings("serial")
@ViewScoped
@Named
public class ManageTemplatesPage implements java.io.Serializable {

    private DataverseRepository dataverseRepo;
    private PermissionsWrapper permissionsWrapper;
    private DatasetFieldsInitializer datasetFieldsInitializer;
    private TemplateService templateService;
    private UIMessages ui;

    private List<Template> templatesForView = new LinkedList<>();
    private Dataverse dataverse;
    private Long dataverseId;
    private boolean inheritTemplatesValue;
    private boolean inheritTemplatesAllowed = false;

    private Template selectedTemplate = null;
    private Map<MetadataBlock, List<DatasetFieldsOfType>> mdbForView;
    private ViewTemplateDialog viewTemplateDialog = new ViewTemplateDialog(EMPTY, emptyList());

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public ManageTemplatesPage() {
    }

    @Inject
    public ManageTemplatesPage(final DataverseRepository dataverseRepo,
                               final PermissionsWrapper permissionsWrapper,
                               final DatasetFieldsInitializer datasetFieldsInitializer, 
                               final TemplateService templateService,
                               final UIMessages ui) {
    	
        this.dataverseRepo = dataverseRepo;
        this.permissionsWrapper = permissionsWrapper;
        this.datasetFieldsInitializer = datasetFieldsInitializer;
        this.templateService = templateService;
        this.ui = ui;
    }

    // -------------------- GETTERS --------------------

    public List<Template> getTemplatesForView() {
        return this.templatesForView;
    }

    public Dataverse getDataverse() {
        return this.dataverse;
    }

    public Long getDataverseId() {
        return this.dataverseId;
    }

    public Template getSelectedTemplate() {
        return this.selectedTemplate;
    }

    public boolean isInheritTemplatesValue() {
        return this.inheritTemplatesValue;
    }

    public boolean isInheritTemplatesAllowed() {
        return this.inheritTemplatesAllowed;
    }

    public Map<MetadataBlock, List<DatasetFieldsOfType>> getMdbForView() {
        return this.mdbForView;
    }

    // -------------------- LOGIC --------------------

    public String init() {
    	
        final Optional<Dataverse> dataverse = this.dataverseRepo.findById(this.dataverseId);
        if (!dataverse.isPresent()) {
        	return permissionsWrapper.notFound();
        }
    	this.dataverse = dataverse.get();
        if (!this.permissionsWrapper.canIssueUpdateDataverseCommand(this.dataverse)) {
            return this.permissionsWrapper.notAuthorized();
        }

        if (this.dataverse.isNotRoot() && 
        		this.dataverse.getRootMetadataBlocks().equals(this.dataverse.getOwner().getRootMetadataBlocks())) {
            this.inheritTemplatesAllowed = true;
        }

        this.inheritTemplatesValue = !this.dataverse.isTemplateRoot();

        if (this.inheritTemplatesValue && this.dataverse.isNotRoot()) {
            this.templatesForView.addAll(this.dataverse.getParentTemplates());
        }

        this.templatesForView.addAll(this.dataverse.getTemplates());

        return EMPTY;
    }


    public void makeDefault(final Template template) {

        this.templateService.makeTemplateDefaultForDataverse(this.dataverse, template)
                .onFailure(throwable -> this.ui.addErrorMessage(getStringFromBundle("template.makeDefault.error")))
                .onSuccess(dataverse -> this.ui.addSuccessMessage(getStringFromBundle("template.makeDefault")));
    }

    public void unselectDefault() {

        this.templateService.removeDataverseDefaultTemplate(this.dataverse)
                .onFailure(throwable -> this.ui.addErrorMessage(getStringFromBundle("template.update.error")))
                .onSuccess(dataverse -> this.ui.addSuccessMessage(getStringFromBundle("template.unselectDefault")));
    }

    public String cloneTemplate(final Template template) {
    	
        return "/template.xhtml?id=" + template.getId() + "&mode=CLONE&faces-redirect=true";
    }

    public void deleteTemplate() {
        this.templateService.deleteTemplate(this.dataverse, this.selectedTemplate)
                .onFailure(throwable -> this.ui.addErrorMessage(getStringFromBundle("template.delete.error")))
                .onSuccess(dataverse -> {
                    this.ui.addSuccessMessage(getStringFromBundle("template.delete"));
                    this.templatesForView.remove(this.selectedTemplate);
                });

    }

    public void viewSelectedTemplate(final Template selectedTemplate) {
    	
        this.selectedTemplate = selectedTemplate;

        final List<DatasetField> dsfForView = this.datasetFieldsInitializer.
        		prepareDatasetFieldsForView(selectedTemplate.getDatasetFields(), true);
        this.mdbForView = DatasetFieldUtil.groupByBlockAndType(dsfForView);
        this.viewTemplateDialog =  new ViewTemplateDialog(this.selectedTemplate.getName(), 
        		new ArrayList<>(this.mdbForView.entrySet()));
    }

    /**
     * Updates dataverse regarding which templates it can use, since you can inherit templates from parent.
     */
    public String updateTemplatesRoot(final AjaxBehaviorEvent event) 
    		throws AbortProcessingException {

        if (this.dataverse.isNotRoot()) {
            this.templateService.updateTemplateInheritance(this.dataverse, isInheritTemplatesValue())
                    .onSuccess(updatedDataverse -> this.dataverse = updatedDataverse);

            if (this.inheritTemplatesValue) {
                this.templatesForView.addAll(this.dataverse.getParentTemplates());
            } else {
                this.templatesForView.removeAll(this.dataverse.getParentTemplates());
            }
        }

        return EMPTY;
    }

    public List<String> retrieveDataverseNamesWithDefaultTemplate() {
    	
        return this.templateService.retrieveDataverseNamesWithDefaultTemplate(this.selectedTemplate.getId());
    }

    public String editTemplateRedirect(final Template template) {
    	
        return "/template.xhtml?id=" + template.getId() + "&mode=EDIT&faces-redirect=true";
    }

    // -------------------- SETTERS --------------------

    public void setDataverseId(final Long id) {
    	
        this.dataverseId = id;
    }

    public void setInheritTemplatesValue(final boolean value) {
    	
        this.inheritTemplatesValue = value;
    }

    public void setInheritTemplatesAllowed(final boolean allowed) {
    	
        this.inheritTemplatesAllowed = allowed;
    }

    public void setSelectedTemplate(final Template template) {
    	
        this.selectedTemplate = template;
    }
    
    public ViewTemplateDialog getViewTemplateDialog() {
    	
    	return this.viewTemplateDialog;
    }

    //--------------------------------------------------------------------------
    public class ViewTemplateDialog {
    	
    	private final List<Map.Entry<MetadataBlock, List<DatasetFieldsOfType>>> blockList;
    	private final String templateName;
    	
    	public ViewTemplateDialog(final String templateName, 
    			final List<Entry<MetadataBlock, List<DatasetFieldsOfType>>> blockList) {
    		
    		this.templateName = templateName;
    		this.blockList = blockList;
		}

		public List<Map.Entry<MetadataBlock, List<DatasetFieldsOfType>>> getMetadataBlocks() {
    		return this.blockList;
    	}
    	
    	public boolean shouldRenderBlock(final int index) {
    		return true;
    	}
    	
    	public boolean shouldRenderField(final int blockIndex, final int fieldOfTypeIndex) {
    		return true;
    	}
    	
    	public String getTemplateName() {
    		return this.templateName;
    	}
    	
    	public String getDatasetGlobalIdString() {
    		
    		return EMPTY;
    	}
    	
    	public String getAlternativePersistentIdentifier() {
    		
    		return EMPTY;
    	}
    	
    	public String getPublicationDate() {
    		
    		return EMPTY;
    	}
    }
}
