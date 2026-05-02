package ua.university.passwordmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.university.passwordmanager.entity.StoredPassword;

import java.util.List;

@Repository
public interface PasswordRepository extends JpaRepository<StoredPassword, String> {
    List<StoredPassword> findByServiceNameContainingIgnoreCase(String serviceName);
    List<StoredPassword> findByUsernameContainingIgnoreCase(String username);
    List<StoredPassword> findByCategoryIgnoreCase(String category);
    List<StoredPassword> findTop10ByOrderByLastUsedAtDesc();
}
