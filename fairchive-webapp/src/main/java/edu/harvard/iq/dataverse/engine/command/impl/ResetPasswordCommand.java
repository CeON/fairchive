package edu.harvard.iq.dataverse.engine.command.impl;

import java.io.Serializable;

import org.apache.commons.lang3.RandomStringUtils;

import edu.harvard.iq.dataverse.authorization.providers.builtin.PasswordEncryption;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.BuiltinUser;

/**
 * Reset password for user.
 */
@SuppressWarnings("serial")
// the permission annotation is open, since this is a superuser-only command - 
// and that's enforced in the command body:
@RequiredPermissions({})
public class ResetPasswordCommand extends AbstractCommand<AuthenticatedUser> implements Serializable {

    private final AuthenticatedUser targetUser;
    
    public ResetPasswordCommand(AuthenticatedUser targetUser, DataverseRequest aRequest) {
        super(aRequest, (Dataverse) null);
        this.targetUser = targetUser;
    }

    @Override
    public AuthenticatedUser execute(CommandContext ctxt) {
        
    	if (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser()) {
            throw new PermissionException("Reset Password command can only be called by superusers.",
                    this, null, null);
        }

        try {
            BuiltinUser builtinuser = ctxt.builtinUsers().findByUserName(targetUser.getUserIdentifier());
        	
        	String newHashedPass = PasswordEncryption.get().encrypt(RandomStringUtils.randomAscii(20));
        	
            int latestVersionNumber = PasswordEncryption.getLatestVersionNumber();
            builtinuser.updateEncryptedPassword(newHashedPass, latestVersionNumber);
            ctxt.builtinUsers().save(builtinuser);

            return targetUser;

        } catch (Exception ex) {
            throw new CommandException("Failed to reset password", this);
        }
    }
}
