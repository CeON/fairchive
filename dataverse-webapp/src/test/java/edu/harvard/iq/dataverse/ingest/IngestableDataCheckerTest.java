package edu.harvard.iq.dataverse.ingest;

import static edu.harvard.iq.dataverse.ingest.IngestableDataChecker.STATA_13_HEADER;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;

public class IngestableDataCheckerTest {

    private final IngestableDataChecker instance = new IngestableDataChecker();

    private ByteBuffer createBufferContaining(final String fileContents)
            throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.put(fileContents.getBytes());
        buffer.rewind();

        return buffer;
    }

    private AbstractStringAssert<?> assertThatADATAFormat(final String content)
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
        assertThatADATAFormat("l   ").isEqualTo("application/x-stata");
        assertThatADATAFormat(STATA_13_HEADER).isEqualTo("application/x-stata-13");
    }

    @Test
    public void testADATAformat_returnsNull_forBrokenContent() throws Exception {
        assertThatADATAFormat("").isNull();
        assertThatADATAFormat("hello-non-stata-file-how-are-you").isNull();
    }

    @Test
    public void testingSAVformat_returnsMimeType_forProperContent() throws Exception {
        assertThatSAVFormat("$FL2").isEqualTo("application/x-spss-sav");
    }

    @Test
    public void testingSAVformat_returnsNull_forBrokenContent() throws Exception {
        assertThatSAVFormat("").isNull();
        assertThatSAVFormat("i-am-not-a-x-spss-sav-file").isNull();
    }
}
