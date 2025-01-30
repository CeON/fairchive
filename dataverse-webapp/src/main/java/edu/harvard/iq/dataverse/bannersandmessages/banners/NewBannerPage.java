package edu.harvard.iq.dataverse.bannersandmessages.banners;

import static edu.harvard.iq.dataverse.bannersandmessages.validation.ImageValidator.imageExceedes;
import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.persistence.config.URLValidator.isURLValid;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;

import javax.ejb.EJB;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.bannersandmessages.UnsupportedLanguageCleaner;
import edu.harvard.iq.dataverse.bannersandmessages.banners.dto.BannerMapper;
import edu.harvard.iq.dataverse.bannersandmessages.banners.dto.DataverseBannerDto;
import edu.harvard.iq.dataverse.bannersandmessages.banners.dto.DataverseLocalizedBannerDto;
import edu.harvard.iq.dataverse.bannersandmessages.validation.DataverseTextMessageValidator;
import edu.harvard.iq.dataverse.bannersandmessages.validation.EndDateMustBeAFutureDate;
import edu.harvard.iq.dataverse.bannersandmessages.validation.EndDateMustNotBeEarlierThanStartingDate;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages.DataverseBanner;
import edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages.DataverseLocalizedBanner;
import edu.harvard.iq.dataverse.util.UIMessages;

@SuppressWarnings("serial")
@ViewScoped
@Named("EditBannerPage")
public class NewBannerPage implements Serializable {

    @EJB
    private BannerDAO dao;

    @Inject
    private PermissionsWrapper permissionsWrapper;

    @Inject
    private BannerMapper mapper;

    @EJB
    private DataverseRepository dataverseRepo;

    @Inject
    private UnsupportedLanguageCleaner languageCleaner;

    @Inject
    private BannerLimits bannerLimits;
    
    @Inject
    private UIMessages uiMessages;
    
    
    private Long dataverseId;
    private Dataverse dataverse;
    private Long bannerId;
    private String link;

    private UIInput fromTimeInput;

    private DataverseBannerDto dto;

    public String init() {
        if (!permissionsWrapper.canEditDataverseTextMessagesAndBanners(dataverseId)) {
            return permissionsWrapper.notAuthorized();
        }

        if (dataverseId == null) {
            return permissionsWrapper.notFound();
        }

        this.dataverse = this.dataverseRepo.findById(this.dataverseId).get();

        dto = bannerId != null ?
                mapper.mapToDto(dao.getBanner(bannerId)) :
                mapper.mapToNewBanner(dataverseId);

        if (dto.getId() != null) {
            languageCleaner.removeBannersLanguagesNotPresentInDataverse(dto);
        }

        return EMPTY;
    }

    public boolean hasDisplayLocalizedBanner(DataverseLocalizedBannerDto localizedBanner) {
        return localizedBanner.getContent() != null;
    }
    
    public StreamedContent getDisplayLocalizedBanner(DataverseLocalizedBannerDto localizedBanner) {
        if (localizedBanner.getContent() == null) {
            return null;
        }
        return DefaultStreamedContent.builder()
                            .contentType(localizedBanner.getContentType())
                            .name(localizedBanner.getFilename())
                            .stream(() -> new ByteArrayInputStream(localizedBanner.getContent()))
                            .build();
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

                dto.getDataverseLocalizedBanner().stream()
                        .filter(dlb -> dlb.getLocale().equals(locale))
                        .forEach(dlb -> {
                            dlb.setContentType(event.getFile().getContentType());
                            dlb.setFilename(event.getFile().getFileName());
                            dlb.setContent(event.getFile().getContent());
                        });
            }
        } catch (final IOException e) {
            this.uiMessages.addComponentErrorMessage(event.getComponent(),
                    getStringFromBundle("messages.error"),
                    getStringFromBundle("dataversemessages.banners.formatError"));
        }
    }

    public String save() {      
        DataverseBanner banner =
                mapper.mapToEntity(dto, this.dataverseRepo.findById(dto.getDataverseId()).get());

        banner.getDataverseLocalizedBanner().forEach(dlb -> handleBannerAddingErrors(banner, dlb));

        if(this.dto.getFromTime() == null) {
            this.uiMessages.addComponentErrorMessage("edit-text-messages-form:message-fromtime", 
                    getStringFromBundle("messages.error"),
                    getStringFromBundle("field.required"));
        } 
           
        if(this.dto.getToTime() == null) {
            this.uiMessages.addComponentErrorMessage("edit-text-messages-form:message-totime", 
                    getStringFromBundle("messages.error"),
                    getStringFromBundle("field.required"));
        }      
        try {
            DataverseTextMessageValidator.validateEndDate(this.dto.getFromTime(), this.dto.getToTime());
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

        dao.save(banner);
        this.uiMessages.addFlashSuccessMessage(getStringFromBundle("dataversemessages.banners.new.success"));
        return redirectToTextMessages();
    }
    
    public void handleBannerAddingErrors(DataverseBanner banner,
            DataverseLocalizedBanner dlb) {

        int localizedBannerIndex = banner.getDataverseLocalizedBanner().indexOf(dlb);
        
        if(dlb.getImageLink().isPresent() && !isURLValid(dlb.getImageLink().get())) {
            addLinkErrorMessage(localizedBannerIndex, "textmessages.url.invalid");
        }

        if (dlb.getImage().length < 1) {
            addImageErrorMessage(localizedBannerIndex, "dataversemessages.banners.missingError");
        } 
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
        return "/dataverse-textMessages.xhtml?dataverseId=" + dataverseId + "&activeTab=banners&faces-redirect=true";
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

    public Long getBannerId() {
        return bannerId;
    }

    public void setBannerId(Long bannerId) {
        this.bannerId = bannerId;
    }

    public DataverseBannerDto getDto() {
        return dto;
    }

    public void setDto(DataverseBannerDto dto) {
        this.dto = dto;
    }

    public UIInput getFromTimeInput() {
        return fromTimeInput;
    }

    public void setFromTimeInput(UIInput fromTimeInput) {
        this.fromTimeInput = fromTimeInput;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
