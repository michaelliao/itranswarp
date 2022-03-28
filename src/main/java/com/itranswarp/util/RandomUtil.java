package com.itranswarp.util;

import java.security.SecureRandom;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Random util.
 */
public class RandomUtil {

    static final Logger logger = LoggerFactory.getLogger(RandomUtil.class);

    static final SecureRandom SECURE_RANDOM = createSecureRandom();

    static final Random RANDOM = new Random();

    /**
     * Holds "A-Z".
     */
    public static final String ALPHABET_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * Holds "a-z".
     */
    public static final String ALPHABET_LOWER = "abcdefghijklmnopqrstuvwxyz";

    /**
     * Holds "0-9"
     */
    public static final String DIGITS = "0123456789";

    /**
     * Holds "0-9a-f"
     */
    public static final String HEX_DIGITS = "0123456789abcdef";

    /**
     * Holds words "A~Z a~z 0~9"
     */
    public static final String WORDS = ALPHABET_UPPER + ALPHABET_LOWER + DIGITS;

    private static SecureRandom createSecureRandom() {
        return new SecureRandom();
    }

    /**
     * Create a random bytes with specific length.
     *
     * @param length Length of bytes.
     * @return Random bytes.
     */
    public static byte[] createRandomBytes(int length) {
        byte[] buffer = new byte[length];
        SECURE_RANDOM.nextBytes(buffer);
        return buffer;
    }

    /**
     * Create a secure random string with specific length based on char list.
     *
     * @param charList A string that holds chars.
     * @param length   The length of random string.
     * @return Random string.
     */
    public static String createRandomString(String charList, int length) {
        char[] buffer = new char[length];
        int n = charList.length();
        for (int i = 0; i < length; i++) {
            buffer[i] = charList.charAt(SECURE_RANDOM.nextInt(n));
        }
        return new String(buffer);
    }

    /**
     * Create a secure random string using a-z, A-Z and 0-9.
     * 
     * @param length The length of random string.
     * @return Random string.
     */
    public static String createRandomString(int length) {
        return createRandomString(WORDS, length);
    }

}
