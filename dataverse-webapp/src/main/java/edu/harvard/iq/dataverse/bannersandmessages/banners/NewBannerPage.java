package edu.harvard.iq.dataverse.bannersandmessages.banners;

import static edu.harvard.iq.dataverse.bannersandmessages.validation.ImageValidator.imageExceedes;
import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages.DataverseLocalizedBanner.isOfAllowableType;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import edu.harvard.iq.dataverse.PermissionsWrapper;
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

    private final PermissionsWrapper permissionsWrapper;
    private final DataverseRepository dataverseRepo;
    private final BannerLimits bannerLimits;
    private final UIMessages uiMessages;
    private final DataverseBannerRepository bannerRepo;
    private final SettingsWrapper settingsWrapper;

    private Long dataverseId;
    private Dataverse dataverse;
    private final DataverseBanner banner = new DataverseBanner();

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

    @PostConstruct
    public void init() {
        this.settingsWrapper.getConfiguredLocales().keySet()
                .forEach(this.banner::addLocalizedBanner);
    }

    public String verifyAccess() {
        return this.dataverseId == null
                ? this.permissionsWrapper.notFound()
                : this.permissionsWrapper
                        .authorizedToEditTextMessagesAndBannersOf(this.dataverseId);
    }

    public DataverseBanner getBanner() {
        return this.banner;
    }

    public StreamedContent getBannerImage(
            final DataverseLocalizedBanner localizedBanner) {
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

    public Long getDataverseId() {
        return this.dataverseId;
    }

    public void setDataverseId(final Long dataverseId) {
        this.dataverseId = dataverseId;
        this.dataverse = this.dataverseRepo.findById(this.dataverseId).orElse(null);
    }

    public Dataverse getDataverse() {
        return this.dataverse;
    }

    public void uploadFileEvent(final FileUploadEvent event) {
        if (isFileCorrect(event)) {
            final String locale = (String) event.getComponent().getAttributes()
                    .get("imageLocale");

            this.banner.getBannerFor(locale).ifPresent(lb -> {
                lb.setContentType(event.getFile().getContentType());
                lb.setImageName(event.getFile().getFileName());
                lb.setImage(event.getFile().getContent());
            });
        }
    }

    private boolean isFileCorrect(final FileUploadEvent event) {
        boolean result = true;
        try {
            if (event.getFile().getSize() > this.bannerLimits.getMaxSizeInBytes()) {
                this.uiMessages.addComponentErrorMessage(event.getComponent(),
                        getStringFromBundle("dataversemessages.banners.sizeError"));
                result = false;
            }
            if (imageExceedes(event.getFile().getInputStream(), this.bannerLimits)) {
                this.uiMessages.addComponentErrorMessage(event.getComponent(),
                        getStringFromBundle(
                                "dataversemessages.banners.resolutionError"));
                result = false;
            }
            if (!isOfAllowableType(event.getFile().getFileName())) {
                this.uiMessages.addComponentErrorMessage(event.getComponent(),
                        getStringFromBundle(
                                "dataversemessages.banners.extensionError"));
                result = false;
            }
        } catch (final IOException e) {
            this.uiMessages.addComponentErrorMessage(event.getComponent(),
                    getStringFromBundle("dataversemessages.banners.formatError"));
            result = false;
        }
        return result;
    }

    public String save() {
        if (isDataCorrect()) {
            this.bannerRepo.save(banner);
            this.uiMessages.addFlashSuccessMessage(
                    getStringFromBundle("dataversemessages.banners.new.success"));
            return redirectToTextMessages();
        } else {
            return EMPTY;
        }
    }

    private boolean isDataCorrect() {
        boolean result = true;
        final List<DataverseLocalizedBanner> banners = this.banner
                .getLocalizedBanners();
        for (int index = 0; index < banners.size(); ++index) {
            if (!banners.get(index).isImagePresent()) {
                this.uiMessages.addComponentErrorMessage(
                        "edit-text-messages-form:repeater:" + index + ":upload",
                        getStringFromBundle(
                                "dataversemessages.banners.missingError"));
                result = false;
            }
            if (!banners.get(index).isImageLinkValid()) {
                this.uiMessages.addComponentErrorMessage(
                        "edit-text-messages-form:repeater:" + index
                                + ":message-link",
                        getStringFromBundle("textmessages.url.invalid"));
                result = false;
            }
        }
        ;
        if (!this.banner.isFromTimePresent()) {
            this.uiMessages.addComponentErrorMessage(
                    "edit-text-messages-form:message-fromtime",
                    getStringFromBundle("field.required"));
            result = false;
        }
        if (!this.banner.isToTimePresent()) {
            this.uiMessages.addComponentErrorMessage(
                    "edit-text-messages-form:message-totime",
                    getStringFromBundle("field.required"));
            result = false;
        }
        if (!this.banner.isFromTimeBeforeEndTime()) {
            this.uiMessages.addComponentErrorMessage(
                    "edit-text-messages-form:message-totime",
                    getStringFromBundle("textmessages.endDateTime.valid"));
            result = false;
        }
        if (!this.banner.isToTimeInFuture()) {
            this.uiMessages.addComponentErrorMessage(
                    "edit-text-messages-form:message-totime",
                    getStringFromBundle("textmessages.endDateTime.future"));
            result = false;
        }
        return result;
    }

    public String cancel() {
        return redirectToTextMessages();
    }

    private String redirectToTextMessages() {
        return "/dataverse-textMessages.xhtml?activeTab=banners&faces-redirect=true&dataverseId="
                + this.dataverseId;
    }
}
