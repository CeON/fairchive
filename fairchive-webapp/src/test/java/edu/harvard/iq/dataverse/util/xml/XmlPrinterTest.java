package edu.harvard.iq.dataverse.util.xml;

import org.junit.jupiter.api.Test;

import static edu.harvard.iq.dataverse.util.xml.XmlPrinter.prettyPrintXml;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class XmlPrinterTest {

    @Test
    public void testPrettyPrintXmlShort() {

    	String result = prettyPrintXml("<foo><bar>baz</bar></foo>");
        
        assertThat(result).containsSubsequence(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<foo>",
                "  <bar>baz</bar>",
                "</foo>"
        );
    }

    @Test
    public void testPrettyPrintXmlNonXML() {

        assertThat(prettyPrintXml("THIS IS NOT XML")).isEqualTo("THIS IS NOT XML");
    }

    @Test
    public void testPrettyPrintXmlEmptyString() {

        assertThat(prettyPrintXml("")).isEmpty();
    }

    @Test
    public void testPrettyPrintXmlNull() {

        assertThatNullPointerException().isThrownBy(() -> prettyPrintXml(null));
    }
}
