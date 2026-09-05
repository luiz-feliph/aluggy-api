package com.aluggy.api.controllers;

import com.aluggy.api.entities.User;
import com.aluggy.api.entities.enums.Role;
import com.aluggy.api.exceptions.UserAlreadyExistsException;
import com.aluggy.api.infra.security.SecurityConfigurations;
import com.aluggy.api.infra.security.SecurityFilter;
import com.aluggy.api.repositories.UserRepository;
import com.aluggy.api.services.TokenService;
import com.aluggy.api.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthenticationController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({SecurityConfigurations.class, SecurityFilter.class})
@AutoConfigureMockMvc
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserRepository userRepository;

    private User createTestUser() {
        User user = new User("johndoe", "john@email.com", "99123456789", "encoded-password", Role.USER);
        user.setId(UUID.randomUUID());
        return user;
    }

    @Test
    void login_validCredentials_returns204WithAuthCookie() throws Exception {
        User user = createTestUser();
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(tokenService.generateToken(user)).thenReturn("mock-jwt-token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"johndoe\",\"password\":\"password123\"}"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(cookie().exists("AUTH_TOKEN"))
                .andExpect(cookie().httpOnly("AUTH_TOKEN", true))
                .andExpect(cookie().path("AUTH_TOKEN", "/"))
                .andExpect(cookie().maxAge("AUTH_TOKEN", 7200))
                .andExpect(cookie().sameSite("AUTH_TOKEN", "Strict"));
    }

    @Test
    void login_validEmailCredentials_returns204WithCookie() throws Exception {
        User user = createTestUser();
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(tokenService.generateToken(user)).thenReturn("mock-jwt-token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"john@email.com\",\"password\":\"password123\"}"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("AUTH_TOKEN"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"johndoe\"," +
                                "\"password\":\"wrongpassword\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_nonExistentUser_returns401() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"nonexistent\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_nullLoginField_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_nullPasswordField_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"johndoe\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_validData_returns201WithCookie() throws Exception {
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

        User savedUser = createTestUser();
        savedUser.setPassword("encoded-password");
        when(userService.insert(any(User.class))).thenReturn(savedUser);
        when(tokenService.generateToken(any(User.class))).thenReturn("mock-jwt-token");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"johndoe\",\"emailAddress\":\"john@email.com\",\"contactNumber\":\"99123456789\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(cookie().exists("AUTH_TOKEN"))
                .andExpect(cookie().httpOnly("AUTH_TOKEN", true))
                .andExpect(cookie().path("AUTH_TOKEN", "/"))
                .andExpect(cookie().maxAge("AUTH_TOKEN", 7200))
                .andExpect(cookie().sameSite("AUTH_TOKEN", "Strict"))
                .andExpect(content().string(""));
    }

    @Test
    void register_validData_createsUserWithRoleUSER() throws Exception {
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

        User savedUser = createTestUser();
        when(userService.insert(any(User.class))).thenReturn(savedUser);
        when(tokenService.generateToken(any(User.class))).thenReturn("mock-jwt-token");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"johndoe\",\"emailAddress\":\"john@email.com\",\"contactNumber\":\"99123456789\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());

        verify(userService).insert(argThat(user -> user.getRole() == Role.USER));
    }

    @Test
    void register_duplicateUser_returns409() throws Exception {
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userService.insert(any(User.class)))
                .thenThrow(new UserAlreadyExistsException("User already exists"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"johndoe\",\"emailAddress\":\"john@email.com\",\"contactNumber\":\"99123456789\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());

        verify(userService).insert(any(User.class));
    }

    @Test
    void register_missingUserName_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailAddress\":\"john@email.com\",\"contactNumber\":\"99123456789\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_missingContactNumber_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"johndoe\",\"emailAddress\":\"john@email.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_emptyUserName_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"\",\"emailAddress\":\"john@email.com\",\"contactNumber\":\"99123456789\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_roleFieldIsIgnored_alwaysCreatesAsUSER() throws Exception {
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

        User savedUser = new User("adminuser", "admin@email.com", "99123456789", "encoded-password", Role.USER);
        savedUser.setId(UUID.randomUUID());
        when(userService.insert(any(User.class))).thenReturn(savedUser);
        when(tokenService.generateToken(any(User.class))).thenReturn("mock-jwt-token");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"adminuser\",\"emailAddress\":\"admin@email.com\",\"contactNumber\":\"99123456789\",\"password\":\"password123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated());

        verify(userService).insert(argThat(user -> user.getRole() == Role.USER));
    }

    @Test
    void register_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_emptyPassword_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"johndoe\",\"emailAddress\":\"john@email.com\",\"contactNumber\":\"99123456789\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_invalidEmailFormat_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"johndoe\",\"emailAddress\":\"not-an-email\",\"contactNumber\":\"99123456789\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyData_authenticatedUser_returnsUserResponseDTO() throws Exception {
        User user = createTestUser();
        user.setFullName("John Doe");
        user.setDescription("Test description");
        when(tokenService.validateToken("valid-token")).thenReturn("johndoe");
        when(userRepository.findByUserNameOrEmailAddress("johndoe", "johndoe"))
                .thenReturn(Optional.of(user));

        mockMvc.perform(get("/auth/me")
                        .cookie(new Cookie("AUTH_TOKEN", "valid-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.userName").value("johndoe"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.emailAddress").value("john@email.com"))
                .andExpect(jsonPath("$.contactNumber").value("99123456789"))
                .andExpect(jsonPath("$.description").value("Test description"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void getMyData_adminUser_returnsAdminRole() throws Exception {
        User admin = new User("adminuser", "admin@email.com", "99123456789", "encoded-password", Role.ADMIN);
        admin.setId(UUID.randomUUID());
        when(tokenService.validateToken("admin-token")).thenReturn("adminuser");
        when(userRepository.findByUserNameOrEmailAddress("adminuser", "adminuser"))
                .thenReturn(Optional.of(admin));

        mockMvc.perform(get("/auth/me")
                        .cookie(new Cookie("AUTH_TOKEN", "admin-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void getMyData_withoutAuthCookie_returns401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyData_unknownUserToken_returns401() throws Exception {
        when(tokenService.validateToken("valid-token")).thenReturn("deleteduser");
        when(userRepository.findByUserNameOrEmailAddress("deleteduser", "deleteduser"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/auth/me")
                        .cookie(new Cookie("AUTH_TOKEN", "valid-token")))
                .andExpect(status().isUnauthorized());
    }
}
