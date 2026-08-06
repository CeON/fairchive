package edu.harvard.iq.dataverse.bannersandmessages;

import java.util.List;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.bannersandmessages.banners.BannerDAO;
import edu.harvard.iq.dataverse.bannersandmessages.banners.dto.ImageWithLinkDto;
import edu.harvard.iq.dataverse.bannersandmessages.messages.DataverseTextMessageServiceBean;

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

}
