package edu.harvard.iq.dataverse.persistence.datafile;

import java.util.List;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.persistence.JpaRepository;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.Type;

@Stateless
public class ExternalToolRepository extends JpaRepository<Long, ExternalTool> {

    // -------------------- CONSTRUCTORS --------------------

    public ExternalToolRepository() {
        super(ExternalTool.class);
    }

    // -------------------- LOGIC --------------------

    public List<ExternalTool> findByType(final Type type, final String contentType) {
        return this.em.createQuery(
                "SELECT OBJECT(o) FROM ExternalTool AS o " +
                        "WHERE o.type = :type AND o.contentType = :contentType",
                ExternalTool.class)
                .setParameter("type", type)
                .setParameter("contentType", contentType)
                .getResultList();
    }

    public List<ExternalTool> findByType(final Type type) {
        return this.em.createQuery(
                "SELECT OBJECT(o) FROM ExternalTool AS o WHERE o.type = :type",
                ExternalTool.class)
                .setParameter("type", type)
                .getResultList();
    }
}
