package edu.harvard.iq.dataverse.persistence.group;

import static javax.persistence.CascadeType.ALL;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

@SuppressWarnings("serial")
@NamedQueries({
    @NamedQuery(name = "IpGroup.findAll",
            query = "SELECT g FROM IpGroup g"),
    @NamedQuery(name = "IpGroup.findByPersistedGroupAlias",
            query = "SELECT g FROM IpGroup g WHERE g.persistedGroupAlias=:persistedGroupAlias")
})
@Entity
public class IpGroup extends PersistedGlobalGroup {

    public final static String GROUP_TYPE = "ip";
    
    @OneToMany(mappedBy = "owner", cascade = ALL, orphanRemoval = true)
    private Set<IPv6Range> ipv6Ranges = new HashSet<>();

    @OneToMany(mappedBy = "owner", cascade = ALL, orphanRemoval = true)
    private Set<IPv4Range> ipv4Ranges = new HashSet<>();

    public IpGroup() {
        super(GROUP_TYPE);
    }

    public boolean containsAddress(final IpAddress addr) {
        for (IpAddressRange range : ((addr instanceof IPv4Address) ? this.ipv4Ranges : this.ipv6Ranges)) {
            final Boolean contains = range.contains(addr);
            if ((contains != null) && contains) {
                return true;
            }
        }
        return false;
    }

    public <T extends IpAddressRange> T add(final T range) {
        range.setOwner(this);
        if (range instanceof IPv4Range) {
            this.ipv4Ranges.add((IPv4Range) range);
        } else {
            this.ipv6Ranges.add((IPv6Range) range);
        }
        return range;
    }

    public void remove(final IpAddressRange range) {
        ((range instanceof IPv4Range) ? this.ipv4Ranges : this.ipv6Ranges).remove(range);
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    /**
     * Returns a <strong>read only</strong> set of all the ranges  in the group,
     * both IPv6 and IPv4.
     *
     * @return
     */
    public Set<IpAddressRange> getRanges() {
        final Set<IpAddressRange> ranges = new HashSet<>();
        ranges.addAll(getIpv4Ranges());
        ranges.addAll(getIpv6Ranges());
        return ranges;
    }

    /**
     * Low-level JPA accessor
     *
     * @return
     * @see #getRanges()
     */
    public Set<IPv6Range> getIpv6Ranges() {
        return this.ipv6Ranges;
    }

    /**
     * Low-level JPA accessor
     *
     * @param ipv6Ranges
     */
    public void setIpv6Ranges(final Set<IPv6Range> ranges) {
        this.ipv6Ranges = ranges;
        updateOwnership(this.ipv6Ranges);
    }

    /**
     * Low-level JPA accessor
     *
     * @return
     * @see #getRanges()
     */
    public Set<IPv4Range> getIpv4Ranges() {
        return this.ipv4Ranges;
    }

    public void setIpv4Ranges(final Set<IPv4Range> ranges) {
        this.ipv4Ranges = ranges;
        updateOwnership(this.ipv4Ranges);
    }

    @Override
    public boolean equals(final Object o) {
        if(o != null && o.getClass().equals(getClass())) {
	        final IpGroup other = (IpGroup) o;
	        return Objects.equals(getId(), other.getId())
	        		&& Objects.equals(getDescription(), other.getDescription())
	        		&& Objects.equals(getDisplayName(), other.getDisplayName())
	        		&& Objects.equals(this.ipv4Ranges, other.ipv4Ranges) 
	        		&& Objects.equals(this.ipv6Ranges, other.ipv6Ranges);
        } else {
        	return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "[IpGroup id:" + getId() + " ranges:" + getIpv4Ranges() + "," + getIpv6Ranges() + "]";
    }

    private void updateOwnership(final Collection<? extends IpAddressRange> ranges) {
    	ranges.forEach(range -> range.setOwner(this));
    }
}
