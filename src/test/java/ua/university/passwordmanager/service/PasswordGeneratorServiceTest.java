package ua.university.passwordmanager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PasswordGeneratorServiceTest {

    private PasswordGeneratorService service;
    private static final String SPECIAL = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    @BeforeEach
    void setUp() {
        service = new PasswordGeneratorService();
    }

    @Test
    void testGenerateDefaultLength() {
        String pw = service.generate(12, true, true, true, false);
        assertEquals(12, pw.length());
    }

    @Test
    void testGenerateSpecificLength() {
        for (int len : new int[]{8, 16, 32, 64}) {
            assertEquals(len, service.generate(len, true, true, true, true).length());
        }
    }

    @Test
    void testContainsUppercase() {
        String pw = service.generate(20, true, false, false, false);
        assertTrue(pw.chars().anyMatch(Character::isUpperCase));
    }

    @Test
    void testContainsLowercase() {
        String pw = service.generate(20, false, true, false, false);
        assertTrue(pw.chars().anyMatch(Character::isLowerCase));
    }

    @Test
    void testContainsDigits() {
        String pw = service.generate(20, false, false, true, false);
        assertTrue(pw.chars().anyMatch(Character::isDigit));
    }

    @Test
    void testContainsSpecial() {
        String pw = service.generate(20, false, false, false, true);
        assertTrue(pw.chars().anyMatch(c -> SPECIAL.indexOf(c) >= 0));
    }

    @Test
    void testNoUppercaseWhenDisabled() {
        String pw = service.generate(20, false, true, true, false);
        assertFalse(pw.chars().anyMatch(Character::isUpperCase));
    }

    @Test
    void testNoSpecialWhenDisabled() {
        String pw = service.generate(20, true, true, true, false);
        assertFalse(pw.chars().anyMatch(c -> SPECIAL.indexOf(c) >= 0));
    }

    @Test
    void testMinLength8Enforced() {
        assertThrows(IllegalArgumentException.class, () -> service.generate(7, true, true, true, false));
    }

    @Test
    void testMaxLength64Enforced() {
        assertThrows(IllegalArgumentException.class, () -> service.generate(65, true, true, true, false));
    }

    @Test
    void testAtLeastOneTypeRequired() {
        assertThrows(IllegalArgumentException.class, () -> service.generate(12, false, false, false, false));
    }

    @Test
    void testStrengthWeakPassword() {
        int score = service.calculateStrength("password");
        assertTrue(score < 50, "Weak password should have low score, got: " + score);
    }

    @Test
    void testStrengthStrongPassword() {
        int score = service.calculateStrength("Str0ng!Pass#2024");
        assertTrue(score >= 70, "Strong password should have high score, got: " + score);
    }

    @Test
    void testStrengthOnlyDigits() {
        int score = service.calculateStrength("12345678");
        assertTrue(score < 40, "Only digits should be low score");
    }

    @Test
    void testUniquenessOfGenerated() {
        Set<String> passwords = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            passwords.add(service.generate(16, true, true, true, true));
        }
        assertEquals(10, passwords.size(), "All 10 generated passwords should be unique");
    }

    @Test
    void testSecureRandomUsed() {
        Set<String> passwords = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            passwords.add(service.generate(16, true, true, true, false));
        }
        assertTrue(passwords.size() > 45, "High entropy expected from SecureRandom");
    }

    @Test
    void testGuaranteedCharTypes() {
        for (int i = 0; i < 20; i++) {
            String pw = service.generate(8, true, true, true, true);
            assertTrue(pw.chars().anyMatch(Character::isUpperCase), "Must contain uppercase");
            assertTrue(pw.chars().anyMatch(Character::isLowerCase), "Must contain lowercase");
            assertTrue(pw.chars().anyMatch(Character::isDigit), "Must contain digit");
            assertTrue(pw.chars().anyMatch(c -> SPECIAL.indexOf(c) >= 0), "Must contain special");
        }
    }

    @Test
    void testCalculateStrengthRange() {
        String[] passwords = {"a", "password", "Password1", "Str0ng!Pass#2024", ""};
        for (String pw : passwords) {
            int score = service.calculateStrength(pw);
            assertTrue(score >= 0 && score <= 100, "Score must be 0-100 for: " + pw);
        }
    }
}
