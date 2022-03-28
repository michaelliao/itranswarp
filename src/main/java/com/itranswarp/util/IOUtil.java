package com.itranswarp.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public class IOUtil {

    /**
     * Copy all bytes from one input stream to another output stream.
     *
     * @param from Source input.
     * @param to   Target output.
     * @throws IOException If IO error.
     */
    public static void copy(InputStream from, OutputStream to) throws IOException {
        byte[] buffer = new byte[1024];
        for (;;) {
            int n = from.read(buffer);
            if (n <= 0) {
                break;
            }
            to.write(buffer, 0, n);
        }
        to.flush();
    }

    public static byte[] readAsBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(1024 * 1024);
        copy(input, output);
        return output.toByteArray();
    }

    public static String readAsString(InputStream input) throws IOException {
        return new String(readAsBytes(input), StandardCharsets.UTF_8);
    }

    /**
     * Copy all characters from one reader to another writer.
     *
     * @param from Source reader.
     * @param to   Target writer.
     * @throws IOException If IO error.
     */
    public static void copy(Reader from, Writer to) throws IOException {
        char[] buffer = new char[1024];
        for (;;) {
            int n = from.read(buffer);
            if (n <= 0) {
                break;
            }
            to.write(buffer, 0, n);
        }
        to.flush();
    }

}
