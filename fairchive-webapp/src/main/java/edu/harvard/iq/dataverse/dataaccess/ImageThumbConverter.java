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
package edu.harvard.iq.dataverse.dataaccess;

import static java.awt.Image.SCALE_FAST;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.io.File.createTempFile;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.size;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.lang.StringUtils.startsWithIgnoreCase;
import static org.slf4j.LoggerFactory.getLogger;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;

/**
 * @author Leonid Andreev
 */
@ApplicationScoped
public class ImageThumbConverter {
    private static final Logger logger = getLogger(ImageThumbConverter.class);

    public static final String THUMBNAIL_SUFFIX = "thumb";
    public static final  String WORLDMAP_IMAGE_SUFFIX = "img";
    public static final  String THUMBNAIL_MIME_TYPE = "image/png";

    public static final int DEFAULT_CARDIMAGE_SIZE = 48;
    public static final int DEFAULT_THUMBNAIL_SIZE = 64;
    public static final int DEFAULT_PREVIEW_SIZE = 400;

    @Inject
    private SystemConfig config;
    
    private DataAccess dataAccess = DataAccess.dataAccess();

    public ImageThumbConverter() {
    }
    
    public ImageThumbConverter(final SystemConfig sysConfig) {
        this.config = sysConfig;
    }

    public boolean isThumbnailAvailable(final DataFile file) {
        return isThumbnailAvailable(file, DEFAULT_THUMBNAIL_SIZE);
    }

    public boolean isThumbnailAvailable(final DataFile file, final int size) {
        requireNonNull(file);
        if (FileUtil.isThumbnailSupported(file)) {
            try (final StorageIO<DataFile> storageIO = getStorage(file)) {
                if (storageIO.isAuxObjectCached(suffix(size))) {
                    return true;
                } else if (isImage(file)) {
                    return generateImageThumbnail(storageIO, size,
                            file.getFilesize());
                } else if (isPDF(file)) {
                    return generatePDFThumbnail(storageIO, size,
                            file.getFilesize());
                } else if (isShape(file)) {
                    return generateWorldMapThumbnail(storageIO, size);
                } else {
                    return false;
                }
            } catch (final Exception e) {
                logger.warn("Thumbnail creation failed.", e);
                return false;
            }
        } else {
            return false;
        }
    }

    // Note that this method works on ALL file types for which thumbnail 
    // generation is supported - image/*, pdf, worldmap and geo-tagged tabular; 
    // not just on images! The type differentiation is handled inside 
    // isThumbnailAvailable(); if the thumbnail is not yet cached, that 
    // method will attempt to generate and cache it. And once it's cached, 
    // it is the same "auxiliary file", or an extra file with the .thumb[size]
    // extension - which is the same for all supported types.
    // Note that this method is mainly used by the data access API methods. 
    // Whenever a page needs a thumbnail, we prefer to rely on the Base64
    // string version.
    public InputStreamIO getImageThumbnailAsInputStream(final DataFile datafile,
            final int size) {
        if (isThumbnailAvailable(datafile, size)) {
            InputStream in = null;
            try (final StorageIO<DataFile> storageIO = getStorage(datafile)) {
                in = storageIO.getAuxFileAsInputStream(suffix(size));
                requireNonNull(in, "Null input stream.");
                final int fileSize = (int) storageIO.getAuxObjectSize(suffix(size));

                String fileName = storageIO.getFileName();
                if (fileName != null) {
                    fileName = fileName.replaceAll("\\.[^\\.]*$", ".png");
                }
                return new InputStreamIO(in, fileSize, fileName, THUMBNAIL_MIME_TYPE);
            } catch (Exception e) {
                logger.warn("Thumbnail retrieval failed.", e);
                closeQuietly(in);
                return null;
            }
        } else {
            return null;
        }
    }

    private boolean generatePDFThumbnail(final StorageIO<DataFile> storageIO, final int size, 
            final long fileSizeFromDatabase) {
        if (isPdfFileOverSizeLimit(fileSizeFromDatabase)) {
            return false;
        }

        // We rely on ImageMagick to convert PDFs; so if it's not installed, 
        // better give up right away: 
        if (!isImageMagickInstalled()) {
            return false;
        }

        Optional<File> tempPdfFile = Optional.empty();
        File tempThumbnailFile = new File(FileUtils.getTempDirectory(), 
                UUID.randomUUID().toString());
        Optional<File> sourcePdfFile = Optional.empty();
        try {
            sourcePdfFile = Optional.of(StorageIOUtils.obtainAsLocalFile(storageIO,
                    storageIO.isRemoteFile()));

            tempPdfFile = storageIO.isRemoteFile() ? sourcePdfFile : Optional.empty();

            generatePDFThumbnailFromFile(sourcePdfFile.get().getAbsolutePath(), 
                    size, tempThumbnailFile.getAbsolutePath());

            if (!tempThumbnailFile.exists()) {
                return false;
            }
            storageIO.savePathAsAux(tempThumbnailFile.toPath(), suffix(size));
        } catch (final IOException e) {
            logger.warn("Failed to save generated pdf thumbnail.", e);
            return false;
        } finally {
            tempThumbnailFile.delete();
            tempPdfFile.ifPresent(File::delete);
        }

        return true;
    }

    private boolean generateImageThumbnail(final StorageIO<DataFile> storage,
            final int size, final long fileSizeFromDatabase) {

        if (isImageOverSizeLimit(fileSizeFromDatabase)) {
            return false;
        } else {
            try {
                storage.open();
                try (final InputStream in = storage.getInputStream()) {
                    return generateImageThumbnailFromInputStream(storage, size, in);
                }
            } catch (final IOException e) {
                logger.warn("Failed to generate thumbnail for " 
                            + storage.getFileName(), e);
                return false;
            }
        }
    }

    /*
     * Note that the "WorldMapThumbnail" generator does the exact same thing as the
     * "regular image" thumbnail generator.
     * The only difference is that the image generator uses the main file as
     * as the source; and the one for the worldmap uses an auxiliary file
     * with the ".img" extension (or the swift, etc. equivalent). This file is
     * produced and dropped into the Dataset directory (Swift container, etc.)
     * the first time the user actually runs WorldMap on the main file.
     * Also note that it works the exact same way for tabular-mapped-as-worldmap
     * files as well.
     */
    private boolean generateWorldMapThumbnail(StorageIO<DataFile> storageIO, int size) {

        try {
            storageIO.open();

            boolean worldMapImageExists = storageIO.isAuxObjectCached(WORLDMAP_IMAGE_SUFFIX);
            if (!worldMapImageExists) {
                logger.warn("WorldMap image doesn't exists");
                return false;
            }

            long worldMapImageSize = storageIO.getAuxObjectSize(WORLDMAP_IMAGE_SUFFIX);

            if (isImageOverSizeLimit(worldMapImageSize)) {
                logger.warn("WorldMap image too large - skipping");
                return false;
            }
        } catch (IOException ioex) {
            logger.warn("caught IOException trying to open an input stream for worldmap .img file (" 
                        + storageIO.getStorageLocation() + "). Original Error: " + ioex);
            return false;
        }

        try (InputStream worldMapImageInputStream = storageIO.getAuxFileAsInputStream(WORLDMAP_IMAGE_SUFFIX)) {
            return generateImageThumbnailFromInputStream(storageIO, size, worldMapImageInputStream);
        } catch (IOException e) {
            logger.warn("caught IOException trying to open an input stream for WorldMap .img file (" 
                        + storageIO.getStorageLocation() + "). Original Error: " + e);
            return false;
        }
    }

    private boolean generateImageThumbnailFromInputStream(
            final StorageIO<DataFile> storageIO, final int size,
            final InputStream inputStream) {
        try {
            final BufferedImage fullSizeImage = ImageIO.read(inputStream);
            requireNonNull(fullSizeImage, "Could not read image.");
            final int width = fullSizeImage.getWidth(null);
            final int height = fullSizeImage.getHeight(null);

            try (final OutputStream out = storageIO.openAuxOutput(suffix(size))) {
                rescaleImage(fullSizeImage, width, height, size, out);
            } catch (final Exception e) {
                logger.warn("Exception during thumbnail generation.", e);
                // With some storage drivers, we can open a WritableChannel, or
                // OutputStream
                // to directly write the generated thumbnail that we want to cache;
                // Some drivers (like Swift) do not support that, and will give us an
                // "operation not supported" exception. If that's the case, we'll have
                // to save the output into a temp file, and then copy it over to the
                // permanent storage using the DataAccess IO "save" command:
                final File tempFile = createTempFile("tempFileToRescale", ".tmp");
                try (final OutputStream out = new FileOutputStream(tempFile)) {
                    rescaleImage(fullSizeImage, width, height, size, out);
                    storageIO.savePathAsAux(Paths.get(tempFile.getAbsolutePath()),
                            suffix(size));
                } finally {
                    tempFile.delete();
                }
            }
            return true;
        } catch (final Exception e) {
            logger.warn("Faild to generate thumbnail.", e);
            return false;
        }
    }

    /**
     * This method is suitable for returning a string to embed in an HTML img
     * tag (or JSF h:graphicImage tag) because the string begins with
     * "data:image/png;base64," but it is not suitable for returning a
     * downloadable image via an API call.
     */
    public String getImageThumbnailAsBase64(DataFile file, int size) {

        // if thumbnails are not even supported on this file type, no need
        // to check anything else:
        if (!FileUtil.isThumbnailSupported(file)) {
            return null;
        }

        StorageIO<DataFile> storageIO = null;

        try {
            storageIO = DataAccess.dataAccess().getStorageIO(file);
        } catch (Exception ioEx) {
            logger.warn("Caught an exception while trying to obtain a thumbnail as Base64 string - could not open StorageIO on the datafile.");
            return null;
        }

        // skip the "isAvailable()" check - and just try to open the cached object. 
        // if we can't open it, then we'll try to generate it. In other words, we are doing it in 
        // the reverse order - and his way we can save one extra lookup, for a thumbnail 
        // that's already cached - and on some storage media (specifically, S3)
        // lookups are actually more expensive than reads. 

        Channel cachedThumbnailChannel = null;
        try {
            cachedThumbnailChannel = storageIO.openAuxChannel(suffix(size));
        } catch (Exception ioEx) {
            cachedThumbnailChannel = null;
        }

        if (cachedThumbnailChannel == null) {

            // try to generate, if not available: 
            boolean generated = false;
            if (file.getContentType().substring(0, 6).equalsIgnoreCase("image/")) {
                generated = generateImageThumbnail(storageIO, size, file.getFilesize());
            } else if (file.getContentType().equalsIgnoreCase("application/pdf")) {
                generated = generatePDFThumbnail(storageIO, size, file.getFilesize());
            } else if (file.getContentType().equalsIgnoreCase("application/zipped-shapefile") 
                    || (file.isTabularData() && file.hasGeospatialTag())) {
                generated = generateWorldMapThumbnail(storageIO, size);
            }

            if (generated) {
                // try to open again: 
                try {
                    cachedThumbnailChannel = storageIO.openAuxChannel(suffix(size));
                } catch (Exception ioEx) {
                    cachedThumbnailChannel = null;
                }
            }

            // if still null - give up:
            if (cachedThumbnailChannel == null) {
                return null;
            }
        }

        InputStream cachedThumbnailInputStream = 
                Channels.newInputStream((ReadableByteChannel) cachedThumbnailChannel);

        return getImageAsBase64FromInputStream(cachedThumbnailInputStream); 

    }

    private String getImageAsBase64FromInputStream(InputStream inputStream) { 
        try {
            if (inputStream != null) {

                byte[] buffer = new byte[8192];
                ByteArrayOutputStream cachingByteStream = new ByteArrayOutputStream();
                int bytes = 0;
                int total = 0;

                // No, you don't want to try and inputStream.read() the entire thumbSize
                // bytes at once; it's a thumbnail, but it can still be several K in size. 
                // And with some input streams - notably, with swift - you CANNOT read 
                // more than 8192 bytes in one .read().

                while ((bytes = inputStream.read(buffer)) > -1) {
                    cachingByteStream.write(buffer, 0, bytes);
                    total += bytes;
                }

                String imageDataBase64 = Base64.getEncoder().encodeToString(cachingByteStream.toByteArray());
                return FileUtil.DATA_URI_SCHEME + imageDataBase64;
            }
        } catch (IOException ex) {
            logger.warn("getImageAsBase64FromFile: Failed to read data from input stream.");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                }
            }
        }

        return null;
    }

    /**
     * This method is suitable for returning a string to embed in an HTML img
     * tag (or JSF h:graphicImage tag) because the string begins with
     * "data:image/png;base64," but it is not suitable for returning a
     * downloadable image via an API call.
     */
    /*
     * This is a version of the getImageAsBase64...() method that operates on
     * a File; it's used for generating Dataverse and Dataset thumbnails
     * from usr-uploaded images (i.e., from files not associated with datafiles)
     */
    public String getImageAsBase64FromFile(final File file) {
        try (final InputStream in = new FileInputStream(file)) {
            return getImageAsBase64FromInputStream(in);
        } catch (final IOException e) {
            logger.warn("Failed to read thumbnail for " + file, e);
            return null;
        }
    }

    /*
     * This is a version of generateImageThumbnail...() that works directly on
     * local files, for input and output. We still need it for various places
     * in the application - when we process uploaded images that are not
     * datafiles, etc.
     *
     */
    public boolean generateImageThumbnailFromFile(final String fileLocation,
            final int size, final String thumbFileLocation) {
        final Path thumb = Paths.get(thumbFileLocation);
        if (exists(thumb)) {
            return true; // already generated
        } else {
            try {
                final Path image = Paths.get(fileLocation);
                if (isImageOverSizeLimit(size(image))) {
                    return false;
                } else {
                    try (final InputStream in = newInputStream(image)) {
                        final BufferedImage fullSizeImage = ImageIO.read(in);
                        requireNonNull(fullSizeImage, "Cannot read image.");
                        final int width = fullSizeImage.getWidth(null);
                        final int height = fullSizeImage.getHeight(null);
                        rescaleImage(fullSizeImage, width, height, size,
                                thumbFileLocation);
                        return exists(thumb);
                    }
                }
            } catch (final Exception e) {
                logger.warn("Thumbnail creation failed for " + fileLocation, e);
                return false;
            }
        }
    }

    /*
     * This is another public version of generateImageThumbnail...() that works directly on
     * local files, for input and output. This one returns the output as Base64.
     * Used by the DatasetWidgetsPage, to rescale the uploaded dataset logo.
     *
     */
    public String generateImageThumbnailFromFileAsBase64(File file, int size) {
        File tempThumbnailFile = new File(FileUtils.getTempDirectory(), 
                UUID.randomUUID().toString());
        
        try {
            generateImageThumbnailFromFile(file.getAbsolutePath(), size, 
                    tempThumbnailFile.getAbsolutePath());

            if (tempThumbnailFile.exists()) {
                return getImageAsBase64FromFile(tempThumbnailFile);
            }
        } finally {
            tempThumbnailFile.delete();
        }
        return null;
    }

    // Public version of the rescaleImage() method; it takes the location of the output
    // file as a string argument. This method is used by external utilities for 
    // rescaling the non-datafile Dataverse and Dataset logos. 
    public boolean rescaleImage(BufferedImage fullSizeImage, int width, 
            int height, int size, String thumbFileLocation) {
        File outputFile = new File(thumbFileLocation);
        OutputStream outputFileStream = null;

        try {
            outputFileStream = new FileOutputStream(outputFile);
        } catch (IOException ioex) {
            logger.warn("caught IO exception trying to open output stream for " 
        + thumbFileLocation);
            return false;
        }

        try {
            rescaleImage(fullSizeImage, width, height, size, outputFileStream);
        } catch (Exception ioex) {
            logger.warn("caught Exceptiopn trying to create rescaled image " 
        + thumbFileLocation);
            return false;
        } finally {
            IOUtils.closeQuietly(outputFileStream);
        }

        return true;
    }

    private void rescaleImage(final BufferedImage image, final int width,
            final int height, final int size, final OutputStream out)
            throws IOException {

        int thumbHeight = size;
        int thumbWidth = size;

        if (width > height) {
            final double scaleFactor = ((double) size) / (double) width;
            thumbHeight = (int) (height * scaleFactor);
        } else {
            final double scaleFactor = ((double) size) / (double) height;
            thumbWidth = (int) (width * scaleFactor);
        }

        // If we are willing to spend a few extra CPU cycles to generate
        // better-looking thumbnails, we can the SCALE_SMOOTH flag.
        final Image thumbImage = image.getScaledInstance(thumbWidth, thumbHeight,
                SCALE_FAST);
        final ImageWriter writer = getPNGWriter();

        final BufferedImage lowRes = new BufferedImage(thumbWidth, thumbHeight,
                TYPE_INT_ARGB);
        final Graphics2D g2 = lowRes.createGraphics();
        g2.drawImage(thumbImage, 0, 0, null);
        g2.dispose();
        try {
            final ImageOutputStream ios = ImageIO.createImageOutputStream(out);
            writer.setOutput(ios);
            writer.write(lowRes);
        } finally {
            writer.dispose();
            thumbImage.flush();
            lowRes.flush();
        }
    }
    
    private static ImageWriter getPNGWriter() throws IOException{
        final Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName("png");
        if (it.hasNext()) {
            return it.next();
        } else {
            throw new IOException("Failed to locatie ImageWriter plugin for image type PNG");
        }
    }

    public boolean generatePDFThumbnailFromFile(String fileLocation, int size, String thumbFileLocation) {

        // see if the thumb is already generated and saved:
        if (new File(thumbFileLocation).exists()) {
            return true;
        }

        long fileSize = new File(fileLocation).length();
        if (isPdfFileOverSizeLimit(fileSize)) {
            return false;
        }

        String imageMagickExec = System.getProperty("dataverse.path.imagemagick.convert");

        if (imageMagickExec != null) {
            imageMagickExec = imageMagickExec.trim();
        }

        // default location:
        if (imageMagickExec == null || imageMagickExec.equals("")) {
            imageMagickExec = "/usr/bin/convert";
        }

        if (new File(imageMagickExec).exists()) {

            // Based on the lessons recently learned in production: 
            //  - use "-thumbnail" instead of "-resize";
            //  - use "-flatten"
            //  - use "-strip"
            //  - (maybe?) use jpeg instead of png - ?
            return runImageMagick(imageMagickExec, fileLocation, thumbFileLocation, size);
        }

        return false;

    }

    private boolean runImageMagick(String imageMagickExec, String fileLocation, String thumbFileLocation, int size) {
        String imageMagickCmd = null;

        imageMagickCmd = imageMagickExec + " pdf:" + fileLocation 
                + "[0] -thumbnail " + size + "x" + size + " -flatten -strip png:" 
                + thumbFileLocation;

        int exitValue = 1;

        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(imageMagickCmd);
            exitValue = process.waitFor();
        } catch (Exception e) {
            exitValue = 1;
        }

        if (exitValue == 0 && new File(thumbFileLocation).exists()) {
            return true;
        }

        return false;
    }

    private boolean isImageOverSizeLimit(final long size) {
        if (this.config.isThumbnailGenerationDisabledForImages()) {
            return true;
        } else if (this.config.getThumbnailSizeLimitImage() == 0) {
            return false;
        } else {
            return size == 0 || size > config.getThumbnailSizeLimitImage();
        }
    }

    private boolean isPdfFileOverSizeLimit(long fileSize) {
        if (config.isThumbnailGenerationDisabledForPDF()) {
            return true;
        }
        if (config.getThumbnailSizeLimitPDF() == 0) {
            return false;
        }
        return fileSize == 0 || fileSize > config.getThumbnailSizeLimitPDF();
    }

    private boolean isImageMagickInstalled() {
        return findImageMagickConvert() != null;
    }
    
    private static boolean isImage(final DataFile file) {
        return startsWithIgnoreCase(file.getContentType(), "image/");
    }
    
    private static boolean isPDF(final DataFile file) {
        return file.getContentType().equalsIgnoreCase("application/pdf");
    }
    
    private static boolean isShape(final DataFile file) {
        return file.getContentType().equalsIgnoreCase("application/zipped-shapefile")
                || (file.isTabularData() && file.hasGeospatialTag());
    }
    
    private StorageIO<DataFile> getStorage(final DataFile file)
            throws IOException {
        return this.dataAccess.getStorageIO(file);
    }
    
    private static String suffix(final int size) {
        switch (size) {
        case 48:
            return "thumb48";
        case 64:
            return "thumb64";
        default:
            return "thumb" + size;
        }
    }

    private String findImageMagickConvert() {
        String imageMagickExec = System.getProperty("dataverse.path.imagemagick.convert");

        if (imageMagickExec != null) {
            imageMagickExec = imageMagickExec.trim();
        }

        // default/standard location:
        if (imageMagickExec == null || imageMagickExec.equals("")) {
            imageMagickExec = "/usr/bin/convert";
        }

        if (new File(imageMagickExec).exists()) {
            return imageMagickExec;
        }

        return null;
    }
}
