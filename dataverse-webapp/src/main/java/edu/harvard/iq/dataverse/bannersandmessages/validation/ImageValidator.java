package edu.harvard.iq.dataverse.bannersandmessages.validation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

public class ImageValidator {

    private static final Logger logger = Logger.getLogger(ImageValidator.class.getCanonicalName());

    public static boolean isImageResolutionTooBig(byte[] imageBytes, int maxWidth, int maxHeight) {

        InputStream in = new ByteArrayInputStream(imageBytes);

        BufferedImage buf = null;
        try {
            buf = ImageIO.read(in);
        } catch (IOException e) {
            logger.fine("There was an error when reading an image");
        }

        return buf != null && (buf.getWidth() > maxWidth || buf.getHeight() > maxHeight);

    }
    
    public static boolean imageExceedes(final InputStream in, final int maxWidth,
            final int maxHeight)
            throws IOException {
        final BufferedImage img = ImageIO.read(in);
        if (img != null) {
            return img.getWidth() > maxWidth || img.getHeight() > maxHeight;
        } else {
            throw new IOException("Unsupported image format");
        }
    }
}
