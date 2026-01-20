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
                    "SELECT logo FROM dataversetheme WHERE dataverse_id = " + id)
                    .getSingleResult());
        } catch (final NoResultException e) {
            return Optional.empty();
        }
    }
}
