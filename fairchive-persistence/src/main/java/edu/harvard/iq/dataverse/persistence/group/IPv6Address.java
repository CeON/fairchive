package edu.harvard.iq.dataverse.persistence.group;

import java.util.Arrays;

/**
 * @author michael
 */
@SuppressWarnings("serial")
public class IPv6Address extends IpAddress implements Comparable<IPv6Address> {

    public static IPv6Address valueOf(String in) {
        if (in.contains("%")) {
            // remove network interface name, if present.
            in = in.split("%")[0];
        }
        if (in.contains(".")) {
            return valueOfMapped(in);
        }
        if (in.contains("::")) {
            // expand the :: abbreviation
            int existingFields = 0;
            for (final String cmp : in.split(":")) {
                if (!cmp.trim().isEmpty()) {
                    existingFields++;
                }
            }

            final int missingFieldCount = 8 - existingFields;
            final StringBuilder builder = new StringBuilder(in.startsWith("::") ? "" : ":");
            for (int i = 0; i < missingFieldCount; i++) {
                builder.append("0:");
            }
            if (in.endsWith("::")) {
                builder.setLength(builder.length() - 1);
            }
            in = in.replace("::", builder.toString());

        }

        // Invariant: in is expanded (no "::" abbreviation)
        final String[] comps = in.split(":", -1);
        if (comps.length != 8) {
            throw new IllegalArgumentException("IPv6 requires 8 words (or the usage of the :: abbreviation)");
        }
        final int[] words = new int[8];

        // Invariant: in is of the form "n:n:n:n:n:n:n:n", where n is hopefully a hex number.
        int wordIdx = 0;
        for (final String comp : comps) {
            try {
                words[wordIdx++] = Integer.parseInt(comp, 16);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Numbers in IPv6 addresses should be in hexadecimal notation.", nfe);
            }
        }

        return new IPv6Address(words, false);
    }

    public static IPv6Address valueOfMapped(final String in) {
        // Split parts
        final int lastColon = in.lastIndexOf(":");
        final String ipv4Part = in.substring(lastColon + 1);
        final String ipv6Part = in.substring(0, lastColon + 1) + "0:0";

        // Parse
        final short[] ipv4bytes = IPv4Address.valueOf(ipv4Part).bytes;
        final int[] ipv6words = IPv6Address.valueOf(ipv6Part).words;

        // merge
        ipv6words[6] = (((int) ipv4bytes[0]) << 8) + ipv4bytes[1];
        ipv6words[7] = (((int) ipv4bytes[2]) << 8) + ipv4bytes[3];
        return new IPv6Address(ipv6words);
    }

    private final int[] words;

    /**
     * Constructor that does not copy the int array - but can be used
     * only from within this class. Especially made for the {@link valueOf} method.
     *
     * @param words
     * @param dummy
     */
    private IPv6Address(final int[] words, final boolean dummy) {
        this.words = words;
    }

    public IPv6Address(final int[] words) {
        if (words.length != 8) {
            throw new IllegalArgumentException("IPv6 address requires exactly 8 ints. Consider using the valueOf method to support abbreviations");
        }
        this.words = Arrays.copyOf(words, words.length);
    }

    public IPv6Address(final long[] longs) {
        this.words = new int[]{
                (int) (longs[0] >>> 32),
                (int) (longs[0] & 0xffffffffl),
                (int) (longs[1] >>> 32),
                (int) (longs[1] & 0xffffffffl),
                (int) (longs[2] >>> 32),
                (int) (longs[2] & 0xffffffffl),
                (int) (longs[3] >>> 32),
                (int) (longs[3] & 0xffffffffl)
        };
    }

    public IPv6Address(final int w1, final int w2, final int w3, final int w4, 
    		final int w5, final int w6, final int w7, final int w8) {
        this.words = new int[]{w1, w2, w3, w4, w5, w6, w7, w8};
    }

    public int get(final int idx) {
        return this.words[idx];
    }

    public long[] toLongArray() {
        final long[] result = new long[4];
        for (int i = 0; i < 4; i++) {
            result[i] = this.words[2 * i];
            result[i] = (result[i] << 32);
            result[i] = result[i] + this.words[2 * i + 1];
        }
        return result;
    }

    @Override
    public int hashCode() {
        return  Arrays.hashCode(this.words);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof IPv6Address)) {
            return false;
        }
        final IPv6Address other = (IPv6Address) obj;
        return Arrays.equals(this.words, other.words);
    }

    @Override
    public boolean isLocalhost() {
        return Arrays.equals(this.words, new int[]{0, 0, 0, 0, 0, 0, 0, 1});
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < this.words.length; i++) {
            result.append(Integer.toString(this.words[i], 16))
                    .append(i < 7 ? ":" : "");
        }
        return result.toString();
    }

    @Override
    public int compareTo(final IPv6Address other) {
        for (int i = 0; i < 8; i++) {
            if (get(i) != other.get(i)) {
                return get(i) - other.get(i);
            }
        }
        return 0;
    }
}