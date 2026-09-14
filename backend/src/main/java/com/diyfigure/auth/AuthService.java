package com.diyfigure.auth;

import com.diyfigure.auth.dto.AuthResponse;
import com.diyfigure.auth.dto.LoginRequest;
import com.diyfigure.auth.dto.RegisterRequest;
import com.diyfigure.auth.dto.UpdatePasswordRequest;
import com.diyfigure.common.enums.AuthTokenPurpose;
import com.diyfigure.common.enums.UserRole;
import com.diyfigure.common.exception.BusinessException;
import com.diyfigure.common.response.ResultCode;
import com.diyfigure.entity.AuthToken;
import com.diyfigure.entity.User;
import com.diyfigure.notification.EmailService;
import com.diyfigure.repository.AuthTokenRepository;
import com.diyfigure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * 认证服务:注册、登录、邮箱验证、忘记密码。
 *
 * 未配置 SMTP 时邮件降级为日志,token 仍写入 auth_token,测试可从库中读取。
 * 未验证邮箱不阻断登录,避免演示环境没有 SMTP 时整个产品不能用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${diy.app.public-url:http://localhost:5173}")
    private String appPublicUrl;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS, "用户名已被注册");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS, "邮箱已被注册");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .emailVerified(false)
                .enabled(true)
                .build();
        user = userRepository.save(user);
        issueEmailVerification(user);
        log.info("用户注册成功: id={}, username={}", user.getId(), user.getUsername());
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getAccount())
                .or(() -> userRepository.findByEmail(request.getAccount()))
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND, "账号不存在"));

        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.PASSWORD_INCORRECT);
        }
        log.info("用户登录成功: id={}, username={}", user.getId(), user.getUsername());
        return buildAuthResponse(user);
    }

    public AuthResponse.UserInfo getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
        return toUserInfo(user);
    }

    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.PASSWORD_INCORRECT, "原密码不正确");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("用户修改密码成功: id={}", userId);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        AuthToken token = requireValidToken(rawToken, AuthTokenPurpose.EMAIL_VERIFY,
                ResultCode.EMAIL_TOKEN_INVALID);
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
        user.setEmailVerified(true);
        userRepository.save(user);
        token.setConsumedAt(LocalDateTime.now());
        authTokenRepository.save(token);
        log.info("邮箱已验证: userId={}", user.getId());
    }

    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (Boolean.TRUE.equals(user.getEmailVerified())) {
                return;
            }
            issueEmailVerification(user);
        });
    }

    /**
     * 无论邮箱是否存在都返回成功,避免被用来枚举账号。
     */
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(this::issuePasswordReset);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        AuthToken token = requireValidToken(rawToken, AuthTokenPurpose.PASSWORD_RESET,
                ResultCode.PASSWORD_RESET_TOKEN_INVALID);
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        token.setConsumedAt(LocalDateTime.now());
        authTokenRepository.save(token);
        log.info("用户通过重置链接修改密码: id={}", user.getId());
    }

    private void issueEmailVerification(User user) {
        String raw = persistToken(user.getId(), AuthTokenPurpose.EMAIL_VERIFY, 48);
        String link = trimSlash(appPublicUrl) + "/verify-email?token=" + raw;
        emailService.sendAfterCommit(user.getEmail(), "验证你的 DIY Figure 邮箱",
                "请在 48 小时内打开以下链接完成邮箱验证:\n" + link + "\n\n如果不是你本人操作,请忽略本邮件。");
        log.info("已签发邮箱验证令牌: userId={}", user.getId());
    }

    private void issuePasswordReset(User user) {
        String raw = persistToken(user.getId(), AuthTokenPurpose.PASSWORD_RESET, 1);
        String link = trimSlash(appPublicUrl) + "/reset-password?token=" + raw;
        emailService.sendAfterCommit(user.getEmail(), "重置 DIY Figure 密码",
                "请在 1 小时内打开以下链接重置密码:\n" + link + "\n\n如果不是你本人操作,请忽略本邮件。");
        log.info("已签发密码重置令牌: userId={}", user.getId());
    }

    private String persistToken(Long userId, AuthTokenPurpose purpose, int hoursValid) {
        List<AuthToken> open = authTokenRepository.findByUserIdAndPurposeAndConsumedAtIsNull(userId, purpose);
        LocalDateTime now = LocalDateTime.now();
        for (AuthToken existing : open) {
            existing.setConsumedAt(now);
        }
        authTokenRepository.saveAll(open);

        byte[] extra = new byte[16];
        secureRandom.nextBytes(extra);
        String raw = UUID.randomUUID().toString().replace("-", "") + HexFormat.of().formatHex(extra);
        AuthToken token = AuthToken.builder()
                .userId(userId)
                .purpose(purpose)
                .tokenHash(sha256(raw))
                .expiresAt(now.plusHours(hoursValid))
                .build();
        authTokenRepository.save(token);
        return raw;
    }

    private AuthToken requireValidToken(String rawToken, AuthTokenPurpose purpose, ResultCode invalidCode) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException(invalidCode);
        }
        AuthToken token = authTokenRepository.findByTokenHashAndPurpose(sha256(rawToken.trim()), purpose)
                .orElseThrow(() -> new BusinessException(invalidCode));
        if (token.getConsumedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(invalidCode);
        }
        return token;
    }

    static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private static String trimSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:5173";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        return AuthResponse.builder()
                .token(token)
                .user(toUserInfo(user))
                .build();
    }

    private AuthResponse.UserInfo toUserInfo(User user) {
        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .emailVerified(Boolean.TRUE.equals(user.getEmailVerified()))
                .build();
    }
}
