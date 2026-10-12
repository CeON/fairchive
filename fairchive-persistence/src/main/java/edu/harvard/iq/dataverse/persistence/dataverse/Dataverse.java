package edu.harvard.iq.dataverse.persistence.dataverse;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundleWithLocale;
import static edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion.State.DRAFT;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Locale.ENGLISH;
import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REMOVE;
import static javax.persistence.FetchType.LAZY;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;

import edu.harvard.iq.dataverse.common.MarkupChecker;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.DvObjectContainer;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataset.Template;
import edu.harvard.iq.dataverse.persistence.dataverse.link.DatasetLinkingDataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.link.DataverseLinkingDataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.link.SavedSearch;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.User;

/**
 * @author gdurand
 * @author mbarsinai
 */
//@NamedQueries({
//        @NamedQuery(name = "Dataverse.filterByAlias", query = "SELECT dv FROM Dataverse dv WHERE LOWER(dv.alias) LIKE :alias order by dv.alias"),
//        @NamedQuery(name = "Dataverse.filterByAliasNameAffiliation", query = "SELECT dv FROM Dataverse dv WHERE (LOWER(dv.alias) LIKE :alias) OR (LOWER(dv.name) LIKE :name) OR (LOWER(dv.affiliation) LIKE :affiliation) order by dv.alias")    
//})
@NamedNativeQueries({
	@NamedNativeQuery(
		    name = "Dataverse.findDataForSolrResults2",
		    query =
		        "SELECT t0.ID, " +
		        "       dv1.AFFILIATION, " +
		        "       dv1.ALIAS, " +
		        "       dv2.ALIAS " +
		        "FROM DATAVERSE t0 " +
		        "JOIN DVOBJECT t1 ON t0.ID = t1.ID " +
		        "JOIN LATERAL ( " +
		        "    SELECT v1.AFFILIATION, v1.ALIAS " +
		        "    FROM DATAVERSEVERSION v1 " +
		        "    WHERE v1.DATAVERSE_ID = t0.ID " +
		        "      AND v1.STATE = 'RELEASED' " +
		        "    ORDER BY v1.CREATETIME DESC " +
		        "    LIMIT 1 " +
		        ") dv1 ON TRUE " +
		        "LEFT JOIN LATERAL ( " +
		        "    SELECT v2.ALIAS " +
		        "    FROM DATAVERSEVERSION v2 " +
		        "    WHERE v2.DATAVERSE_ID = t1.OWNER_ID " +
		        "      AND v2.STATE = 'RELEASED' " +
		        "    ORDER BY v2.CREATETIME DESC " +
		        "    LIMIT 1 " +
		        ") dv2 ON TRUE " +
		        "WHERE t0.ID IN (?, ?)"
		),
	@NamedNativeQuery(
		    name = "Dataverse.findDataForSolrResults6",
		    query =
		        "SELECT t0.ID, " +
		        "       dv1.AFFILIATION, " +
		        "       dv1.ALIAS, " +
		        "       dv2.ALIAS " +
		        "FROM DATAVERSE t0 " +
		        "JOIN DVOBJECT t1 ON t0.ID = t1.ID " +
		        "JOIN LATERAL ( " +
		        "    SELECT v1.AFFILIATION, v1.ALIAS " +
		        "    FROM DATAVERSEVERSION v1 " +
		        "    WHERE v1.DATAVERSE_ID = t0.ID " +
		        "      AND v1.STATE = 'RELEASED' " +
		        "    ORDER BY v1.CREATETIME DESC " +
		        "    LIMIT 1 " +
		        ") dv1 ON TRUE " +
		        "LEFT JOIN LATERAL ( " +
		        "    SELECT v2.ALIAS " +
		        "    FROM DATAVERSEVERSION v2 " +
		        "    WHERE v2.DATAVERSE_ID = t1.OWNER_ID " +
		        "      AND v2.STATE = 'RELEASED' " +
		        "    ORDER BY v2.CREATETIME DESC " +
		        "    LIMIT 1 " +
		        ") dv2 ON TRUE " +
		        "WHERE t0.ID IN (?, ?, ?, ?, ?, ?)"
		),

	@NamedNativeQuery(
		    name = "Dataverse.findDataForSolrResults10",
		    query =
		        "SELECT t0.ID, " +
		        "       dv1.AFFILIATION, " +
		        "       dv1.ALIAS, " +
		        "       dv2.ALIAS " +
		        "FROM DATAVERSE t0 " +
		        "JOIN DVOBJECT t1 ON t0.ID = t1.ID " +
		        "JOIN LATERAL ( " +
		        "    SELECT v1.AFFILIATION, v1.ALIAS " +
		        "    FROM DATAVERSEVERSION v1 " +
		        "    WHERE v1.DATAVERSE_ID = t0.ID " +
		        "      AND v1.STATE = 'RELEASED' " +
		        "    ORDER BY v1.CREATETIME DESC " +
		        "    LIMIT 1 " +
		        ") dv1 ON TRUE " +
		        "LEFT JOIN LATERAL ( " +
		        "    SELECT v2.ALIAS " +
		        "    FROM DATAVERSEVERSION v2 " +
		        "    WHERE v2.DATAVERSE_ID = t1.OWNER_ID " +
		        "      AND v2.STATE = 'RELEASED' " +
		        "    ORDER BY v2.CREATETIME DESC " +
		        "    LIMIT 1 " +
		        ") dv2 ON TRUE " +
		        "WHERE t0.ID IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
		)
})
@Entity
@Table(indexes = {@Index(columnList = "defaultcontributorrole_id")
        , @Index(columnList = "defaulttemplate_id")
        , @Index(columnList = "alias")
        , @Index(columnList = "affiliation")
        , @Index(columnList = "dataversetype")
        , @Index(columnList = "facetroot")
        , @Index(columnList = "guestbookroot")
        , @Index(columnList = "metadatablockroot")
        , @Index(columnList = "templateroot")
        , @Index(columnList = "permissionroot")
        , @Index(columnList = "themeroot")})
public class Dataverse extends DvObjectContainer {

    public enum DataverseType {
        RESEARCHERS("dataverse.type.selectTab.researcher"),
        RESEARCH_PROJECTS("dataverse.type.selectTab.research_project"),
        JOURNALS("dataverse.type.selectTab.journal"),
        ORGANIZATIONS_INSTITUTIONS("dataverse.type.selectTab.organization_or_institution"), 
        TEACHING_COURSES("dataverse.type.selectTab.teaching_course"),
        UNCATEGORIZED("dataverse.type.selectTab.uncategorized"),
        LABORATORY("dataverse.type.selectTab.laboratory"),
        RESEARCH_GROUP("dataverse.type.selectTab.research_group"),
        DEPARTMENT("dataverse.type.selectTab.department");
        
        public final String i18nKey;

		private DataverseType(final String i18nKey) {
			this.i18nKey = i18nKey;
		}
    }

    public enum FeaturedDataversesSorting {
        BY_HAND, BY_NAME_ASC, BY_NAME_DESC, BY_DATASET_COUNT
    }

    private static final long serialVersionUID = 1L;
    
    @OneToMany(mappedBy = "dataverse", cascade = {REMOVE, MERGE, PERSIST}, orphanRemoval = true)
    @NotEmpty(message = "At least one version is required.")
    @OrderBy("createTime")
    private List<DataverseVersion> versions = new ArrayList<>();

    /**
     * When {@code true}, users are not granted permissions the got for parent
     * dataverses.
     */
    protected boolean permissionRoot;
    
    @Transient
    private final String uncategorizedString = "Uncategorized";

    // Note: We can't have "Remove" here, as there are role assignments that refer
    //       to this role. So, adding it would mean violating a forign key contstraint.
    @OneToMany(cascade = {MERGE}, fetch = LAZY, mappedBy = "owner")
    private Set<DataverseRole> roles;

    @ManyToOne
    @JoinColumn(nullable = true)
    private DataverseRole defaultDatasetContributorRole;
    
    @ManyToOne
    @JoinColumn(nullable = true)
    private DataverseRole defaultDataverseContributorRole;
    
    private boolean metadataBlockRoot;
    private boolean facetRoot;
    // By default, themeRoot should be true, as new dataverses should start with the default theme
    private boolean themeRoot = true;
    private boolean templateRoot;

    private boolean allowMessagesBanners;

    @Enumerated(EnumType.STRING)
    private FeaturedDataversesSorting featuredDataversesSorting;

    @ManyToMany(cascade = {MERGE})
    @OrderBy("displayOrder")
    private List<MetadataBlock> metadataBlocks = new ArrayList<>();

    @ManyToMany(cascade = {MERGE})
    @JoinTable(name = "dataverse_citationDatasetFieldTypes",
            joinColumns = @JoinColumn(name = "dataverse_id"),
            inverseJoinColumns = @JoinColumn(name = "citationdatasetfieldtype_id"))
    private List<DatasetFieldType> citationDatasetFieldTypes = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "dataversesubjects",
            joinColumns = @JoinColumn(name = "dataverse_id"),
            inverseJoinColumns = @JoinColumn(name = "controlledvocabularyvalue_id"))
    private Set<ControlledVocabularyValue> dataverseSubjects;

    @OneToMany(mappedBy = "dataverse", cascade = {REMOVE, MERGE, PERSIST})
    private List<DataverseFeaturedDataverse> dataverseFeaturedDataverses;

    @ManyToOne
    @JoinColumn(nullable = true)
    private Template defaultTemplate;

    @OneToMany(mappedBy = "definitionPoint", cascade = {REMOVE, MERGE, PERSIST})
    private List<SavedSearch> savedSearches;
    
    @OneToMany(mappedBy = "featuredDataverse", cascade = {REMOVE, MERGE, PERSIST})
    private List<DataverseFeaturedDataverse> dataverseFeaturingDataverses;
    
    @OneToMany(mappedBy = "dataverse", cascade = {REMOVE, MERGE, PERSIST})
    private List<DataverseLinkingDataverse> dataverseLinkingDataverses;
    
    @OneToMany(mappedBy = "linkingDataverse", cascade = {REMOVE, MERGE, PERSIST})
    private List<DataverseLinkingDataverse> dataverseLinkedDataverses;
    
    @OneToMany(mappedBy = "linkingDataverse", cascade = {REMOVE, MERGE, PERSIST})
    private List<DatasetLinkingDataverse> datasetLinkingDataverses;
    
    @OneToMany(mappedBy = "dataverse", cascade = {MERGE, REMOVE}, orphanRemoval = true)
    private List<Template> templates = new ArrayList<>();

    @OneToMany(mappedBy = "dataverse", cascade = {MERGE, REMOVE})
    private List<Guestbook> guestbooks;
    
    @OneToMany(mappedBy = "dataverse", cascade = {MERGE, REMOVE})
    private List<HarvestingClient> harvestingClientConfigs;

    private boolean guestbookRoot;

    public DataverseType getDataverseType() {
    	//TODO: verify if this behavior is correct
    	return getLatestVersion().getDataverseType();
    }

    public String getFriendlyCategoryName() {
        switch (getDataverseType()) {
            case RESEARCHERS:
                return getStringFromBundle("dataverse.type.selectTab.researcher");
            case RESEARCH_PROJECTS:
                return getStringFromBundle("dataverse.type.selectTab.research_project");
            case JOURNALS:
                return getStringFromBundle("dataverse.type.selectTab.journal");
            case ORGANIZATIONS_INSTITUTIONS:
                return getStringFromBundle("dataverse.type.selectTab.organization_or_institution");
            case TEACHING_COURSES:
                return getStringFromBundle("dataverse.type.selectTab.teaching_course");
            case LABORATORY:
                return getStringFromBundle("dataverse.type.selectTab.laboratory");
            case RESEARCH_GROUP:
                return getStringFromBundle("dataverse.type.selectTab.research_group");
            case DEPARTMENT:
                return getStringFromBundle("dataverse.type.selectTab.department");
            case UNCATEGORIZED:
                return uncategorizedString;
            default:
                return "";
        }
    }

    private String getCategoryNameForIndex() {
        switch (getDataverseType()) {
            case RESEARCHERS:
                return getStringFromBundleWithLocale("dataverse.type.selectTab.researcher", ENGLISH);
            case RESEARCH_PROJECTS:
                return getStringFromBundleWithLocale("dataverse.type.selectTab.research_project", ENGLISH);
            case JOURNALS:
                return getStringFromBundleWithLocale("dataverse.type.selectTab.journal", ENGLISH);
            case ORGANIZATIONS_INSTITUTIONS:
                return getStringFromBundleWithLocale("dataverse.type.selectTab.organization_or_institution", ENGLISH);
            case TEACHING_COURSES:
                return getStringFromBundleWithLocale("dataverse.type.selectTab.teaching_course", ENGLISH);
            case LABORATORY:
                return getStringFromBundleWithLocale("dataverse.type.selectTab.laboratory", ENGLISH);
            case RESEARCH_GROUP:
                return getStringFromBundleWithLocale("dataverse.type.selectTab.research_group", ENGLISH);
            case DEPARTMENT:
                return getStringFromBundleWithLocale("dataverse.type.selectTab.department", ENGLISH);
            case UNCATEGORIZED:
                return getStringFromBundleWithLocale("dataverse.type.selectTab.uncategorized", ENGLISH);
            default:
                return "";
        }
    }
    @Override
    public String getAuthorString() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public String getTargetUrl() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    public Dataverse getDataverseContext() {
        return this;
    }

    public String getIndexableCategoryName() {
        return getCategoryNameForIndex();
    }

    public DataverseRole getDefaultDatasetContributorRole() {
        return this.defaultDatasetContributorRole;
    }

    public void setDefaultDatasetContributorRole(final DataverseRole role) {
        this.defaultDatasetContributorRole = role;
    }

    public DataverseRole getDefaultDataverseContributorRole() {
        return this.defaultDataverseContributorRole;
    }

    public void setDefaultDataverseContributorRole(final DataverseRole role) {
        this.defaultDataverseContributorRole = role;
    }
    
    public List<DataverseFeaturedDataverse> getDataverseFeaturedDataverses() {
        return dataverseFeaturedDataverses;
    }

    public void setDataverseFeaturedDataverses(List<DataverseFeaturedDataverse> dataverseFeaturedDataverses) {
        this.dataverseFeaturedDataverses = dataverseFeaturedDataverses;
    }

    public List<DataverseFeaturedDataverse> getDataverseFeaturingDataverses() {
        return dataverseFeaturingDataverses;
    }

    public void setDataverseFeaturingDataverses(List<DataverseFeaturedDataverse> dataverseFeaturingDataverses) {
        this.dataverseFeaturingDataverses = dataverseFeaturingDataverses;
    }

    public List<DataverseLinkingDataverse> getDataverseLinkingDataverses() {
        return dataverseLinkingDataverses;
    }

    public void setDataverseLinkingDataverses(List<DataverseLinkingDataverse> dataverseLinkingDataverses) {
        this.dataverseLinkingDataverses = dataverseLinkingDataverses;
    }

    public List<DataverseLinkingDataverse> getDataverseLinkedDataverses() {
        return dataverseLinkedDataverses;
    }

    public void setDataverseLinkedDataverses(List<DataverseLinkingDataverse> dataverseLinkedDataverses) {
        this.dataverseLinkedDataverses = dataverseLinkedDataverses;
    }

    public List<DatasetLinkingDataverse> getDatasetLinkingDataverses() {
        return datasetLinkingDataverses;
    }

    public void setDatasetLinkingDataverses(List<DatasetLinkingDataverse> datasetLinkingDataverses) {
        this.datasetLinkingDataverses = datasetLinkingDataverses;
    }

    public Set<ControlledVocabularyValue> getDataverseSubjects() {
        return dataverseSubjects;
    }

    public void setDataverseSubjects(Set<ControlledVocabularyValue> dataverseSubjects) {
        this.dataverseSubjects = dataverseSubjects;
    }

    public List<SavedSearch> getSavedSearches() {
        return savedSearches;
    }

    public void setSavedSearches(List<SavedSearch> savedSearches) {
        this.savedSearches = savedSearches;
    }

    public List<Guestbook> getGuestbooks() {
        return guestbooks;
    }

    public void setGuestbooks(List<Guestbook> guestbooks) {
        this.guestbooks = guestbooks;
    }

    public List<HarvestingClient> getHarvestingClientConfigs() {
        return this.harvestingClientConfigs;
    }

    public void setHarvestingClientConfigs(List<HarvestingClient> harvestingClientConfigs) {
        this.harvestingClientConfigs = harvestingClientConfigs;
    }

    public List<Guestbook> getParentGuestbooks() {
        List<Guestbook> retList = new ArrayList<>();
        Dataverse testDV = this;
        while (testDV.isNotRoot()) {
            retList.addAll(testDV.getOwner().getGuestbooks());
            if (testDV.getOwner().isGuestbookRoot()) {
                break;
            }
            testDV = testDV.getOwner();
        }
        return retList;
    }

    public List<Guestbook> getAvailableGuestbooks() {
        //get all guestbooks
        List<Guestbook> retList = new ArrayList<>();
        Dataverse testDV = this;
        List<Guestbook> allGbs = new ArrayList<>();
        if (!this.isGuestbookRoot()) {
            while (testDV.isNotRoot()) {

                allGbs.addAll(testDV.getOwner().getGuestbooks());
                if (testDV.getOwner().isGuestbookRoot()) {
                    break;
                }
                testDV = testDV.getOwner();
            }
        }

        allGbs.addAll(this.getGuestbooks());
        //then only display them if they are enabled
        for (Guestbook gbt : allGbs) {
            if (gbt.isEnabled()) {
                retList.add(gbt);
            }
        }
        return retList;

    }
    @Override
    public boolean canBeReleased() {
    	return (!isReleased() || hasDraft()) && (isRoot() || getOwner().isReleased());
    }

    public boolean isGuestbookRoot() {
        return guestbookRoot;
    }

    public void setGuestbookRoot(boolean guestbookRoot) {
        this.guestbookRoot = guestbookRoot;
    }

    public Template getDefaultTemplate() {
        return defaultTemplate;
    }

    public void setDefaultTemplate(Template defaultTemplate) {
        this.defaultTemplate = defaultTemplate;
    }

    public List<Template> getTemplates() {
        return templates;
    }

    public void setTemplates(List<Template> templates) {
        this.templates = templates;
    }

    public List<Template> getParentTemplates() {
        List<Template> retList = new ArrayList<>();
        Dataverse testDV = this;
        while (testDV.isNotRoot()) {

            if (!testDV.getRootMetadataBlocks().equals(testDV.getOwner().getRootMetadataBlocks())) {
                break;
            }
            retList.addAll(testDV.getOwner().getTemplates());

            if (testDV.getOwner().isTemplateRoot()) {
                break;
            }
            testDV = testDV.getOwner();
        }
        return retList;
    }
    
    public List<Template> getApplicableTemplates() {
        final List<Template> result = new ArrayList<>(getTemplates());
        if (!isTemplateRoot()) {
            result.addAll(getParentTemplates());
        }
        result.sort(Template.comparator);
        return result;
    }

    public boolean isThemeRoot() {
        return themeRoot;
    }

    public boolean getThemeRoot() {
        return themeRoot;
    }

    public void setThemeRoot(boolean themeRoot) {
        this.themeRoot = themeRoot;
    }

    public boolean isTemplateRoot() {
        return templateRoot;
    }

    public void setTemplateRoot(boolean templateRoot) {
        this.templateRoot = templateRoot;
    }

    public Dataverse getMetadataBlockRootDataverse() {
        return isMetadataBlockRoot() || isRoot()
                ? this
                : getOwner().getMetadataBlockRootDataverse();
    }

    public List<MetadataBlock> getRootMetadataBlocks() {
        return getMetadataBlockRootDataverse().getMetadataBlocks();
    }

    public List<MetadataBlock> getMetadataBlocks() {
        return metadataBlocks;
    }

    public Long getMetadataRootId() {
        return getMetadataBlockRootDataverse().getId();
    }

    public String getGuestbookRootDataverseName() {
        Dataverse testDV = this;
        String retName = "Parent";
        while (testDV.isNotRoot()) {
            retName = testDV.getOwner().getDisplayName();
            if (testDV.getOwner().isGuestbookRoot()) {
                break;
            }
            testDV = testDV.getOwner();
        }
        return retName;
    }

    public String getTemplateRootDataverseName() {
        Dataverse testDV = this;
        String retName = "Parent";
        while (testDV.isNotRoot()) {
            retName = testDV.getOwner().getDisplayName();
            if (testDV.getOwner().isTemplateRoot()) {
                break;
            }
            testDV = testDV.getOwner();
        }
        return retName;
    }

    public String getThemeRootDataverseName() {
        Dataverse testDV = this;
        String retName = "Parent";
        while (testDV.isNotRoot()) {
            retName = testDV.getOwner().getDisplayName();
            if (testDV.getOwner().isThemeRoot()) {
                break;
            }
            testDV = testDV.getOwner();
        }
        return retName;
    }

    public String getMetadataParentRootDataverseName() {
        return isRoot()
                ? getDisplayName()
                : getOwner().getMetadataBlockRootDataverse().getDisplayName();
    }

    public String getFacetRootDataverseName() {
        Dataverse testDV = this;
        String retName = "Parent";
        while (testDV.isNotRoot()) {
            retName = testDV.getOwner().getDisplayName();
            if (testDV.getOwner().isFacetRoot()) {
                break;
            }
            testDV = testDV.getOwner();
        }
        return retName;
    }

    public String getLogoOwnerId() {
        return isThemeRoot() || isRoot()
                ? getId().toString()
                : getOwner().getId().toString();
    }
    
    public DataverseTheme getDataverseTheme() {
        return getDataverseTheme(false);
    }

    public DataverseTheme getDataverseTheme(final boolean returnActual) {
        return returnActual || isThemeRoot() || isRoot()
        		//TODO: verify if this behavior is correct
                ? getLatestVersion().getTheme()
                : getOwner().getDataverseTheme(returnActual);
    }

    public void setMetadataBlocks(List<MetadataBlock> metadataBlocks) {
        this.metadataBlocks = metadataBlocks;
    }
    
	public List<DataverseFieldTypeInputLevel> getDataverseFieldTypeInputLevels() {
    	//TODO: verify if this behavior is correct
    	return getLatestVersion().getDataverseFieldTypeInputLevels();
	}

    public List<DatasetFieldType> getCitationDatasetFieldTypes() {
        return citationDatasetFieldTypes;
    }

    public void setCitationDatasetFieldTypes(List<DatasetFieldType> citationDatasetFieldTypes) {
        this.citationDatasetFieldTypes = citationDatasetFieldTypes;
    }

    public Long getFacetRootId() {
        return isFacetRoot() || isRoot()
                ? getId()
                : getOwner().getFacetRootId();
    }
    
    public Dataverse getFacetRoot() {
        return isFacetRoot() || isRoot()
                ? this
                : getOwner().getFacetRoot();
    }

    public List<DataverseContact> getDataverseContacts() {
    	//TODO: verify if this behavior is correct
    	return getLatestVersion().getContacts();
    }

    public String getName() {
    	//TODO: verify if this behavior is correct
    	return getLatestVersion().getName();
    }

    public String getAlias() {
    	//TODO: verify if this behavior is correct
    	return getLatestVersion().getAlias();
    }

    public String getDescription() {
    	//TODO: verify if this behavior is correct
    	return getLatestVersion().getDescription();
    }

    public String getAdditionalDescription() {
    	//TODO: verify if this behavior is correct
    	return getLatestVersion().getAdditionalDescription();
    }

    public String getAffiliation() {
    	//TODO: verify if this behavior is correct
    	return getLatestVersion().getAffiliation();
    }
    
    public boolean isDraft() {
    	//TODO: verify if this behavior is correct
    	return getLatestVersion().isDraft();
    }

    public boolean isMetadataBlockRoot() {
        return metadataBlockRoot;
    }

    public void setMetadataBlockRoot(boolean metadataBlockRoot) {
        this.metadataBlockRoot = metadataBlockRoot;
    }

    public boolean isFacetRoot() {
        return facetRoot;
    }

    public void setFacetRoot(boolean facetRoot) {
        this.facetRoot = facetRoot;
    }


    public void addRole(final DataverseRole role) {
        role.setOwner(this);
        getRoles().add(role);
    }

    /**
     * Note: to add a role, use {@link #addRole(edu.harvard.iq.dataverse.authorization.DataverseRole)},
     * do not call this method and try to add directly to the list.
     *
     * @return the roles defined in this Dataverse.
     */
    public Set<DataverseRole> getRoles() {
        if (roles == null) {
            roles = new HashSet<>();
        }
        return roles;
    }

    public List<Dataverse> getOwners() {
        if (isNotRoot()) {
            final List<Dataverse> owners = new ArrayList<>(getOwner().getOwners());
            owners.add(getOwner());
            return owners;
        } else {
            return emptyList();
        }
    }
    
    public Dataverse getRoot() {
        return isRoot() ? this : getOwner().getRoot();
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Dataverse)) {
            return false;
        }
        Dataverse other = (Dataverse) object;
        return Objects.equals(getId(), other.getId());
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visit(this);
    }

    /**
     * @todo implement in https://github.com/IQSS/dataverse/issues/551
     */
    public String getDepositTermsOfUse() {
        return "Dataverse Deposit Terms of Use will be implemented in https://github.com/IQSS/dataverse/issues/551";
    }

    @Override
    public String getDisplayName() {
        return MarkupChecker.stripAllTags(MarkupChecker.sanitizeBasicHTML(getName()));
    }

    @Override
    public boolean isPermissionRoot() {
        return this.permissionRoot;
    }

    public void setPermissionRoot(final boolean permissionRoot) {
        this.permissionRoot = permissionRoot;
    }

    @Override
    public boolean isAncestorOf(DvObject other) {
        while (other != null) {
            if (equals(other)) {
                return true;
            }
            other = other.getOwner();
        }
        return false;
    }

    public boolean isAllowMessagesBanners() {
        return this.allowMessagesBanners;
    }

    public void setAllowMessagesBanners(boolean allowMessagesBanners) {
        this.allowMessagesBanners = allowMessagesBanners;
    }

    public FeaturedDataversesSorting getFeaturedDataversesSorting() {
        if (featuredDataversesSorting == null) {
            featuredDataversesSorting = FeaturedDataversesSorting.BY_HAND;
        }
        return featuredDataversesSorting;
    }

    public void setFeaturedDataversesSorting(FeaturedDataversesSorting featuredDataversesSorting) {
        this.featuredDataversesSorting = featuredDataversesSorting;
    }
    
    public static String getSuggestedNameOnCreate(final User user) {
        return user.getDisplayInfo().getTitle() + SPACE + getStringFromBundle("dataverse");
    }

	public List<DataverseVersion> getVersions() {
		if(this.versions.isEmpty()) {
			addVersion(DRAFT);
		}
		return unmodifiableList(this.versions);
	}
	
	public DataverseVersion getLatestVersion() {
		return this.versions.isEmpty()
			 ? addVersion(DRAFT)
			 : this.versions.get(this.versions.size()-1);
	}
	
	public DataverseVersion getEditableVersion() {
		final DataverseVersion latest = getLatestVersion();
        return latest.isDraft() ? latest : latest.createNewDraft();
	}
	
	public DataverseVersion addVersion(final DataverseVersion.State state) {
		final DataverseVersion version = new DataverseVersion(this, state);
		version.addContact(EMPTY);
		this.versions.add(version);
		return version;
	}
	
	public DataverseVersion addVersion(final DataverseVersion version) {
		if(this.versions.isEmpty()) {
			this.versions.add(version);
		} else if(getLatestVersion() != version) {
			this.versions.add(version);
		}
		return version;
	}
	
	public Optional<DataverseVersion> getLatestVersion(final DataverseVersion.State state) {
		if(this.versions.isEmpty()) {
			addVersion(DRAFT);
		}
		// assumes versions are sorted ascending by creation time 
		for(int index = this.versions.size() -1; index >= 0; --index) {
			final DataverseVersion version = this.versions.get(index);
			if(version.getState().equals(state)) {
				return Optional.of(version);
			}
		}
		return Optional.empty();
	}
	
	public boolean hasDraft() {
		return getLatestVersion().isDraft();
	}
}
