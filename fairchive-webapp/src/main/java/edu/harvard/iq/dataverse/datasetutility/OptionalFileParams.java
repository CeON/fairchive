/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import java.io.Serializable;
import java.util.List;

import edu.harvard.iq.dataverse.api.dto.FileTermsOfUseDTO;
import edu.harvard.iq.dataverse.common.Util;

/**
 * This is used in conjunction with the AddReplaceFileHelper
 * <p>
 * It encapsulates these optional parameters:
 * <p>
 * - description
 * - file tags (can be custom)
 * - tabular tags (controlled vocabulary)
 * <p>
 * Future params:
 * - Provenance related information
 *
 * @author rmp553
 */
public class OptionalFileParams implements Serializable {

    private static final long serialVersionUID = 9103033252084387893L;

    private String description;

    private List<String> categories;

    private FileTermsOfUseDTO fileTermsOfUseDTO;

    /**
     * Set description
     *
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get for description
     *
     * @return String
     */
    public String getDescription() {
        return this.description;
    }

    public boolean hasCategories() {
        return (categories != null) && (!this.categories.isEmpty());
    }

    public boolean hasDescription() {
        return (description != null) && (!this.description.isEmpty());
    }

    /**
     * Set tags
     *
     * @param tags
     */
    public void setCategories(List<String> newCategories) {

        if (newCategories != null) {
            newCategories = Util.removeDuplicatesNullsEmptyStrings(newCategories);
            if (newCategories.isEmpty()) {
                newCategories = null;
            }
        }

        this.categories = newCategories;
    }

    /**
     * Get for tags
     *
     * @return List<String>
     */
    public List<String> getCategories() {
        return this.categories;
    }

    public FileTermsOfUseDTO getFileTermsOfUseDTO() {
        return fileTermsOfUseDTO;
    }

    public void setFileTermsOfUseDTO(FileTermsOfUseDTO fileTermsOfUseDTO) {
        this.fileTermsOfUseDTO = fileTermsOfUseDTO;
    }

}
