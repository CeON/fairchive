package edu.harvard.iq.dataverse.engine.command.impl;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.notification.NotificationObjectType.DATAVERSE_VERSION;
import static edu.harvard.iq.dataverse.notification.NotificationParameter.MESSAGE;
import static edu.harvard.iq.dataverse.notification.NotificationParameter.REQUESTOR_ID;
import static edu.harvard.iq.dataverse.persistence.dataverse.DataverseLock.Reason.InReview;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.SUBMITTEDDV;
import static edu.harvard.iq.dataverse.persistence.user.Permission.PublishDataverse;
import static java.time.Instant.now;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseLock;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.Permission;

@SuppressWarnings("serial")
@RequiredPermissions(Permission.EditDataverse)
public class SubmitDataverseForReviewCommand extends AbstractCommand<Dataverse> {

	private Dataverse dataverse;
    private final String comment;

    public SubmitDataverseForReviewCommand(final DataverseRequest request, 
    		final Dataverse dataverse, final String comment) {
    	
        super(request, dataverse);
        this.dataverse = dataverse;
        this.comment = comment;
    }

    @Override
    public Dataverse execute(final CommandContext context)  {

        if (! this.dataverse.hasDraft()) {
            throw new IllegalCommandException(getStringFromBundle("dataset.submit.failure.isReleased"), this);
        }
        if (this.dataverse.isInReview()) {
            throw new IllegalCommandException(getStringFromBundle("dataset.submit.failure.inReview"), this);
        }

        this.dataverse.addLock(
        		new DataverseLock(InReview, getRequest().getAuthenticatedUser(), 
        				this.comment));
        this.dataverse = context.dataverseRepository().save(this.dataverse);
        sendNotification(context, this.dataverse);
        context.index().indexDataverse(this.dataverse);
        
        return this.dataverse;
    }

    private void sendNotification(final CommandContext context, final Dataverse dataverse) {
        final List<AuthenticatedUser> curators = context.permissions().
        		getUsersWithPermissionOn(PublishDataverse, dataverse);
        final AuthenticatedUser requestor = (AuthenticatedUser) getUser();
        final Timestamp timestamp = Timestamp.from(now());
        final Map<String, String> parameters = new HashMap<>();
        parameters.put(REQUESTOR_ID.key(), requestor.getId().toString());
        parameters.put(MESSAGE.key(), this.comment);

        curators.forEach(c -> context.notifications()
                .sendNotificationWithEmail(c, timestamp, SUBMITTEDDV,
                        this.dataverse.getLatestVersion().getId(), DATAVERSE_VERSION, parameters));
    }
}
