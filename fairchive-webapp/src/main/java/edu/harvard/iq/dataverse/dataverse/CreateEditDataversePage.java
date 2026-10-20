package edu.harvard.iq.dataverse.dataverse;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion.State.RELEASED;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.CollectionFormPrefillEnabled;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DisableRootDataverseTheme;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.list;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Collections.emptyList;
import static javax.faces.application.FacesMessage.SEVERITY_ERROR;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DualListModel;
import org.primefaces.model.file.UploadedFile;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.dataverse.themewidget.ThemeWidgetService;
import edu.harvard.iq.dataverse.error.DataverseError;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse.DataverseType;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme.Alignment;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme.ImageFormat;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.UIMessages;
import io.vavr.control.Either;

@SuppressWarnings("serial")
@ViewScoped
@Named("CreateEditDataversePage")
public class CreateEditDataversePage implements Serializable {

    private DataverseService dataverseService;
    private ThemeWidgetService themeWidgetService;
    private PermissionsWrapper permissionsWrapper;
    private DataverseSession session;
    private SystemConfig config;
    private UIMessages ui;

    private Long dataverseId;
    private Long ownerId;
    private String dataverseOwnerAlias;

    private Dataverse dataverse;
    private DataverseVersion version;
    
    private Path tempDir;
    private Path uploadedFile;
    private HtmlInputText linkUrlInput;
    private HtmlInputText taglineInput;
    
    public CreateEditDataversePage() {}
    @Inject
    public CreateEditDataversePage(final DataverseService dataverseService,
    	    					   final ThemeWidgetService themeWidgetService,
    							   final PermissionsWrapper permissionsWrapper,
    							   final DataverseSession session,
    							   final SystemConfig config,
    							   final UIMessages ui) {
    	this.dataverseService = dataverseService;
    	this.themeWidgetService = themeWidgetService;
    	this.permissionsWrapper = permissionsWrapper;
    	this.session = session;
    	this.config = config;
    	this.ui = ui;
    }
    
    // -------------------- GETTERS --------------------
    
    public String getTitle() {
    	return this.dataverse != null && this.dataverse.isNotNew()
    			? getStringFromBundle("dataverse.edit.header") + " - " + this.dataverse.getName()
    			: getStringFromBundle("newDataverse");
    }
    
    public boolean isDataverseNotNew() {
    	return this.dataverse.isNotNew();
    }
    
    public String getName() {
    	return this.version.getName();
    }
    
    public void setName(final String name) {
    	this.version.setName(name);
    }
    
    public String getAffiliation() {
    	return this.version.getAffiliation();
    }
    
    public void setAffiliation(final String affiliation) {
    	this.version.setAffiliation(affiliation);
    }
    
    public String getAlias() {
    	return this.version.getAlias();
    }
    
    public void setAlias(final String alias) {
    	this.version.setAlias(alias);
    }
    
    public DataverseType getType() {
        return this.version.getDataverseType();
    }
    
    public void setType(final DataverseType type) {
    	this.version.setDataverseType(type);
    }
    
    public String getDescription() {
    	return this.version.getDescription();
    }
    
    public void setDescription(final String description) {
    	this.version.setDescription(description);
    }
    
    public List<DataverseContact> getContacts() {
    	return this.version.getContacts();
    }
    
    public void addContact(final int index) {
    	this.version.addContact(index);
    }

    public void removeContact(final int index) {
    	this.version.removeContact(index);
    }
    
    public String getAdditionalDescription() {
    	return this.version.getAdditionalDescription();
    }
    
    public void setAdditionalDescription(final String description) {
    	this.version.setAdditionalDescription(description);
    }
    
    public String getMetadataParentRootDataverseName() {
        return this.dataverse.getMetadataParentRootDataverseName();
    }
    
    public String getFacetRootDataverseName() {
        return this.dataverse.getFacetRootDataverseName();
    }

    public Dataverse getDataverse() {
        return this.dataverse;
    }

    public Long getDataverseId() {
        return this.dataverseId;
    }

    public Long getOwnerId() {
        return this.ownerId;
    }

    public String getDataverseOwnerAlias() {
        return this.dataverseOwnerAlias;
    }
    
    public boolean isFacetPickListDisabled() {
    	return !this.dataverse.isFacetRoot() && this.dataverse.isNotRoot();
    }
    
    public boolean isMessagesBannersAllowed() {
        return this.version.isMessagesBannersAllowed();
    }

    public void setMessagesBannersAllowed(final boolean allowed) {
        this.version.setMessagesBannersAllowed(allowed);
    }
    
    public String getDataverseName() {
    	return this.dataverse.getName();
    }
    
    public HtmlInputText getLinkUrlInput() {
        return this.linkUrlInput;
    }

    public void setLinkUrlInput(final HtmlInputText linkUrlInput) {
        this.linkUrlInput = linkUrlInput;
    }

    public HtmlInputText getTaglineInput() {
        return this.taglineInput;
    }

    public void setTaglineInput(final HtmlInputText taglineInput) {
        this.taglineInput = taglineInput;
    }
    
    public String getTagline() {
        return this.version.getTheme().getTagline();
    }

    public void setTagline(final String line) {
        this.version.getTheme().setTagline(line);
    }
    
    public String getLinkUrl() {
        return this.version.getTheme().getLinkUrl();
    }

    public void setLinkUrl(final String url) {
    	this.version.getTheme().setLinkUrl(url);
    }
    
    public String getLogo() {
        return this.version.getTheme().getLogo();
    }
    
    public boolean isLogoPresent() {
    	boolean result = isNotEmpty(this.version.getTheme().getLogo());
    	return result;
    }
    
    public String getLogoPath() {
    	return this.uploadedFile != null
    		? "/logos/temp/" + getTempDirName() + '/' + getLogo()
    		: "/logos/" + this.dataverse.getId() + '/' + getLogo();
    }

    public void setLogo(final String logo) {
        this.version.getTheme().setLogo(logo);
    }
    
    public ImageFormat getLogoFormat() {
        return this.version.getTheme().getLogoFormat();
    }

    public void setLogoFormat(final ImageFormat format) {
        this.version.getTheme().setLogoFormat(format);
    }
    
    public Alignment getLogoAlignment() {
        return this.version.getTheme().getLogoAlignment();
    }

    public void setLogoAlignment(final Alignment alignment) {
        this.version.getTheme().setLogoAlignment(alignment);
    }
    
    public String getLogoBackgroundColor() {
        return this.version.getTheme().getLogoBackgroundColor();
    }

    public void setLogoBackgroundColor(final String color) {
        this.version.getTheme().setLogoBackgroundColor(color);
    }
    
    public String getBackgroundColor() {
        return this.version.getTheme().getBackgroundColor();
    }

    public void setBackgroundColor(final String color) {
        this.version.getTheme().setBackgroundColor(color);
    }
    
    public String getLinkColor() {
        return this.version.getTheme().getLinkColor();
    }

    public void setLinkColor(final String color) {
        this.version.getTheme().setLinkColor(color);
    }
    
    public String getTextColor() {
        return this.version.getTheme().getTextColor();
    }

    public void setTextColor(final String color) {
        this.version.getTheme().setTextColor(color);
    }
    
    public boolean isDataverseNotRoot() {
    	return this.dataverse.isNotRoot();
    }
    
    public String getThemeRootDataverseName() {
    	return this.dataverse.getThemeRootDataverseName();
    }
    
    public boolean isThemeRoot() {
        return this.dataverse.getThemeRoot();
    }
    
    public long getUploadLogoSizeLimit() {
        return this.config.getUploadLogoSizeLimit();
    }
    
    public boolean isDataverseReleased() {
    	return this.dataverse.isReleased();
    }

    private void createTempDir() {
        try {
            final Path tempRoot = createDirectories(Paths.get("../docroot/logos/temp"));
            this.tempDir = createTempDirectory(tempRoot, this.dataverse.getId().toString());
        } catch (final IOException e) {
            throw new RuntimeException("Error creating temp directory", e);
        }
    }

    @PreDestroy
    /**
     *  Cleanup by deleting temp directory and uploaded files  
     */
    public void cleanupTempDirectory() {
        try {
            if (this.tempDir != null) {
            	list(this.tempDir).forEach(file -> {
            		try {
            			deleteIfExists(file);
            		} catch (final IOException e) {
                        throw new RuntimeException("Error deleting temp directory", e); 
                    }
            	});
                deleteIfExists(this.tempDir);
            }
        } catch (final IOException e) {
            throw new RuntimeException("Error deleting temp directory", e); 
        }
        this.uploadedFile = null;
        this.tempDir = null;
    }

    // -------------------- LOGIC --------------------

    public String init() {
        if (this.dataverseId == null) {
            return initForCreation();
        } else {
        	return initForEdition();
        }
    }
    
    private String initForCreation() {
    	final Optional<Dataverse> owner = this.dataverseService.findById(this.ownerId);

    	if (!owner.isPresent()) {
    	    return this.permissionsWrapper.notFound();
    	}
    	
        if (!this.permissionsWrapper.canIssueCreateDataverseCommand(owner.get())) {
            return this.permissionsWrapper.notAuthorized();
        }
        
        this.dataverse = new Dataverse();
        this.dataverse.setOwner(owner.get());
        this.version = this.dataverse.getEditableVersion();

        if (isPrefillEnabled()) {
        	final User user = this.session.getUser();
            this.version.addContact(user.getDisplayInfo().getEmailAddress());
            this.version.setAffiliation(user.getDisplayInfo().getAffiliation());
            this.version.setName(Dataverse.getSuggestedNameOnCreate(user));
        } 

        initTheme();
        
        return EMPTY;
    }
    
    private String initForEdition() {
    	final Optional<Dataverse> dataverse = this.dataverseService.findById(this.dataverseId);
    	
        if (! dataverse.isPresent()) {
            return this.permissionsWrapper.notFound();
        }
      
        if (!this.permissionsWrapper.canIssueUpdateDataverseCommand(dataverse.get())) {
            return permissionsWrapper.notAuthorized();
        } 
        
        this.dataverse = dataverse.get();
        this.version = this.dataverse.getEditableVersion();
    	this.ownerId = this.dataverse.getOwnerId();
    	
    	initTheme();
    	
    	return EMPTY;
    }
    
    private void initTheme() {
        if (this.dataverse.isRoot()) {
            this.dataverse.setThemeRoot(true);
        }
        
        if (this.dataverse.getDataverseTheme() == null && this.dataverse.isThemeRoot()) {
            this.version.setTheme( new DataverseTheme());

        }
    }

    public void validateAlias(final FacesContext context, final 
    		UIComponent toValidate, final Object value) {
    	final String alias = (String) value;
        if (isNotEmpty(alias)) {
            this.dataverseService.findByAlias(alias)
            	.filter(this::isNotCurrentDataverse)
            	.ifPresent(d -> {
            		 ((UIInput) toValidate).setValid(false);
                     final FacesMessage message = new FacesMessage(SEVERITY_ERROR, 
                     		getStringFromBundle("dataverse.alias"), 
                     		getStringFromBundle("dataverse.alias.taken"));
                     context.addMessage(toValidate.getClientId(context), message);
            	});
        }
    }
    
    public void validateTagline(final FacesContext context, 
    		final UIComponent component, final Object value) throws ValidatorException {
        if (!isEmpty((String) value) && ((String) value).length() > 140) {
            final FacesMessage msg = new FacesMessage(getStringFromBundle("theme.validateTagline"));
            msg.setSeverity(SEVERITY_ERROR);

            throw new ValidatorException(msg);
        }

    }

    public void validateUrl(final FacesContext context, 
    		final UIComponent component, final Object value) throws ValidatorException {
        try {
            if (!isEmpty((String) value)) {
                new URL((String) value);
            }
        } catch (final MalformedURLException e) {
            final FacesMessage msg
                    = new FacesMessage(getStringFromBundle("theme.urlValidate"),
                                       getStringFromBundle("theme.urlValidate.msg"));
            msg.setSeverity(SEVERITY_ERROR);

            throw new ValidatorException(msg);
        }
    }
    
    /**
     * Copy uploaded file to temp area, until we are ready to save
     * Copy filename into Dataverse logo
     *
     * @param event
     */

    public void handleImageFileUpload(final FileUploadEvent event) {

        if (this.tempDir == null) {
            createTempDir();
        }
        final UploadedFile uFile = event.getFile();
        try {
            this.uploadedFile = this.tempDir.resolve(uFile.getFileName());
            if (!exists(this.uploadedFile)) {
                createFile(this.uploadedFile);
            }
            copy(uFile.getInputStream(), this.uploadedFile, REPLACE_EXISTING);
            this.dataverse.getDataverseTheme().setLogo(uFile.getFileName());

        } catch (final IOException e) {
            throw new RuntimeException("Error uploading logo file", e); // improve error handling
        }
    }

    public void removeLogo() {
        this.dataverse.getDataverseTheme().setLogo(null);
        this.cleanupTempDirectory();

    }

    public boolean getInheritCustomization() {
        return this.dataverse == null || !this.dataverse.getThemeRoot();
    }

    public void setInheritCustomization(final boolean inherit) {
        this.dataverse.setThemeRoot(!inherit);
        if (!inherit) {
            if (this.dataverse.getDataverseTheme(true) == null) {
                this.dataverse.getLatestVersion().setTheme(new DataverseTheme());
            }
        }
    }

    public void resetForm() {
        PrimeFaces.current().resetInputs(":dataverseForm:themeWidgetsTabView");
    }

    public boolean displayThemeTab() {
        return this.dataverse.isRoot()
            ? !this.config.isTrueForKey(DisableRootDataverseTheme)
            : true;
    }

    private String getTempDirName() {
        return this.tempDir != null ? this.tempDir.getFileName().toString().replace("\\", "/") : null;
    }
    
    private boolean isNotCurrentDataverse(final Dataverse found) {
    	return this.dataverse.isNew()
    		? true
    		: !this.dataverse.getId().equals(found.getId());
    }

    public boolean isInheritFacetFromParent() {
        return !this.dataverse.isFacetRoot();
    }

    public boolean isUserCanChangeAllowMessageAndBanners() {
        return this.session.isSuperUserLoggedIn();
    }
    
    
    public String saveAndRelease() {
    	if(canUserPublishCollection()) {
    		this.version.setState(RELEASED);
    		return save();
    	} else {
    		return this.permissionsWrapper.notAuthorized();
    	}
    }

    public String save() {
    	if(session.isUserLoggedIn()) {
	        if (this.dataverse.isNew()) {
	            return saveNewDataverse();
	        } else {
	        	return saveEditedDataverse();
	        }
    	} else {
    		this.ui.addErrorMessage(getStringFromBundle("dataverse.create.authenticatedUsersOnly"));
    		return EMPTY;
    	}
    }   
    

    
    private String saveEditedDataverse() {
    	this.dataverse.addVersion(this.version);
        final Either<DataverseError, Dataverse> editResult = 
        		this.dataverseService.saveEditedDataverse(emptyList(), 
        				this.dataverse, new DualListModel<DatasetFieldType>());
        if (editResult.isLeft()) {
            this.ui.addErrorMessage(editResult.getLeft().getErrorMsg(), EMPTY);
            return EMPTY;
        } else {
        	try {
    	        if (this.dataverse.isThemeRoot()) {
    	            this.themeWidgetService.
    	            		saveOrUpdateUploadedTheme(this.dataverse, this.uploadedFile);
    	        } else {
    	        	this.dataverse = this.themeWidgetService.inheritThemeFromRoot(this.dataverse);
    	        }
	        	this.ui.addFlashSuccessMessage(getStringFromBundle("dataverse.update.success"));
	        	return returnRedirect();
        	} catch(final Exception e) {
                this.ui.addErrorMessage(getStringFromBundle("dataverse.save.failed"),
                        				  getStringFromBundle("dataverse.theme.failure"));
                return null;
            } finally {
            	this.cleanupTempDirectory();
            }
        }
    }

    private String saveNewDataverse() {
    	
    	this.dataverse.addVersion(this.version);
        final Either<DataverseError, Dataverse> saveResult = 
        		this.dataverseService.saveNewDataverse(emptyList(), 
        				this.dataverse, new DualListModel<DatasetFieldType>());

        if (saveResult.isLeft()) {
            this.ui.addErrorMessage(saveResult.getLeft().getErrorMsg(), EMPTY);
            return EMPTY;
        } else {
            try {
    	        if (this.dataverse.isThemeRoot()) {
    	            this.themeWidgetService.
    	            		saveOrUpdateUploadedTheme(this.dataverse, this.uploadedFile);
    	        } else {
    	        	this.dataverse = this.themeWidgetService.inheritThemeFromRoot(this.dataverse);
    	        }
                this.ui.addFlashSuccessMessage(getStringFromBundle("dataverse.create.success",
                        this.config.getGuidesBaseUrl(this.session.getLocale()),
                        this.config.getGuidesVersion()));
            	return returnRedirect();
            } catch(final Exception e) {
                this.ui.addErrorMessage(getStringFromBundle("dataverse.save.failed"),
                        				  getStringFromBundle("dataverse.theme.failure"));
                return null;
            } finally {
            	this.cleanupTempDirectory();
            }
        }
    }

    public String cancel() {
        return returnRedirect();
    }

    private String returnRedirect() {
    	return "/dataverse.xhtml?faces-redirect=true&alias=".concat(
    			this.dataverse.isNew() ? this.dataverseOwnerAlias : this.dataverse.getAlias());
    }

    // -------------------- SETTERS --------------------

    public void setDataverse(final Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public void setDataverseId(final Long id) {
        this.dataverseId = id;
    }

    public void setOwnerId(final Long id) {
        this.ownerId = id;
    }

    public void setInheritFacetFromParent(final boolean inherit) {
        this.dataverse.setFacetRoot(!inherit);
    }

    public void setDataverseOwnerAlias(final String alias) {
        this.dataverseOwnerAlias = alias;
    }

    // -------------------- PRIVATE --------------------

    private boolean isPrefillEnabled() {
        return this.config.isTrueForKey(CollectionFormPrefillEnabled);
    }
    
    public boolean canUserPublishCollection() {
    	return this.permissionsWrapper.canIssuePublishDataverseCommand(this.dataverse.getOwner());
    }
}
