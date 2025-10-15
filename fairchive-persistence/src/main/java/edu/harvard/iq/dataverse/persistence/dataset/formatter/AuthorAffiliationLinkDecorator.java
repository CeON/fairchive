package edu.harvard.iq.dataverse.persistence.dataset.formatter;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import io.vavr.control.Option;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;

public class AuthorAffiliationLinkDecorator implements DatasetFieldFormattedValueDecorator {

    // -------------------- LOGIC --------------------

    /**
     * Returns the pre-formatted value decorated as a link with title
     */
    @Override
    public Option<String> decorate(DatasetField field, String formattedValue) {

        return Option.of(formattedValue.replace("target=\"_blank\"", "target=\"_blank\" title=\""
                + getStringFromBundle("opensInNewTab")
                + "\""));
    }
}
