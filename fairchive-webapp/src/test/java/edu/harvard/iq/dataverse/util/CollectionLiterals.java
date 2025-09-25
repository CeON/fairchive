package edu.harvard.iq.dataverse.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author michael
 */
public class CollectionLiterals {

    public static <T> Set<T> setOf(@SuppressWarnings("unchecked") T... args) {
        return new HashSet<>(Arrays.asList(args));
    }

    public static <T> List<T> listOf(@SuppressWarnings("unchecked") T... args) {
        return Arrays.asList(args);
    }
}
