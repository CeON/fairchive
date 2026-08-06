package edu.harvard.iq.dataverse.persistence.group;

import static java.lang.String.format;
import static java.lang.System.arraycopy;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * @author michael
 */
@SuppressWarnings("serial")
public class IPv4Address extends IpAddress implements Comparable<IPv4Address> {

    public static IPv4Address valueOf(final String input) {
        final String[] comps = input.split("\\.");
        if (comps.length != 4) {
            throw new IllegalArgumentException("IPv4Address string expected to be in xxx.xxx.xxx.xxx format (only 4 byte ipv4 addresses are supported)");
        }
        final short[] arr = new short[4];
        for (int i = 0; i < 4; i++) {
            arr[i] = Short.parseShort(comps[i]);
        }
        return new IPv4Address(arr);
    }

    protected final short[] bytes = new short[4];

    public IPv4Address(short[] arr) {
        arraycopy(arr, 0, this.bytes, 0, 4);
    }

    public IPv4Address(final short a, final short b, final short c, final short d) {
        this(new short[]{a, b, c, d});
    }

    public IPv4Address(final int a, final int b, final int c, final int d) {
        this(new short[]{(short) a, (short) b, (short) c, (short) d});
    }

    public IPv4Address(final BigInteger bits) {
        this(bits.longValue());
    }

    public IPv4Address(final long l) {
        this.bytes[0] = (short) ((l >>> 24) & 0xFF);
        this.bytes[1] = (short) ((l >>> 16) & 0xFF);
        this.bytes[2] = (short) ((l >>> 8) & 0xFF);
        this.bytes[3] = (short) (l & 0xFF);
    }

    @Override
    public boolean isLocalhost() {
        return Arrays.equals(new short[]{127, 0, 0, 1}, this.bytes);
    }

    @Override
    public String toString() {
        return format("%d.%d.%d.%d", get(0), get(1), get(2), get(3));
    }

    public short get(int idx) {
        return this.bytes[idx];
    }

    public short[] getBytes() {
        return this.bytes;
    }

    public long toLong() {
        return (get(0) << 24) | (get(1) << 16) | (get(2) << 8) | get(3);
    }

    public BigInteger toBigInteger() {
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < 3; i++) {
            result = result.add(BigInteger.valueOf(get(i)))
                    .shiftLeft(8);
        }
        return result.add(BigInteger.valueOf(get(3)));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.bytes);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof IPv4Address)) {
            return false;
        }
        final IPv4Address other = (IPv4Address) obj;
        return Arrays.equals(this.bytes, other.bytes);
    }

    @Override
    public int compareTo(final IPv4Address other) {
        for (int i = 0; i < 4; i++) {
            if (get(i) != other.get(i)) {
                return get(i) - other.get(i);
            }
        }
        return 0;
    }

}
