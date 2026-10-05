package edu.harvard.iq.dataverse.engine.command.impl;

import static edu.harvard.iq.dataverse.mocks.MockRequestFactory.makeRequest;
import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.PasswordEncryption;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.BuiltinUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.persistence.user.User;

@ExtendWith(MockitoExtension.class)
public class ResetPasswordCommandTest {

    @Mock
    private BuiltinUserServiceBean builtinusers;
    
    private TestDataverseEngine testEngine;
    
    private AuthenticatedUser targetUser;
    
    @BeforeEach
    void beforeEach() {
    	targetUser = new AuthenticatedUser();
    	targetUser.setUserIdentifier("test");

    	testEngine = new TestDataverseEngine(new TestCommandContext() {
            @Override
            public BuiltinUserServiceBean builtinUsers() {
            	return builtinusers;
            }
        });
    }
    
    @Test
    public void testResetPassword_notAuthenticatedNorSuperuser() throws Exception {
    
        // Execute
        ResetPasswordCommand command = new ResetPasswordCommand(targetUser, makeRequest(getUser(false)));

        // Asserts
        assertThatThrownBy(() -> testEngine.submit(command)).isInstanceOf(PermissionException.class);
        Mockito.verify(builtinusers, Mockito.times(0)).save(Mockito.any());
    }

    
    @Test
    public void testResetPassword_authenticatedNotSuperuser() throws Exception {
    
        // Execute
        ResetPasswordCommand command = new ResetPasswordCommand(targetUser, makeRequest(getAuthenticatedUser(false)));

        // Asserts
        assertThatThrownBy(() -> testEngine.submit(command)).isInstanceOf(PermissionException.class);
        Mockito.verify(builtinusers, Mockito.times(0)).save(Mockito.any());
    }

    @Test
    public void testResetPassword_targetUserAsBuiltin() throws Exception {
    
    	// Given
    	BuiltinUser builtinuser = new BuiltinUser();
    	builtinuser.setEncryptedPassword("1234567890");
        
    	Mockito.when(builtinusers.findByUserName("test")).thenReturn(builtinuser);
    	
    	// Execute
        ResetPasswordCommand command = new ResetPasswordCommand(targetUser, makeRequest(getAuthenticatedUser(true)));
        testEngine.submit(command);
        
        // Asserts
        assertNotEquals(builtinuser.getEncryptedPassword(), "1234567890");
        assertEquals(builtinuser.getPasswordEncryptionVersion(), PasswordEncryption.getLatestVersionNumber());
        Mockito.verify(builtinusers, Mockito.times(1)).save(Mockito.any());
    }

    @Test
    public void testResetPassword_targetUserNotBuiltin() throws Exception {
    
    	// Given
    	Mockito.when(builtinusers.findByUserName("test")).thenReturn(null);
    	
    	// Execute
        ResetPasswordCommand command = new ResetPasswordCommand(targetUser, makeRequest(getAuthenticatedUser(true)));
        testEngine.submit(command);

        // Asserts
        Mockito.verify(builtinusers, Mockito.times(0)).save(Mockito.any());

    }

    @SuppressWarnings("serial")
    private User getUser(boolean superuser) {
    	return new User() {
			
			@Override
			public String getIdentifier() {
				return "test";
			}
			
			@Override
			public RoleAssigneeDisplayInfo getDisplayInfo() {
				return new RoleAssigneeDisplayInfo("Test", null);			}
			
			@Override
			public boolean isSuperuser() {
				return superuser;
			}
			
			@Override
			public boolean isAuthenticated() {
				return true;
			}
			
			@Override
			public Locale getNotificationsLanguage() {
		        return ENGLISH;
			}
		};
    }

    @SuppressWarnings("serial")
    private User getAuthenticatedUser(boolean superuser) {
    	return new AuthenticatedUser() {
			
			@Override
			public String getIdentifier() {
				return "test";
			}
			
			@Override
			public boolean isSuperuser() {
				return superuser;
			}
			
			@Override
			public boolean isAuthenticated() {
				return true;
			}
			
			@Override
			public Locale getNotificationsLanguage() {
		        return ENGLISH;
			}
		};
    }
}
