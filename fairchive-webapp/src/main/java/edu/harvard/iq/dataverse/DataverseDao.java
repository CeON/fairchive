package edu.harvard.iq.dataverse;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataverse.DataverseLinkingService;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseThemeRepository;
import edu.harvard.iq.dataverse.persistence.group.Group;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;

/**
 * @author gdurand
 */
@SuppressWarnings("serial")
@Stateless
@Named
public class DataverseDao implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DataverseDao.class.getCanonicalName());
    @EJB
    private IndexServiceBean indexService;

    @EJB
    private AuthenticationServiceBean authService;

    @EJB
    private DatasetRepository datasetRepository;

    @EJB
    private DataverseLinkingService dataverseLinkingService;

    @EJB
    private DatasetLinkingServiceBean datasetLinkingService;

    @EJB
    private GroupServiceBean groupService;

    @EJB
    private DataverseRoleServiceBean rolesService;

    @EJB
    private PermissionServiceBean permissionService;
    
    @EJB
    private DataverseRepository dataverseRepo;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @Inject
    private ImageThumbConverter imageThumbConverter;
    
    @Inject
    private DataverseThemeRepository dataverseThemeRepo;

    public Dataverse save(Dataverse dataverse) {

        dataverse.setModificationTime(new Timestamp(new Date().getTime()));
        Dataverse savedDataverse = this.dataverseRepo.save(dataverse);
        /**
         * @todo check the result to see if indexing was successful or not
         */
        indexService.indexDataverse(savedDataverse);
        return savedDataverse;
    }

    public Dataverse find(Object pk) {
        return this.dataverseRepo.findById((Long) pk).orElse(null);
    }

    public List<Dataverse> findAll() {
        return this.dataverseRepo.findAll();
    }
    
    /**
     * @return Dataverses that should be reindexed either because they have
     * never been indexed or their index time is before their modification time.
     */
    public List<Dataverse> findStaleOrMissingDataverses() {
        return findAll()
                .stream()
                .filter(Dataverse::isNotRoot)
                .filter(Dataverse::isStale)
                .collect(toList());
    }

    public List<Long> findDataverseIdsForIndexing(final boolean skipIndexed) {      
        return skipIndexed 
                ? this.dataverseRepo.findAllUnindexedIDs()
                : this.dataverseRepo.findAllIDs();
    }

    public List<Dataverse> findByOwnerId(final Long ownerId) {
        return this.dataverseRepo.findByOwnerId(ownerId);
    }

    /**
     * @return the root dataverse
     * @todo Do we really want this method to sometimes throw a
     * NoResultException which is a RuntimeException?
     */
    public Dataverse findRootDataverse() {
        return this.dataverseRepo.findRoot();
    }
    
    public String getRootDataverseName() {
        return findRootDataverse().getName();
    }

    /**
     * A lookup of a dataverse alias should be case insensitive. If "cfa"
     * belongs to the Center for Astrophysics, we don't want to allow Code for
     * America to start using "CFA". Force all queries to be lower case.
     */
    public Dataverse findByAlias(final String alias) {
        return alias.equalsIgnoreCase(":root")
                ? this.dataverseRepo.findRoot()
                : this.dataverseRepo.findByAlias(alias).orElse(null);
    }

    public boolean hasData(final Dataverse dv) {
        return this.dataverseRepo.countChildrenOf(dv) > 0;
    }

    public boolean isRootDataverseExists() {
        return this.dataverseRepo.countRoots() == 1;
    }

    public CharSequence determineDataversePath(final Dataverse dataverse) {
        final StringBuilder result = new StringBuilder();
        for (final String segment : this.indexService.findPathSegments(dataverse)) {
            result.append("/").append(segment);
        }
        return result;
    }

    public String getDataverseLogoThumbnailFilePath(Long dvId) {

        File dataverseLogoFile = getLogoById(dvId);

        if (dataverseLogoFile != null) {
            String logoThumbNailPath = dataverseLogoFile + ".thumb" + 48;

            if (new File(logoThumbNailPath).exists()) {
                return logoThumbNailPath;
            } else {
                imageThumbConverter.generateImageThumbnailFromFile(dataverseLogoFile.getAbsolutePath(), 48, logoThumbNailPath);

                if (new File(logoThumbNailPath).exists()) {
                    return logoThumbNailPath;
                }
            }
        }
        return null;
    }

    private File getLogoById(final Long id) {
        final Optional<String> logoFileName = this.dataverseThemeRepo.findLogoByDataverseId(id);
        if (logoFileName.isPresent() && !logoFileName.get().isEmpty()) {
            final String domainRoot = System.getProperty("com.sun.aas.instanceRoot");
            if (isNotEmpty(domainRoot)) {
                return new File(domainRoot + File.separator +
                                        "docroot" + File.separator +
                                        "logos" + File.separator +
                                        id + File.separator +
                                        logoFileName.get());
            }
        }
        return null;
    }

    public List<Dataverse> findDataversesThisIdHasLinkedTo(long dataverseId) {
        return dataverseLinkingService.findLinkedDataverses(dataverseId);
    }

    public List<Dataverse> findDataversesThatLinkToThisDvId(long dataverseId) {
        return dataverseLinkingService.findLinkingDataverses(dataverseId);
    }

    public List<Dataset> findDatasetsThisIdHasLinkedTo(long dataverseId) {
        return datasetLinkingService.findDatasetsThisDataverseIdHasLinkedTo(dataverseId);
    }

    public List<Dataverse> findDataversesThatLinkToThisDatasetId(long datasetId) {
        return datasetLinkingService.findLinkingDataverses(datasetId);
    }

    public List<Dataverse> filterDataversesForLinking(String query, DataverseRequest req, Dataset dataset) {

        List<Dataverse> dataverseList = new ArrayList<>();
        List<Dataverse> results = this.dataverseRepo.findByAliasOrName(query, query);

        List<?> alreadyLinkeddv_ids = em.createNativeQuery(
                "SELECT linkingdataverse_id   FROM datasetlinkingdataverse WHERE dataset_id = " + dataset.getId())
                .getResultList();
        
        List<Dataverse> toRemove = new ArrayList<>();
            alreadyLinkeddv_ids.stream().map(this::find).forEachOrdered(toRemove::add);

        for (Dataverse res : results) {
            if (!toRemove.contains(res)) {
                if (permissionService.requestOn(req, res).has(Permission.PublishDataset)) {
                    dataverseList.add(res);
                }
            }
        }

        return dataverseList;
    }

    public Long countDataverses() {
        return this.dataverseRepo.countAll();
    }

    /**
     * Method to recursively find ids of all children of a dataverse that
     * are also of type dataverse
     */
    public List<Long> findAllDataverseDataverseChildren(Long dvId) {
        // get list of Dataverse children
        List<Long> dataverseChildren = this.dataverseRepo.findIDsByOwnerID(dvId);

        List<Long> newChildren = new ArrayList<>();
        for (Long childDvId : dataverseChildren) {
            newChildren.addAll(findAllDataverseDataverseChildren(childDvId));
        }
        dataverseChildren.addAll(newChildren);
        return dataverseChildren;
    }

    // function to recursively find ids of all children of a dataverse that are
    // of type dataset
    public List<Long> findAllDataverseDatasetChildren(Long dvId) {
        // get list of Dataverse children
        List<Long> dataverseChildren = this.dataverseRepo.findIDsByOwnerID(dvId);
        // get list of Dataset children
        List<Long> datasetChildren = datasetRepository.findIdsByOwnerId(dvId);

        for (Long childDvId : dataverseChildren) {
            datasetChildren.addAll(findAllDataverseDatasetChildren(childDvId));
        }
        return datasetChildren;
    }

    @SuppressWarnings("unchecked")
    public String addRoleAssignmentsToChildren(Dataverse owner, List<String> rolesToInherit,
                                               boolean inheritAllRoles) {
        /*
         * This query recursively finds all Dataverses that are inside/children of the
         * specified one. It recursively finds dvobjects of dtype 'Dataverse' whose
         * owner_id equals an id already in the list and then returns the list of ids
         * found, excluding the id of the original specified Dataverse.
         */
        String qstr = "WITH RECURSIVE path_elements AS ((" + " SELECT id, dtype FROM dvobject WHERE id in ("
                + owner.getId() + "))" + " UNION\n"
                + " SELECT o.id, o.dtype FROM path_elements p, dvobject o WHERE o.owner_id = p.id and o.dtype='Dataverse') "
                + "SELECT id FROM path_elements WHERE id !=" + owner.getId() + ";";

        List<Integer> childIds;
        try {
            childIds = em.createNativeQuery(qstr).getResultList();
        } catch (Exception ex) {
            childIds = null;
        }

        // Set up to track the set of users/groups that get assigned a role and those
        // that don't
        JsonArrayBuilder usedNames = Json.createArrayBuilder();
        JsonArrayBuilder unusedNames = Json.createArrayBuilder();
        // Set up to track the list of dataverses, by id and alias, that are traversed.
        JsonArrayBuilder dataverseIds = Json.createArrayBuilder();
        JsonArrayBuilder dataverseAliases = Json.createArrayBuilder();
        // Get the Dataverses for the returned ids

        List<Dataverse> children = new ArrayList<>();

        for (Integer childId : childIds) {
            Dataverse child = find(childId.longValue());
            if (child != null) {
                // Add to the list of Dataverses
                children.add(child);
                // Add ids and aliases to the tracking arrays
                dataverseIds.add(childId.longValue());
                dataverseAliases.add(child.getAlias());
            }
        }
        // Find the role assignments on the specified Dataverse
        List<RoleAssignment> allRAsOnOwner = rolesService.directRoleAssignments(owner);

        // Create a list of just the inheritable role assignments on the original
        // dataverse
        List<RoleAssignment> inheritableRAsOnOwner = new ArrayList<>();
        for (RoleAssignment role : allRAsOnOwner) {
            if (inheritAllRoles || rolesToInherit.contains(role.getRole().getAlias())) {
                //Only supporting built-in/non-dataverse-specific custom roles. Custom roles all have an owner.
                if (role.getRole().getOwner() == null) {
                    inheritableRAsOnOwner.add(role);
                }
            }
        }

        String privateUrlToken = null;
        // Create lists of the existing inheritable roles for each child Dataverse
        Map<Long, List<RoleAssignment>> existingRAs = new HashMap<>();
        for (Dataverse childDv : children) {
            List<RoleAssignment> allRAsOnChild = rolesService.directRoleAssignments(childDv);
            List<RoleAssignment> inheritableRoles = new ArrayList<>();
            for (RoleAssignment role : allRAsOnChild) {
                if (inheritAllRoles || rolesToInherit.contains(role.getRole().getAlias())) {
                    inheritableRoles.add(role);
                }
            }
            existingRAs.put(childDv.getId(), inheritableRoles);
        }

        for (RoleAssignment roleAssignment : inheritableRAsOnOwner) {
            DataverseRole inheritableRole = roleAssignment.getRole();
            String identifier = roleAssignment.getAssigneeIdentifier();
            if (identifier.startsWith(AuthenticatedUser.IDENTIFIER_PREFIX)) {
                // The RoleAssignment is for an individual user
                // Add their name to the tracking list
                usedNames.add(identifier);
                // Strip the Identifier prefix so we can retrieve the user
                identifier = identifier.substring(AuthenticatedUser.IDENTIFIER_PREFIX.length());
                AuthenticatedUser roleUser = authService.getAuthenticatedUser(identifier);
                // Now loop over all children and add the roleUser in this role if they don't
                // yet have this role
                for (Dataverse childDv : children) {
                    try {
                        RoleAssignment ra = new RoleAssignment(inheritableRole, roleUser, childDv, privateUrlToken);
                        if (!existingRAs.get(childDv.getId()).contains(ra)) {
                            rolesService.save(ra);
                        }
                    } catch (Exception e) {
                        logger.warning("Unable to assign " + roleAssignment.getAssigneeIdentifier()
                                               + "as an admin for new Dataverse: " + childDv.getName());
                        logger.warning(e.getMessage());
                        throw (e);
                    }
                }
            } else if (identifier.startsWith(Group.IDENTIFIER_PREFIX)) {
                // The role assignment is for a group
                usedNames.add(identifier);
                identifier = identifier.substring(Group.IDENTIFIER_PREFIX.length());
                Group roleGroup = groupService.getGroup(identifier);
                if (roleGroup != null) {
                    for (Dataverse childDv : children) {
                        try {
                            RoleAssignment ra = new RoleAssignment(inheritableRole, roleGroup, childDv,
                                                                   privateUrlToken);
                            if (!existingRAs.get(childDv.getId()).contains(ra)) {
                                rolesService.save(ra);
                            }
                        } catch (Exception e) {
                            logger.warning("Unable to assign " + roleAssignment.getAssigneeIdentifier()
                                                   + "as an admin for new Dataverse: " + childDv.getName());
                            logger.warning(e.getMessage());
                            throw (e);
                        }
                    }
                } else {
                    // Add any groups of types not yet supported
                    unusedNames.add(identifier);
                }
            } else {
                // Add any other types of entity found (not user or group) that aren't supported
                unusedNames.add(identifier);
            }
        }
        /*
         * Report the list of Dataverses affected and the set of users/groups that
         * should now have admin roles on them (they may already have had them) and any
         * entities that had an admin role on the specified dataverse which were not
         * handled. Add this to the log and the API return message.
         */
        String result = Json.createObjectBuilder().add("Dataverses Updated", dataverseIds)
                .add("Updated Dataverse Aliases", dataverseAliases).add("Assignments added for", usedNames)
                .add("Assignments not added for", unusedNames).build().toString();
        logger.info(result);
        return (result);
    }
}
