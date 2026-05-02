package ua.university.passwordmanager.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PasswordGeneratorService {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate(int length, boolean useUppercase, boolean useLowercase,
                           boolean useDigits, boolean useSpecial) {
        if (length < 8 || length > 64) {
            throw new IllegalArgumentException("Password length must be between 8 and 64");
        }
        if (!useUppercase && !useLowercase && !useDigits && !useSpecial) {
            throw new IllegalArgumentException("At least one character type must be enabled");
        }

        StringBuilder alphabet = new StringBuilder();
        List<Character> guaranteed = new ArrayList<>();

        if (useUppercase) {
            alphabet.append(UPPERCASE);
            guaranteed.add(UPPERCASE.charAt(secureRandom.nextInt(UPPERCASE.length())));
        }
        if (useLowercase) {
            alphabet.append(LOWERCASE);
            guaranteed.add(LOWERCASE.charAt(secureRandom.nextInt(LOWERCASE.length())));
        }
        if (useDigits) {
            alphabet.append(DIGITS);
            guaranteed.add(DIGITS.charAt(secureRandom.nextInt(DIGITS.length())));
        }
        if (useSpecial) {
            alphabet.append(SPECIAL);
            guaranteed.add(SPECIAL.charAt(secureRandom.nextInt(SPECIAL.length())));
        }

        String alpha = alphabet.toString();
        List<Character> passwordChars = new ArrayList<>(guaranteed);

        for (int i = guaranteed.size(); i < length; i++) {
            passwordChars.add(alpha.charAt(secureRandom.nextInt(alpha.length())));
        }

        Collections.shuffle(passwordChars, secureRandom);

        StringBuilder sb = new StringBuilder();
        for (char c : passwordChars) {
            sb.append(c);
        }
        return sb.toString();
    }

    public int calculateStrength(String password) {
        if (password == null || password.isEmpty()) return 0;
        int score = 0;
        if (password.length() >= 8) score += 10;
        if (password.length() >= 12) score += 15;
        if (password.length() >= 16) score += 15;
        if (password.chars().anyMatch(Character::isUpperCase)) score += 15;
        if (password.chars().anyMatch(Character::isLowerCase)) score += 15;
        if (password.chars().anyMatch(Character::isDigit)) score += 15;
        if (password.chars().anyMatch(c -> SPECIAL.indexOf(c) >= 0)) score += 15;
        return Math.min(100, score);
    }
}
