package edu.harvard.iq.dataverse.dataverse;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.util.JsfRedirectHelper.redirectToDataverse;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.slf4j.Logger;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.link.DataverseLinkingDataverse;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchService;
import edu.harvard.iq.dataverse.util.UIMessages;

@SuppressWarnings("serial")
@ViewScoped
@Named("LinkToDataverseDialog")
public class LinkToDataverseDialog implements java.io.Serializable {

    private static final Logger logger = getLogger(LinkToDataverseDialog.class);
    
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
        return this.canLinkDataverse;
    }
    
    public boolean isCanLinkSavedSearch() {
        return this.canLinkSavedSearch;
    }
    
    public LinkMode getLinkMode() {
        return this.linkMode;
    }
    
    public String getSearchQuery() {
        return this.searchQuery;
    }
    
    public List<String> getSearchFilterQueriesDebug() {
        return this.searchFilterQueriesDebug;
    }
    
    public List<SelectItem> getLinkingDVSelectItems() {
        return this.linkingDVSelectItems;
    }
    
    public Long getLinkingDataverseId() {
        return this.linkingDataverseId;
    }
    
    public String getTitle() {
    	return this.linkMode == LinkMode.SAVEDSEARCH 
    			? getStringFromBundle("dataverse.savedsearch.link") 
    			: getStringFromBundle("dataverse.link");
    }
    
    // -------------------- LOGIC --------------------

    public void init(final Dataverse dataverse, final String searchQuery, 
    		final List<String> searchFilterQueriesDebug) {
        this.canLinkDataverse = this.session.isSuperUserLoggedIn() && !dataverse.isRoot();
        this.canLinkSavedSearch = this.session.isSuperUserLoggedIn() && isNotEmpty(searchQuery);

        if (this.canLinkDataverse || this.canLinkSavedSearch) {
            this.dataverse = dataverse;
            this.searchQuery = searchQuery;
            this.searchFilterQueriesDebug = searchFilterQueriesDebug;
        }
    }

    public void setupDialogForDataverseLinking() {
        this.linkMode = LinkMode.LINKDATAVERSE;
        updateLinkableDataverses();
    }
    public void setupDialogForSavedSearchLinking() {
        this.linkMode = LinkMode.SAVEDSEARCH;
        updateLinkableDataverses();
    }
    
    public String saveLinkedDataverse() {
        if (this.linkingDataverseId == null) {
            this.uiMessages.addErrorMessage(getStringFromBundle("dataverse.link.select"));
            return EMPTY;
        }

        if (this.session.isUserLoggedIn()) {
            final String msg = getStringFromBundle("dataverse.link.user");
            logger.error(msg);
            this.uiMessages.addFlashErrorMessage(msg);
            return redirectToDataverse(dataverse.getAlias());
        }

        try {
        	final DataverseLinkingDataverse linkingDataverse = 
        			linkingService.saveLinkedDataverse(
        					dataverseRepository.getById(linkingDataverseId), dataverse);
            this.uiMessages.addFlashSuccessMessage(
            		getStringFromBundle("dataverse.linked.success.wait", 
            				getSuccessMessageArguments(linkingDataverse.getLinkingDataverse())));
        } catch(final Exception e) {
    		final String msg = getStringFromBundle("dataverse.link.error", 
    				this.dataverse.getDisplayName());
    		this.uiMessages.addFlashErrorMessage(msg);
    		logger.error("Unable to link dataverse with id: " + 
    				this.dataverse.getId() + " to " + linkingDataverseId, e);
        }

        return redirectToDataverse(dataverse.getAlias());
    }
    
    public String saveSavedSearch() {
        if (this.linkingDataverseId == null) {
        	this.uiMessages.addErrorMessage(getStringFromBundle("dataverse.link.select"));
            return EMPTY;
        }
        this.targetDataverseLink = this.dataverseRepository.getById(this.linkingDataverseId);

        if (this.session.isUserLoggedIn()) {
            final String msg = getStringFromBundle("dataverse.search.user");
            logger.error(msg);
            this.uiMessages.addFlashErrorMessage(msg);
            return redirectToDataverse(dataverse.getAlias());
        }

        try {
        	this.savedSearchService.saveSavedDataverseSearch(
        			this.searchQuery, this.searchFilterQueriesDebug, this.targetDataverseLink);
            final String hrefArgument = "<a href=\"/dataverse/" + 
            		targetDataverseLink.getAlias() + '"' + '>' +
            		escapeHtml4(targetDataverseLink.getDisplayName()) + "</a>";
            this.uiMessages.addFlashSuccessMessage(
            		getStringFromBundle("dataverse.saved.search.success", hrefArgument));
        } catch (final Exception e) {
            logger.error("There was a problem linking this search", e);
            this.uiMessages.addFlashErrorMessage(getStringFromBundle("dataverse.saved.search.failure") + ' ' + e);
        }

        return redirectToDataverse(dataverse.getAlias());
    }

    // -------------------- PRIVATE --------------------

    private void updateLinkableDataverses() {
        this.linkingDVSelectItems = new ArrayList<>();

        //Since only a super user function add all dvs
        final List<Dataverse> dataversesForLinking = this.dataverseRepository.findAll();


        //for linking - make sure the link hasn't occurred and its not int the tree
        if (this.linkMode.equals(LinkMode.LINKDATAVERSE)) {

            // remove this and it's parent tree
            Dataverse testDV = this.dataverse;
            while (testDV != null) {
                dataversesForLinking.remove(testDV);
                testDV = testDV.getOwner();
            }

            for (final Dataverse removeLinked : this.linkingService.
            		findLinkingDataverses(this.dataverse.getId())) {
                dataversesForLinking.remove(removeLinked);
            }
        }


        for (final Dataverse selectDV : dataversesForLinking) {
            this.linkingDVSelectItems.add(new SelectItem(selectDV.getId(),
                    selectDV.getDisplayName() + ' ' + '(' + getStringFromBundle("dataverse.alias")
                            + ':' + ' ' + selectDV.getAlias() + ')'));
        }

        if (dataversesForLinking.size() == 1 && dataversesForLinking.get(0) != null) {
            this.targetDataverseLink = dataversesForLinking.get(0);
            this.linkingDataverseId = this.targetDataverseLink.getId();
        }
    }


    private Object[] getSuccessMessageArguments(final Dataverse savedTargetDataverseLink) {
        final Object[] result = new Object[2];
        result[0] = escapeHtml4(this.dataverse.getDisplayName());
        result[1] = "<a href=\"/dataverse/" + savedTargetDataverseLink.getAlias() + 
        		'"' + '>' + escapeHtml4(savedTargetDataverseLink.getDisplayName()) + "</a>";
        return result;
    }

    // -------------------- SETTERS --------------------

    public void setLinkingDataverseId(final Long linkingDataverseId) {
        this.linkingDataverseId = linkingDataverseId;
    }
    
}
