package ua.university.passwordmanager.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.university.passwordmanager.dto.PasswordCreateRequest;
import ua.university.passwordmanager.dto.PasswordResponse;
import ua.university.passwordmanager.dto.PasswordUpdateRequest;
import ua.university.passwordmanager.entity.StoredPassword;
import ua.university.passwordmanager.repository.PasswordRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class PasswordService {

    private final PasswordRepository passwordRepository;
    private final EncryptionService encryptionService;
    private final MasterPasswordService masterPasswordService;

    public PasswordService(PasswordRepository passwordRepository,
                           EncryptionService encryptionService,
                           MasterPasswordService masterPasswordService) {
        this.passwordRepository = passwordRepository;
        this.encryptionService = encryptionService;
        this.masterPasswordService = masterPasswordService;
    }

    private void checkUnlocked() {
        if (!masterPasswordService.isUnlocked()) {
            throw new IllegalStateException("Vault is locked. Please unlock first.");
        }
    }

    public PasswordResponse createPassword(PasswordCreateRequest request) {
        checkUnlocked();
        try {
            StoredPassword entity = new StoredPassword();
            entity.setServiceName(request.getServiceName());
            entity.setUsername(request.getUsername());
            entity.setEncryptedPassword(
                    encryptionService.encrypt(request.getPassword(), masterPasswordService.getMasterPassword()));
            entity.setCategory(request.getCategory());
            entity.setNotes(request.getNotes());
            StoredPassword saved = passwordRepository.save(entity);
            return toResponse(saved, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create password", e);
        }
    }

    public PasswordResponse getPassword(String id) {
        checkUnlocked();
        StoredPassword entity = passwordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Password not found: " + id));
        try {
            String decrypted = encryptionService.decrypt(
                    entity.getEncryptedPassword(), masterPasswordService.getMasterPassword());
            entity.setLastUsedAt(LocalDateTime.now());
            passwordRepository.save(entity);
            return toResponse(entity, decrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt password", e);
        }
    }

    @Transactional(readOnly = true)
    public List<PasswordResponse> getAllPasswords() {
        return passwordRepository.findAll().stream()
                .map(e -> toResponse(e, null))
                .collect(Collectors.toList());
    }

    public PasswordResponse updatePassword(String id, PasswordUpdateRequest request) {
        checkUnlocked();
        StoredPassword entity = passwordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Password not found: " + id));
        try {
            if (request.getServiceName() != null) entity.setServiceName(request.getServiceName());
            if (request.getUsername() != null) entity.setUsername(request.getUsername());
            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                entity.setEncryptedPassword(
                        encryptionService.encrypt(request.getPassword(), masterPasswordService.getMasterPassword()));
            }
            if (request.getCategory() != null) entity.setCategory(request.getCategory());
            if (request.getNotes() != null) entity.setNotes(request.getNotes());
            StoredPassword saved = passwordRepository.save(entity);
            return toResponse(saved, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update password", e);
        }
    }

    public void deletePassword(String id) {
        passwordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Password not found: " + id));
        passwordRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<PasswordResponse> searchByService(String serviceName) {
        return passwordRepository.findByServiceNameContainingIgnoreCase(serviceName).stream()
                .map(e -> toResponse(e, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PasswordResponse> searchByUsername(String username) {
        return passwordRepository.findByUsernameContainingIgnoreCase(username).stream()
                .map(e -> toResponse(e, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PasswordResponse> filterByCategory(String category) {
        return passwordRepository.findByCategoryIgnoreCase(category).stream()
                .map(e -> toResponse(e, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PasswordResponse> getRecentlyUsed() {
        return passwordRepository.findTop10ByOrderByLastUsedAtDesc().stream()
                .map(e -> toResponse(e, null))
                .collect(Collectors.toList());
    }

    public String exportToJson() {
        checkUnlocked();
        try {
            List<Map<String, Object>> exportData = passwordRepository.findAll().stream()
                    .map(e -> {
                        try {
                            String decrypted = encryptionService.decrypt(
                                    e.getEncryptedPassword(), masterPasswordService.getMasterPassword());
                            Map<String, Object> map = new java.util.LinkedHashMap<>();
                            map.put("serviceName", e.getServiceName());
                            map.put("username", e.getUsername());
                            map.put("password", decrypted);
                            map.put("category", e.getCategory());
                            map.put("notes", e.getNotes());
                            return map;
                        } catch (Exception ex) {
                            throw new RuntimeException("Failed to decrypt for export", ex);
                        }
                    })
                    .collect(Collectors.toList());
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export passwords", e);
        }
    }

    public void importFromJson(String json) {
        checkUnlocked();
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            List<Map<String, Object>> importData = mapper.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> item : importData) {
                PasswordCreateRequest req = new PasswordCreateRequest();
                req.setServiceName((String) item.get("serviceName"));
                req.setUsername((String) item.get("username"));
                req.setPassword((String) item.get("password"));
                req.setCategory((String) item.get("category"));
                req.setNotes((String) item.get("notes"));
                createPassword(req);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to import passwords", e);
        }
    }

    private PasswordResponse toResponse(StoredPassword entity, String decryptedPassword) {
        PasswordResponse response = new PasswordResponse();
        response.setId(entity.getId());
        response.setServiceName(entity.getServiceName());
        response.setUsername(entity.getUsername());
        response.setDecryptedPassword(decryptedPassword);
        response.setCategory(entity.getCategory());
        response.setNotes(entity.getNotes());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setLastUsedAt(entity.getLastUsedAt());
        return response;
    }
}
