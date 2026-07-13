package edu.harvard.iq.dataverse;

import static org.apache.commons.lang3.StringUtils.isNumeric;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.DvObjectRepository;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

@Stateless
public class DvObjectDao {

    private DataverseDao dataverseDao;
    private DatasetService datasetService;
    private DvObjectRepository dvObjectRepository;

    @Inject
    public DvObjectDao(final DataverseDao dataverseDao, 
    		final DatasetService datasetService, 
    		final DvObjectRepository dvObjectRepository) {
        this.dataverseDao = dataverseDao;
        this.datasetService = datasetService;
        this.dvObjectRepository = dvObjectRepository;
    }

    public DvObjectDao() {
    }

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    protected EntityManager em;

    /**
     * Tries to find a DvObject. If the passed id can be interpreted as a number,
     * it tries to get the DvObject by its id. Else, it tries to get a {@link Dataverse}
     * with that alias. If that fails, tries to get a {@link Dataset} with that global id.
     *
     * @param id a value identifying the DvObject, either numeric of textual.
     * @return A DvObject, or {@code null}
     */
    public DvObject findDvo(String id) {
        if (isNumeric(id)) {
            return this.dvObjectRepository.getById(Long.valueOf(id));
        } else {
            final Dataverse d = dataverseDao.findByAlias(id);
            return (d != null) ? d : datasetService.findByGlobalId(id);
        }
    }
}
