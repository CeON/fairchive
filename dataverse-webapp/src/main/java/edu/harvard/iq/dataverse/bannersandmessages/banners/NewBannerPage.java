package edu.harvard.iq.dataverse.bannersandmessages.banners;

import static edu.harvard.iq.dataverse.bannersandmessages.validation.ImageValidator.imageExceedes;
import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.persistence.config.URLValidator.isURLValid;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.io.IOException;
import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.bannersandmessages.validation.DataverseTextMessageValidator;
import edu.harvard.iq.dataverse.bannersandmessages.validation.EndDateMustBeAFutureDate;
import edu.harvard.iq.dataverse.bannersandmessages.validation.EndDateMustNotBeEarlierThanStartingDate;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages.DataverseBanner;
import edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages.DataverseBannerRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages.DataverseLocalizedBanner;
import edu.harvard.iq.dataverse.settings.SettingsWrapper;
import edu.harvard.iq.dataverse.util.UIMessages;

@SuppressWarnings("serial")
@ViewScoped
@Named
public class NewBannerPage implements Serializable {

    private PermissionsWrapper permissionsWrapper;
    private DataverseRepository dataverseRepo;
    private BannerLimits bannerLimits;
    private UIMessages uiMessages;
    private DataverseBannerRepository bannerRepo;
    private SettingsWrapper settingsWrapper;
    
    private Long dataverseId;
    private Dataverse dataverse;
    
    private DataverseBanner banner;
    
    @Inject
    public NewBannerPage(final PermissionsWrapper permissionsWrapper,
            final DataverseRepository dataverseRepo,
            final BannerLimits bannerLimits,
            final UIMessages uiMessages,
            final DataverseBannerRepository bannerRepo, 
            final SettingsWrapper settingsWrapper) {
        this.permissionsWrapper = permissionsWrapper;
        this.dataverseRepo = dataverseRepo;
        this.bannerLimits = bannerLimits;
        this.uiMessages = uiMessages;
        this.bannerRepo = bannerRepo;
        this.settingsWrapper = settingsWrapper;
    }
    
    public DataverseBanner getBanner() {
        return this.banner;
    }

    public String init() {
        if (!permissionsWrapper.canEditDataverseTextMessagesAndBanners(dataverseId)) {
            return permissionsWrapper.notAuthorized();
        }

        if (dataverseId == null) {
            return permissionsWrapper.notFound();
        }

        this.dataverse = this.dataverseRepo.findById(this.dataverseId).get();
    
        this.banner = new DataverseBanner();
        for (final String locale : this.settingsWrapper.getConfiguredLocales()
                .keySet()) {
            this.banner.addLocalizedBanner(locale);
        }

        return EMPTY;
    }
    
    public StreamedContent getBannerImage(final DataverseLocalizedBanner localizedBanner) {
        if (localizedBanner.isImagePresent()) {
            return DefaultStreamedContent.builder()
                    .contentType(localizedBanner.getContentType())
                    .name(localizedBanner.getImageName())
                    .stream(localizedBanner::getImageAsStream)
                    .build();
        } else {
            return null;
        }
    }
    
    public void uploadFileEvent(final FileUploadEvent event) {
        try {
            if (imageExceedes(event.getFile().getInputStream(),
                    this.bannerLimits.getMaxWidth(), this.bannerLimits.getMaxHeight())) {
                this.uiMessages.addComponentErrorMessage(event.getComponent(),
                        getStringFromBundle("messages.error"),
                        getStringFromBundle(
                                "dataversemessages.banners.resolutionError"));
            } else {
                String locale = (String) event.getComponent().getAttributes()
                        .get("imageLocale");
              
                this.banner.getBannerFor(locale).ifPresent(lb -> {
                    lb.setContentType(event.getFile().getContentType());
                    lb.setImageName(event.getFile().getFileName());
                    lb.setImage(event.getFile().getContent());
                });
                
            }
        } catch (final IOException e) {
            this.uiMessages.addComponentErrorMessage(event.getComponent(),
                    getStringFromBundle("messages.error"),
                    getStringFromBundle("dataversemessages.banners.formatError"));
        }
    }

    public String save() {      

        this.banner.getLocalizedBanners().forEach(lb -> {
            
            int localizedBannerIndex = banner.getLocalizedBanners().indexOf(lb);
            
            if(lb.getImageLink() != null && !isURLValid(lb.getImageLink())) {
                addLinkErrorMessage(localizedBannerIndex, "textmessages.url.invalid");
            }

            if (lb.getImage().length < 1) {
                addImageErrorMessage(localizedBannerIndex, "dataversemessages.banners.missingError");
            } 
        });

        if(this.banner.getFromTime() == null) {
            this.uiMessages.addComponentErrorMessage("edit-text-messages-form:message-fromtime", 
                    getStringFromBundle("messages.error"),
                    getStringFromBundle("field.required"));
        } 
           
        if(this.banner.getToTime() == null) {
            this.uiMessages.addComponentErrorMessage("edit-text-messages-form:message-totime", 
                    getStringFromBundle("messages.error"),
                    getStringFromBundle("field.required"));
        }      
        try {
            DataverseTextMessageValidator.validateEndDate(this.banner.getFromTime(), this.banner.getToTime());
        } catch (EndDateMustNotBeEarlierThanStartingDate e) {
            this.uiMessages.addComponentErrorMessage("edit-text-messages-form:message-totime", 
                    getStringFromBundle("messages.error"),
                    getStringFromBundle("textmessages.endDateTime.valid"));
        } catch (EndDateMustBeAFutureDate e) {
            this.uiMessages.addComponentErrorMessage("edit-text-messages-form:message-totime", 
                    getStringFromBundle("messages.error"),
                    getStringFromBundle("textmessages.endDateTime.future"));
        }
        
        if (errorsOccurred()) {
            return EMPTY;
        }
        
        this.bannerRepo.save(banner);
        this.uiMessages.addFlashSuccessMessage(getStringFromBundle("dataversemessages.banners.new.success"));
        return redirectToTextMessages();
    }
    
    private void addImageErrorMessage(final int index, final String key) {
        this.uiMessages.addComponentErrorMessage(
                "edit-text-messages-form:repeater:" + index + ":upload",
                getStringFromBundle("messages.error"),
                getStringFromBundle(key));
    }
    
    private void addLinkErrorMessage(final int index, final String key) {
        this.uiMessages.addComponentErrorMessage(
                "edit-text-messages-form:repeater:" + index + ":message-link",
                getStringFromBundle("messages.error"),
                getStringFromBundle(key));
    }
    
    public int getBannerFileSizeLimit() {
        return bannerLimits.getMaxSizeInBytes();
    }
    
    private boolean errorsOccurred() {
        return FacesContext.getCurrentInstance().getMessageList().size() > 0;
    }

    public String cancel() {
        return redirectToTextMessages();
    }

    private String redirectToTextMessages() {
        return "/dataverse-textMessages.xhtml?activeTab=banners&faces-redirect=true&dataverseId="
                + this.dataverseId;
    }

    public PermissionsWrapper getPermissionsWrapper() {
        return permissionsWrapper;
    }

    public void setPermissionsWrapper(PermissionsWrapper permissionsWrapper) {
        this.permissionsWrapper = permissionsWrapper;
    }

    public Long getDataverseId() {
        return dataverseId;
    }

    public void setDataverseId(Long dataverseId) {
        this.dataverseId = dataverseId;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }
}
