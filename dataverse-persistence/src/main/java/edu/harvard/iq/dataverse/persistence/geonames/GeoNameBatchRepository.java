package edu.harvard.iq.dataverse.persistence.geonames;

import static javax.transaction.Transactional.TxType.REQUIRES_NEW;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

@ApplicationScoped
public class GeoNameBatchRepository {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @Transactional(value = REQUIRES_NEW)
    public void store(final Collection<GeoName> geoNames) {

        final StringBuilder builder = new StringBuilder(200);
        for (final GeoName gn : geoNames) {
            // JPA does not rupport tsvectors to we need to construct native insert
            builder.setLength(0);
            builder.append("insert into geoname")
                    .append(" (id, name, alternatenames, featurecode, countrycode, admin1code, admin2code, admin3code, admin4code, hierarchy, fullText)")
                    .append(" values (").append(gn.getId()).append(',')
                    .append("'").append(gn.getName().replace('\'', '`')).append("',")
                    .append("'").append(gn.getAlternateNames().replace('\'', '`'))
                    .append("',")
                    .append("'").append(gn.getFeatureCode().replace('\'', '`'))
                    .append("',")
                    .append("'").append(gn.getCountryCode().replace('\'', '`'))
                    .append("',");
            if (gn.getAdmin1Code() != null) {
                builder.append("'").append(gn.getAdmin1Code().replace('\'', '`'))
                        .append("',");
            } else {
                builder.append("null,");
            }
            if (gn.getAdmin2Code() != null) {
                builder.append("'").append(gn.getAdmin2Code().replace('\'', '`'))
                        .append("',");
            } else {
                builder.append("null,");
            }
            if (gn.getAdmin3Code() != null) {
                builder.append("'").append(gn.getAdmin3Code().replace('\'', '`'))
                        .append("',");
            } else {
                builder.append("null,");
            }
            if (gn.getAdmin4Code() != null) {
                builder.append("'").append(gn.getAdmin4Code().replace('\'', '`'))
                        .append("',");
            } else {
                builder.append("null,");
            }
            builder.append("'").append(gn.getHierarchy().replace('\'', '`'))
                    .append("',");
            builder.append("to_tsvector('simple', '")
                    .append(gn.getName().replace('\'', '`')).append(' ')
                    .append(gn.getAlternateNames().replace('\'', '`')).append("'))");

            this.em.createNativeQuery(builder.toString()).executeUpdate();
        }
        this.em.flush();
        this.em.clear();
    }
}
