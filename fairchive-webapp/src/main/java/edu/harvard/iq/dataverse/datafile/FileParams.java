package edu.harvard.iq.dataverse.datafile;

import java.util.ArrayList;
import java.util.List;

import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;

public class FileParams {
    private String description;
    private List<String> categories = new ArrayList<>();
    private FileTermsOfUse termsOfUse;

    public String getDescription() {
        return description;
    }

    public List<String> getCategories() {
        return categories;
    }

    public FileTermsOfUse getTermsOfUse() {
        return termsOfUse;
    }

    public FileParams withDescription(String description) {
        this.description = description;
        return this;
    }

    public FileParams withCategories(List<String> categories) {
        this.categories = categories;
        return this;
    }
    
    public FileParams withTerms(FileTermsOfUse termsOfUse) {
        this.termsOfUse = termsOfUse;
        return this;
    }
}