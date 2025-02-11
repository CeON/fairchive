package edu.harvard.iq.dataverse.persistence.datafile;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Stateless
public class ExternalToolRepository extends JpaRepository<Long, ExternalTool> {

    public ExternalToolRepository() {
        super(ExternalTool.class);
    }
}
