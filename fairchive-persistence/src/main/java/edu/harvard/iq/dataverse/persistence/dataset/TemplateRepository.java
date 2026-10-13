package edu.harvard.iq.dataverse.persistence.dataset;


import java.util.List;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.persistence.JpaRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

@Stateless
public class TemplateRepository extends JpaRepository<Long, Template> {

    public TemplateRepository() {
    	super(Template.class);
    }

    public List<Dataverse> findDataversesByDefaultTemplateId(final Long id) {
        return this.em.createQuery(
        		 "select d from Dataverse d " +
        			        "where d.defaultTemplate.id = :id " +
        			        "order by (" +
        			        "  select dv.name from DataverseVersion dv " +
        			        "  where dv.dataverse.id = d.id " +
        			        "  and dv.createTime = (" +
        			        "    select max(dv2.createTime) from DataverseVersion dv2 " +
        			        "    where dv2.dataverse.id = d.id" +
        			        "  )" +
        			        "  and dv.state = 'RELEASED'" +
        			        ")",
        		Dataverse.class)
        		.setParameter("id", id)
        		.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<String> findDataverseNamesByDefaultTemplateId(final long id) {
        return this.em.createNativeQuery(
				"SELECT dv.name " +
				"FROM dataverse d " +
				"JOIN dataverseversion dv ON dv.dataverse_id = d.id " +
				"WHERE d.defaulttemplate_id = ?1 " +
				"AND dv.createtime = (" +
				"    SELECT MAX(dv2.createtime) " +
				"    FROM dataverseversion dv2 " +
				"    WHERE dv2.dataverse_id = d.id" +
				") " +
				"AND dv.state = 'RELEASED'" +
        		"ORDER BY dv.name")
        		.setParameter(1, id)
        		.getResultList();
    }
}
