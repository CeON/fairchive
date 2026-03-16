package edu.harvard.iq.dataverse.export.ddi;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

class DdiToHtmlTransformerTest {

    private DdiToHtmlTransformer transformer = new DdiToHtmlTransformer();

    @Test
    void transform() throws URISyntaxException, IOException {
        // given & when
        StringWriter output = new StringWriter();
        try (InputStream input = this.getClass().getClassLoader()
                .getResourceAsStream("xml/export/ddi/dataset-forHtml.xml")) {
            transformer.transform(input, output);
        }

        // then
        String transformed = output.toString();
        
        assertThat(transformed).contains("<p>DDI Html CodeBook Test</p>");
        assertThat(transformed).contains("<p>doi:10.5072/FK2/XLDMAW</p>");
        assertThat(transformed).contains("<p>CC0 Creative Commons Zero 1.0 Waiver</p>");
    }
}