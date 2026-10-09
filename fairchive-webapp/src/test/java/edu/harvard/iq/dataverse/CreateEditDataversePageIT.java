package edu.harvard.iq.dataverse;

import javax.inject.Inject;

import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.dataverse.CreateEditDataversePage;
import edu.harvard.iq.dataverse.dataverse.DataverseService;
import edu.harvard.iq.dataverse.dataverse.MetadataBlockService;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository;
import edu.harvard.iq.dataverse.util.SystemConfig;

@Transactional(TransactionMode.ROLLBACK)
public class CreateEditDataversePageIT extends WebappArquillianDeployment {
	
	@Inject
    private DatasetFieldServiceBean datasetFieldService;
	@Inject
    private MetadataBlockService metadataBlockService;
	@Inject
    private DataverseService dataverseService;
	@Inject
	private PermissionServiceBean permissionService;
	@Inject
    private DataverseSession session;
	@Inject
    private SystemConfig config;
	@Inject
	private AuthenticatedUserRepository userRepository;
	

	private DataverseRequestServiceBean requestService;
	private NavigationWrapper navigationWrapper;
    private PermissionsWrapper permissionsWrapper;
	private CreateEditDataversePage page;
	
	
	@BeforeEach
	void setUp() {
		
		this.requestService = new DataverseRequestServiceBean(this.session, null);
		this.navigationWrapper = new NavigationWrapper(this.session);
		this.permissionsWrapper = new PermissionsWrapper(this.permissionService, 
				this.requestService, this.navigationWrapper);
		
		this.page = new CreateEditDataversePage(this.dataverseService, 
				this.datasetFieldService, this.metadataBlockService, 
				this.permissionsWrapper, this.session, this.config);
		
		this.session.logIn(this.userRepository.getById(1L));
	}
	
	@Test 
	void test1() {
		
		this.page.setDataverseId(1L);
		this.page.init();
	}

}
