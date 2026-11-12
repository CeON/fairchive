package edu.harvard.iq.dataverse.engine.command.impl;

import static edu.harvard.iq.dataverse.persistence.dataverse.DataverseLock.Reason.InReview;
import static edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion.State.RELEASED;

import java.sql.Timestamp;
import java.util.Date;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.Permission;

@SuppressWarnings("serial")
@RequiredPermissions(Permission.PublishDataverse)
public class PublishDataverseCommand extends AbstractCommand<Dataverse> {

    private Dataverse dataverse;

    public PublishDataverseCommand(final DataverseRequest aRequest, 
    		final Dataverse dataverse) {
        super(aRequest, dataverse);
        this.dataverse = dataverse;
    }

    @Override
    public Dataverse execute(final CommandContext context)  {
        if (this.dataverse.isReleased() && ! this.dataverse.hasDraft()) {
            throw new IllegalCommandException("Dataverse " + 
                        this.dataverse.getAlias() + 
                        " has already been published.", this);
        }

        final Dataverse parent = this.dataverse.getOwner();
        // root dataverse doesn't have a parent
        if (parent != null && !parent.isReleased()) {
            throw new IllegalCommandException("Dataverse " + dataverse.getAlias() 
                + " may not be published because its host dataverse (" 
                    + parent.getAlias() + ") has not been published.", this);
        }

        //Before setting dataverse to released send notifications to users with download file
        sendAssignRoleNotifications(context);

        if(! this.dataverse.isReleased()) {
	        this.dataverse.setPublicationDate(new Timestamp(new Date().getTime()));
	        this.dataverse.setReleaseUser((AuthenticatedUser) getUser());
        }
        this.dataverse.getLatestVersion().setState(RELEASED);
        context.dataverseVersions().save(this.dataverse.getLatestVersion());
        this.dataverse.removeLocks(InReview);
        this.dataverse = context.dataverses().save(this.dataverse);
        /**
         * @todo consider also
         * ctxt.solrIndex().indexPermissionsOnSelfAndChildren(savedDataverse.getId());
         */
        context.index().delete(this.dataverse);
        context.index().indexDataverse(this.dataverse);
        context.solrIndex().indexPermissionsForOneDvObject(this.dataverse);
        return this.dataverse;
    }

    /**
     * Sends notifications about assigned roles in a dataverse, but only
     * when role did not allow to view unpublished dataverse.
     * Since for this cases we postponed sending the notification
     * until the dataverse can be accessed and now when dataverse is
     * published it can be accessed by anyone
     */
    private void sendAssignRoleNotifications(CommandContext ctxt) {
        ctxt.roles().directRoleAssignments(dataverse).stream()
            .filter(ra -> !ra.has(Permission.ViewUnpublishedDataverse))
            .flatMap(ra -> ctxt.roleAssignees().getExplicitUsers(ctxt.roleAssignees().getRoleAssignee(ra.getAssigneeIdentifier())).stream())
            .distinct()
            .forEach(au -> ctxt.notifications().sendNotificationWithEmail(
                                au, new Timestamp(new Date().getTime()), NotificationType.ASSIGNROLE,
                                dataverse.getId(), NotificationObjectType.DATAVERSE));
    }
}
