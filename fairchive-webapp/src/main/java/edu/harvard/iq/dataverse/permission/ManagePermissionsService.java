package edu.harvard.iq.dataverse.permission;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseDefaultContributorRoleCommand;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.notification.UserNotificationService;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.group.ExplicitGroup;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import io.vavr.control.Try;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("serial")
@Stateless
public class ManagePermissionsService implements Serializable {

    private static final Logger logger = Logger.getLogger(ManagePermissionsService.class.getCanonicalName());

    private EjbDataverseEngine commandEngine;
    private DataverseRequestServiceBean dvRequestService;
    private UserNotificationService userNotificationService;
    private RoleAssigneeServiceBean roleAssigneeService;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public ManagePermissionsService() {
    }

    @Inject
    public ManagePermissionsService(EjbDataverseEngine commandEngine, DataverseRequestServiceBean dvRequestService,
                                    UserNotificationService userNotificationService, RoleAssigneeServiceBean roleAssigneeService) {
        this.commandEngine = commandEngine;
        this.dvRequestService = dvRequestService;
        this.userNotificationService = userNotificationService;
        this.roleAssigneeService = roleAssigneeService;
    }

    // -------------------- LOGIC --------------------

    public RoleAssignment assignRoleWithNotification(DataverseRole role, RoleAssignee roleAssignee, DvObject object) {
            Try<RoleAssignment> assignRoleOperation = Try.of(() -> commandEngine.submit(new AssignRoleCommand(roleAssignee, role, object, dvRequestService.getDataverseRequest(), null)))
                .onSuccess(roleAssignment -> {
                    if (shouldUserBeNotified(role, object)) {
                        notifyRoleChange(roleAssignee, object, NotificationType.ASSIGNROLE);
                    }
                })
                .onFailure(throwable -> logger.log(Level.SEVERE, "Role assignment failed.", throwable));

         return assignRoleOperation.get();
    }

    /***
     * For unpublished dataverses and datasets we do not send a notification
     * if the assigned role do not allow to go to the dataverse or dataset page.
     * If we would send such notification then the user would obtain a notification
     * with a link that he cannot access.
     * Sending this notification will be postponed when objects will be published.
     */
    private boolean shouldUserBeNotified(DataverseRole role, DvObject object) {
        if (object.isReleased()) {
            return true;
        }
        if (object.isInstanceofDataverse()) {
            return role.has(Permission.ViewUnpublishedDataverse);
        }
        return role.has(Permission.ViewUnpublishedDataset);
    }


    public Void removeRoleAssignmentWithNotification(RoleAssignment roleAssignment) {
        Try<Void> removeOperation = Try.run(() -> commandEngine.submit(new RevokeRoleCommand(roleAssignment, dvRequestService.getDataverseRequest())))
                .onSuccess(Void -> {
                    RoleAssignee assignee = roleAssigneeService.getRoleAssignee(roleAssignment.getAssigneeIdentifier());
                    DvObject dvObject = roleAssignment.getDefinitionPoint();
                    notifyRoleChange(assignee, dvObject, NotificationType.REVOKEROLE);
                })
                .onFailure(throwable -> logger.log(Level.SEVERE, "Role assignment removal failed.", throwable));

        return removeOperation.get();
    }

    public DataverseRole saveOrUpdateRole(DataverseRole role) {
        return commandEngine.submit(new CreateRoleCommand(role, dvRequestService.getDataverseRequest(), (Dataverse) role.getOwner()));
    }

    public Dataverse setDataverseDefaultContributorRole(DataverseRole defaultRole, Dataverse affectedDataverse) {
        return commandEngine.submit(new UpdateDataverseDefaultContributorRoleCommand(defaultRole, dvRequestService.getDataverseRequest(), affectedDataverse));
    }

    // -------------------- PRIVATE ---------------------
    /**
     * Notify a {@code RoleAssignee} that a role was either assigned or revoked.
     * Will notify all members of a group.
     *
     * @param ra   The {@code RoleAssignee} to be notified.
     * @param type The type of notification.
     */
    private void notifyRoleChange(RoleAssignee ra, DvObject dvObject, String type) {
        if (ra instanceof AuthenticatedUser) {
            userNotificationService.sendNotificationWithEmail((AuthenticatedUser) ra, Timestamp.from(Instant.now()), type, dvObject.getId(), determineObjectType(dvObject));
        } else if (ra instanceof ExplicitGroup) {
            ExplicitGroup eg = (ExplicitGroup) ra;

            eg.getContainedRoleAssgineeIdentifiers().stream()
                    .map(roleId -> roleAssigneeService.getRoleAssignee(roleId))
                    .filter(roleAssignee -> roleAssignee instanceof AuthenticatedUser)
                    .forEach(explicitGroupMember -> userNotificationService.sendNotificationWithEmail((AuthenticatedUser) explicitGroupMember,
                            Timestamp.from(Instant.now()), type, dvObject.getId(), determineObjectType(dvObject)));
        }
    }

    private NotificationObjectType determineObjectType(DvObject dvObject) {

        if (dvObject instanceof Dataverse) {
            return NotificationObjectType.DATAVERSE;
        }
        if (dvObject instanceof Dataset) {
            return NotificationObjectType.DATASET;
        }

        return NotificationObjectType.DATAFILE;
    }
}
