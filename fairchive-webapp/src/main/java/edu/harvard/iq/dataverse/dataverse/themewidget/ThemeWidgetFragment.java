package edu.harvard.iq.dataverse.dataverse.themewidget;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.list;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
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

import org.apache.commons.lang3.StringUtils;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseThemeCommand;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Try;

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
    private static final Logger logger = Logger.getLogger(ThemeWidgetFragment.class.getCanonicalName());


    private Path tempDir;
    private Path uploadedFile;
    private Dataverse editDv = new Dataverse();
    private HtmlInputText linkUrlInput;
    private HtmlInputText taglineInput;


    @EJB
    DataverseDao dataverseDao;
    @Inject
    SettingsServiceBean settingsService;
    @Inject
    private ThemeWidgetService themeWidgetService;


    @Inject
    PermissionsWrapper permissionsWrapper;


    public HtmlInputText getLinkUrlInput() {
        return linkUrlInput;
    }

    public void setLinkUrlInput(HtmlInputText linkUrlInput) {
        this.linkUrlInput = linkUrlInput;
    }

    public HtmlInputText getTaglineInput() {
        return taglineInput;
    }

    public void setTaglineInput(HtmlInputText taglineInput) {
        this.taglineInput = taglineInput;
    }


    private void createTempDir() {
        try {
            Path tempRoot = createDirectories(Paths.get("../docroot/logos/temp"));
            this.tempDir = createTempDirectory(tempRoot, editDv.getId().toString());
        } catch (IOException e) {
            throw new RuntimeException("Error creating temp directory", e); // improve error handling
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
        uploadedFile = null;
        tempDir = null;
    }

    public void checkboxListener() {
        // not sure if this is needed for the ajax component
    }


    public String initEditDv() {
        editDv = dataverseDao.find(editDv.getId());

        // check if dv exists and user has permission
        if (editDv == null) {
            return permissionsWrapper.notFound();
        }
        if (!permissionsWrapper.canIssueCommand(editDv, UpdateDataverseThemeCommand.class)) {
            return permissionsWrapper.notAuthorized();
        }


        if (editDv.isRoot()) {
            editDv.setThemeRoot(true);
        }
        if (editDv.getDataverseTheme() == null && editDv.isThemeRoot()) {
            editDv.getLatestVersion().setTheme( new DataverseTheme());

        }
        return null;
    }

    public Dataverse getEditDv() {
        return editDv;
    }

    public void setEditDv(Dataverse editDV) {
        this.editDv = editDV;


    }

    public void validateTagline(FacesContext context, UIComponent component, Object value) throws ValidatorException {

        if (!StringUtils.isEmpty((String) value) && ((String) value).length() > 140) {
            FacesMessage msg = new FacesMessage(BundleUtil.getStringFromBundle("theme.validateTagline"));
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);

            throw new ValidatorException(msg);
        }

    }

    public void validateUrl(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        try {
            if (!StringUtils.isEmpty((String) value)) {
                new URL((String) value);
            }
        } catch (MalformedURLException e) {
            FacesMessage msg
                    = new FacesMessage(BundleUtil.getStringFromBundle("theme.urlValidate"),
                                       BundleUtil.getStringFromBundle("theme.urlValidate.msg"));
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);

            throw new ValidatorException(msg);
        }
    }


    public String getTempDirName() {
        return this.tempDir != null ? this.tempDir.getFileName().toString() : null;
    }

    public boolean uploadExists() {
        return uploadedFile != null;
    }

    /**
     * Copy uploaded file to temp area, until we are ready to save
     * Copy filename into Dataverse logo
     *
     * @param event
     */

    public void handleImageFileUpload(FileUploadEvent event) {

        logger.finer("entering fileUpload");
        if (this.tempDir == null) {
            createTempDir();
            logger.finer("created tempDir");
        }
        UploadedFile uFile = event.getFile();
        try {
            uploadedFile = tempDir.resolve(uFile.getFileName());
            if (!exists(uploadedFile)) {
                createFile(uploadedFile);
            }
            logger.finer("created file");
            Files.copy(uFile.getInputStream(), uploadedFile, REPLACE_EXISTING);
            logger.finer("copied inputstream to file");
            editDv.getDataverseTheme().setLogo(uFile.getFileName());

        } catch (IOException e) {
            logger.finer("caught IOException");
            logger.throwing("ThemeWidgetFragment", "handleImageFileUpload", e);
            throw new RuntimeException("Error uploading logo file", e); // improve error handling
        }
        // If needed, set the default values for the logo
        if (editDv.getDataverseTheme().getLogoFormat() == null) {
            editDv.getDataverseTheme().setLogoFormat(DataverseTheme.ImageFormat.SQUARE);
        }
        logger.finer("end handelImageFileUpload");
    }

    public void removeLogo() {
        editDv.getDataverseTheme().setLogo(null);
        this.cleanupTempDirectory();

    }

    public boolean getInheritCustomization() {
        return editDv == null || !editDv.getThemeRoot();
    }

    public void setInheritCustomization(boolean inherit) {
        editDv.setThemeRoot(!inherit);
        if (!inherit) {
            if (editDv.getDataverseTheme(true) == null) {
                editDv.getLatestVersion().setTheme(new DataverseTheme());
            }
        }
    }

    public void resetForm() {
        PrimeFaces.current().resetInputs(":dataverseForm:themeWidgetsTabView");
    }

    public String cancel() {
        return "dataverse.xhtml?faces-redirect=true&alias=" + editDv.getAlias();  // go to dataverse page
    }

    public boolean isThemeEnabled() {
        if (editDv.isRoot()) {
            return !settingsService.isTrueForKey(Key.DisableRootDataverseTheme);
        }
        return true;
    }


    public String save() {
        Try<Dataverse> saveOperation;

        if (editDv.isThemeRoot()) {
            saveOperation = Try.of(() -> themeWidgetService.saveOrUpdateUploadedTheme(editDv, uploadedFile.toFile()));
        } else {
            saveOperation = Try.of(() -> themeWidgetService.inheritThemeFromRoot(editDv))
                    .onSuccess(editedDataverse -> editDv = editedDataverse);
        }

        saveOperation.andThen(dataverse -> this.cleanupTempDirectory());

        if (saveOperation.isSuccess()) {
            JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.theme.success"));
            return "dataverse.xhtml?faces-redirect=true&alias=" + editDv.getAlias();
        } else {
            logger.log(Level.SEVERE, "error updating dataverse theme", saveOperation.getCause());
            JsfHelper.addErrorMessage(
                    BundleUtil.getStringFromBundle("dataverse.save.failed"),
                    BundleUtil.getStringFromBundle("dataverse.theme.failure"));

            return null;
        }
    }
}



