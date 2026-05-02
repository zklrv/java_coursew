package ua.university.passwordmanager.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class MasterPasswordService {

    private static final String HASH_FILE = "data/master.hash";
    private static final int BCRYPT_ROUNDS = 12;
    private static final int MAX_ATTEMPTS = 5;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(BCRYPT_ROUNDS);

    private String masterPasswordInMemory = null;
    private boolean unlocked = false;
    private int failedAttempts = 0;

    public boolean isFirstRun() {
        return !Files.exists(Paths.get(HASH_FILE));
    }

    public void setupMasterPassword(String password) {
        if (!isPasswordStrong(password)) {
            throw new IllegalArgumentException("Password is not strong enough");
        }
        try {
            Path path = Paths.get(HASH_FILE);
            Files.createDirectories(path.getParent());
            String hash = encoder.encode(password);
            Files.writeString(path, hash);
            masterPasswordInMemory = password;
            unlocked = true;
            failedAttempts = 0;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save master password", e);
        }
    }

    public boolean unlock(String password) {
        if (failedAttempts >= MAX_ATTEMPTS) {
            throw new IllegalStateException("Too many failed attempts. Application is locked.");
        }
        try {
            String hash = Files.readString(Paths.get(HASH_FILE));
            if (encoder.matches(password, hash)) {
                masterPasswordInMemory = password;
                unlocked = true;
                failedAttempts = 0;
                return true;
            } else {
                failedAttempts++;
                return false;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read master password hash", e);
        }
    }

    public void lock() {
        masterPasswordInMemory = null;
        unlocked = false;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public String getMasterPassword() {
        if (!unlocked) {
            throw new IllegalStateException("Vault is locked");
        }
        return masterPasswordInMemory;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        return hasUpper && hasLower && hasDigit;
    }

    public int calculateStrength(String password) {
        if (password == null || password.isEmpty()) return 0;
        int score = 0;
        if (password.length() >= 8) score += 20;
        if (password.length() >= 12) score += 10;
        if (password.length() >= 16) score += 10;
        if (password.chars().anyMatch(Character::isUpperCase)) score += 15;
        if (password.chars().anyMatch(Character::isLowerCase)) score += 15;
        if (password.chars().anyMatch(Character::isDigit)) score += 15;
        if (password.chars().anyMatch(c -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(c) >= 0)) score += 15;
        return Math.min(100, score);
    }

    public void changeMasterPassword(String oldPassword, String newPassword) {
        if (!unlock(oldPassword)) {
            throw new IllegalArgumentException("Old password is incorrect");
        }
        setupMasterPassword(newPassword);
    }
}
