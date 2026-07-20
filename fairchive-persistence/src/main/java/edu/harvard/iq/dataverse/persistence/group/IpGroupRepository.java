package edu.harvard.iq.dataverse.persistence.group;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import javax.ejb.Singleton;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@SuppressWarnings("serial")
@Singleton
public class IpGroupRepository extends JpaRepository<Long, IpGroup> implements Serializable {

    public IpGroupRepository() {
        super(IpGroup.class);
    }
    
    public Optional<IpGroup> getByAlias(final String alias) {
    	return getSingleResult(createQuery(
    			"SELECT g FROM IpGroup g WHERE g.persistedGroupAlias=:alias")
    			.setParameter("alias", alias));
    }
    
    public List<IpGroup> findContainingV4Address(final BigInteger address) {
    	
    	return createQuery(
    			"SELECT DISTINCT r.owner from IPv4Range r " + 
    			" WHERE r.bottomAsLong<=:address AND r.topAsLong>=:address")
                .setParameter("address", address)
                .getResultList();
    }
    
    public List<IpGroup> findContainingV6Address(final long[] abcd) {
    	return createQuery(
    			"SELECT DISTINCT r.owner FROM IPv6Range r " +
                "WHERE " +
                "( (r.topA>:a) " +
                "or (r.topA=:a and r.topB>:b) " +
                "or (r.topA=:a and r.topB=:b and r.topC>:c) " +
                "or (r.topA=:a and r.topB=:b and r.topC=:c and r.topD>=:d))" +
                " and ( (r.bottomA<:a) " +
                "or (r.bottomA=:a and r.bottomB<:b) " +
                "or (r.bottomA=:a and r.bottomB=:b and r.bottomC<:c) " +
                "or (r.bottomA=:a and r.bottomB=:b and r.bottomC=:c and r.bottomD<=:d) )")
                .setParameter("a", abcd[0])
                .setParameter("b", abcd[1])
                .setParameter("c", abcd[2])
                .setParameter("d", abcd[3])
                .getResultList();
    }
}
