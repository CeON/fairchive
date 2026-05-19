package edu.harvard.iq.dataverse.dataverse;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchService;
import edu.harvard.iq.dataverse.util.UIMessages;
import io.vavr.control.Try;
import org.omnifaces.cdi.ViewScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.util.JsfRedirectHelper.redirectToDataverse;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
@ViewScoped
@Named("LinkToDataverseDialog")
public class LinkToDataverseDialog implements java.io.Serializable {

    private static final Logger logger = LoggerFactory.getLogger(LinkToDataverseDialog.class);
    
    public enum LinkMode {
        SAVEDSEARCH, LINKDATAVERSE
    }

    @EJB
    private DataverseRepository dataverseRepository;
    @Inject
    private SavedSearchService savedSearchService;
    @EJB
    private DataverseLinkingService linkingService;
    @Inject
    private DataverseSession session;
    @Inject 
    private UIMessages uiMessages;
    
    private boolean canLinkDataverse;
    private boolean canLinkSavedSearch;
    
    private LinkMode linkMode;
    
    private Dataverse dataverse;
    private String searchQuery;
    private List<String> searchFilterQueriesDebug;

    private Long linkingDataverseId;
    private List<SelectItem> linkingDVSelectItems;
    private Dataverse targetDataverseLink;


    // -------------------- GETTERS --------------------

    public boolean isCanLinkDataverse() {
        return canLinkDataverse;
    }
    public boolean isCanLinkSavedSearch() {
        return canLinkSavedSearch;
    }
    public LinkMode getLinkMode() {
        return linkMode;
    }
    public String getSearchQuery() {
        return searchQuery;
    }
    public List<String> getSearchFilterQueriesDebug() {
        return searchFilterQueriesDebug;
    }
    public List<SelectItem> getLinkingDVSelectItems() {
        return linkingDVSelectItems;
    }
    public Long getLinkingDataverseId() {
        return linkingDataverseId;
    }
    
    // -------------------- LOGIC --------------------

    public void init(Dataverse dataverse, String searchQuery, List<String> searchFilterQueriesDebug) {
        canLinkDataverse = session.isSuperUserLoggedIn() && !dataverse.isRoot();
        canLinkSavedSearch = session.isSuperUserLoggedIn() && isNotEmpty(searchQuery);

        if (canLinkDataverse || canLinkSavedSearch) {
            this.dataverse = dataverse;
            this.searchQuery = searchQuery;
            this.searchFilterQueriesDebug = searchFilterQueriesDebug;
        }
    }

    public void setupDialogForDataverseLinking() {
        linkMode = LinkMode.LINKDATAVERSE;
        updateLinkableDataverses();
    }
    public void setupDialogForSavedSearchLinking() {
        linkMode = LinkMode.SAVEDSEARCH;
        updateLinkableDataverses();
    }
    
    public String saveLinkedDataverse() {

        if (linkingDataverseId == null) {
            this.uiMessages.addErrorMessage(getStringFromBundle("dataverse.link.select"));
            return EMPTY;
        }

        if (this.session.isUserLoggedIn()) {
            String msg = getStringFromBundle("dataverse.link.user");
            logger.error(msg);
            this.uiMessages.addFlashErrorMessage(msg);
            return redirectToDataverse(dataverse.getAlias());
        }

        Try.of(() -> linkingService.saveLinkedDataverse(dataverseRepository.getById(linkingDataverseId), dataverse))
                .onFailure(ex -> handleSaveLinkedDataverseExceptions(ex, linkingDataverseId))
                .onSuccess(savedLinkingDv -> {
                    Dataverse savedTargetDataverseLink = savedLinkingDv.getLinkingDataverse();

                    this.uiMessages.addFlashSuccessMessage(getStringFromBundle("dataverse.linked.success.wait", 
                    		getSuccessMessageArguments(savedTargetDataverseLink)));
                });

        return redirectToDataverse(dataverse.getAlias());
    }
    
    public String saveSavedSearch() {
        if (linkingDataverseId == null) {
        	this.uiMessages.addErrorMessage(getStringFromBundle("dataverse.link.select"));
            return EMPTY;
        }
        targetDataverseLink = dataverseRepository.getById(linkingDataverseId);

        if (this.session.isUserLoggedIn()) {
            String msg = getStringFromBundle("dataverse.search.user");
            logger.error(msg);
            this.uiMessages.addFlashErrorMessage(msg);
            return redirectToDataverse(dataverse.getAlias());
        }


        Try.of(() -> savedSearchService.saveSavedDataverseSearch(searchQuery, searchFilterQueriesDebug, targetDataverseLink))
                .onSuccess(savedSearch -> {
                    String hrefArgument = "<a href=\"/dataverse/" + 
                    		targetDataverseLink.getAlias() + '"' + '>' +
                    		escapeHtml4(targetDataverseLink.getDisplayName()) + "</a>";
                    this.uiMessages.addFlashSuccessMessage(getStringFromBundle("dataverse.saved.search.success", hrefArgument));
                })
                .onFailure(ex -> {
                    logger.error("There was a problem linking this search", ex);
                    this.uiMessages.addFlashErrorMessage(getStringFromBundle("dataverse.saved.search.failure") + ' ' + ex);
                });


        return redirectToDataverse(dataverse.getAlias());
    }

    // -------------------- PRIVATE --------------------

    private void updateLinkableDataverses() {
        linkingDVSelectItems = new ArrayList<>();

        //Since only a super user function add all dvs
        List<Dataverse> dataversesForLinking = dataverseRepository.findAll();


        //for linking - make sure the link hasn't occurred and its not int the tree
        if (this.linkMode.equals(LinkMode.LINKDATAVERSE)) {

            // remove this and it's parent tree
            Dataverse testDV = dataverse;
            while (testDV != null) {
                dataversesForLinking.remove(testDV);
                testDV = testDV.getOwner();
            }

            for (Dataverse removeLinked : linkingService.findLinkingDataverses(dataverse.getId())) {
                dataversesForLinking.remove(removeLinked);
            }
        }


        for (Dataverse selectDV : dataversesForLinking) {
            linkingDVSelectItems.add(new SelectItem(selectDV.getId(),
                    selectDV.getDisplayName() + ' ' + '(' + getStringFromBundle("dataverse.alias")
                            + ':' + ' ' + selectDV.getAlias() + ')'));
        }

        if (dataversesForLinking.size() == 1 && dataversesForLinking.get(0) != null) {
            targetDataverseLink = dataversesForLinking.get(0);
            linkingDataverseId = targetDataverseLink.getId();
        }
    }
    

    private void handleSaveLinkedDataverseExceptions(Throwable ex, long dataverseToLinkId) {
        String msg = getStringFromBundle("dataverse.link.error", dataverse.getDisplayName());
        this.uiMessages.addFlashErrorMessage(msg);

        logger.error("Unable to link dataverse with id: " + dataverse.getId() +
        		" to " + dataverseToLinkId, ex);
    }


    private Object[] getSuccessMessageArguments(Dataverse savedTargetDataverseLink) {
        final Object[] result = new Object[2];
        result[0] = escapeHtml4(this.dataverse.getDisplayName());
        result[1] = "<a href=\"/dataverse/" + savedTargetDataverseLink.getAlias() + 
        		'"' + '>' + escapeHtml4(savedTargetDataverseLink.getDisplayName()) + "</a>";
        return result;
    }

    // -------------------- SETTERS --------------------

    public void setLinkingDataverseId(Long linkingDataverseId) {
        this.linkingDataverseId = linkingDataverseId;
    }
    
}
