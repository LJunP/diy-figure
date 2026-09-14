package com.diyfigure.integration;

import com.diyfigure.common.enums.AuthTokenPurpose;
import com.diyfigure.entity.AuthToken;
import com.diyfigure.entity.User;
import com.diyfigure.repository.AuthTokenRepository;
import com.diyfigure.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("注册登录与邮箱验证/重置密码")
class AuthFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;
    @Autowired private org.flywaydb.core.Flyway flyway;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthTokenRepository authTokenRepository;

    @BeforeEach
    void reset() {
        flyway.clean();
        flyway.migrate();
    }

    @Test
    @DisplayName("注册后未验证邮箱仍可登录;验证令牌生效")
    void registerLoginAndVerifyEmail() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "u" + suffix;
        String email = username + "@test.local";
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "username", username,
                                "email", email,
                                "password", "secret12"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.user.emailVerified").value(false));

        User user = userRepository.findByEmail(email).orElseThrow();
        assertFalse(user.getEmailVerified());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "account", email,
                                "password", "secret12"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String raw = "verify-token-" + suffix;
        authTokenRepository.save(AuthToken.builder()
                .userId(user.getId())
                .purpose(AuthTokenPurpose.EMAIL_VERIFY)
                .tokenHash(sha256(raw))
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build());

        mockMvc.perform(post("/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("token", raw))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertTrue(userRepository.findById(user.getId()).orElseThrow().getEmailVerified());
    }

    @Test
    @DisplayName("重复用户名返回业务错误;禁用账号无法登录")
    void duplicateAndDisabled() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "d" + suffix;
        String email = username + "@test.local";
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "username", username, "email", email, "password", "secret12"))))
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "username", username, "email", "other" + email, "password", "secret12"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001));

        User user = userRepository.findByEmail(email).orElseThrow();
        user.setEnabled(false);
        userRepository.save(user);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "account", username, "password", "secret12"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4004));
    }

    @Test
    @DisplayName("忘记密码不泄露账号是否存在;有效令牌可重置")
    void forgotAndResetPassword() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "r" + suffix;
        String email = username + "@test.local";
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "username", username, "email", email, "password", "oldpass1"))))
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "nobody@test.local"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email))))
                .andExpect(jsonPath("$.code").value(200));

        User user = userRepository.findByEmail(email).orElseThrow();
        String raw = "reset-token-" + suffix;
        authTokenRepository.save(AuthToken.builder()
                .userId(user.getId())
                .purpose(AuthTokenPurpose.PASSWORD_RESET)
                .tokenHash(sha256(raw))
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build());

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "token", raw, "newPassword", "newpass1"))))
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "account", email, "password", "newpass1"))))
                .andExpect(jsonPath("$.code").value(200));
        assertEquals(username, userRepository.findByEmail(email).orElseThrow().getUsername());
    }

    private static String sha256(String raw) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
    }
}
