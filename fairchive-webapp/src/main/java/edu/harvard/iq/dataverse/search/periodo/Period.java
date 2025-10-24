package edu.harvard.iq.dataverse.search.periodo;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static java.lang.Long.MAX_VALUE;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.Collection;

import org.apache.solr.client.solrj.beans.Field;

public class Period {
    
    static final String base = "http://n2t.net/ark:/99152/";
    
    @Field
    private String id;
    @Field
    private String label;
    @Field
    private long start;
    @Field
    private long stop;
    @Field
    private String authorityTitle;
    @Field
    private String coverageName;
    @Field
    private Collection<String> locations;

    public Period() {
        
    }
    
    public Period(final String id, final String label,
            final long start, final long stop, final String authorityTitle,
            final String coverageName, final Collection<String> locations) {
        this.id = id;
        this.label = label;
        this.start = start;
        this.stop = stop;
        this.authorityTitle = authorityTitle;
        this.coverageName = coverageName;
        this.locations = locations;
    }

    public String getValue() {
        return base.concat(this.id);
    }

    @Override
    public String toString() {
        return getValue();
    }

    public String getDetails(final String beginDecorator,
            final String endDecorator, final String separator) {
        final StringBuilder result = new StringBuilder(80);
        result.append(beginDecorator).append(getStringFromBundle("periodo.label"))
                .append(endDecorator).append(": ")
                .append(this.label).append(separator);
        result.append(beginDecorator)
                .append(getStringFromBundle("periodo.location"))
                .append(endDecorator).append(": ");
        String coma = "";
        for (final String location : this.locations) {
            result.append(coma).append(location);
            coma = ", ";
        }
        result.append(separator);
        result.append(beginDecorator)
                .append(getStringFromBundle("periodo.location.desc"))
                .append(endDecorator).append(": ")
                .append(this.coverageName).append(separator);
        result.append(beginDecorator).append(getStringFromBundle("periodo.start"))
                .append(endDecorator).append(": ")
                .append(this.start).append(separator);
        result.append(beginDecorator).append(getStringFromBundle("periodo.end"))
                .append(endDecorator).append(": ");
        if (this.stop == MAX_VALUE) {
            result.append(getStringFromBundle("periodo.present"));
        } else {
            result.append(this.stop);
        }
        result.append(separator);
        result.append(beginDecorator)
                .append(getStringFromBundle("periodo.authority"))
                .append(endDecorator).append(": ")
                .append(this.authorityTitle);
        return result.toString();
    }

    public String getDetails() {
        return getDetails(EMPTY, EMPTY, " ");
    }

    public String getDetailsHTML() {
        return getDetails("<b>", "</b>", " ");
    }

    public String getId() {
        return this.id;
    }

    public String getLabel() {
        return this.label;
    }

    public long getStart() {
        return this.start;
    }

    public long getStop() {
        return this.stop;
    }

    public String getAuthorityTitle() {
        return this.authorityTitle;
    }

    public String getCoverageName() {
        return this.coverageName;
    }

    public Collection<String> getLocations() {
        return this.locations;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public void setStart(final long start) {
        this.start = start;
    }

    public void setStop(final long stop) {
        this.stop = stop;
    }

    public void setAuthorityTitle(final String authorityTitle) {
        this.authorityTitle = authorityTitle;
    }

    public void setCoverageName(final String coverageName) {
        this.coverageName = coverageName;
    }

    public void setLocations(final Collection<String> locations) {
        this.locations = locations;
    }
    
    
}
