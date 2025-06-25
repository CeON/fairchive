package edu.harvard.iq.dataverse.ingest;

import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static edu.harvard.iq.dataverse.ingest.IngestableDataChecker.STATA_13_HEADER;
import static org.assertj.core.api.Assertions.assertThat;

public class IngestableDataCheckerTest {

    private final IngestableDataChecker instance = new IngestableDataChecker();

    private ByteBuffer createBufferContaining(final String fileContents)
            throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.put(fileContents.getBytes());
        buffer.rewind();

        return buffer;
    }

    private AbstractStringAssert<?> assertThatDTAFormat(final String content)
            throws Exception {
        ByteBuffer buff = createBufferContaining(content);

        return assertThat(this.instance.testDTAformat(buff));
    }

    private AbstractStringAssert<?> assertThatSAVFormat(final String content)
            throws Exception {
        ByteBuffer buff = createBufferContaining(content);

        return assertThat(this.instance.testSAVformat(buff));
    }

    @Test
    public void testADATAformat_returnsMimeType_forProperContent() throws Exception {
        assertThatDTAFormat("l   ").isEqualTo("application/x-stata");
        assertThatDTAFormat(STATA_13_HEADER).isEqualTo("application/x-stata-13");
    }

    @Test
    public void testADATAformat_returnsNull_forBrokenContent() throws Exception {
        assertThatDTAFormat("").isNull();
        assertThatDTAFormat("hello-non-stata-file-how-are-you").isNull();
    }

    @Test
    public void testSAVformat_returnsMimeType_forProperContent() throws Exception {
        assertThatSAVFormat("$FL2").isEqualTo("application/x-spss-sav");
    }

    @Test
    public void testSAVformat_returnsNull_forBrokenContent() throws Exception {
        assertThatSAVFormat("").isNull();
        assertThatSAVFormat("i-am-not-a-x-spss-sav-file").isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "sav/frequency-test.sav,            application/x-spss-sav ",
            "dta/stata13-auto.dta,              application/x-stata-13",
            "dta/dates.dta,                     application/x-stata-14",
            "dta/50by1000.dta,                  application/x-stata",
            "ingest/example-1.por,              application/x-spss-por",
            "ingest/test_file_compressed.rda,   application/x-rlang-transport",
            "ingest/test_file.rda,              application/x-rlang-transport",
    })
    public void detectTabularDataFormat(String file, String expectedFormat) {
        assertThat(instance.detectTabularDataFormat(getFile(file)))
                .isEqualTo(expectedFormat);
    }

    private File getFile(String resourcesPath) {
        return new File(getClass().getClassLoader().getResource(resourcesPath).getFile());
    }
}
