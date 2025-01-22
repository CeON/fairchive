package edu.harvard.iq.dataverse.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import edu.harvard.iq.dataverse.search.response.SolrSearchResult;

/**
 * Results of the last performed search by the user.
 * Existence of this bean is needed since {@link SearchIncludeFragment}
 * bean is request scoped. When we perform some action on the search page
 * (for example clicking on some button) we sometimes still need for search
 * results to be present in the jsf context.
 */
@SessionScoped
@Named("lastSearchValue")
public class LastSearchValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<SolrSearchResult> searchResultsList = new ArrayList<>();
    private boolean rootDv;
    private long dataverseId;

    public List<SolrSearchResult> getSearchResultsList() {

        return this.searchResultsList;
    }

    /**
     * Returns true if last search was inside root dataverse
     */
    public boolean isRootDv() {

        return rootDv;
    }

    /**
     * Returns id of dataverse from which last search was performed
     */
    public long getDataverseId() {

        return dataverseId;
    }

    public void setSearchResultsList(List<SolrSearchResult> searchResultsList) {

        this.searchResultsList = searchResultsList;
    }

    public void setRootDv(boolean rootDv) {

        this.rootDv = rootDv;
    }

    public void setDataverseId(long dataverseId) {

        this.dataverseId = dataverseId;
    }
}
