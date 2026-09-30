package edu.harvard.iq.dataverse.feedback;

import io.vavr.control.Option;

/**
 * Contact information of a feedback recipient.
 */
public class FeedbackContact {

    private final String name;
    private final String email;

    // -------------------- CONSTRUCTORS --------------------

    public FeedbackContact(final String email) {
        this(null, email);
    }

    public FeedbackContact(final String name, String email) {
        this.name = name;
        this.email = email;
    }

    // -------------------- GETTERS --------------------

    public Option<String> getName() {
        return Option.of(this.name);
    }

    public String getEmail() {
        return this.email;
    }
}
