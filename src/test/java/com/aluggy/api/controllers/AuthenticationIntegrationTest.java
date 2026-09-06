package com.aluggy.api.controllers;

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
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registerBody(String userName, String emailAddress, String contactNumber, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "userName", userName,
                "emailAddress", emailAddress,
                "contactNumber", contactNumber,
                "password", password
        ));
    }

    private String loginBody(String login, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "login", login,
                "password", password
        ));
    }

    @Test
    void registerThenGetMyData_returnsAuthenticatedUser() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("user1", "user1@email.com", "99123456788", "password123")))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists("AUTH_TOKEN"))
                .andReturn();

        String authCookie = registerResult.getResponse().getCookie("AUTH_TOKEN").getValue();

        mockMvc.perform(get("/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("AUTH_TOKEN", authCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("user1"))
                .andExpect(jsonPath("$.emailAddress").value("user1@email.com"));
    }

    @Test
    void loginThenGetMyData_returnsAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("user1", "user1@email.com", "99123456787", "password123")))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("user1", "password123")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("AUTH_TOKEN"))
                .andReturn();

        String authCookie = loginResult.getResponse().getCookie("AUTH_TOKEN").getValue();

        mockMvc.perform(get("/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("AUTH_TOKEN", authCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("user1"))
                .andExpect(jsonPath("$.emailAddress").value("user1@email.com"));
    }

    @Test
    void loginWithEmailThenGetMyData_returnsAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("user1", "user1@email.com", "99123456786", "password123")))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("user1@email.com", "password123")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("AUTH_TOKEN"))
                .andReturn();

        String authCookie = loginResult.getResponse().getCookie("AUTH_TOKEN").getValue();

        mockMvc.perform(get("/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("AUTH_TOKEN", authCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("user1"));
    }

    @Test
    void getMyData_withoutCookie_returns401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_authenticated_returns204AndClearsCookie() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("user1", "user1@email.com", "99123456785", "password123")))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists("AUTH_TOKEN"))
                .andReturn();

        String authCookie = registerResult.getResponse().getCookie("AUTH_TOKEN").getValue();

        mockMvc.perform(post("/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("AUTH_TOKEN", authCookie)))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("AUTH_TOKEN", 0));
    }

    @Test
    void logout_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized());
    }
}
