package edu.harvard.iq.dataverse.dashboard;

import static java.lang.Boolean.FALSE;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.io.Serializable;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldTypeRepository;
import edu.harvard.iq.dataverse.util.SystemConfig;

@ViewScoped
@Named("ExportSearchResultsPage")
public class DashboardExportSearchResultsPage implements Serializable {

    private final DataverseSession session;
    private final PermissionsWrapper permissionsWrapper;
    private final DataverseDao dataverseDao;
    private final SystemConfig systemConfig;
    private final DatasetFieldTypeRepository datasetFiledTypeRepo;

    private List<Metadata> metadataTypes;

    @Inject
    public DashboardExportSearchResultsPage(final DataverseSession session,
            final PermissionsWrapper permissionsWrapper,
            final DataverseDao dataverseDao, final SystemConfig systemConfig,
            final DatasetFieldTypeRepository datasetFiledTypeRepo) {
        this.session = session;
        this.permissionsWrapper = permissionsWrapper;
        this.dataverseDao = dataverseDao;
        this.systemConfig = systemConfig;
        this.datasetFiledTypeRepo = datasetFiledTypeRepo;
    }

    public List<Metadata> getMetadataTypes() {
        return this.metadataTypes;
    }

    public String init() {
        if (canEdit()) {
            initMetadataTypes();
            return EMPTY;
        } else {
            return this.permissionsWrapper.notAuthorized();
        }
    }

    private void initMetadataTypes() {
        this.metadataTypes = this.datasetFiledTypeRepo.findAll().stream()
                .map(Metadata::new).sorted(comparing(Metadata::getTitle))
                .collect(toList());
    }

    private boolean canEdit() {
        return !this.systemConfig.isReadonlyMode()
                && this.session.getUser().isSuperuser();
    }

    public String save() {
        for (final DatasetFieldType fieldType : this.datasetFiledTypeRepo.findAll()) {
            fieldType.setExportToFile(isExportedToFile(fieldType.getId()));
            this.datasetFiledTypeRepo.save(fieldType);
        }
        return EMPTY;
    }

    private boolean isExportedToFile(final Long id) {
        return this.metadataTypes.stream().filter(mt -> mt.getId().equals(id))
                .findFirst().map(Metadata::isExportable).orElse(FALSE);
    }

    public String cancel() {
        return "/dashboard.xhtml?faces-redirect=true&dataverseId="
                + this.dataverseDao.findRootDataverse().getId();
    }

    public static class Metadata {

        private final Long id;
        private final String title;
        private final String description;
        private boolean exportable;

        private Metadata(final DatasetFieldType fieldType) {
            this.id = fieldType.getId();
            this.title = fieldType.getTitle();
            this.description = fieldType.getDescription();
            this.exportable = fieldType.isExportToFile();
        }

        public boolean isExportable() {
            return this.exportable;
        }

        public void setExportable(final boolean exportable) {
            this.exportable = exportable;
        }

        public Long getId() {
            return this.id;
        }

        public String getTitle() {
            return this.title;
        }

        public String getDescription() {
            return this.description;
        }
    }
}
