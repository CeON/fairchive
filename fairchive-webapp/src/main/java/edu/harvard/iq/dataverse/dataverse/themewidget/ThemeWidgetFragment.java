package edu.harvard.iq.dataverse.dataverse.themewidget;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion.State.RELEASED;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DisableRootDataverseTheme;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.list;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getLogger;
import static javax.faces.application.FacesMessage.SEVERITY_ERROR;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.dataverse.DataverseService;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseThemeCommand;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme.Alignment;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme.ImageFormat;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;

/**
 * @author ellenk
 */
@SuppressWarnings("serial")
@ViewScoped
@Named
public class ThemeWidgetFragment implements java.io.Serializable {
    static final String DEFAULT_LOGO_BACKGROUND_COLOR = "FFFFFF";
    static final String DEFAULT_BACKGROUND_COLOR = "FFFFFF";
    static final String DEFAULT_LINK_COLOR = "428BCA";
    static final String DEFAULT_TEXT_COLOR = "888888";
    private static final Logger logger = getLogger(ThemeWidgetFragment.class.getCanonicalName());

    private Path tempDir;
    private Path uploadedFile;
    private Dataverse dataverse = new Dataverse();
    private DataverseVersion version;
    private HtmlInputText linkUrlInput;
    private HtmlInputText taglineInput;

    @EJB
    private DataverseDao dataverseDao;
    @Inject
    private SystemConfig config;
    @Inject
    private ThemeWidgetService themeWidgetService;
    @Inject
    private DataverseService dataverseService;
    @Inject
    private PermissionsWrapper permissionsWrapper;

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
    	return isNotEmpty(this.version.getTheme().getLogo());
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

    public String init() {
        this.dataverse = this.dataverseDao.find(this.dataverse.getId());

        // check if dv exists and user has permission
        if (this.dataverse == null) {
            return this.permissionsWrapper.notFound();
        }
        
        if (!this.permissionsWrapper.canIssueCommand(this.dataverse, 
        		UpdateDataverseThemeCommand.class)) {
            return this.permissionsWrapper.notAuthorized();
        }

        this.version = this.dataverse.getEditableVersion();

        if (this.dataverse.isRoot()) {
            this.dataverse.setThemeRoot(true);
        }
        
        if (this.dataverse.getDataverseTheme() == null && this.dataverse.isThemeRoot()) {
            this.version.setTheme( new DataverseTheme());

        }
        return null;
    }

    public Dataverse getDataverse() {
        return this.dataverse;
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

    private String getTempDirName() {
        return this.tempDir != null ? this.tempDir.getFileName().toString().replace("\\", "/") : null;
    }

    /**
     * Copy uploaded file to temp area, until we are ready to save
     * Copy filename into Dataverse logo
     *
     * @param event
     */

    public void handleImageFileUpload(final FileUploadEvent event) {

        logger.finer("entering fileUpload");
        if (this.tempDir == null) {
            createTempDir();
            logger.finer("created tempDir");
        }
        final UploadedFile uFile = event.getFile();
        try {
            this.uploadedFile = this.tempDir.resolve(uFile.getFileName());
            if (!exists(this.uploadedFile)) {
                createFile(this.uploadedFile);
            }
            logger.finer("created file");
            copy(uFile.getInputStream(), this.uploadedFile, REPLACE_EXISTING);
            logger.finer("copied inputstream to file");
            this.dataverse.getDataverseTheme().setLogo(uFile.getFileName());

        } catch (final IOException e) {
            logger.finer("caught IOException");
            logger.throwing("ThemeWidgetFragment", "handleImageFileUpload", e);
            throw new RuntimeException("Error uploading logo file", e); // improve error handling
        }
        logger.finer("end handelImageFileUpload");
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

    public String cancel() {
        return "dataverse.xhtml?faces-redirect=true&alias=".
        		concat(dataverse.getAlias());
    }

    public boolean displayThemeTab() {
        return this.dataverse.isRoot()
            ? !this.config.isTrueForKey(DisableRootDataverseTheme)
            : true;
    }
    
    public String saveAndRelease() {
    	this.version.setState(RELEASED);
    	return save();
    }

    public String save() {
        try {
        	this.dataverse.addVersion(this.version);
	        if (this.dataverse.isThemeRoot()) {
	            this.themeWidgetService.
	            		saveOrUpdateUploadedTheme(this.dataverse, this.uploadedFile.toFile());
	        } else {
	        	this.dataverse = this.themeWidgetService.inheritThemeFromRoot(this.dataverse);
	        }
	        this.dataverseService.saveEditedDataverse(this.dataverse);
            JsfHelper.addFlashSuccessMessage(getStringFromBundle("dataverse.theme.success"));
            return "dataverse.xhtml?faces-redirect=true&alias=".
            		concat(dataverse.getAlias());
        } catch(final Exception e) {
            logger.log(SEVERE, "error updating dataverse theme", e);
            JsfHelper.addErrorMessage(getStringFromBundle("dataverse.save.failed"),
                    				  getStringFromBundle("dataverse.theme.failure"));
            return null;
        } finally {
        	this.cleanupTempDirectory();
        }
    }
    
    public boolean canUserPublishCollection() {
    	return this.permissionsWrapper.canIssuePublishDataverseCommand(this.dataverse.getOwner());
    }
}
