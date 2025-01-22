package edu.harvard.iq.dataverse.persistence.datafile.license;

import static java.util.Locale.CANADA;
import static java.util.Locale.CHINA;
import static java.util.Locale.ITALY;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class LicenseTest {

    @Test
    public void containsNameWithLocale() {
        
        License license = new License();

        assertThat(license.containsNameWithLocale(CANADA)).isFalse();
        
        license.addLocalizedName(new LocaleText(CANADA, "abc"));
        license.addLocalizedName(new LocaleText(CHINA, "abc"));
        
        assertThat(license.containsNameWithLocale(CANADA)).isTrue();
        assertThat(license.containsNameWithLocale(ITALY)).isFalse();
    }
}
