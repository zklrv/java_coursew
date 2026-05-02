package ua.university.passwordmanager.dto;

import java.time.LocalDateTime;

public class PasswordResponse {

    private String id;
    private String serviceName;
    private String username;
    private String decryptedPassword;
    private String category;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastUsedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDecryptedPassword() { return decryptedPassword; }
    public void setDecryptedPassword(String decryptedPassword) { this.decryptedPassword = decryptedPassword; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
