package edu.harvard.iq.dataverse.dashboard;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.NavigationWrapper;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldTypeRepository;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlockRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.util.UIMessages;

@SuppressWarnings("serial")
@ViewScoped
@Named("ExportSearchResultsPage")
public class DashboardExportSearchResultsPage implements Serializable {

    private final DataverseSession session;
    private final NavigationWrapper navigation;
    private final DataverseDao dataverseDao;
    private final DatasetFieldTypeRepository datasetFieldTypeRepo;
    private final MetadataBlockRepository metadataBlockRepo;
    private final UIMessages uiMessages;

    private List<MetadataBlock> blocks;
    private Dataverse root;

    @Inject
    public DashboardExportSearchResultsPage(final DataverseSession session,
            final NavigationWrapper navigation,
            final DataverseDao dataverseDao, 
            final MetadataBlockRepository metadataBlockRepo,
            final DatasetFieldTypeRepository datasetFieldTypeRepo,
            final UIMessages uiMessages) {
        this.session = session;
        this.navigation = navigation;
        this.dataverseDao = dataverseDao;
        this.metadataBlockRepo = metadataBlockRepo;
        this.datasetFieldTypeRepo = datasetFieldTypeRepo;
        this.uiMessages = uiMessages;
    }
    
    @PostConstruct
    public void init() {
    	this.root = this.dataverseDao.findRootDataverse();
        this.blocks = this.metadataBlockRepo.findSystemMetadataBlocks();
    }
    
    public List<MetadataBlock> getBlocks() {
        return this.blocks;
    }		
    
    public long getExportedCount() {
    	return this.blocks.stream()
    			.map(MetadataBlock::getDatasetFieldTypes)
    			.flatMap(List::stream)
    			.filter(DatasetFieldType::isExportToFile)
    			.count();
    }

    public String verifyAccess() {
        return this.session.canEditDashboard() ? EMPTY : this.navigation.notAuthorized();
    }
    
	public void selectAll(final MetadataBlock block, final boolean select) {
		block.getDatasetFieldTypes().stream()
			.filter(DatasetFieldType::isExportableToFile)
			.forEach(type -> type.setExportToFile(select));
	}

    public String save() {   
    	final Stream<DatasetFieldType> types = this.blocks.stream()
                .map(MetadataBlock::getDatasetFieldTypes)
                .flatMap(List::stream);
    	this.datasetFieldTypeRepo.saveAll(types);
        this.uiMessages.addSuccessMessage(getStringFromBundle("dashboard.card.exportsearchresults.success"));
        return EMPTY;
    }
    
    public Dataverse getRootDataverse() {
    	return this.root;
    }

    public String cancel() {
        return "/dashboard.xhtml?faces-redirect=true&dataverseId="
                + getRootDataverse().getId();
    }
}

