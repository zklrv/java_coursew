package ua.university.passwordmanager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
    }

    @Test
    void testEncryptDecrypt() throws Exception {
        String original = "mySecret123";
        String master = "MasterPass1";
        String encrypted = encryptionService.encrypt(original, master);
        String decrypted = encryptionService.decrypt(encrypted, master);
        assertEquals(original, decrypted);
    }

    @Test
    void testDifferentIVEachTime() throws Exception {
        String original = "mySecret123";
        String master = "MasterPass1";
        String e1 = encryptionService.encrypt(original, master);
        String e2 = encryptionService.encrypt(original, master);
        assertNotEquals(e1, e2, "Same plaintext should produce different ciphertexts");
    }

    @Test
    void testWrongPasswordThrowsException() throws Exception {
        String original = "mySecret123";
        String encrypted = encryptionService.encrypt(original, "CorrectPass1");
        assertThrows(Exception.class, () -> encryptionService.decrypt(encrypted, "WrongPass1"));
    }

    @Test
    void testEncryptNotNullOrEmpty() throws Exception {
        String encrypted = encryptionService.encrypt("test", "MasterPass1");
        assertNotNull(encrypted);
        assertFalse(encrypted.isEmpty());
    }

    @Test
    void testEncryptIsBase64() throws Exception {
        String encrypted = encryptionService.encrypt("test", "MasterPass1");
        assertDoesNotThrow(() -> Base64.getDecoder().decode(encrypted));
    }

    @Test
    void testDecryptWithEmptyStringThrows() {
        assertThrows(Exception.class, () -> encryptionService.decrypt("", "MasterPass1"));
    }

    @Test
    void testEncryptLongPassword() throws Exception {
        String longPw = "a".repeat(500);
        String encrypted = encryptionService.encrypt(longPw, "MasterPass1");
        assertEquals(longPw, encryptionService.decrypt(encrypted, "MasterPass1"));
    }

    @Test
    void testEncryptSpecialChars() throws Exception {
        String special = "!@#$%^&*()_+-=[]{}|;:,.<>?";
        String encrypted = encryptionService.encrypt(special, "MasterPass1");
        assertEquals(special, encryptionService.decrypt(encrypted, "MasterPass1"));
    }

    @Test
    void testEncryptUnicodeChars() throws Exception {
        String unicode = "Привіт Ñoño 日本語";
        String encrypted = encryptionService.encrypt(unicode, "MasterPass1");
        assertEquals(unicode, encryptionService.decrypt(encrypted, "MasterPass1"));
    }

    @Test
    void testEncryptWithEmptyMasterThrows() {
        assertThrows(Exception.class, () -> encryptionService.encrypt("test", ""));
    }

    @Test
    void testEncryptedLengthGreaterThanPlain() throws Exception {
        String plain = "short";
        String encrypted = encryptionService.encrypt(plain, "MasterPass1");
        assertTrue(encrypted.length() > plain.length());
    }

    @Test
    void testMultipleEncryptDecryptCycles() throws Exception {
        String original = "TestPassword@123";
        String master = "MasterPass1";
        for (int i = 0; i < 10; i++) {
            String enc = encryptionService.encrypt(original, master);
            assertEquals(original, encryptionService.decrypt(enc, master));
        }
    }
}
