package ua.university.passwordmanager.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int IV_SIZE = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGO = "PBKDF2WithHmacSHA256";

    public String encrypt(String plaintext, String masterPassword) throws Exception {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Plaintext cannot be null or empty");
        }
        if (masterPassword == null || masterPassword.isEmpty()) {
            throw new IllegalArgumentException("Master password cannot be null or empty");
        }

        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);

        SecretKey key = deriveKey(masterPassword, iv);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));

        byte[] combined = new byte[IV_SIZE + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, IV_SIZE);
        System.arraycopy(encrypted, 0, combined, IV_SIZE, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public String decrypt(String encryptedBase64, String masterPassword) throws Exception {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
            throw new IllegalArgumentException("Encrypted text cannot be null or empty");
        }
        if (masterPassword == null || masterPassword.isEmpty()) {
            throw new IllegalArgumentException("Master password cannot be null or empty");
        }

        byte[] combined = Base64.getDecoder().decode(encryptedBase64);
        byte[] iv = new byte[IV_SIZE];
        System.arraycopy(combined, 0, iv, 0, IV_SIZE);
        byte[] encrypted = new byte[combined.length - IV_SIZE];
        System.arraycopy(combined, IV_SIZE, encrypted, 0, encrypted.length);

        SecretKey key = deriveKey(masterPassword, iv);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] decrypted = cipher.doFinal(encrypted);

        return new String(decrypted, "UTF-8");
    }

    private SecretKey deriveKey(String masterPassword, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGO);
        PBEKeySpec spec = new PBEKeySpec(
                masterPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}
