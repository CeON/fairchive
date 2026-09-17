package edu.harvard.iq.dataverse.dataverse;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.annotations.PermissionNeeded;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.interceptors.LoggedCall;
import edu.harvard.iq.dataverse.interceptors.Restricted;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.link.DataverseLinkingDataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.link.DataverseLinkingDataverseRepository;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;

/**
 * @author skraffmiller
 */
@SuppressWarnings("serial")
@Stateless
public class DataverseLinkingService implements java.io.Serializable {

    @Inject
    private DataverseLinkingDataverseRepository repository;
    @Inject
    private IndexServiceBean indexService;
    @Inject
    private DataverseSession session;

    // -------------------- LOGIC --------------------

    public List<Dataverse> findLinkedDataverses(final Long linkingDataverseId) {
        return this.repository.findByLinkingDataverseId(linkingDataverseId).stream()
                .map(DataverseLinkingDataverse::getDataverse)
                .collect(toList());
    }

    public List<Dataverse> findLinkingDataverses(final Long dataverseId) {
        return this.repository.findByDataverseId(dataverseId).stream()
                .map(DataverseLinkingDataverse::getLinkingDataverse)
                .collect(toList());
    }

    public DataverseLinkingDataverse findDataverseLinkingDataverse(
    		final Long dataverseId, final Long linkingDataverseId) {
        return this.repository.findByDataverseIdAndLinkingDataverseId(dataverseId, 
        		linkingDataverseId)
                .orElse(null);
    }

    public boolean alreadyLinked(final Dataverse definitionPoint, 
    		final Dataverse dataverseToLinkTo) {
        return this.repository.findByDataverseIdAndLinkingDataverseId(
        		dataverseToLinkTo.getId(), definitionPoint.getId()).isPresent();
    }

    /**
     * Operation to link one dataverse to the other.
     */
    @LoggedCall
    @Restricted(@PermissionNeeded(needs = {Permission.PublishDataset}))
    @TransactionAttribute(REQUIRES_NEW)
    public DataverseLinkingDataverse saveLinkedDataverse(
            final @PermissionNeeded Dataverse dataverseToBeLinked, 
            final Dataverse dataverse) {

        if (!this.session.isSuperUserLoggedIn()) {
            throw new PermissionException(
            		"Link Dataverse can only be called by superusers.",
                    singleton(Permission.PublishDataverse), dataverseToBeLinked);
        }
        if (dataverse.equals(dataverseToBeLinked)) {
            throw new IllegalCommandException("Can't link a dataverse to itself");
        }
        if (dataverse.getOwners().contains(dataverseToBeLinked)) {
            throw new IllegalCommandException("Can't link a dataverse to its parents");
        }

        DataverseLinkingDataverse dataverseLinkingDataverse = new DataverseLinkingDataverse();
        dataverseLinkingDataverse.setDataverse(dataverse);
        dataverseLinkingDataverse.setLinkingDataverse(dataverseToBeLinked);
        dataverseLinkingDataverse.setLinkCreateTime(new Timestamp(new Date().getTime()));
        dataverseLinkingDataverse = this.repository.save(dataverseLinkingDataverse);
        this.indexService.indexDataverse(dataverse);
        return dataverseLinkingDataverse;
    }
}
