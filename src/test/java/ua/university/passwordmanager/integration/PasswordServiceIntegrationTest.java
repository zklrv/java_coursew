package ua.university.passwordmanager.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import ua.university.passwordmanager.dto.PasswordCreateRequest;
import ua.university.passwordmanager.dto.PasswordResponse;
import ua.university.passwordmanager.dto.PasswordUpdateRequest;
import ua.university.passwordmanager.repository.PasswordRepository;
import ua.university.passwordmanager.service.EncryptionService;
import ua.university.passwordmanager.service.MasterPasswordService;
import ua.university.passwordmanager.service.PasswordService;
import ua.university.passwordmanager.ui.MainWindow;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "java.awt.headless=true"
})
class PasswordServiceIntegrationTest {

    @MockBean
    private MainWindow mainWindow;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private MasterPasswordService masterPasswordService;

    @Autowired
    private PasswordRepository passwordRepository;

    @Autowired
    private EncryptionService encryptionService;

    private static final String TEST_MASTER_PASSWORD = "TestMaster1";
    private static boolean masterSetup = false;

    @BeforeEach
    void setUp() {
        passwordRepository.deleteAll();
        if (!masterSetup || masterPasswordService.isFirstRun()) {
            try {
                masterPasswordService.setupMasterPassword(TEST_MASTER_PASSWORD);
            } catch (Exception e) {
                if (!masterPasswordService.isUnlocked()) {
                    masterPasswordService.unlock(TEST_MASTER_PASSWORD);
                }
            }
            masterSetup = true;
        }
        if (!masterPasswordService.isUnlocked()) {
            masterPasswordService.unlock(TEST_MASTER_PASSWORD);
        }
    }

    private PasswordCreateRequest createRequest(String service, String user, String pw, String cat, String notes) {
        PasswordCreateRequest req = new PasswordCreateRequest();
        req.setServiceName(service);
        req.setUsername(user);
        req.setPassword(pw);
        req.setCategory(cat);
        req.setNotes(notes);
        return req;
    }

    @Test
    void testCreateAndRetrievePassword() {
        PasswordResponse created = passwordService.createPassword(
                createRequest("Gmail", "user@gmail.com", "secret123", "Email", null));
        assertNotNull(created.getId());
        assertEquals("Gmail", created.getServiceName());

        PasswordResponse retrieved = passwordService.getPassword(created.getId());
        assertEquals("secret123", retrieved.getDecryptedPassword());
    }

    @Test
    void testPasswordIsEncryptedInDB() {
        passwordService.createPassword(createRequest("GitHub", "dev", "plaintext", null, null));
        var stored = passwordRepository.findAll().get(0);
        assertNotEquals("plaintext", stored.getEncryptedPassword(), "Password must be encrypted in DB");
    }

    @Test
    void testUpdatePassword() {
        PasswordResponse created = passwordService.createPassword(
                createRequest("Twitter", "user", "oldPass", null, null));
        PasswordUpdateRequest upd = new PasswordUpdateRequest();
        upd.setPassword("newPass456");
        passwordService.updatePassword(created.getId(), upd);

        PasswordResponse updated = passwordService.getPassword(created.getId());
        assertEquals("newPass456", updated.getDecryptedPassword());
    }

    @Test
    void testDeletePassword() {
        PasswordResponse created = passwordService.createPassword(
                createRequest("Facebook", "fb_user", "fbPass123", null, null));
        String id = created.getId();
        passwordService.deletePassword(id);
        assertThrows(RuntimeException.class, () -> passwordService.getPassword(id));
    }

    @Test
    void testSearchByServiceName() {
        passwordService.createPassword(createRequest("Google", "user1", "p1", null, null));
        passwordService.createPassword(createRequest("Gmail", "user2", "p2", null, null));
        passwordService.createPassword(createRequest("GitHub", "user3", "p3", null, null));

        List<PasswordResponse> results = passwordService.searchByService("git");
        assertEquals(1, results.size());
        assertEquals("GitHub", results.get(0).getServiceName());
    }

    @Test
    void testSearchByUsername() {
        passwordService.createPassword(createRequest("Site1", "alice@test.com", "p1", null, null));
        passwordService.createPassword(createRequest("Site2", "bob@test.com", "p2", null, null));

        List<PasswordResponse> results = passwordService.searchByUsername("alice");
        assertEquals(1, results.size());
        assertEquals("alice@test.com", results.get(0).getUsername());
    }

    @Test
    void testFilterByCategory() {
        passwordService.createPassword(createRequest("Gmail", "u1", "p1", "Email", null));
        passwordService.createPassword(createRequest("Yahoo", "u2", "p2", "Email", null));
        passwordService.createPassword(createRequest("GitHub", "u3", "p3", "Dev", null));

        List<PasswordResponse> emailPws = passwordService.filterByCategory("Email");
        assertEquals(2, emailPws.size());
    }

    @Test
    void testGetRecentlyUsed() {
        for (int i = 1; i <= 5; i++) {
            PasswordResponse created = passwordService.createPassword(
                    createRequest("Service" + i, "user" + i, "pass" + i, null, null));
            passwordService.getPassword(created.getId());
        }
        List<PasswordResponse> recent = passwordService.getRecentlyUsed();
        assertFalse(recent.isEmpty());
        assertTrue(recent.size() <= 10);
    }

    @Test
    void testCreateFailsWhenLocked() {
        masterPasswordService.lock();
        assertThrows(IllegalStateException.class, () ->
                passwordService.createPassword(createRequest("Test", "user", "pass", null, null)));
        masterPasswordService.unlock(TEST_MASTER_PASSWORD);
    }

    @Test
    void testDecryptionWithWrongMasterPasswordFails() throws Exception {
        PasswordResponse created = passwordService.createPassword(
                createRequest("Test", "user", "mySecret", null, null));
        var stored = passwordRepository.findById(created.getId()).get();
        assertThrows(Exception.class, () ->
                encryptionService.decrypt(stored.getEncryptedPassword(), "WrongMaster1"));
    }

    @Test
    void testExportToJson() {
        passwordService.createPassword(createRequest("ExportTest", "user", "pass123", "Test", null));
        String json = passwordService.exportToJson();
        assertNotNull(json);
        assertTrue(json.contains("ExportTest"));
        assertTrue(json.contains("pass123"));
    }

    @Test
    void testImportFromJson() {
        String json = "[{\"serviceName\":\"ImportTest\",\"username\":\"imported\",\"password\":\"imp123\",\"category\":\"Test\",\"notes\":null}]";
        passwordService.importFromJson(json);
        List<PasswordResponse> all = passwordService.getAllPasswords();
        assertTrue(all.stream().anyMatch(p -> "ImportTest".equals(p.getServiceName())));
    }

    @Test
    void testGetAllPasswords() {
        passwordService.createPassword(createRequest("S1", "u1", "p1", null, null));
        passwordService.createPassword(createRequest("S2", "u2", "p2", null, null));
        List<PasswordResponse> all = passwordService.getAllPasswords();
        assertEquals(2, all.size());
        assertNull(all.get(0).getDecryptedPassword(), "Passwords should not be decrypted in list");
    }

    @Test
    void testUpdatePasswordWithoutChangingPassword() throws Exception {
        PasswordResponse created = passwordService.createPassword(
                createRequest("UpdateTest", "user", "keepThisPassword", null, null));
        String originalEncrypted = passwordRepository.findById(created.getId()).get().getEncryptedPassword();

        PasswordUpdateRequest upd = new PasswordUpdateRequest();
        upd.setServiceName("UpdatedService");
        passwordService.updatePassword(created.getId(), upd);

        var stored = passwordRepository.findById(created.getId()).get();
        assertEquals("UpdatedService", stored.getServiceName());
        assertEquals(originalEncrypted, stored.getEncryptedPassword(), "Encryption should not change");
    }

    @Test
    void testDuplicateServiceNamesAllowed() {
        passwordService.createPassword(createRequest("Gmail", "user1", "pass1", null, null));
        passwordService.createPassword(createRequest("Gmail", "user2", "pass2", null, null));
        List<PasswordResponse> all = passwordService.getAllPasswords();
        assertEquals(2, all.size());
    }
}
