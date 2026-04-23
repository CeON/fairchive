package edu.harvard.iq.dataverse.persistence.dataverse.link;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Stateless;

import java.util.List;
import java.util.Optional;

@Stateless
public class DataverseLinkingDataverseRepository extends JpaRepository<Long, DataverseLinkingDataverse> {

    // -------------------- CONSTRUCTORS --------------------

    public DataverseLinkingDataverseRepository() {
        super(DataverseLinkingDataverse.class);
    }

    // -------------------- LOGIC --------------------

    public Optional<DataverseLinkingDataverse> findByDataverseIdAndLinkingDataverseId(Long dataverseId, Long linkingDataverseId) {
        String query = "SELECT OBJECT(o) FROM DataverseLinkingDataverse AS o WHERE o.linkingDataverse.id = :linkingDataverseId AND o.dataverse.id = :dataverseId";

        return JpaRepository.getSingleResult(createQuery(query)
                    .setParameter("dataverseId", dataverseId)
                    .setParameter("linkingDataverseId", linkingDataverseId));
    }

    public List<DataverseLinkingDataverse> findByLinkingDataverseId(Long linkingDataverseId) {
        String query = "select object(o) from DataverseLinkingDataverse as o where o.linkingDataverse.id =:linkingDataverseId order by o.id";
        return createQuery(query)
                .setParameter("linkingDataverseId", linkingDataverseId)
                .getResultList();
    }
    
    public List<DataverseLinkingDataverse> findByDataverseId(Long dataverseId) {
        String query = "select object(o) from DataverseLinkingDataverse as o where o.dataverse.id =:dataverseId order by o.id";
        return createQuery(query)
                .setParameter("dataverseId", dataverseId)
                .getResultList();
    }
}
