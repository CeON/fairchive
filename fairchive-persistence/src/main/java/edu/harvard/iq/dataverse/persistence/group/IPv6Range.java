package edu.harvard.iq.dataverse.persistence.group;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * @author michael
 */
@SuppressWarnings("serial")
@Table(indexes = {@Index(columnList = "owner_id")})
@Entity
public class IPv6Range extends IpAddressRange implements Serializable {

    // Low-level bit representation of the addresses.
    long topA, topB, topC, topD;
    long bottomA, bottomB, bottomC, bottomD;

    public IPv6Range(final IPv6Address bottom, final IPv6Address top) {
        setTop(top);
        setBottom(bottom);
    }

    public IPv6Range() {
    }

    @Override
    public boolean contains(final IpAddress address) {
        if (address instanceof IPv6Address) {
            final IPv6Address ip6 = (IPv6Address) address;
            return getBottom().compareTo(ip6) <= 0 && getTop().compareTo(ip6) >= 0;
        } else {
        	return false;
        }
    }

    @Override
    public IPv6Address getTop() {
        return new IPv6Address(new long[]{topA, topB, topC, topD});
    }

    @Override
    public IPv6Address getBottom() {
        return new IPv6Address(new long[]{bottomA, bottomB, bottomC, bottomD});
    }

    public final void setTop(final IPv6Address address) {
        final long[] tArr = address.toLongArray();
        this.topA = tArr[0];
        this.topB = tArr[1];
        this.topC = tArr[2];
        this.topD = tArr[3];
    }

    public final void setBottom(IPv6Address address) {
        final long[] bArr = address.toLongArray();
        this.bottomA = bArr[0];
        this.bottomB = bArr[1];
        this.bottomC = bArr[2];
        this.bottomD = bArr[3];
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public long getTopA() {
        return this.topA;
    }

    public void setTopA(final long topA) {
        this.topA = topA;
    }

    public long getTopB() {
        return this.topB;
    }

    public void setTopB(final long topB) {
        this.topB = topB;
    }

    public long getTopC() {
        return this.topC;
    }

    public void setTopC(final long topC) {
        this.topC = topC;
    }

    public long getTopD() {
        return this.topD;
    }

    public void setTopD(final long topD) {
        this.topD = topD;
    }

    public long getBottomA() {
        return this.bottomA;
    }

    public void setBottomA(final long bottomA) {
        this.bottomA = bottomA;
    }

    public long getBottomB() {
        return this.bottomB;
    }

    public void setBottomB(final long bottomB) {
        this.bottomB = bottomB;
    }

    public long getBottomC() {
        return this.bottomC;
    }

    public void setBottomC(final long bottomC) {
        this.bottomC = bottomC;
    }

    public long getBottomD() {
        return this.bottomD;
    }

    public void setBottomD(final long bottomD) {
        this.bottomD = bottomD;
    }
}
