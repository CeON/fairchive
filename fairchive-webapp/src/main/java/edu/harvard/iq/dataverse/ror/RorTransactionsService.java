package edu.harvard.iq.dataverse.ror;

import edu.harvard.iq.dataverse.persistence.ror.RorData;
import edu.harvard.iq.dataverse.persistence.ror.RorDataRepository;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.transaction.Transactional;

import static javax.transaction.Transactional.TxType.REQUIRES_NEW;

import java.util.Set;

/**
 * Class dedicated only for ability to control transactions.
 */
@Stateless
public class RorTransactionsService {

    private RorDataRepository rorRepository;

    // -------------------- CONSTRUCTORS --------------------

    public RorTransactionsService() { }

    @Inject
    public RorTransactionsService(final RorDataRepository rorRepository) {
        this.rorRepository = rorRepository;
    }

    // -------------------- PUBLIC --------------------

    @Transactional(REQUIRES_NEW)
    public void truncateAll() {
        this.rorRepository.truncateAll();
    }

    @Transactional(REQUIRES_NEW)
    public void saveMany(final Set<RorData> entities) {
        entities.forEach(rorRepository::save);
    }
}
