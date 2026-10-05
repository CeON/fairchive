package edu.harvard.iq.dataverse.persistence.dataverse;

import java.util.Optional;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;

import edu.harvard.iq.dataverse.persistence.JpaRepository;
@Stateless
public class DataverseThemeRepository extends JpaRepository<Long, DataverseTheme> {

    public DataverseThemeRepository() {
        super(DataverseTheme.class);
    }

    public Optional<String> findLogoByDataverseId(final Long id) {
        try {
            return Optional.ofNullable((String) this.em.createNativeQuery(
            		"SELECT dt.logo " +
                            "FROM dataversetheme dt " +
                            "JOIN dataverseversion dv ON dv.id = dt.version_id " +
                            "WHERE dv.dataverse_id = ? " +
                            "AND dv.state = 'RELEASED' " +
                            "ORDER BY dv.createtime DESC " +
                            "LIMIT 1")
            		.setParameter(1, id)
                    .getSingleResult());
        } catch (final NoResultException e) {
            return Optional.empty();
        }
    }
}
