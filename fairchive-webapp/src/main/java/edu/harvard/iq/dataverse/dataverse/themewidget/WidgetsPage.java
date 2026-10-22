package edu.harvard.iq.dataverse.dataverse.themewidget;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;

import java.io.Serializable;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;

import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.dataverse.DataverseService;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseThemeCommand;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.settings.SettingsWrapper;


@SuppressWarnings("serial")
@ViewScoped
@Named
public class WidgetsPage implements Serializable {

    private Dataverse dataverse;
    private Long dataverseId;

    private DataverseService dataverseService;
    private PermissionsWrapper permissionsWrapper;
    private SettingsWrapper settingsWrapper;
    
    public WidgetsPage() {}

    @Inject
    public WidgetsPage(final DataverseService dataverseService, 
    				   final PermissionsWrapper permissionsWrapper,
    				   final SettingsWrapper settingsWrapper) {

		this.dataverseService = dataverseService;
		this.permissionsWrapper = permissionsWrapper;
		this.settingsWrapper = settingsWrapper;
	}
    
	public Long getDataverseId() {
		return this.dataverseId;
	}

	public void setDataverseId(final Long id) {
		this.dataverseId = id;
	}
	
    public String init() {
        final Optional<Dataverse> dataverse = this.dataverseService.findById(this.dataverseId);

        if (! dataverse.isPresent()) {
            return this.permissionsWrapper.notFound();
        }
        
        this.dataverse = dataverse.get();
        
        if (!this.permissionsWrapper.canIssueCommand(this.dataverse, 
        		UpdateDataverseThemeCommand.class)) {
            return this.permissionsWrapper.notAuthorized();
        }
        
        return null;
    }

    public String getTitle() {
    	return getStringFromBundle("dataverse.widgets.title") + " - " + 
    			this.dataverse.getName();
    }
	
    public String getGuidesBaseUrl() {
        return this.settingsWrapper.getGuidesBaseUrl();
    }

    public String getGuidesVersion() {
        return this.settingsWrapper.getGuidesVersion();
    }
    
    public boolean isDataverseReleased() {
    	return this.dataverse.isReleased();
    }

    public Dataverse getDataverse() {
        return this.dataverse;
    }

    public String cancel() {
        return "dataverse.xhtml?faces-redirect=true&alias=".
        		concat(this.dataverse.getAlias());
    }
    
    public String getSearchText() {
    	final String url = this.settingsWrapper.getDataverseSiteUrl();
    	final String alias = this.dataverse.getAlias();
    	
    	return "<script src=\"" + url +
    			"/resources/js/widgets.js?alias=" + alias + 
    			"&amp;dvUrl=" + url  +
                "&amp;widget=search&amp;text=Search+my+collection&amp;buttonText=Find\"></script>";
    }
    
    public String getListingText() {
    	final String url = this.settingsWrapper.getDataverseSiteUrl();
    	final String alias = this.dataverse.getAlias();
    	
    	return "<script src=\"" + url +
    			"/resources/js/widgets.js?alias=" + alias +
    			"&amp;dvUrl=" + url +
    			"&amp;widgetScope=" + alias +
    			"&amp;widget=iframe&amp;heightPx=500\"></script>";
    }
}
