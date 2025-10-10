package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.search.SearchServiceBean.SortOrder;

import static org.apache.commons.lang.StringUtils.isBlank;

public class FileSortFieldAndOrder {

    private final String sortField;
    private final SortOrder sortOrder;

    private static String displayOrder = "displayOrder";
    private static String label = "label";
    private static String createDate = "dataFile.createDate";
    private static String size = "dataFile.filesize";
    private static String type = "dataFile.contentType";

    public FileSortFieldAndOrder(final String userSuppliedSortField, 
            final SortOrder userSuppliedSortOrder) {
        if (isBlank(userSuppliedSortField)) {
            this.sortField = displayOrder;
        } else if (isUserSuppliedSortField(userSuppliedSortField)) {
            this.sortField = userSuppliedSortField;
        } else {
            this.sortField = label;
        }  
        this.sortOrder = userSuppliedSortOrder != null 
                ? userSuppliedSortOrder 
                : SortOrder.asc;
    }

    public String getSortField() {
        return this.sortField;
    }

    public SortOrder getSortOrder() {
        return this.sortOrder;
    }

    private boolean isUserSuppliedSortField(final String field) {
        return field.equals(displayOrder) ||
                field.equals(label) ||
                field.equals(createDate) ||
                field.equals(size) ||
                field.equals(type);
    }
}
