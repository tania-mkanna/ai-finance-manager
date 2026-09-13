package com.api.controller;

import com.api.dto.LoginRequest;
import com.api.dto.RegisterRequest;
import com.api.model.RefreshToken;
import com.api.model.User;
import com.api.repository.RefreshTokenRepository;
import com.api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void register_shouldCreateUserAndSetAuthCookies() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "tania@gmail.com",
                "SecurePassword123!",
                "Tania Mkanna",
                "USD",
                "Asia/Beirut"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(jsonPath("$.user.email").value("tania@gmail.com"))
                .andExpect(jsonPath("$.user.fullName").value("Tania Mkanna"));

        assertThat(userRepository.findByEmail("tania@gmail.com")).isPresent();
    }

    @Test
    void duplicateEmail_shouldReturnConflict() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "duplicate@gmail.com",
                "SecurePassword123!",
                "User One",
                "USD",
                "Europe/Paris"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void login_shouldAuthenticateAndSetCookies() throws Exception {
        User user = new User();
        user.setEmail("tania@gmail.com");
        user.setPasswordHash(passwordEncoder.encode("SecurePassword123!"));
        user.setFullName("Tania Mkanna");
        user.setDefaultCurrency("USD");
        user.setTimezone("Asia/Beirut");
        userRepository.save(user);

        LoginRequest request = new LoginRequest("tania@gmail.com", "SecurePassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(jsonPath("$.user.email").value("tania@gmail.com"));
    }

    @Test
    void invalidCredentials_shouldReturnUnauthorized() throws Exception {
        User user = new User();
        user.setEmail("tania@gmail.com");
        user.setPasswordHash(passwordEncoder.encode("SecurePassword123!"));
        user.setFullName("Tania Mkanna");
        userRepository.save(user);

        LoginRequest request = new LoginRequest("tania@gmail.com", "WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordShouldBeBCryptHashed() {
        String rawPassword = "SecurePassword123!";
        User user = new User();
        user.setEmail("hash@test.com");
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName("Hash User");

        assertThat(user.getPasswordHash()).doesNotContain(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, user.getPasswordHash())).isTrue();
    }

    @Test
    void protectedMe_shouldRequireAuthentication() throws Exception {

        MvcResult result = mockMvc.perform(get("/api/v1/auth/me"))
                .andDo(print())
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);

        // Register user
        var registerRequest = new RegisterRequest(
                "me@example.com",
                "SecurePassword123!",
                "Current User",
                "USD",
                "Europe/Paris"
        );

        MvcResult registerResult = mockMvc.perform(
                        post("/api/v1/auth/register")
                                .header("Origin", "http://localhost:5173")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest))
                )
                .andExpect(status().isCreated())
                .andReturn();

        var accessCookie = registerResult
                .getResponse()
                .getCookie("access_token");

        assertThat(accessCookie).isNotNull();

        // Authenticated using the access_token cookie
        mockMvc.perform(
                        get("/api/v1/auth/me")
                                .cookie(accessCookie)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"));
    }

    @Test
    void refresh_shouldRotateRefreshTokenAndReturnSuccess() throws Exception {
        User user = new User();
        user.setEmail("refresh@test.com");
        user.setPasswordHash(passwordEncoder.encode("SecurePassword123!"));
        user.setFullName("Refresh User");
        userRepository.save(user);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("refresh@test.com", "SecurePassword123!"))))
                .andExpect(status().isOk())
                .andReturn();

        var oldRefreshCookie = loginResult.getResponse().getCookie("refresh_token");
        assertThat(oldRefreshCookie).isNotNull();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(oldRefreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"));

        assertThat(refreshTokenRepository.findAll()).hasSize(2);
    }

    @Test
    void revokedRefreshToken_shouldReturnUnauthorized() throws Exception {
        User user = new User();
        user.setEmail("revoked@test.com");
        user.setPasswordHash(passwordEncoder.encode("SecurePassword123!"));
        user.setFullName("Revoked User");
        userRepository.save(user);

        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("revoked@test.com", "SecurePassword123!"))))
                .andExpect(status().isOk())
                .andReturn();

        var refreshCookie = loginResult.getResponse().getCookie("refresh_token");
        assertThat(refreshCookie).isNotNull();

        RefreshToken token = refreshTokenRepository.findAll().getFirst();
        token.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_shouldRevokeRefreshTokenAndClearCookies() throws Exception {
        User user = new User();
        user.setEmail("logout@test.com");
        user.setPasswordHash(passwordEncoder.encode("SecurePassword123!"));
        user.setFullName("Logout User");
        userRepository.save(user);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("logout@test.com", "SecurePassword123!"))))
                .andExpect(status().isOk())
                .andReturn();

        var refreshCookie = loginResult.getResponse().getCookie("refresh_token");
        assertThat(refreshCookie).isNotNull();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(refreshCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("refresh_token", 0))
                .andExpect(cookie().maxAge("access_token", 0));

        assertThat(refreshTokenRepository.findAll()).singleElement().satisfies(token -> assertThat(token.getRevokedAt()).isNotNull());
    }
}
