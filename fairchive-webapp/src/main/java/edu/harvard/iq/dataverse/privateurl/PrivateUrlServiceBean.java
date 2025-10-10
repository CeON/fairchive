package edu.harvard.iq.dataverse.privateurl;

import java.io.Serializable;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.persistence.user.PrivateUrlUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignmentRepository;
import edu.harvard.iq.dataverse.util.SystemConfig;

/**
 * PrivateUrlServiceBean depends on Glassfish and Postgres being available and
 * it is tested with API tests in DatasetIT. Code that can execute without any
 * runtime dependencies should be put in PrivateUrlUtil so it can be unit
 * tested.
 */
@SuppressWarnings("serial")
@Stateless
public class PrivateUrlServiceBean implements Serializable {

    @EJB
    private RoleAssignmentRepository roleAssignmentRepo;

    @EJB
    DatasetService datasetService;

    @EJB
    SystemConfig systemConfig;

    /**
     * @return A PrivateUrl if the dataset has one or null.
     */
    public PrivateUrl getPrivateUrlFromDatasetId(long datasetId, final boolean anonymized) {
        RoleAssignment roleAssignment = this.roleAssignmentRepo.getPrivateUrlRoleAssignmentFromDataset(datasetService.find(datasetId), anonymized);
        return PrivateUrlUtil.getPrivateUrlFromRoleAssignment(roleAssignment, systemConfig.getDataverseSiteUrl());
    }

    /**
     * @return A PrivateUrlUser if one can be found using the token or null.
     */
    public PrivateUrlUser getPrivateUrlUserFromToken(String token) {
        return PrivateUrlUtil.getPrivateUrlUserFromRoleAssignment(this.roleAssignmentRepo.getRoleAssignmentFromPrivateUrlToken(token));
    }

    /**
     * @return PrivateUrlRedirectData if it can be found using the token or
     * null.
     */
    public PrivateUrlRedirectData getPrivateUrlRedirectDataFromToken(String token) {
        return PrivateUrlUtil.getPrivateUrlRedirectData(this.roleAssignmentRepo.getRoleAssignmentFromPrivateUrlToken(token));
    }



}
