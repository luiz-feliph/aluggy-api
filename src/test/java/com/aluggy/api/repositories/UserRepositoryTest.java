package com.aluggy.api.repositories;

import com.aluggy.api.entities.User;
import com.aluggy.api.entities.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User createAndPersistUser(String userName, String emailAddress) {
        User user = new User(userName, emailAddress, "1234567890", "encoded-password", Role.USER);
        return entityManager.persistAndFlush(user);
    }

    @Test
    void findByUserNameOrEmailAddress_foundByUsername() {
        User user = createAndPersistUser("johndoe", "john@email.com");

        Optional<User> result = userRepository.findByUserNameOrEmailAddress("johndoe", "anything");

        assertTrue(result.isPresent());
        assertEquals("johndoe", result.get().getUsername());
    }

    @Test
    void findByUserNameOrEmailAddress_foundByEmail() {
        User user = createAndPersistUser("johndoe", "john@email.com");

        Optional<User> result = userRepository.findByUserNameOrEmailAddress("anything", "john@email.com");

        assertTrue(result.isPresent());
        assertEquals("johndoe", result.get().getUsername());
    }

    @Test
    void findByUserNameOrEmailAddress_foundByBothParams() {
        User user = createAndPersistUser("johndoe", "john@email.com");

        Optional<User> result = userRepository.findByUserNameOrEmailAddress("johndoe", "john@email.com");

        assertTrue(result.isPresent());
        assertEquals("johndoe", result.get().getUsername());
    }

    @Test
    void findByUserNameOrEmailAddress_notFound() {
        Optional<User> result = userRepository.findByUserNameOrEmailAddress("nonexistent", "nonexistent@email.com");

        assertTrue(result.isEmpty());
    }

    @Test
    void existsByUserName_true() {
        createAndPersistUser("johndoe", "john@email.com");

        assertTrue(userRepository.existsByUserName("johndoe"));
    }

    @Test
    void existsByUserName_false() {
        assertFalse(userRepository.existsByUserName("nonexistent"));
    }

    @Test
    void existsByEmailAddress_true() {
        createAndPersistUser("johndoe", "john@email.com");

        assertTrue(userRepository.existsByEmailAddress("john@email.com"));
    }

    @Test
    void existsByEmailAddress_false() {
        assertFalse(userRepository.existsByEmailAddress("nonexistent@email.com"));
    }

    @Test
    void softDelete_userNotReturnedByQuery() {
        User user = createAndPersistUser("johndoe", "john@email.com");
        UUID userId = user.getId();

        entityManager
                .getEntityManager()
                .createNativeQuery("UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
                .setParameter(1, userId)
                .executeUpdate();
        entityManager.clear();

        Optional<User> result = userRepository.findById(userId);

        assertTrue(result.isEmpty(), "Soft-deleted user should not be returned by findById");
    }

    @Test
    void softDelete_userNotReturnedByUserNameQuery() {
        User user = createAndPersistUser("johndoe", "john@email.com");
        UUID userId = user.getId();

        entityManager
                .getEntityManager()
                .createNativeQuery("UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
                .setParameter(1, userId)
                .executeUpdate();
        entityManager.clear();

        Optional<User> result = userRepository.findByUserNameOrEmailAddress("johndoe", "john@email.com");

        assertTrue(result.isEmpty(), "Soft-deleted user should not be returned by findByUserNameOrEmailAddress");
    }

    @Test
    void softDelete_userNotCountedByExistsByUserName() {
        User user = createAndPersistUser("johndoe", "john@email.com");

        entityManager
                .getEntityManager()
                .createNativeQuery("UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
                .setParameter(1, user.getId())
                .executeUpdate();
        entityManager.clear();

        assertFalse(userRepository.existsByUserName("johndoe"),
                "Soft-deleted user should not be counted by existsByUserName");
    }

    @Test
    void softDelete_userNotCountedByExistsByEmailAddress() {
        User user = createAndPersistUser("johndoe", "john@email.com");

        entityManager
                .getEntityManager()
                .createNativeQuery("UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
                .setParameter(1, user.getId())
                .executeUpdate();
        entityManager.clear();

        assertFalse(userRepository.existsByEmailAddress("john@email.com"),
                "Soft-deleted user should not be counted by existsByEmailAddress");
    }

    @Test
    void softDelete_userStillExistsInDatabase_viaNativeQuery() {
        User user = createAndPersistUser("johndoe", "john@email.com");
        UUID userId = user.getId();

        entityManager
                .getEntityManager()
                .createNativeQuery("UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
                .setParameter(1, userId)
                .executeUpdate();
        entityManager.clear();

        Object[] result = (Object[]) entityManager
                .getEntityManager()
                .createNativeQuery("SELECT CAST(id AS VARCHAR) as id, user_name, deleted_at FROM users WHERE id = ?")
                .setParameter(1, userId)
                .getSingleResult();

        assertNotNull(result, "Soft-deleted user should still exist in the database via native query");
        assertEquals(userId.toString(), result[0]);
    }

    @Test
    void save_setsCreatedAt() {
        User user = new User("johndoe", "john@email.com", "1234567890", "encoded-password", Role.USER);

        User saved = userRepository.save(user);
        entityManager.flush();

        assertNotNull(saved.getRegisteredAt(), "registeredAt should be set after save");
    }

    @Test
    void save_persistsWithRole() {
        User user = new User("johndoe", "john@email.com", "1234567890", "encoded-password", Role.ADMIN);

        User saved = userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        Optional<User> found = userRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(Role.ADMIN, found.get().getRole());
    }

    @Test
    void findByUserNameOrEmailAddress_duplicateUsername_returnsFirst() {
        User user1 = createAndPersistUser("johndoe", "john1@email.com");

        Optional<User> result = userRepository.findByUserNameOrEmailAddress("johndoe", "anything");

        assertTrue(result.isPresent());
        assertEquals("john1@email.com", result.get().getEmailAddress());
    }

    @Test
    void findByUserNameOrEmailAddress_caseSensitiveUsername() {
        createAndPersistUser("johndoe", "john@email.com");

        Optional<User> result = userRepository.findByUserNameOrEmailAddress("JOHNDOE", "anything");

        assertTrue(result.isEmpty(), "Username lookup should be case-sensitive");
    }

    @Test
    void findByUserNameOrEmailAddress_caseSensitiveEmail() {
        createAndPersistUser("johndoe", "john@email.com");

        Optional<User> result = userRepository.findByUserNameOrEmailAddress("anything", "JOHN@email.com");

        assertTrue(result.isEmpty(), "Email lookup should be case-sensitive");
    }

    @Test
    void save_persistsFullName() {
        User user = new User("johndoe", "johndoe@email.com", "12345678900", "encoded-password", Role.USER);
        user.setFullName("John Doe");

        User saved = userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        Optional<User> found = userRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("John Doe", found.get().getFullName());
    }

    @Test
    void save_persistsDescription() {
        User user = new User("johndoe", "johndoe@email.com", "12345678900", "encoded-password", Role.USER);
        user.setDescription("A test user");

        User saved = userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        Optional<User> found = userRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("A test user", found.get().getDescription());
    }
}
