/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/
package edu.harvard.iq.dataverse.ingest;


import static java.lang.Math.min;
import static org.apache.commons.lang.builder.ToStringStyle.MULTI_LINE_STYLE;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a virtually unchanged DVN v2-3 implementation by
 *
 * @author Akio Sone
 * <p>
 * incorporated into 4.0 by
 * @author Leonid Andreev
 */
@SuppressWarnings("serial")
public class IngestableDataChecker implements java.io.Serializable {

    private static Logger log = LoggerFactory.getLogger(IngestableDataChecker.class);

    // supported formats
    private static final String[] TABULAR_DATA_FORMAT_SET = {"POR", "SAV", "DTA", "RDA"};

    // Map that returns a Stata Release number
    private static Map<Byte, String> stataReleaseNumber = new HashMap<Byte, String>();
    public static String STATA_13_HEADER = "<stata_dta><header><release>117</release>";
    public static String STATA_14_HEADER = "<stata_dta><header><release>118</release>";
    public static String STATA_15_HEADER = "<stata_dta><header><release>119</release>";
    // Map that returns a reader-implemented mime-type
    private static Set<String> readableFileTypes = new HashSet<String>();
    private static Map<String, Method> testMethods = new HashMap<String, Method>();
    public static int POR_MARK_POSITION_DEFAULT = 461;
    public static String POR_MARK = "SPSSPORT";
    private static int DEFAULT_BUFFER_SIZE = 500;
    private static String regex = "^test(\\w+)format$";

    // static initialization block
    private static String rdargx = "^(52)(44)(41|42|58)(31|32|33)(0A)$";
    private static int RDA_HEADER_SIZE = 5;
    private static Pattern ptn;


    static {

        stataReleaseNumber.put((byte) 104, "rel_3");
        stataReleaseNumber.put((byte) 105, "rel_4or5");
        stataReleaseNumber.put((byte) 108, "rel_6");
        stataReleaseNumber.put((byte) 110, "rel_7first");
        stataReleaseNumber.put((byte) 111, "rel_7scnd");
        stataReleaseNumber.put((byte) 113, "rel_8_or_9");
        stataReleaseNumber.put((byte) 114, "rel_10");
        stataReleaseNumber.put((byte) 115, "rel_12");
        // 116 was an in-house experimental version that was never 
        // released.
        // STATA v.13 introduced a new format, 117. It's a completely
        // new development, unrelated to the old format. 
        stataReleaseNumber.put((byte) 117, "rel_13");

        readableFileTypes.add("application/x-stata");
        readableFileTypes.add("application/x-spss-sav");
        readableFileTypes.add("application/x-spss-por");
        readableFileTypes.add("application/x-rlang-transport");
        readableFileTypes.add("application/x-stata-13");
        readableFileTypes.add("application/x-stata-14");
        readableFileTypes.add("application/x-stata-15");

        Pattern p = Pattern.compile(regex);
        ptn = Pattern.compile(rdargx);

        for (Method m : IngestableDataChecker.class.getDeclaredMethods()) {
            Matcher mtr = p.matcher(m.getName());
            if (mtr.matches()) {
                testMethods.put(mtr.group(1), m);
            }
        }
    }

    private boolean windowsNewLine = true;

    // constructors
    public IngestableDataChecker() {
    }

    // instance methods

    // test methods start here ------------------------------------------------

    /**
     * test this byte buffer against SPSS-SAV spec
     */
    public String testSAVformat(ByteBuffer buff) {
        String result = null;
        buff.rewind();

        // -----------------------------------------
        // Avoid java.nio.BufferUnderflowException
        // -----------------------------------------
        if (buff.capacity() < 4) {
            return null;
        }

        log.debug("applying the sav test");

        byte[] hdr4 = new byte[4];
        buff.get(hdr4, 0, 4);
        String hdr4sav = new String(hdr4);

        log.debug("from string={}", hdr4sav);
        if (hdr4sav.equals("$FL2")) {
            log.debug("this file is spss-sav type");
            result = "application/x-spss-sav";
        } else {
            log.debug("this file is NOT spss-sav type");
        }

        return result;
    }


    /**
     * test this byte buffer against STATA DTA spec
     */
    public String testDTAformat(ByteBuffer buff) {
        String result = null;
        buff.rewind();

        log.debug("applying the dta test");

        // -----------------------------------------
        // Avoid java.nio.BufferUnderflowException
        // -----------------------------------------
        if (buff.capacity() < 4) {
            return result;
        }

        // We first check if it's a "classic", old DTA format 
        // (up to version 115): 

        byte[] hdr4 = new byte[4];
        buff.get(hdr4, 0, 4);

        if (log.isDebugEnabled()) {
            for (int i = 0; i < hdr4.length; ++i) {
                log.debug(String.format("%d\t%02X\n", i, hdr4[i]));
            }
        }

        if (hdr4[2] != 1) {
            log.debug("3rd byte is not 1: given file is not stata-dta type");
        } else if ((hdr4[1] != 1) && (hdr4[1] != 2)) {
            log.debug("2nd byte is neither 0 nor 1: this file is not stata-dta type");
        } else if (!IngestableDataChecker.stataReleaseNumber.containsKey(hdr4[0])) {
            log.debug("1st byte ({}) is not within the ingestable range [rel. 3-10]: this file is NOT stata-dta type", hdr4[0]);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("this file is stata-dta type: {} (No in HEX={})", IngestableDataChecker.stataReleaseNumber.get(hdr4[0]), hdr4[0]);
            }
            result = "application/x-stata";
        }

        if ((result == null) && (buff.capacity() >= STATA_13_HEADER.length())) {
            // Let's see if it's a "new" STATA (v.13+) format: 
            buff.rewind();
            byte[] headerBuffer = null;
            String headerString = null;
            try {
                headerBuffer = new byte[STATA_13_HEADER.length()];
                buff.get(headerBuffer, 0, STATA_13_HEADER.length());
                headerString = new String(headerBuffer, StandardCharsets.US_ASCII);
            } catch (Exception ex) {
                // probably a buffer underflow exception; 
                // we don't have to do anything... null will 
                // be returned, below. 
            }

            if (STATA_13_HEADER.equals(headerString)) {
                result = "application/x-stata-13";
            }

        }

        if ((result == null) && (buff.capacity() >= STATA_14_HEADER.length())) {
            // Let's see if it's a "new" STATA (v.14+) format:
            buff.rewind();
            byte[] headerBuffer = null;
            String headerString = null;
            try {
                headerBuffer = new byte[STATA_14_HEADER.length()];
                buff.get(headerBuffer, 0, STATA_14_HEADER.length());
                headerString = new String(headerBuffer, StandardCharsets.US_ASCII);
            } catch (Exception ex) {
                // probably a buffer underflow exception;
                // we don't have to do anything... null will
                // be returned, below.
            }
            if (STATA_14_HEADER.equals(headerString)) {
                result = "application/x-stata-14";
            }
        }

        if ((result == null) && (buff.capacity() >= STATA_15_HEADER.length())) {
            // Let's see if it's a "new" STATA (v.14+) format:
            buff.rewind();
            byte[] headerBuffer = null;
            String headerString = null;
            try {
                headerBuffer = new byte[STATA_15_HEADER.length()];
                buff.get(headerBuffer, 0, STATA_15_HEADER.length());
                headerString = new String(headerBuffer, StandardCharsets.US_ASCII);
            } catch (Exception ex) {
                // probably a buffer underflow exception;
                // we don't have to do anything... null will
                // be returned, below.
            }
            if (STATA_15_HEADER.equals(headerString)) {
                result = "application/x-stata-15";
            }
        }

        return result;
    }

    /**
     * test this byte buffer against SPSS Portable (POR) spec
     */
    public String testPORformat(ByteBuffer buff) {
        String result = null;
        buff.rewind();

        log.debug("applying the spss-por test");

        // size test
        int bufferCapacity = buff.capacity();
        log.debug("Subsettable Checker: buffer capacity: {}", bufferCapacity);

        if (bufferCapacity < 491) {
            log.debug("this file is NOT spss-por type");

            return result;
        }

        //windows [0D0A]=>   [1310] = [CR/LF]
        //unix    [0A]  =>   [10]
        //mac     [0D]  =>   [13]
        // 3char  [0D0D0A]=> [131310] spss for windows rel 15
        // expected results
        // unix    case: [0A]   : [80], [161], [242], [323], [404], [485]
        // windows case: [0D0A] : [81], [163], [245], [327], [409], [491]
        //  : [0D0D0A] : [82], [165], [248], [331], [414], [495]

        buff.rewind();
        byte[] nlch = new byte[36];
        int pos1;
        int pos2;
        int pos3;
        int ucase = 0;
        int wcase = 0;
        int mcase = 0;
        int three = 0;
        int nolines = 6;
        int nocols = 80;
        for (int i = 0; i < nolines; ++i) {
            int baseBias = nocols * (i + 1);
            // 1-char case
            pos1 = baseBias + i;

            if (pos1 > bufferCapacity - 1) {
                log.debug("Subsettable Checker: request to go beyond buffer capacity ({})", pos1);
                return result;
            }

            buff.position(pos1);
            log.debug("position(1)={}", buff.position());
            int j = 6 * i;
            nlch[j] = buff.get();

            if (nlch[j] == 10) {
                ucase++;
            } else if (nlch[j] == 13) {
                mcase++;
            }

            // 2-char case
            pos2 = baseBias + 2 * i;

            if (pos2 > bufferCapacity - 2) {
                log.debug("Subsettable Checker: request to read 2 bytes beyond buffer capacity ({})", pos2);
                return result;
            }


            buff.position(pos2);
            log.debug("position(2)={}", buff.position());
            nlch[j + 1] = buff.get();
            nlch[j + 2] = buff.get();

            // 3-char case
            pos3 = baseBias + 3 * i;

            if (pos3 > bufferCapacity - 3) {
                log.debug("Subsettable Checker: request to read 3 bytes beyond buffer capacity ({})", pos3);
                return result;
            }


            buff.position(pos3);
            log.debug("position(3)={}", buff.position());
            nlch[j + 3] = buff.get();
            nlch[j + 4] = buff.get();
            nlch[j + 5] = buff.get();
            log.debug("{}-th iteration position ={}\t{}\t{}\t{}\t{}\t{}", i, nlch[j], nlch[j + 1], nlch[j + 2], nlch[j + 3], nlch[j + 4], nlch[j + 5]);

            if ((nlch[j + 3] == 13) && (nlch[j + 4] == 13) && (nlch[j + 5] == 10)) {
                three++;
            } else if ((nlch[j + 1] == 13) && (nlch[j + 2] == 10)) {
                wcase++;
            }

            buff.rewind();
        }
        if (three == nolines) {
            log.debug("0D0D0A case");
            windowsNewLine = false;
        } else if ((ucase == nolines) && (wcase < nolines)) {
            log.debug("0A case");
            windowsNewLine = false;
        } else if ((ucase < nolines) && (wcase == nolines)) {
            log.debug("0D0A case");
        } else if ((mcase == nolines) && (wcase < nolines)) {
            log.debug("0D case");
            windowsNewLine = false;
        }


        buff.rewind();
        int PORmarkPosition = POR_MARK_POSITION_DEFAULT;
        if (windowsNewLine) {
            PORmarkPosition = PORmarkPosition + 5;
        } else if (three == nolines) {
            PORmarkPosition = PORmarkPosition + 10;
        }

        byte[] pormark = new byte[8];
        buff.position(PORmarkPosition);
        buff.get(pormark, 0, 8);
        String pormarks = new String(pormark);

        log.debug("pormark =>{}<-", pormarks);

        if (pormarks.equals(POR_MARK)) {
            log.debug("this file is spss-por type");
            result = "application/x-spss-por";
        } else {
            log.debug("this file is NOT spss-por type");
        }

        return result;
    }

    /**
     * test this byte buffer against R data file
     */
    public String testRDAformat(ByteBuffer buff) {
        String result = null;
        buff.rewind();

        if (buff.capacity() < 4) {
            return null;
        }

        log.debug("applying the RData test. buffer capacity={}", buff.capacity());
        if (log.isDebugEnabled()) {
            byte[] rawhdr = new byte[4];
            buff.get(rawhdr, 0, 4);
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 4; j++) {
                sb.append(String.format("%02X ", rawhdr[j]));
            }
            log.debug("{}", sb);
            buff.rewind();
        }
        // get the first 4 bytes as an int and check its value; 
        // if it is 0x1F8B0800, then gunzip and its first 4 bytes
        int magicNumber = buff.getInt();

        if (log.isDebugEnabled()) {
            log.debug("magicNumber in decimal ={}\tin binary={}\tin oct={}\tin hex={}",
                    magicNumber,
                    Integer.toBinaryString(magicNumber),
                    Integer.toOctalString(magicNumber),
                    Integer.toHexString(magicNumber));
        }

        try {
            if (magicNumber == 0x1F8B0800) {
                log.debug("magicNumber is GZIP");
                // gunzip the first 5 bytes and check their bye-pattern

                // get gzip buffer size

                int gzip_buffer_size = this.getGzipBufferSize(buff);

                byte[] hdr = new byte[gzip_buffer_size];
                buff.get(hdr, 0, gzip_buffer_size);

                try (GZIPInputStream gzin = new GZIPInputStream(new ByteArrayInputStream(hdr))) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < RDA_HEADER_SIZE; i++) {
                        sb.append(String.format("%02X", gzin.read()));
                    }
                    String fisrt5bytes = sb.toString();
                    result = this.checkUncompressedFirst5bytes(fisrt5bytes);
                }
                // end of compressed case
            } else {
                // uncompressed case?
                log.debug("magicNumber is not GZIP:{}\ttest as an uncompressed RData file", magicNumber);

                buff.rewind();
                byte[] uchdr = new byte[5];
                buff.get(uchdr, 0, 5);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < uchdr.length; i++) {
                    sb.append(String.format("%02X", uchdr[i]));
                }
                String fisrt5bytes = sb.toString();

                result = this.checkUncompressedFirst5bytes(fisrt5bytes);
                // end of uncompressed case
            }
        } catch (IOException ex) {
            log.error("RDA format error", ex);
        }
        return result;
    }

    // public instance methods ------------------------------------------------
    public String detectTabularDataFormat(File fh) {
        String readableFormatType = null;
        
        
        try (final FileInputStream inp = new FileInputStream(fh)) {

            final byte[] bytes = new byte[(int) min(fh.length(), DEFAULT_BUFFER_SIZE)];
            inp.read(bytes);
            ByteBuffer buff = ByteBuffer.wrap(bytes);

            log.trace("before the for loop");
            for (String fmt : TABULAR_DATA_FORMAT_SET) {

                // get a test method
                Method mthd = testMethods.get(fmt);

                try {
                    // invoke this method
                    Object retobj = mthd.invoke(this, buff);
                    String result = (String) retobj;

                    if (result != null) {
                        log.trace("result for ({})={}", fmt, result);
                        if (readableFileTypes.contains(result)) {
                            readableFormatType = result;
                        }
                        log.trace("readableFormatType={}", readableFormatType);
                        return readableFormatType;
                    } else {
                        log.trace("null was returned for {} test", fmt);
                    }
                } catch (InvocationTargetException | IllegalAccessException | BufferUnderflowException  e) {
                    log.error("Tabular invocation error", e);
                }
            }

            return readableFormatType;

        } catch (IOException fe) {
            log.error("Error occurred during processing of tabular format", fe);
        }
        return readableFormatType;
    }

    /**
     * identify the first 5 bytes
     */
    private String checkUncompressedFirst5bytes(String fisrt5bytes) {
        String result = null;
        log.debug("first5bytes={}", fisrt5bytes);
        Matcher mtr = ptn.matcher(fisrt5bytes);

        if (mtr.matches()) {
            log.debug("RDATA type");
            result = "application/x-rlang-transport";
        } else {
            log.debug("not binary RDATA type");
        }

        return result;
    }

    private int getGzipBufferSize(ByteBuffer buff) {
        int GZIP_BUFFER_SIZE = 120;
        /*
        note:
        gzip buffer size <= 118  causes "java.io.EOFException:
        Unexpected end of ZLIB input stream"
        with a byte buffer of 500 bytes
         */
        // adjust gzip buffer size if necessary
        // file.size might be less than the default gzip buffer size
        if (buff.capacity() < GZIP_BUFFER_SIZE) {
            GZIP_BUFFER_SIZE = buff.capacity();
        }
        buff.rewind();
        return GZIP_BUFFER_SIZE;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, MULTI_LINE_STYLE);
    }
}
