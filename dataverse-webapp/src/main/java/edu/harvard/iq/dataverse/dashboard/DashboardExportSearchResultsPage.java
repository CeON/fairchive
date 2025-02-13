package edu.harvard.iq.dataverse.dashboard;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.NavigationWrapper;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldTypeRepository;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlockRepository;
import edu.harvard.iq.dataverse.util.UIMessages;

@SuppressWarnings("serial")
@ViewScoped
@Named("ExportSearchResultsPage")
public class DashboardExportSearchResultsPage implements Serializable {

    private final DataverseSession session;
    private final NavigationWrapper navigation;
    private final DataverseDao dataverseDao;
    private final DatasetFieldTypeRepository datasetFiledTypeRepo;
    private final MetadataBlockRepository metadataBlockRepo;
    private final UIMessages uiMessages;

    private List<MetadataBlock> blocks;

    @Inject
    public DashboardExportSearchResultsPage(final DataverseSession session,
            final NavigationWrapper navigation,
            final DataverseDao dataverseDao, 
            final MetadataBlockRepository metadataBlockRepo,
            final DatasetFieldTypeRepository datasetFiledTypeRepo,
            final UIMessages uiMessages) {
        this.session = session;
        this.navigation = navigation;
        this.dataverseDao = dataverseDao;
        this.metadataBlockRepo = metadataBlockRepo;
        this.datasetFiledTypeRepo = datasetFiledTypeRepo;
        this.uiMessages = uiMessages;
    }
    
    @PostConstruct
    public void init() {
        this.blocks = this.metadataBlockRepo.findSystemMetadataBlocks();
    }
    
    public List<MetadataBlock> getBlocks() {
        return this.blocks;
    }

    public String verifyAccess() {
        return this.session.canEditDashboard() ? EMPTY : this.navigation.notAuthorized();
    }

    public String save() {      
        this.blocks.stream()
            .map(MetadataBlock::getDatasetFieldTypes).forEach(this.datasetFiledTypeRepo::saveAll);
        this.uiMessages.addSuccessMessage(getStringFromBundle("dashboard.card.exportsearchresults.success"));
        return EMPTY;
    }

    public String cancel() {
        return "/dashboard.xhtml?faces-redirect=true&dataverseId="
                + this.dataverseDao.findRootDataverse().getId();
    }
}

