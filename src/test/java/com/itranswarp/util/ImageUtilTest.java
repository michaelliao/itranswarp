package com.itranswarp.util;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.itranswarp.bean.ImageBean;
import com.itranswarp.common.ApiException;

class ImageUtilTest {

    @Test
    void testReadImageAsJpeg() throws IOException {
        byte[] data = IOUtil.readAsBytes(getClass().getResourceAsStream("/400x320.jpg"));
        ImageBean bean = ImageUtil.readImage(data);
        assertNotNull(bean.image);
        assertEquals(400, bean.width);
        assertEquals(320, bean.height);
        assertEquals("image/jpeg", bean.mime);
    }

    @Test
    void testReadImageAsPng() throws IOException {
        byte[] data = IOUtil.readAsBytes(getClass().getResourceAsStream("/400x320.png"));
        ImageBean bean = ImageUtil.readImage(data);
        assertNotNull(bean.image);
        assertEquals(400, bean.width);
        assertEquals(320, bean.height);
        assertEquals("image/png", bean.mime);
    }

    @Test
    void testReadImageAsGif() throws IOException {
        byte[] data = IOUtil.readAsBytes(getClass().getResourceAsStream("/400x320.gif"));
        ImageBean bean = ImageUtil.readImage(data);
        assertNotNull(bean.image);
        assertEquals(400, bean.width);
        assertEquals(320, bean.height);
        assertEquals("image/gif", bean.mime);
    }

    @Test
    void testReadImageAsBmp() throws IOException {
        byte[] data = IOUtil.readAsBytes(getClass().getResourceAsStream("/test.bmp"));
        assertThrows(ApiException.class, () -> {
            ImageUtil.readImage(data);
        });
    }

    @Test
    void testReadImageAsWebp() throws IOException {
        byte[] data = IOUtil.readAsBytes(getClass().getResourceAsStream("/test.webp"));
        assertThrows(ApiException.class, () -> {
            ImageUtil.readImage(data);
        });
    }

    @Test
    void testCreateThumbWithSameWidth() throws IOException {
        BufferedImage input = ImageIO.read(getClass().getResourceAsStream("/400x320.jpg"));
        BufferedImage output = ImageUtil.resizeKeepRatio(input, 400);
        assertSame(output, input);
    }

    @Test
    void testCreateThumbWithLargerWidth() throws IOException {
        BufferedImage input = ImageIO.read(getClass().getResourceAsStream("/400x320.jpg"));
        BufferedImage output = ImageUtil.resizeKeepRatio(input, 420);
        assertSame(output, input);
    }

    @Test
    void testCreateThumbWithHalfWidth() throws IOException {
        BufferedImage input = ImageIO.read(getClass().getResourceAsStream("/400x320.jpg"));
        BufferedImage output = ImageUtil.resizeKeepRatio(input, 200);
        assertEquals(200, output.getWidth());
        assertEquals(160, output.getHeight());
        String base64 = ImageUtil.encodeJpegAsBase64(output);
        System.out.println("<img src=\"data:image/jpeg;base64," + base64 + "\" />");
    }

    @Test
    void testCreateThumbWithSmallWidth() throws IOException {
        BufferedImage input = ImageIO.read(getClass().getResourceAsStream("/400x320.png"));
        BufferedImage output = ImageUtil.resizeKeepRatio(input, 40);
        assertEquals(40, output.getWidth());
        assertEquals(32, output.getHeight());
        String base64 = ImageUtil.encodeJpegAsBase64(output);
        System.out.println("<img src=\"data:image/jpeg;base64," + base64 + "\" />");
    }

    @Test
    void testCreateThumbWithSmallWidth2() throws IOException {
        BufferedImage input = ImageIO.read(getClass().getResourceAsStream("/400x320.gif"));
        BufferedImage output = ImageUtil.resizeKeepRatio(input, 40);
        assertEquals(40, output.getWidth());
        assertEquals(32, output.getHeight());
        String base64 = ImageUtil.encodeJpegAsBase64(output);
        System.out.println("<img src=\"data:image/jpeg;base64," + base64 + "\" />");
    }

}
