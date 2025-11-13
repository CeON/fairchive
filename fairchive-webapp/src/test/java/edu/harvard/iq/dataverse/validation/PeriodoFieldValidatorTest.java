package edu.harvard.iq.dataverse.validation;

import static edu.harvard.iq.dataverse.validation.field.FieldValidationResult.ok;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.harvard.iq.dataverse.search.periodo.Period;
import edu.harvard.iq.dataverse.search.periodo.PeriodoDataFinder;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import edu.harvard.iq.dataverse.validation.field.validators.PeriodoFieldValidator;

@ExtendWith(MockitoExtension.class)
public class PeriodoFieldValidatorTest {

    @Mock
    private PeriodoDataFinder periods;

    private PeriodoFieldValidator validator;

    @BeforeEach
    void setUp() {
        this.validator = new PeriodoFieldValidator(this.periods);
    }

    @Test
    void validate_ok() {
        when(this.periods.getByUrl(anyString())).thenReturn(Optional.of(new Period()));

        assertThat(validate("http://n2t.net/ark:/99152/p09hq4ng7hm"))
                .isEqualTo(ok());
    }

    @Test
    void validate_invalid() {
        when(this.periods.getByUrl(anyString())).thenReturn(Optional.empty());
        
        assertThat(validate(null)).isNotEqualTo(ok());
        assertThat(validate(" ")).isNotEqualTo(ok());
        assertThat(validate("abc")).isNotEqualTo(ok());
        assertThat(validate("http://n2t.net/ark:/99152/p0"))
                .isNotEqualTo(ok());
        assertThat(validate("http://n2t.net/ark:/99152/p09hq4ng7hmxyz"))
                .isNotEqualTo(ok());
        assertThat(validate("http://n2t.net/ark:/99152/p09hq4ng7h$"))
                .isNotEqualTo(ok());

        assertThat(validate("http://n2t.net/ark:/99152/p0f65r2v400").getErrorCode())
                .isEqualTo("periodo.invalid.id");
    }

    private FieldValidationResult validate(final String periodoUrl) {
        return this.validator.validateValue(periodoUrl, null, null, null);
    }
}
