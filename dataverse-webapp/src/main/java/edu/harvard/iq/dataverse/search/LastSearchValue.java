package edu.harvard.iq.dataverse.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import edu.harvard.iq.dataverse.search.response.SolrSearchResult;

@SessionScoped
@Named("lastSearchValue")
public class LastSearchValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<SolrSearchResult> searchResultsList = new ArrayList<>();

    // -------------------------------------------------------------------------
    public List<SolrSearchResult> getSearchResultsList() {

        return this.searchResultsList;
    }

    // -------------------------------------------------------------------------
    public void setSearchResultsList(List<SolrSearchResult> searchResultsList) {

        this.searchResultsList = searchResultsList;
    }
}
