/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.datavariable;

import edu.harvard.iq.dataverse.persistence.datafile.datavariable.DataVariable;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.VariableMetadata;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * @author Leonid Andreev
 * <p>
 * Basic skeleton of the new DataVariable service for DVN 4.0
 */

@Stateless
public class VariableServiceBean {
    public static final String[] summaryStatisticTypes = 
        {"mean", "medn", "mode", "vald", "invd", "min", "max", "stdev"};

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public DataVariable save(final DataVariable variable) {
        return this.em.merge(variable);
    }

    public DataVariable find(Object pk) {
        return this.em.find(DataVariable.class, pk);
    }

    public List<DataVariable> findByDataTableId(final Long dtId) {
        return this.em.createQuery(
                "select object(o) from DataVariable as o " +
                "where o.dataTable.id =:id order by o.fileOrder",
                DataVariable.class)
                .setParameter("id", dtId)
                .getResultList();
    }

    public List<VariableMetadata> findByDataVarIdAndFileMetaId(final Long datVarId,
            final Long metaId) {
        return this.em.createQuery(
                "SELECT object(o) FROM VariableMetadata as o " + 
                "where o.dataVariable.id =:dvId and o.fileMetadata.id =:fmId",
                VariableMetadata.class)
                .setParameter("dvId", datVarId)
                .setParameter("fmId", metaId)
                .getResultList();
    }
}
