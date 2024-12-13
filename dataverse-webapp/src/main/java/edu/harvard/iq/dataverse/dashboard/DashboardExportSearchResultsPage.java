package edu.harvard.iq.dataverse.dashboard;

import static java.util.Comparator.comparing;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.NavigationWrapper;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldTypeRepository;

@SuppressWarnings("serial")
@RequestScoped
@Named("ExportSearchResultsPage")
public class DashboardExportSearchResultsPage implements Serializable {

    private final DataverseSession session;
    private final NavigationWrapper navigation;
    private final DataverseDao dataverseDao;
    private final DatasetFieldTypeRepository datasetFiledTypeRepo;

    private List<DatasetFieldType> fieldTypes;

    @Inject
    public DashboardExportSearchResultsPage(final DataverseSession session,
            final NavigationWrapper navigation,
            final DataverseDao dataverseDao, 
            final DatasetFieldTypeRepository datasetFiledTypeRepo) {
        this.session = session;
        this.navigation = navigation;
        this.dataverseDao = dataverseDao;
        this.datasetFiledTypeRepo = datasetFiledTypeRepo;
        
        this.fieldTypes = this.datasetFiledTypeRepo.findAll();
        this.fieldTypes.sort(comparing(DatasetFieldType::getTitle));
    }

    public List<DatasetFieldType> getFieldTypes() {
        return this.fieldTypes;
    }

    public String verifyAccess() {
        return this.session.canEditDashboard() ? EMPTY : this.navigation.notAuthorized();
    }

    public String save() {
        this.datasetFiledTypeRepo.saveAll(this.fieldTypes);
        return EMPTY;
    }

    public String cancel() {
        return "/dashboard.xhtml?faces-redirect=true&dataverseId="
                + this.dataverseDao.findRootDataverse().getId();
    }
}
