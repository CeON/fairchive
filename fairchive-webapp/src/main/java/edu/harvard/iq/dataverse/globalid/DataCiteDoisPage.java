package edu.harvard.iq.dataverse.globalid;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * One page of the DataCite REST API search for DOIs
 * ({@code GET /dois}): the records, the total number of matches and the
 * link to the next page.
 *
 * @see <a href="https://support.datacite.org/reference/get_dois">GET /dois</a>
 */
public class DataCiteDoisPage {

    @JsonProperty
    private List<DataCiteFindDoiResponse> data = new ArrayList<>();

    @JsonProperty
    private Meta meta = new Meta();

    @JsonProperty
    private Links links = new Links();

    // -------------------- GETTERS --------------------

    public List<DataCiteFindDoiResponse> getData() {
        return data;
    }

    public Meta getMeta() {
        return meta;
    }

    public Links getLinks() {
        return links;
    }

    // -------------------- SETTERS --------------------

    public void setData(List<DataCiteFindDoiResponse> data) {
        this.data = data;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public void setLinks(Links links) {
        this.links = links;
    }

    // -------------------- INNER CLASSES --------------------

    public static class Meta {

        @JsonProperty
        private long total;

        // -------------------- GETTERS --------------------

        /**
         * Number of all records matching the search, not only those on this
         * page.
         */
        public long getTotal() {
            return total;
        }

        // -------------------- SETTERS --------------------

        public void setTotal(long total) {
            this.total = total;
        }
    }

    public static class Links {

        @JsonProperty
        private String next;

        // -------------------- GETTERS --------------------

        /**
         * URL of the next page, or {@code null} on the last page.
         */
        public String getNext() {
            return next;
        }

        // -------------------- SETTERS --------------------

        public void setNext(String next) {
            this.next = next;
        }
    }
}
