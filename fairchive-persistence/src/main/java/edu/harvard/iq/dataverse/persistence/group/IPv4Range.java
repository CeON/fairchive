package edu.harvard.iq.dataverse.persistence.group;

import java.io.Serializable;
import java.math.BigInteger;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * A range of IPv4 addresses. In order to make SQL querying efficient, the actual fields
 * are stored as {@code long} numbers. This is why we have the {@link #getTopAsLong()} and other
 * such methods. For most non-JPA uses, use the higher API of {@link #getTop()}
 * which returns the IP address object.
 *
 * @author michael
 */
@SuppressWarnings("serial")
@Table(indexes = {@Index(columnList = "owner_id")})
@Entity
public class IPv4Range extends IpAddressRange implements Serializable {

    /**
     * The most significant bits of {@code this} range's top address, i.e the first two numbers of the IP address
     */
    private BigInteger topAsLong;

    /**
     * The least significant bits, i.e the last tow numbers of the IP address
     */
    private BigInteger bottomAsLong;

    public IPv4Range() {
    }

    public IPv4Range(final IPv4Address bottom, final IPv4Address top) {
        this.topAsLong = top.toBigInteger();
        this.bottomAsLong = bottom.toBigInteger();
    }

    @Override
    public IPv4Address getTop() {
        return new IPv4Address(getTopAsLong());
    }

    public void setTop(final IPv4Address address) {
        setTopAsLong(address.toBigInteger());
    }

    @Override
    public IPv4Address getBottom() {
        return new IPv4Address(getBottomAsLong());
    }

    public void setBottom(final IPv4Address address) {
        setBottomAsLong(address.toBigInteger());
    }

    public BigInteger getTopAsLong() {
        return this.topAsLong;
    }

    public void setTopAsLong(final BigInteger address) {
        this.topAsLong = address;
    }

    public BigInteger getBottomAsLong() {
        return this.bottomAsLong;
    }

    public void setBottomAsLong(final BigInteger address) {
        this.bottomAsLong = address;
    }

    @Override
    public boolean contains(final IpAddress address) {
        if (address instanceof IPv4Address) {
            final IPv4Address ip4 = (IPv4Address) address;
            return getBottom().compareTo(ip4) <= 0 && getTop().compareTo(ip4) >= 0;
        } else {
        	return false;
        }
    }

}
