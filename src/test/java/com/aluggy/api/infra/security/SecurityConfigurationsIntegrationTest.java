package com.aluggy.api.infra.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class SecurityConfigurationsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registerBody() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "userName", "secuser",
                "emailAddress", "secuser@email.com",
                "contactNumber", "99123456780",
                "password", "password123"
        ));
    }

    private String loginBody() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "login", "secuser",
                "password", "password123"
        ));
    }

    private String registerAndGetCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody()))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getCookie("AUTH_TOKEN").getValue();
    }

    @Test
    void register_isPubliclyAccessible() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody()))
                .andExpect(status().isCreated());
    }

    @Test
    void login_isPubliclyAccessible() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody()))
                .andExpect(status().isNoContent());
    }

    @Test
    void getMyData_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyData_withValidCookie_returns200() throws Exception {
        String authCookie = registerAndGetCookie();

        mockMvc.perform(get("/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("AUTH_TOKEN", authCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("secuser"));
    }

    @Test
    void getMyData_withInvalidCookie_returns401() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("AUTH_TOKEN", "garbage-invalid-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedRequest_returns401WithJsonBody() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").value("Authentication required"));
    }

    @Test
    void authenticatedRequest_setsNoAuthCookie() throws Exception {
        String authCookie = registerAndGetCookie();

        mockMvc.perform(get("/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("AUTH_TOKEN", authCookie)))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("AUTH_TOKEN"));
    }
}
