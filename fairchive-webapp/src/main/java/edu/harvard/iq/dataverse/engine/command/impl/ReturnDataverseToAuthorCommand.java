package edu.harvard.iq.dataverse.engine.command.impl;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.notification.NotificationObjectType.DATAVERSE_VERSION;
import static edu.harvard.iq.dataverse.persistence.dataverse.DataverseLock.Reason.InReview;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.RETURNEDDV;
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
import edu.harvard.iq.dataverse.notification.NotificationParameter;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowComment;

@SuppressWarnings("serial")
@RequiredPermissions(Permission.PublishDataverse)
public class ReturnDataverseToAuthorCommand extends AbstractCommand<Dataverse> {

	private Dataverse dataverse;
	private final String comment;
	private final String replyTo;
	private final boolean sendCopy;

    public ReturnDataverseToAuthorCommand(final DataverseRequest request, 
    		final Dataverse dataverse, final String comment, final String replyTo, 
    		final boolean sendCopy) {
        super(request, dataverse);
        this.dataverse = dataverse;
        this.comment = comment;
        this.replyTo = replyTo;
        this.sendCopy = sendCopy;
    }

    @Override
    public Dataverse execute(CommandContext context)  {

        if (!this.dataverse.isInReview()) {
            throw new IllegalCommandException(
            		getStringFromBundle("datverse.reject.datasetNotInReview"), this);
        }

        final WorkflowComment workflowComment = new WorkflowComment(this.dataverse.getEditableVersion(),
                WorkflowComment.Type.RETURN_TO_AUTHOR, this.comment, (AuthenticatedUser) this.getUser());
        context.datasets().addWorkflowComment(workflowComment);

        this.dataverse.removeLocks(InReview);
        this.dataverse = context.dataverseRepository().save(this.dataverse);

        sendNotification(context);
        context.index().indexDataverse(this.dataverse);

        return this.dataverse;
    }

    private void sendNotification(final CommandContext context) {
        final AuthenticatedUser requestor = (AuthenticatedUser) getUser();

        //    So what we're doing here is sending notifications to the authors
        //    who do not have publish permissions.
        //    First get users who can publish - or in this case review
        //    Then get authors.
        //    Then remove reviewers from the autors list
        //    Finally send a notification to the remaining (non-reviewing) authors.
        final List<AuthenticatedUser> authors = context.permissions().
        		getUsersWithPermissionOn(Permission.EditDataverse, this.dataverse);
        authors.removeAll(context.permissions().
        		getUsersWithPermissionOn(Permission.PublishDataverse, this.dataverse));

        final Map<String, String> parameters = new HashMap<>();
        parameters.put(NotificationParameter.MESSAGE.key(), this.comment);
        parameters.put(NotificationParameter.REPLY_TO.key(), this.replyTo);
        parameters.put(NotificationParameter.SEND_COPY.key(), String.valueOf(this.sendCopy));
        parameters.put(NotificationParameter.REQUESTOR_ID.key(), requestor.getId().toString());
        authors.forEach(a -> context.notifications()
                .sendNotificationWithEmail(a, Timestamp.from(now()), RETURNEDDV,
                    this.dataverse.getLatestVersion().getId(), DATAVERSE_VERSION, parameters));
    }
}
