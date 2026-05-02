package ua.university.passwordmanager.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.university.passwordmanager.dto.PasswordCreateRequest;
import ua.university.passwordmanager.dto.PasswordResponse;
import ua.university.passwordmanager.dto.PasswordUpdateRequest;
import ua.university.passwordmanager.service.PasswordService;

import java.util.List;

@RestController
@RequestMapping("/api/passwords")
public class PasswordController {

    private final PasswordService passwordService;

    public PasswordController(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    @GetMapping
    public ResponseEntity<List<PasswordResponse>> getAllPasswords() {
        return ResponseEntity.ok(passwordService.getAllPasswords());
    }

    @PostMapping
    public ResponseEntity<PasswordResponse> createPassword(@Valid @RequestBody PasswordCreateRequest request) {
        return ResponseEntity.ok(passwordService.createPassword(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PasswordResponse> getPassword(@PathVariable String id) {
        return ResponseEntity.ok(passwordService.getPassword(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PasswordResponse> updatePassword(@PathVariable String id,
                                                           @RequestBody PasswordUpdateRequest request) {
        return ResponseEntity.ok(passwordService.updatePassword(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePassword(@PathVariable String id) {
        passwordService.deletePassword(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<PasswordResponse>> search(@RequestParam String q) {
        List<PasswordResponse> byService = passwordService.searchByService(q);
        if (!byService.isEmpty()) {
            return ResponseEntity.ok(byService);
        }
        return ResponseEntity.ok(passwordService.searchByUsername(q));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<PasswordResponse>> filterByCategory(@PathVariable String category) {
        return ResponseEntity.ok(passwordService.filterByCategory(category));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<PasswordResponse>> getRecentlyUsed() {
        return ResponseEntity.ok(passwordService.getRecentlyUsed());
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportPasswords() {
        return ResponseEntity.ok(passwordService.exportToJson());
    }

    @PostMapping("/import")
    public ResponseEntity<String> importPasswords(@RequestBody String json) {
        passwordService.importFromJson(json);
        return ResponseEntity.ok("Import successful");
    }
}
