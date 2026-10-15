package edu.harvard.iq.dataverse.engine.command.impl;

import static edu.harvard.iq.dataverse.mocks.MockRequestFactory.makeRequest;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeAuthenticatedUser;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeDatasetFieldType;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeDataverse;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeDataverseFieldTypeInputLevel;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeRole;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.nextId;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.timestamp;
import static edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole.ADMIN;
import static edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole.CURATOR;
import static edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole.EDITOR;
import static java.lang.Math.abs;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseFacetServiceBean;
import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevelServiceBean;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevelRepository;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;

/**
 * @author michael
 */
public class CreateDataverseCommandTest {

    boolean indexCalled = false;
    Map<String, Dataverse> dvByAliasStore = new HashMap<>();
    Map<Long, Dataverse> dvStore = new HashMap<>();
    boolean isRootDvExists;
    List<DataverseFieldTypeInputLevel> createdDftils;
    List<DataverseFacet> createdFacets;

    @SuppressWarnings("serial")
    DataverseDao dataverses = new DataverseDao() {
        @Override
        public boolean isRootDataverseExists() {
            return isRootDvExists;
        }

        @Override
        public Dataverse findByAlias(String anAlias) {
            return dvByAliasStore.get(anAlias);
        }

        @Override
        public Dataverse save(Dataverse dataverse) {
            if (dataverse.getId() == null) {
                dataverse.setId(nextId());
            }
            dvStore.put(dataverse.getId(), dataverse);
            if (dataverse.getAlias() != null) {
                dvByAliasStore.put(dataverse.getAlias(), dataverse);
            }
            return dataverse;
        }

    };

    @SuppressWarnings("serial")
    DataverseRoleServiceBean roles = new DataverseRoleServiceBean() {

        List<RoleAssignment> assignments = new LinkedList<>();

        Map<BuiltInRole, DataverseRole> builtInRoles;

        {
            builtInRoles = new HashMap<>();
            builtInRoles.put(EDITOR, makeRole("default-editor"));
            builtInRoles.put(ADMIN, makeRole("default-admin"));
            builtInRoles.put(CURATOR, makeRole("default-curator"));
        }

        @Override
        public DataverseRole findBuiltinRoleByAlias(BuiltInRole builtInRole) {
            return builtInRoles.get(builtInRole);
        }

        @Override
        public RoleAssignment save(RoleAssignment assignment) {
            assignment.setId(nextId());
            assignments.add(assignment);
            return assignment;
        }

        @Override
        public List<RoleAssignment> directRoleAssignments(DvObject dvObject) {
            // works since there's only one dataverse involved in the context
            // of this unit test.
            return assignments;
        }


    };

    IndexServiceBean index = new IndexServiceBean() {
        @Override
        public Future<String> indexDataverse(Dataverse dataverse) {
            indexCalled = true;
            return null;
        }
    };

    DataverseFieldTypeInputLevelServiceBean dfils = new DataverseFieldTypeInputLevelServiceBean() {
        @Override
        public void create(DataverseFieldTypeInputLevel dataverseFieldTypeInputLevel) {
            createdDftils.add(dataverseFieldTypeInputLevel);
        }

        @Override
        public void deleteFacetsFor(Dataverse d) {
        }
    };
    
    DataverseFieldTypeInputLevelRepository inputLevels = new DataverseFieldTypeInputLevelRepository() {

		@Override
		public DataverseFieldTypeInputLevel save(DataverseFieldTypeInputLevel entity) {
			return entity;
		}
    };
    
    

    @SuppressWarnings("serial")
    DataverseFacetServiceBean facets = new DataverseFacetServiceBean() {
//        @Override
//        public DataverseFacet create(int displayOrder, DatasetFieldType fieldType, Dataverse ownerDv) {
//            DataverseFacet df = new DataverseFacet();
//            df.setDatasetFieldType(fieldType);
//            df.setDataverse(ownerDv);
//            df.setDisplayOrder(displayOrder);
//            createdFacets.add(df);
//            return df;
//        }


        @Override
        public void deleteFacetsFor(Dataverse d) {
        }
        
        @Override
        public void save(final DataverseFacet facet) {
        	
        }

    };

    TestDataverseEngine engine;


    @BeforeEach
    public void setUp() {
        indexCalled = false;
        dvStore.clear();
        dvByAliasStore.clear();
        isRootDvExists = true;
        createdDftils = new ArrayList<>();
        createdFacets = new ArrayList<>();

        engine = new TestDataverseEngine(new TestCommandContext() {
            @Override
            public IndexServiceBean index() {
                return index;
            }

            @Override
            public DataverseRoleServiceBean roles() {
                return roles;
            }

            @Override
            public DataverseDao dataverses() {
                return dataverses;
            }

            @Override
            public DataverseFacetServiceBean facets() {
                return facets;
            }

            @Override
            public DataverseFieldTypeInputLevelServiceBean fieldTypeInputLevels() {
                return dfils;
            }
            @Override
            public DataverseFieldTypeInputLevelRepository inputLevels() {
            	return inputLevels;
            }

        });
    }


    @Test
    public void testDefaultOptions() {
        Dataverse dv = makeDataverse();
        dv.setCreateDate(null);
        dv.setId(null);
        dv.setCreator(null);
        dv.setDefaultDatasetContributorRole(null);
        dv.setOwner(makeDataverse());
        final DataverseRequest request = makeRequest(makeAuthenticatedUser("jk", "rollin'"));

        CreateDataverseCommand sut = new CreateDataverseCommand(dv, request, null, null);
        Dataverse result = engine.submit(sut);

        assertNotNull(result.getCreateDate());
        assertNotNull(result.getId());

        assertEquals(result.getCreator(), request.getUser());
        assertEquals(Dataverse.DataverseType.UNCATEGORIZED, result.getDataverseType());
        assertEquals(roles.findBuiltinRoleByAlias(BuiltInRole.EDITOR), result.getDefaultDatasetContributorRole());

        // Assert that the creator is admin.
        final RoleAssignment roleAssignment = roles.directRoleAssignments(dv).get(0);
        assertEquals(roles.findBuiltinRoleByAlias(BuiltInRole.ADMIN), roleAssignment.getRole());
        assertEquals(dv, roleAssignment.getDefinitionPoint());
        assertEquals(roleAssignment.getAssigneeIdentifier(), request.getUser().getIdentifier());

        // The following is a pretty weird way to test that the create date defaults to
        // now, but it works across date changes.
        assertTrue(abs(currentTimeMillis() - result.getCreateDate().toInstant().toEpochMilli()) < 1000, 
        		"When the supplied creation date is null, date shuld default to command execution time");

        assertTrue(result.isPermissionRoot());
        assertTrue(result.isThemeRoot());
        assertTrue(indexCalled);
    }

    @Test
    public void testCustomOptions() {
        Dataverse dv = makeDataverse();

        Timestamp creation = timestamp(1990, 12, 12);
        AuthenticatedUser creator = makeAuthenticatedUser("Joe", "Walsh");

        dv.setCreateDate(creation);

        dv.setId(null);
        dv.setCreator(creator);
        dv.setDefaultDatasetContributorRole(null);
        dv.setOwner(makeDataverse());
        dv.getVersions().get(0).setDataverseType(Dataverse.DataverseType.JOURNALS);
        dv.setDefaultDatasetContributorRole(roles.findBuiltinRoleByAlias(CURATOR));

        final DataverseRequest request = makeRequest(creator);
        List<DatasetFieldType> expectedFacets = 
        		asList(makeDatasetFieldType(), makeDatasetFieldType(), makeDatasetFieldType());
        List<DataverseFieldTypeInputLevel> dftils = 
        		asList(makeDataverseFieldTypeInputLevel(makeDatasetFieldType()),
                       makeDataverseFieldTypeInputLevel(makeDatasetFieldType()),
                       makeDataverseFieldTypeInputLevel(makeDatasetFieldType()));

        CreateDataverseCommand sut = new CreateDataverseCommand(dv, request,
        		new LinkedList<>(expectedFacets), new LinkedList<>(dftils));
        Dataverse result = engine.submit(sut);

        assertEquals(creation, result.getCreateDate());
        assertNotNull(result.getId());

        assertEquals(creator, result.getCreator());
        assertEquals(Dataverse.DataverseType.JOURNALS, result.getDataverseType());
        assertEquals(roles.findBuiltinRoleByAlias(CURATOR), result.getDefaultDatasetContributorRole());

        
        // Assert that the creator is admin.
        final RoleAssignment roleAssignment = roles.directRoleAssignments(dv).get(0);
        assertEquals(roles.findBuiltinRoleByAlias(ADMIN), roleAssignment.getRole());
        assertEquals(dv, roleAssignment.getDefinitionPoint());
        assertEquals(roleAssignment.getAssigneeIdentifier(), request.getUser().getIdentifier());
        

        assertTrue(result.isPermissionRoot());
        assertTrue(result.isThemeRoot());
        assertTrue(indexCalled);

        int i = 0;
        for (DataverseFacet df : createdFacets) {
            assertEquals(i, df.getDisplayOrder());
            assertEquals(result, df.getDataverse());
            assertEquals(expectedFacets.get(i), df.getDatasetFieldType());

            i++;
        }

        for (DataverseFieldTypeInputLevel dftil : createdDftils) {
            assertEquals(result, dftil.getDataverse());
        }
    }

    @Test
    public void testCantCreateAdditionalRoot() throws Exception {
        assertThrows(IllegalCommandException.class, () ->
                engine.submit(new CreateDataverseCommand(makeDataverse(), makeRequest(), null, null)));
    }

    @Test
    public void testGuestCantCreateDataverse() throws Exception {
        final DataverseRequest request = new DataverseRequest(GuestUser.get(), IpAddress.valueOf("::"));
        isRootDvExists = false;
        assertThrows(IllegalCommandException.class, () ->
                engine.submit(new CreateDataverseCommand(makeDataverse(), request, null, null)));
    }

    @Test
    public void testCantCreateAnotherWithSameAlias() throws Exception {

        String alias = "alias";
        final Dataverse dvFirst = makeDataverse();
        dvFirst.getVersions().get(0).setAlias(alias);
        dvFirst.setOwner(makeDataverse());
        assertThrows(IllegalCommandException.class, () ->
                engine.submit(new CreateDataverseCommand(dvFirst, makeRequest(), null, null)));

        final Dataverse dv = makeDataverse();
        dv.setOwner(makeDataverse());
        dv.getVersions().get(0).setAlias(alias);
        assertThrows(IllegalCommandException.class, () ->
                engine.submit(new CreateDataverseCommand(dv, makeRequest(), null, null)));
    }

}
