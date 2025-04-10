package edu.harvard.iq.dataverse.validation;

import static edu.harvard.iq.dataverse.validation.ValidationResult.ok;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PeriodoValidatorTest {

    private final PeriodoValidator validator = new PeriodoValidator();

    @Test
    void validate_ok() {

        assertThat(this.validator.validate("http://n2t.net/ark:/99152/p09hq4ng7hm"))
                .isEqualTo(ok());
    }

    @Test
    void validate_invalid() {

        assertThat(this.validator.validate(null)).isNotEqualTo(ok());
        assertThat(this.validator.validate(" ")).isNotEqualTo(ok());
        assertThat(this.validator.validate("abc")).isNotEqualTo(ok());
        assertThat(this.validator.validate("http://n2t.net/ark:/99152/p0"))
                .isNotEqualTo(ok());
        assertThat(
                this.validator.validate("http://n2t.net/ark:/99152/p09hq4ng7hmxyz"))
                .isNotEqualTo(ok());
        assertThat(this.validator.validate("http://n2t.net/ark:/99152/p09hq4ng7h$"))
                .isNotEqualTo(ok());
    }
}
