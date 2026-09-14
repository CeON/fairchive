package edu.harvard.iq.dataverse.engine.command.impl;

import static edu.harvard.iq.dataverse.persistence.user.Permission.AddDataverse;
import static edu.harvard.iq.dataverse.persistence.user.Permission.DeleteDataverse;
import static edu.harvard.iq.dataverse.persistence.user.Permission.ManageDataverse;
import static edu.harvard.iq.dataverse.persistence.user.Permission.MatchStrategy.atLeastOneRequired;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissionsMap;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.Permission;

public class AbstractCommandTest {
	
	private final static Set<Permission> addDataverseSet = Permission.setOf(AddDataverse);
	private final static Set<Permission> deleteDataverseSet = Permission.setOf(DeleteDataverse);
	private final static Set<Permission> addDeleteDataverseSet = 
			Permission.setOf(AddDataverse, DeleteDataverse);
	private final static Set<Permission> addDeleteManageDataverseSet = 
			Permission.setOf(AddDataverse, DeleteDataverse, ManageDataverse);

	@SuppressWarnings("serial")
	static class BaseCommand extends AbstractCommand<Dataverse> {

		BaseCommand() {
			super(new DataverseRequest(GuestUser.get(), 
					IpAddress.valueOf("0.0.0.0")), new Dataverse());
		}
		
		BaseCommand(final String name1, final String name2) {
			super(new DataverseRequest(GuestUser.get(), 
					IpAddress.valueOf("0.0.0.0")), dv(name1, new Dataverse()), 
					dv(name2, new Dataverse()));
		}

		@Override
		public Dataverse execute(final CommandContext ctxt) {
			return null;
		}
	}
	//--------------------------------------------------------------------------
	@SuppressWarnings("serial")
	static class NoPermissionsDeclared extends BaseCommand {
	}

	@Test
	void getRequiredPermissions__throws_ifNoPermissionsDeclared() {

		// given
		final Command<Dataverse> command = new NoPermissionsDeclared();

		// when & then - an undeclared command must fail loudly, never run unchecked
		assertThatThrownBy(() -> command.getRequiredPermissions())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("NoPermissionsDeclared")
			.hasMessageContaining("and its superclasses, do not declare required permissions");
	}
	//--------------------------------------------------------------------------
	@SuppressWarnings("serial")
	@RequiredPermissions(AddDataverse)
	static class OnePermissionRequired extends BaseCommand {
	}

	@Test
	void onePermissionRequired() {

		Command<Dataverse> command = new OnePermissionRequired();
		Map<String, Set<Permission>> required = command.getRequiredPermissions();
		
		assertThat(required).hasSize(1);
		assertThat(required.get("")).isEqualTo(addDataverseSet);
		
		try {
			command.verifyPermissions((req, obj) -> Permission.none());
			fail();
		} catch (final PermissionException e) {
			assertThat(e.getMessage()).startsWith("Can't execute command");
			assertThat(e.getMessage()).contains("OnePermissionRequired");
			assertThat(e.getMessage()).contains("AddDataverse");
		}
		
		assertDoesNotThrow(
				() -> command.verifyPermissions((req, obj) -> addDataverseSet));
		
		assertDoesNotThrow(
				() -> command.verifyPermissions((req, obj) -> addDeleteDataverseSet));
	}
	
	//--------------------------------------------------------------------------
	@SuppressWarnings("serial")
	@RequiredPermissions ({AddDataverse, DeleteDataverse})
	static class MultiplePermissionsRequired extends BaseCommand {
	}

	@Test
	void multiplePermissionsRequired() {

		Command<Dataverse> command = new MultiplePermissionsRequired();
		Map<String, Set<Permission>> required = command.getRequiredPermissions();
		
		assertThat(required).hasSize(1);
		assertThat(required.get("")).isEqualTo(addDeleteDataverseSet);
		
		try {
			command.verifyPermissions((req, obj) -> Permission.none());
			fail();
		} catch (final PermissionException e) {
			assertThat(e.getMessage()).startsWith("Can't execute command");
			assertThat(e.getMessage()).contains("MultiplePermissionsRequired");
			assertThat(e.getMessage()).contains("AddDataverse");
			assertThat(e.getMessage()).contains("DeleteDataverse");
		}
		
		try {
			command.verifyPermissions((req, obj) -> addDataverseSet);
			fail();
		} catch (final PermissionException e) {
			assertThat(e.getMessage()).startsWith("Can't execute command");
			assertThat(e.getMessage()).contains("MultiplePermissionsRequired");
			assertThat(e.getMessage()).contains("DeleteDataverse");
		}
		
		assertDoesNotThrow(
				() -> command.verifyPermissions((req, obj) -> addDeleteDataverseSet));
		
		assertDoesNotThrow(
				() -> command.verifyPermissions((req, obj) -> addDeleteManageDataverseSet));
	}
	
	//--------------------------------------------------------------------------
	@SuppressWarnings("serial")
	@RequiredPermissions (value = {AddDataverse, DeleteDataverse},
						  strategy = atLeastOneRequired)
	static class MultiplePermissionsOnlyOneRequired extends BaseCommand {
	}

	@Test
	void multiplePermissionsOnlyOneRequired() {

		Command<Dataverse> command = new MultiplePermissionsOnlyOneRequired();
		Map<String, Set<Permission>> required = command.getRequiredPermissions();
		
		assertThat(required).hasSize(1);
		assertThat(required.get("")).isEqualTo(addDeleteDataverseSet);
		
		try {
			command.verifyPermissions((req, obj) -> Permission.none());
			fail();
		} catch (final PermissionException e) {
			assertThat(e.getMessage()).startsWith("Can't execute command");
			assertThat(e.getMessage()).contains("MultiplePermissionsOnlyOneRequired");
			assertThat(e.getMessage()).contains("AddDataverse");
			assertThat(e.getMessage()).contains("DeleteDataverse");
		}
		
		assertDoesNotThrow(
				() -> command.verifyPermissions((req, obj) -> addDataverseSet));
		
		assertDoesNotThrow(
				() -> command.verifyPermissions((req, obj) -> addDeleteDataverseSet));
		
		assertDoesNotThrow(
				() -> command.verifyPermissions((req, obj) -> addDeleteManageDataverseSet));
	}
	
	//--------------------------------------------------------------------------
	@SuppressWarnings("serial")
	@RequiredPermissionsMap ({
			@RequiredPermissions(AddDataverse),
			@RequiredPermissions(dataverseName = "abc", value = DeleteDataverse)
	})
	static class MultinamedPermissionsRequired extends BaseCommand {
		
		MultinamedPermissionsRequired() {
		}
		
		MultinamedPermissionsRequired(final String name1, final String name2) {
			super(name1, name2);
		}
	}

	@Test
	void multinamedPermissionsRequired() {

		Command<Dataverse> command = new MultinamedPermissionsRequired("", "abc");
		Map<String, Set<Permission>> required = command.getRequiredPermissions();
		
		assertThat(required).hasSize(2);
		assertThat(required.get("")).isEqualTo(addDataverseSet);
		assertThat(required.get("abc")).isEqualTo(deleteDataverseSet);
		
		try {
			command.verifyPermissions((req, obj) -> Permission.none());
			fail();
		} catch (final PermissionException e) {
			assertThat(e.getMessage()).startsWith("Can't execute command");
			assertThat(e.getMessage()).contains("MultinamedPermissionsRequired");
			assertThat(e.getMessage()).contains("AddDataverse");
		}
			
		assertDoesNotThrow(
				() -> command.verifyPermissions((req, obj) -> addDeleteManageDataverseSet));
	}
	
	@Test
	void multinamedPermissionsRequired_throwsRuntimeException_ifMissingDvObject() {

		Command<Dataverse> command = new MultinamedPermissionsRequired();
		
		try {
			command.verifyPermissions((req, obj) -> addDeleteDataverseSet);
			fail();
		} catch (final Exception e) {
			assertThat(e.getMessage()).startsWith("Command instance");
			assertThat(e.getMessage()).contains("MultinamedPermissionsRequired");
			assertThat(e.getMessage()).contains("does not have a DvObject named 'abc'");
		}
	}
}
