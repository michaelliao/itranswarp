package com.itranswarp.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility class for hashing.
 *
 * @author liaoxuefeng
 */
public class HashUtil {

    /**
     * Generate SHA-1 as hex string (all lower-case).
     *
     * @param input Input as string.
     * @return Hex string.
     */
    public static String sha1(String input) {
        return sha1(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate SHA-1 as hex string (all lower-case).
     *
     * @param input Input as bytes.
     * @return Hex string.
     */
    public static String sha1(byte[] input) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        md.update(input);
        byte[] digest = md.digest();
        return ByteUtil.toHexString(digest);
    }

    public static byte[] sha1AsBytes(String input) {
        return sha1AsBytes(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate SHA-1 as bytes.
     *
     * @param input Input as bytes.
     * @return Bytes.
     */
    public static byte[] sha1AsBytes(byte[] input) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        md.update(input);
        return md.digest();
    }

    /**
     * Generate SHA-256 as hex string (all lower-case).
     *
     * @param input Input as String.
     * @return Hex string.
     */
    public static String sha256(String input) {
        return sha256(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate SHA-256 as hex string (all lower-case).
     *
     * @param input Input as String.
     * @return Hex string.
     */
    public static byte[] sha256AsBytes(String input) {
        return sha256AsBytes(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate SHA-256 as hex string (all lower-case).
     *
     * @param input Input as bytes.
     * @return Hex string.
     */
    public static String sha256(byte[] input) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        md.update(input);
        byte[] digest = md.digest();
        return ByteUtil.toHexString(digest);
    }

    /**
     * Generate SHA-256 as bytes.
     *
     * @param input Input as bytes.
     * @return SHA bytes.
     */
    public static byte[] sha256AsBytes(byte[] input) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        md.update(input);
        return md.digest();
    }

    /**
     * Generate SHA-512 as bytes.
     *
     * @param input Input as bytes.
     * @return SHA bytes.
     */
    public static byte[] sha512AsBytes(byte[] input) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        md.update(input);
        return md.digest();
    }

    /**
     * Do HMAC-SHA256.
     *
     * @return Hex string.
     */
    public static byte[] hmacSha256AsBytes(byte[] data, byte[] key) {
        SecretKey skey = new SecretKeySpec(key, "HmacSHA256");
        Mac mac;
        try {
            mac = Mac.getInstance("HmacSHA256");
            mac.init(skey);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        mac.update(data);
        return mac.doFinal();
    }

    /**
     * Do HMAC-SHA256.
     *
     * @return Hex string.
     */
    public static String hmacSha256(byte[] data, byte[] key) {
        return ByteUtil.toHexString(hmacSha256AsBytes(data, key));
    }

    /**
     * Do HMAC-SHA1.
     *
     * @return byte[] as result.
     */
    public static byte[] hmacSha1(byte[] data, byte[] key) {
        SecretKey skey = new SecretKeySpec(key, "HmacSHA1");
        Mac mac;
        try {
            mac = Mac.getInstance("HmacSHA1");
            mac.init(skey);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        mac.update(data);
        return mac.doFinal();
    }

    /**
     * Do HMAC-SHA256.
     *
     * @return byte[] as result.
     */
    public static String hmacSha256(String data, String key) {
        return hmacSha256(data.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Do HMAC-SHA256.
     *
     * @return byte[] as result.
     */
    public static byte[] hmacSha256AsBytes(String data, String key) {
        return hmacSha256AsBytes(data.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Do HMAC-SHA256.
     *
     * @return byte[] as result.
     */
    public static String hmacSha256(byte[] data, String key) {
        return hmacSha256(data, key.getBytes(StandardCharsets.UTF_8));
    }
}
