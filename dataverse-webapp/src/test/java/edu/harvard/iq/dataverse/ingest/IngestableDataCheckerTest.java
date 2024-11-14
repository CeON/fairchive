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

    private AbstractStringAssert<?> assertThatADATA(final String content)
            throws Exception {

        ByteBuffer buff = createBufferContaining(content);

        return assertThat(this.instance.testDTAformat(buff));
    }

    private AbstractStringAssert<?> assertThatSAV(final String content)
            throws Exception {

        ByteBuffer buff = createBufferContaining(content);

        return assertThat(this.instance.testSAVformat(buff));
    }

    @Test
    public void testingADATA_returnsMimeType_forProperContent() throws Exception {

        assertThatADATA("l   ").isEqualTo("application/x-stata");
        assertThatADATA(STATA_13_HEADER).isEqualTo("application/x-stata-13");
    }

    @Test
    public void testingADATA_returnsNull_forBrokenContent() throws Exception {

        assertThatADATA("").isNull();
        assertThatADATA("hello-non-stata-file-how-are-you").isNull();
    }

    @Test
    public void testingSAV_returnsMimeType_forProperContent() throws Exception {

        assertThatSAV("$FL2").isEqualTo("application/x-spss-sav");
    }

    @Test
    public void testingSAV_returnsNull_forBrokenContent() throws Exception {

        assertThatSAV("").isNull();
        assertThatSAV("i-am-not-a-x-spss-sav-file").isNull();
    }
}
