package com.aluggy.api.infra.security;

import com.aluggy.api.entities.User;
import com.aluggy.api.entities.enums.Role;
import com.aluggy.api.repositories.UserRepository;
import com.aluggy.api.services.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityFilterTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private SecurityFilter securityFilter;

    private User createTestUser() {
        User user = new User("johndoe", "John Doe", "john@email.com", "1234567890", "password", Role.USER);
        user.setId(UUID.randomUUID());
        return user;
    }

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_validToken_existingUser_setsAuthentication() throws ServletException, IOException {
        User user = createTestUser();
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(tokenService.validateToken("valid-token")).thenReturn("johndoe");
        when(userRepository.findByUserNameOrEmailAddress("johndoe", "johndoe"))
                .thenReturn(Optional.of(user));

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_validToken_existingUser_setsCorrectAuthorities() throws ServletException, IOException {
        User user = createTestUser();
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(tokenService.validateToken("valid-token")).thenReturn("johndoe");
        when(userRepository.findByUserNameOrEmailAddress("johndoe", "johndoe"))
                .thenReturn(Optional.of(user));

        securityFilter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void doFilterInternal_validToken_adminUser_setsAdminAuthorities() throws ServletException, IOException {
        User adminUser = new User("admin", "Admin User", "admin@email.com", "1234567890", "password", Role.ADMIN);
        adminUser.setId(UUID.randomUUID());
        when(request.getHeader("Authorization")).thenReturn("Bearer admin-token");
        when(tokenService.validateToken("admin-token")).thenReturn("admin");
        when(userRepository.findByUserNameOrEmailAddress("admin", "admin"))
                .thenReturn(Optional.of(adminUser));

        securityFilter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void doFilterInternal_validToken_deletedUser_noAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(tokenService.validateToken("valid-token")).thenReturn("deleteduser");
        when(userRepository.findByUserNameOrEmailAddress("deleteduser", "deleteduser"))
                .thenReturn(Optional.empty());

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_validToken_deletedUser_continuesWithoutCrashing() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(tokenService.validateToken("valid-token")).thenReturn("deleteduser");
        when(userRepository.findByUserNameOrEmailAddress("deleteduser", "deleteduser"))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> securityFilter.doFilterInternal(request, response, filterChain));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_invalidToken_doesNotQueryDatabase() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(tokenService.validateToken("invalid-token")).thenReturn("");

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(userRepository, never()).findByUserNameOrEmailAddress(anyString(), anyString());
    }

    @Test
    void doFilterInternal_invalidToken_noAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(tokenService.validateToken("invalid-token")).thenReturn("");

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_noAuthorizationHeader_noAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenService, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_noAuthorizationHeader_doesNotCallTokenService() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(tokenService, never()).validateToken(anyString());
        verify(userRepository, never()).findByUserNameOrEmailAddress(anyString(), anyString());
    }

    @Test
    void doFilterInternal_emptyAuthorizationHeader_noAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("");

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_bearerPrefixStrippedCorrectly() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer my-custom-token-123");
        when(tokenService.validateToken("my-custom-token-123")).thenReturn("johndoe");
        User user = createTestUser();
        when(userRepository.findByUserNameOrEmailAddress("johndoe", "johndoe"))
                .thenReturn(Optional.of(user));

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(tokenService).validateToken("my-custom-token-123");
    }

    @Test
    void doFilterInternal_alwaysCallsFilterChain() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_tokenWithoutBearerPrefix_noAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("some-token-without-prefix");
        when(tokenService.validateToken("some-token-without-prefix")).thenReturn("");

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userRepository, never()).findByUserNameOrEmailAddress(anyString(), anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_filterDoesNotThrowOnValidateTokenFailure() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(tokenService.validateToken("bad-token")).thenReturn("");

        assertDoesNotThrow(() -> securityFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void doFilterInternal_emptyContextAfterNoToken_requestProceeds() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
