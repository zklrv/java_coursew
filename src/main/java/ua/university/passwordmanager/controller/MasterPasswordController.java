package ua.university.passwordmanager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.university.passwordmanager.service.MasterPasswordService;

import java.util.Map;

@RestController
@RequestMapping("/api/master")
public class MasterPasswordController {

    private final MasterPasswordService masterPasswordService;

    public MasterPasswordController(MasterPasswordService masterPasswordService) {
        this.masterPasswordService = masterPasswordService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "firstRun", masterPasswordService.isFirstRun(),
                "unlocked", masterPasswordService.isUnlocked(),
                "failedAttempts", masterPasswordService.getFailedAttempts()
        ));
    }

    @PostMapping("/setup")
    public ResponseEntity<Map<String, String>> setup(@RequestBody Map<String, String> body) {
        String password = body.get("password");
        String confirm = body.get("confirmPassword");
        if (password == null || confirm == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "password and confirmPassword are required"));
        }
        if (!password.equals(confirm)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Passwords do not match"));
        }
        masterPasswordService.setupMasterPassword(password);
        return ResponseEntity.ok(Map.of("message", "Master password set successfully"));
    }

    @PostMapping("/unlock")
    public ResponseEntity<Map<String, Object>> unlock(@RequestBody Map<String, String> body) {
        boolean success = masterPasswordService.unlock(body.get("password"));
        if (success) {
            return ResponseEntity.ok(Map.of("unlocked", true, "message", "Vault unlocked"));
        }
        return ResponseEntity.ok(Map.of("unlocked", false,
                "failedAttempts", masterPasswordService.getFailedAttempts(),
                "message", "Invalid password"));
    }

    @PostMapping("/lock")
    public ResponseEntity<Map<String, String>> lock() {
        masterPasswordService.lock();
        return ResponseEntity.ok(Map.of("message", "Vault locked"));
    }

    @PutMapping("/change")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> body) {
        masterPasswordService.changeMasterPassword(body.get("oldPassword"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Master password changed successfully"));
    }

    @PostMapping("/strength")
    public ResponseEntity<Map<String, Object>> checkStrength(@RequestBody Map<String, String> body) {
        String password = body.get("password");
        int score = masterPasswordService.calculateStrength(password);
        return ResponseEntity.ok(Map.of(
                "score", score,
                "strong", masterPasswordService.isPasswordStrong(password)
        ));
    }
}
