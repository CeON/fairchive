package edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.persistence.ActionLogRecord;
import edu.harvard.iq.dataverse.persistence.group.IPv4Address;
import edu.harvard.iq.dataverse.persistence.group.IPv6Address;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.group.IpGroup;
import edu.harvard.iq.dataverse.persistence.group.IpGroupRepository;

/**
 * Provides CRUD tools to efficiently manage IP groups in a Java EE container.
 *
 * @author michael
 */
@Stateless
public class IpGroupsServiceBean {
    
    @Inject 
    private IpGroupRepository groupRepository;

    @EJB
    ActionLogServiceBean actionLogSvc;

    @EJB
    RoleAssigneeServiceBean roleAssigneeSvc;

    /**
     * Stores (inserts/updates) the passed IP group.
     *
     * @param grp The group to store.
     * @return Managed version of the group.
     */
    public IpGroup store(IpGroup grp) {
        ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.GlobalGroups, "ipCreate");

        alr.setInfo(grp.getIdentifier() + "// " + grp.getRanges());

        if (grp.getId() == null) {
            if (grp.getPersistedGroupAlias() != null) {
                IpGroup existing = getByGroupName(grp.getPersistedGroupAlias());
                if (existing == null) {
                    // new group
                	this.groupRepository.save(grp);
                    actionLogSvc.log(alr);
                    return grp;

                } else {
                    existing.setDescription(grp.getDescription());
                    existing.setDisplayName(grp.getDisplayName());
                    existing.setIpv4Ranges(grp.getIpv4Ranges());
                    existing.setIpv6Ranges(grp.getIpv6Ranges());
                    actionLogSvc.log(alr.setActionSubType("ipUpdate"));
                    return existing;
                }
            } else {
                actionLogSvc.log(alr);
                this.groupRepository.save(grp);
                return grp;
            }
        } else {
            actionLogSvc.log(alr.setActionSubType("ipUpdate"));
            return this.groupRepository.save(grp);
        }
    }

    public IpGroup get(final long id) {
        return this.groupRepository.findById(id).orElse(null);
    }

    public IpGroup getByGroupName(final String alias) {
    	return this.groupRepository.getByAlias(alias).orElse(null);
    }

    public List<IpGroup> findAll() {
        return this.groupRepository.findAll();
    }

    public Set<IpGroup> findAllIncludingIp(final IpAddress ipa) {
        if (ipa instanceof IPv4Address) {
            final IPv4Address ip4 = (IPv4Address) ipa;
            return new HashSet<>(this.groupRepository.findContainingV4Address(ip4.toBigInteger()));
        } else if (ipa instanceof IPv6Address) {
            final IPv6Address ip6 = (IPv6Address) ipa;
            return new HashSet<>(this.groupRepository.findContainingV6Address(ip6.toLongArray()));
        } else {
            throw new IllegalArgumentException("Unknown IpAddress type: " + 
            		ipa.getClass() + " (for IpAddress:" + ipa + ")");
        }
    }

    /**
     * Deletes the group - if it has no assignments.
     *
     * @param grp the group to be deleted
     * @throws IllegalArgumentException if the group has assignments
     * @see RoleAssigneeServiceBean#getAssignmentsFor(java.lang.String)
     */
    public void deleteGroup(IpGroup grp) {
        ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.GlobalGroups, "ipDelete");
        alr.setInfo(grp.getIdentifier());
        if (roleAssigneeSvc.getAssignmentsFor(grp.getIdentifier()).isEmpty()) {
        	this.groupRepository.delete(grp);
            actionLogSvc.log(alr);

        } else {
            String failReason = "Group " + grp.getAlias() + " has assignments and thus can't be deleted.";
            alr.setActionResult(ActionLogRecord.Result.BadRequest);
            alr.setInfo(alr.getInfo() + "// " + failReason);
            actionLogSvc.log(alr);
            throw new IllegalArgumentException(failReason);
        }
    }
}
