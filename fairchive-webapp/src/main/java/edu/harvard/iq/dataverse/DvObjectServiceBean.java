package edu.harvard.iq.dataverse;

import static java.util.Collections.emptyList;
import static java.util.logging.Level.FINE;
import static java.util.logging.Logger.getLogger;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import static org.apache.commons.lang3.StringUtils.join;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.ocpsoft.common.util.Strings;

import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.DvObjectContainer;
import edu.harvard.iq.dataverse.persistence.DvObjectRepository;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

/**
 * Your goto bean for everything {@link DvObject}, that's not tied to any
 * concrete subclass.
 *
 * @author michael
 */
@SuppressWarnings("serial")
@Stateless
public class DvObjectServiceBean implements java.io.Serializable {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    @Inject
    private DvObjectRepository repository;

    private static final Logger logger = getLogger(DvObjectServiceBean.class.getCanonicalName());

    /**
     * @param dvoc The object we check
     * @return {@code true} iff the passed object is the owner of any
     * {@link DvObject}.
     */
    public boolean hasData(DvObjectContainer dvoc) {
    	return this.repository.hasData(dvoc.getId());
    }
    public DvObject getDvObject(final Long id) {
    	return this.repository.getById(id);
    }

    public Optional<DvObject> findDvObject(final Long id) {
    	return this.repository.findById(id);
    }

    public List<DvObject> findAll() {
        return this.repository.findAll();
    }

    public List<DvObject> findByOwnerId(final Long id) {
        return this.repository.findByOwnerId(id);
    }

    public Optional<DvObject> findByGlobalId(final String globalIdString, 
    		final String type) {
    	
    	final GlobalId gid = new GlobalId(globalIdString);
    	return this.repository.findByGlobalId(gid.getProtocol(),
    			gid.getAuthority(), gid.getIdentifier(), type);
    }
    
    public Optional<DvObject> findByAlternativeGlobalId(final String globalIdString, 
    		final String type) {
    	
    	final GlobalId gid = new GlobalId(globalIdString);
    	return this.repository.findByAlternativeGlobalId(gid.getProtocol(),
    			gid.getAuthority(), gid.getIdentifier(), type);
    }

    public List<DvObject> findByAuthenticatedUserId(final AuthenticatedUser user) {
    	return this.repository.findByAuthenticatedUserId(user.getId());
    }

    public DvObject updateContentIndexTime(DvObject dvObject) {
        /**
         * @todo to avoid a possible OptimisticLockException, should we merge
         * dvObject before we try to setIndexTime? See
         * https://github.com/IQSS/dataverse/commit/6ad0ebb272c8cb46368cb76784b55dbf33eea947
         */
        DvObject dvObjectToModify = findDvObject(dvObject.getId()).get();
        dvObjectToModify.setIndexTime(new Timestamp(new Date().getTime()));
        DvObject savedDvObject = em.merge(dvObjectToModify);
        return savedDvObject;
    }

    /**
     * @param dvObject
     * @return
     * @todo DRY! Perhaps we should merge this with the older
     * updateContentIndexTime method.
     */
    public int updatePermissionIndexTime(long dvObjectId) {
        int rowsAffected = em.createQuery("UPDATE DvObject o SET o.permissionIndexTime=:currentTime WHERE o.id=:id")
            .setParameter("id", dvObjectId)
            .setParameter("currentTime", new Timestamp(new Date().getTime()))
            .executeUpdate();

        if (rowsAffected == 1) {
            logger.log(FINE, "Updated permission index time for DvObject id {0}", dvObjectId);
        } else {
            logger.log(FINE, "Unable to update permission index time on DvObject with id of {0}", dvObjectId);
        }
        return rowsAffected;
    }

    /**
     * Updates permission index time in bulk so it is more performant.
     */
    public int updatePermissionIndexTime(Collection<Long> dvObjectIds) {

        return em.createQuery("UPDATE DvObject o SET o.permissionIndexTime=:currentTime WHERE o.id IN :ids")
                 .setParameter("ids", dvObjectIds)
                 .setParameter("currentTime", new Timestamp(new Date().getTime()))
                 .executeUpdate();
    }

    @TransactionAttribute(REQUIRES_NEW)
    public int clearAllIndexTimes() {
        Query clearIndexTimes = em.createQuery("UPDATE DvObject o SET o.indexTime = NULL, o.permissionIndexTime = NULL");
        int numRowsUpdated = clearIndexTimes.executeUpdate();
        return numRowsUpdated;
    }

    public int clearIndexTimes(long dvObjectId) {
        Query clearIndexTimes = em.createQuery("UPDATE DvObject o SET o.indexTime = NULL, o.permissionIndexTime = NULL WHERE o.id =:dvObjectId");
        clearIndexTimes.setParameter("dvObjectId", dvObjectId);
        int numRowsUpdated = clearIndexTimes.executeUpdate();
        return numRowsUpdated;
    }

    private String getDvObjectIdListClause(List<Long> dvObjectIdList) {
        if (dvObjectIdList == null) {
            return null;
        }
        List<String> outputList = new ArrayList<>();

        for (Long id : dvObjectIdList) {
            if (id != null) {
                outputList.add(id.toString());
            }
        }
        if (outputList.isEmpty()) {
            return null;
        }
        return " (" + join(outputList, ",") + ")";
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getDvObjectInfoForMyData(List<Long> dvObjectIdList) {

        String dvObjectClause = getDvObjectIdListClause(dvObjectIdList);
        if (dvObjectClause == null) {
            return emptyList();
        }

        String qstr = "SELECT dv.id, dv.dtype, dv.owner_id"
                 + " FROM dvobject dv"
                 + " WHERE  dv.id IN " + dvObjectClause
                 + ";";

        return em.createNativeQuery(qstr).getResultList();

    }

    /**
     * Used for retrieving DvObject based on a list of parent Ids
     * MyData use case: The Dataverse has file permissions and we want to know
     * the Datasets under that Dataverse (and subsequently query files by
     * their parent id--but in solr)
     *
     * @param dvObjectParentIdList
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> getDvObjectInfoByParentIdForMyData(List<Long> dvObjectParentIdList) {

        String dvObjectClause = getDvObjectIdListClause(dvObjectParentIdList);
        if (dvObjectClause == null) {
            return emptyList();
        }

        String qstr = "SELECT dv.id, dv.dtype, dv.owner_id"
                + " FROM dvobject dv"
                + " WHERE  dv.owner_id IN " + dvObjectClause
                + ";";

        return em.createNativeQuery(qstr).getResultList();

    }

    /**
     * Used to calculate the dvObject tree paths for the search results on the
     * dataverse page. (In order to determine if "linked" or not).
     * *done in recursive 1 query!*
     *
     * @param objectIds
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map<Long, String> getObjectPathsByIds(Set<Long> objectIds) {
        if (objectIds == null || objectIds.size() < 1) {
            return null;
        }

        String datasetIdStr = Strings.join(objectIds, ", ");

        String qstr = "WITH RECURSIVE path_elements AS ((" +
                " SELECT id, owner_id FROM dvobject WHERE id in (" + datasetIdStr + "))" +
                " UNION\n" +
                " SELECT o.id, o.owner_id FROM path_elements p, dvobject o WHERE o.id = p.owner_id) " +
                "SELECT id, owner_id FROM path_elements";

        List<Object[]> searchResults;

        try {
            searchResults = em.createNativeQuery(qstr).getResultList();
        } catch (Exception ex) {
            searchResults = null;
        }

        if (searchResults == null || searchResults.size() < 1) {
            return null;
        }

        Map<Long, Long> treeMap = new HashMap<>();

        for (Object[] result : searchResults) {
            Long objectId;
            Long ownerId;
            if (result[0] != null) {
                try {
                    objectId = ((Integer) result[0]).longValue();
                } catch (Exception ex) {
                    logger.warning("OBJECT PATH: could not cast result[0] (dvobject id) to Integer!");
                    objectId = null;
                }
                if (objectId == null) {
                    continue;
                }

                ownerId = (Long) result[1];
                logger.fine("OBJECT PATH: id: " + objectId + ", owner: " + ownerId);
                treeMap.put(objectId, ownerId);
            }
        }

        Map<Long, String> ret = new HashMap<>();

        for (Long objectId : objectIds) {
            String treePath = "/" + objectId;
            Long treePosition = treeMap.get(objectId);

            while (treePosition != null) {
                treePath = "/" + treePosition + treePath;
                treePosition = treeMap.get(treePosition);
            }

            logger.fine("OBJECT PATH: returning " + treePath + " for " + objectId);
            ret.put(objectId, treePath);
        }
        return ret;
    }

    public String getDataverseHierarchyFor(DvObject dvObject) {
        StringBuilder path = new StringBuilder();
        if (dvObject.isNotRoot()) {
            path.append(getDataverseHierarchyFor(dvObject.getOwner()));
        }
        if (dvObject instanceof Dataverse) {
            path.append('/').append(((Dataverse) dvObject).getAlias());
        }
        return path.toString();
    }
}
