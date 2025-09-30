package edu.harvard.iq.dataverse.authorization;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

import java.util.EnumSet;
import java.util.Set;

/**
 * The enum contains constants used for identification of user account fields.
 */
public enum EditableAccountField {
    NAME,
    FAMILY_NAME,
    EMAIL,
    AFFILIATION,
    AFFILIATION_ROR,
    POSITION,
    NOTIFICATIONS_LANG,
    ORCID;

    private static final Set<EditableAccountField> ALL = unmodifiableSet(
            EnumSet.allOf(EditableAccountField.class));
    private static final Set<EditableAccountField> SECONDARY = unmodifiableSet(
            EnumSet.of(AFFILIATION, POSITION, NOTIFICATIONS_LANG,
                    ORCID, AFFILIATION_ROR));

    public static Set<EditableAccountField> all() {
        return ALL;
    }

    public static Set<EditableAccountField> secondary() {
        return SECONDARY;
    }

    public static Set<EditableAccountField> none() {
        return emptySet();
    }
}
