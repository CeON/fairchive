package edu.harvard.iq.dataverse.search.query;

import edu.harvard.iq.dataverse.search.SearchServiceBean.SortOrder;

import java.util.Objects;

public class SortBy {

    private final String field;
    private final SortOrder order;

    public SortBy(String field, SortOrder order) {
        this.field = field;
        this.order = order;
    }

    @Override
    public String toString() {
        return "SortBy{" + "field=" + field + ", order=" + order + '}';
    }

    public String getField() {
        return field;
    }

    public SortOrder getOrder() {
        return order;
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, order);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SortBy other = (SortBy) obj;
        return Objects.equals(this.field, other.field) && this.order == other.order;
    }

}
