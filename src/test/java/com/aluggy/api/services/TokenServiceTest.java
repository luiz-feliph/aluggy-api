package com.aluggy.api.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.aluggy.api.entities.User;
import com.aluggy.api.entities.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private static final String TEST_SECRET = "test-secret-key-for-unit-testing-12345";

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", TEST_SECRET);
    }

    private User createTestUser() {
        User user = new User("johndoe", "John Doe", "john@email.com", "1234567890", "password123", Role.USER);
        user.setId(UUID.randomUUID());
        return user;
    }

    @Test
    void generateToken_returnsNonEmptyToken() {
        String token = tokenService.generateToken(createTestUser());
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void generateToken_tokenCanBeDecodedWithCorrectSecret() {
        User user = createTestUser();
        String token = tokenService.generateToken(user);

        Algorithm algorithm = Algorithm.HMAC256(TEST_SECRET);
        var decoded = JWT.require(algorithm)
                .withIssuer("auth-api")
                .build()
                .verify(token);

        assertEquals(user.getUsername(), decoded.getSubject());
        assertNotNull(decoded.getExpiresAt());
        assertTrue(decoded.getExpiresAt().toInstant().isAfter(Instant.now()));
    }

    @Test
    void validateToken_validToken_returnsUsername() {
        User user = createTestUser();
        String token = tokenService.generateToken(user);

        String result = tokenService.validateToken(token);

        assertEquals("johndoe", result);
    }

    @Test
    void validateToken_expiredToken_returnsEmpty() {
        Algorithm algorithm = Algorithm.HMAC256(TEST_SECRET);
        String expiredToken = JWT.create()
                .withIssuer("auth-api")
                .withSubject("johndoe")
                .withExpiresAt(Instant.now().minusSeconds(3600))
                .sign(algorithm);

        String result = tokenService.validateToken(expiredToken);

        assertEquals("", result);
    }

    @Test
    void validateToken_tamperedToken_returnsEmpty() {
        String validToken = tokenService.generateToken(createTestUser());
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";

        String result = tokenService.validateToken(tamperedToken);

        assertEquals("", result);
    }

    @Test
    void validateToken_malformedString_returnsEmpty() {
        String result = tokenService.validateToken("not-a-valid-jwt-token");
        assertEquals("", result);
    }

    @Test
    void validateToken_emptyString_returnsEmpty() {
        String result = tokenService.validateToken("");
        assertEquals("", result);
    }

    @Test
    void validateToken_wrongIssuer_returnsEmpty() {
        Algorithm algorithm = Algorithm.HMAC256(TEST_SECRET);
        String wrongIssuerToken = JWT.create()
                .withIssuer("wrong-issuer")
                .withSubject("johndoe")
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(algorithm);

        String result = tokenService.validateToken(wrongIssuerToken);

        assertEquals("", result);
    }

    @Test
    void validateToken_signedWithDifferentSecret_returnsEmpty() {
        Algorithm algorithm = Algorithm.HMAC256("completely-different-secret-key");
        String wrongSecretToken = JWT.create()
                .withIssuer("auth-api")
                .withSubject("johndoe")
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(algorithm);

        String result = tokenService.validateToken(wrongSecretToken);

        assertEquals("", result);
    }

    @Test
    void validateToken_tokenWithNullSubject_returnsEmpty() {
        Algorithm algorithm = Algorithm.HMAC256(TEST_SECRET);
        String nullSubjectToken = JWT.create()
                .withIssuer("auth-api")
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(algorithm);

        String result = tokenService.validateToken(nullSubjectToken);

        assertNull(result);
    }

    @Test
    void generateToken_alwaysSetsIssuerAsAuthApi() {
        String token = tokenService.generateToken(createTestUser());

        Algorithm algorithm = Algorithm.HMAC256(TEST_SECRET);
        var decoded = JWT.require(algorithm).build().verify(token);

        assertEquals("auth-api", decoded.getIssuer());
    }

    @Test
    void generateToken_expirationIsInFuture() {
        String token = tokenService.generateToken(createTestUser());

        Algorithm algorithm = Algorithm.HMAC256(TEST_SECRET);
        var decoded = JWT.require(algorithm).build().verify(token);

        assertTrue(decoded.getExpiresAt().toInstant().isAfter(Instant.now()));
    }

    @Test
    void generateToken_differentUsersProduceDifferentTokens() {
        User user1 = new User("johndoe", "John Doe", "john@email.com", "1234567890", "password123", Role.USER);
        user1.setId(UUID.randomUUID());
        User user2 = new User("janedoe", "Jane Doe", "jane@email.com", "0987654321", "password456", Role.USER);
        user2.setId(UUID.randomUUID());

        String token1 = tokenService.generateToken(user1);
        String token2 = tokenService.generateToken(user2);

        assertNotEquals(token1, token2);
    }

    @Test
    void validateToken_alwaysReturnsEmptyOnFailure_neverThrows() {
        assertDoesNotThrow(() -> tokenService.validateToken(""));
        assertDoesNotThrow(() -> tokenService.validateToken("garbage"));
        assertDoesNotThrow(() -> tokenService.validateToken(null));
    }
}
