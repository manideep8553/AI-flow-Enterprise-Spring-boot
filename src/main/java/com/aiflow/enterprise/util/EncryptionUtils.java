package com.aiflow.enterprise.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class EncryptionUtils {

    private static final Logger log = LoggerFactory.getLogger(EncryptionUtils.class);
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    private EncryptionUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String hashPassword(String password) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            digest.update(salt);
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            byte[] saltAndHash = new byte[salt.length + hash.length];
            System.arraycopy(salt, 0, saltAndHash, 0, salt.length);
            System.arraycopy(hash, 0, saltAndHash, salt.length, hash.length);

            return Base64.getEncoder().encodeToString(saltAndHash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Hashing algorithm not available: {}", HASH_ALGORITHM, e);
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    public static boolean verifyPassword(String password, String storedHash) {
        try {
            byte[] saltAndHash = Base64.getDecoder().decode(storedHash);
            byte[] salt = new byte[SALT_LENGTH];
            byte[] storedHashBytes = new byte[saltAndHash.length - SALT_LENGTH];

            System.arraycopy(saltAndHash, 0, salt, 0, SALT_LENGTH);
            System.arraycopy(saltAndHash, SALT_LENGTH, storedHashBytes, 0, storedHashBytes.length);

            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            digest.update(salt);
            byte[] computedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            return MessageDigest.isEqual(computedHash, storedHashBytes);
        } catch (NoSuchAlgorithmException | IllegalArgumentException e) {
            log.error("Password verification failed", e);
            return false;
        }
    }

    public static String base64Encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String base64Decode(String encoded) {
        return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}
