package com.aluggy.api.infra.security;

import com.aluggy.api.entities.User;
import com.aluggy.api.entities.enums.Role;
import com.aluggy.api.repositories.UserRepository;
import com.aluggy.api.services.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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
        User user = new User("johndoe", "john@email.com", "1234567890", "password", Role.USER);
        user.setId(UUID.randomUUID());
        return user;
    }

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_validCookie_existingUser_setsAuthentication() throws ServletException, IOException {
        User user = createTestUser();
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("AUTH_TOKEN", "valid-token")});
        when(tokenService.validateToken("valid-token")).thenReturn("johndoe");
        when(userRepository.findByUserNameOrEmailAddress("johndoe"))
                .thenReturn(Optional.of(user));

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_validCookie_existingUser_setsCorrectAuthorities() throws ServletException, IOException {
        User user = createTestUser();
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("AUTH_TOKEN", "valid-token")});
        when(tokenService.validateToken("valid-token")).thenReturn("johndoe");
        when(userRepository.findByUserNameOrEmailAddress("johndoe"))
                .thenReturn(Optional.of(user));

        securityFilter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void doFilterInternal_validCookie_adminUser_setsAdminAuthorities() throws ServletException, IOException {
        User adminUser = new User("admin", "admin@email.com", "1234567890", "password", Role.ADMIN);
        adminUser.setId(UUID.randomUUID());
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("AUTH_TOKEN", "admin-token")});
        when(tokenService.validateToken("admin-token")).thenReturn("admin");
        when(userRepository.findByUserNameOrEmailAddress("admin"))
                .thenReturn(Optional.of(adminUser));

        securityFilter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void doFilterInternal_validCookie_deletedUser_noAuthentication() throws ServletException, IOException {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("AUTH_TOKEN", "valid-token")});
        when(tokenService.validateToken("valid-token")).thenReturn("deleteduser");
        when(userRepository.findByUserNameOrEmailAddress("deleteduser"))
                .thenReturn(Optional.empty());

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_validCookie_deletedUser_continuesWithoutCrashing() throws ServletException, IOException {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("AUTH_TOKEN", "valid-token")});
        when(tokenService.validateToken("valid-token")).thenReturn("deleteduser");
        when(userRepository.findByUserNameOrEmailAddress("deleteduser"))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> securityFilter.doFilterInternal(request, response, filterChain));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_invalidCookieToken_doesNotQueryDatabase() throws ServletException, IOException {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("AUTH_TOKEN", "invalid-token")});
        when(tokenService.validateToken("invalid-token")).thenReturn("");

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(userRepository, never()).findByUserNameOrEmailAddress(anyString());
    }

    @Test
    void doFilterInternal_invalidCookieToken_noAuthentication() throws ServletException, IOException {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("AUTH_TOKEN", "invalid-token")});
        when(tokenService.validateToken("invalid-token")).thenReturn("");

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_noCookies_noAuthentication() throws ServletException, IOException {
        when(request.getCookies()).thenReturn(null);

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenService, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_noCookies_doesNotCallTokenService() throws ServletException, IOException {
        when(request.getCookies()).thenReturn(null);

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(tokenService, never()).validateToken(anyString());
        verify(userRepository, never()).findByUserNameOrEmailAddress(anyString());
    }

    @Test
    void doFilterInternal_emptyCookiesArray_noAuthentication() throws ServletException, IOException {
        when(request.getCookies()).thenReturn(new Cookie[]{});

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_multipleCookies_findsAuthToken() throws ServletException, IOException {
        User user = createTestUser();
        Cookie otherCookie = new Cookie("SESSION", "abc123");
        Cookie authCookie = new Cookie("AUTH_TOKEN", "valid-token");
        when(request.getCookies()).thenReturn(new Cookie[]{otherCookie, authCookie});
        when(tokenService.validateToken("valid-token")).thenReturn("johndoe");
        when(userRepository.findByUserNameOrEmailAddress("johndoe"))
                .thenReturn(Optional.of(user));

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenService).validateToken("valid-token");
    }

    @Test
    void doFilterInternal_nonAuthCookie_ignoresToken() throws ServletException, IOException {
        Cookie otherCookie = new Cookie("SESSION", "abc123");
        when(request.getCookies()).thenReturn(new Cookie[]{otherCookie});

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenService, never()).validateToken(anyString());
    }

    @Test
    void doFilterInternal_alwaysCallsFilterChain() throws ServletException, IOException {
        when(request.getCookies()).thenReturn(null);

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_authCookieWithNoValue_noAuthentication() throws ServletException, IOException {
        Cookie emptyValueCookie = new Cookie("AUTH_TOKEN", "");
        when(request.getCookies()).thenReturn(new Cookie[]{emptyValueCookie});
        when(tokenService.validateToken("")).thenReturn("");

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenService).validateToken("");
    }

    @Test
    void doFilterInternal_filterDoesNotThrowOnValidateTokenFailure() {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("AUTH_TOKEN", "bad-token")});
        when(tokenService.validateToken("bad-token")).thenReturn("");

        assertDoesNotThrow(() -> securityFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void doFilterInternal_emptyContextAfterNoToken_requestProceeds() throws ServletException, IOException {
        when(request.getCookies()).thenReturn(null);

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
