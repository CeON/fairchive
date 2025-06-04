package edu.harvard.iq.dataverse.bannersandmessages;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.bannersandmessages.banners.BannerDAO;
import edu.harvard.iq.dataverse.bannersandmessages.banners.dto.ImageWithLinkDto;
import edu.harvard.iq.dataverse.bannersandmessages.messages.DataverseTextMessageServiceBean;
import org.omnifaces.cdi.ViewScoped;

import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.List;

/**
 * Responsible for displaying messages and banners across the dataverse.
 */
@SuppressWarnings("serial")
@ViewScoped
@Named("MessagesAndBannersFragment")
public class MessagesAndBannersFragment implements java.io.Serializable {

    @EJB
    private DataverseTextMessageServiceBean textMessageService;

    @Inject
    private BannerDAO bannerDAO;

    @Inject
    private DataverseSession dataverseSession;


    public List<String> textMessages(Long dataverseId) {
        return textMessageService.getTextMessagesForDataverse(dataverseId, 
                dataverseSession.getLocaleCode());
    }

    public List<ImageWithLinkDto> banners(Long dataverseId) {
        return bannerDAO.getBannersForDataverse(dataverseId, 
                dataverseSession.getLocaleCode());
    }

    public void redirect(String link) throws IOException {

        if (!link.startsWith("http")) {
            link = "http://".concat(link);
        }
        FacesContext.getCurrentInstance().getExternalContext().redirect(link);
    }
}
