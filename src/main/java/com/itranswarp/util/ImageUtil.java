package com.itranswarp.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.itranswarp.bean.ImageBean;
import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;

/**
 * Util for creating thumbnails.
 * 
 * @author liaoxuefeng
 */
public class ImageUtil {

    public static ImageBean readImage(byte[] data) {
        BufferedImage image;
        String format;
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (readers.hasNext()) {
                ImageReader reader = null;
                try {
                    reader = readers.next();
                    reader.setInput(input);
                    format = reader.getFormatName();
                    image = reader.read(0);
                    if (readers.hasNext()) {
                        throw new ApiException(ApiError.PARAMETER_INVALID, "data", "Invalid data.");
                    }
                } finally {
                    if (reader != null) {
                        reader.dispose();
                    }
                }
            } else {
                throw new ApiException(ApiError.PARAMETER_INVALID, "image", "Invalid data.");
            }
        } catch (IOException e) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "image", "Invalid data.");
        }
        ImageBean bean = new ImageBean();
        bean.image = image;
        bean.width = image.getWidth();
        bean.height = image.getHeight();
        bean.size = data.length;
        bean.mime = "image/" + checkFormat(format);
        return bean;
    }

    static String checkFormat(String format) {
        if (format == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "image", "Unsupported image type.");
        }
        format = format.toLowerCase();
        if (!SUPPORTED_FORMAT.contains(format)) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "image", "Unsupported image type.");
        }
        return format;
    }

    static final Set<String> SUPPORTED_FORMAT = Set.of("jpeg", "png", "gif");

    public static BufferedImage resizeKeepRatio(BufferedImage input, int expectedWidth) {
        int originalWidth = input.getWidth();
        int originalHeight = input.getHeight();
        if (originalWidth <= expectedWidth) {
            return input;
        }
        int expectedHeight = originalHeight * expectedWidth / originalWidth;
        if (expectedHeight < 10) {
            expectedHeight = 10;
        }
        BufferedImage output = new BufferedImage(expectedWidth, expectedHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = output.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(input, 0, 0, expectedWidth, expectedHeight, null);
        g2.dispose();
        return output;
    }

    public static String encodeJpegAsBase64(BufferedImage image) throws IOException {
        try (var out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpeg", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        }
    }
}
