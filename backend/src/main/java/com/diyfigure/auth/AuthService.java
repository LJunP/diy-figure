package com.diyfigure.auth;

import com.diyfigure.auth.dto.AuthResponse;
import com.diyfigure.auth.dto.LoginRequest;
import com.diyfigure.auth.dto.RegisterRequest;
import com.diyfigure.auth.dto.UpdatePasswordRequest;
import com.diyfigure.common.enums.UserRole;
import com.diyfigure.common.exception.BusinessException;
import com.diyfigure.common.response.ResultCode;
import com.diyfigure.entity.User;
import com.diyfigure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务
 * 负责用户注册、登录、信息查询、密码修改
 *
 * 密码使用 BCrypt 加密存储,不存明文
 * 登录成功后签发 JWT token
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户注册
     *
     * V1 采用邮箱+密码注册,暂不实现验证码
     * TODO: 后续接入邮件服务后,增加邮箱验证码验证流程
     *
     * @param request 注册请求(username, email, password)
     * @return 登录响应(含 token,注册成功后自动登录)
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 校验用户名唯一
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS, "用户名已被注册");
        }

        // 校验邮箱唯一
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS, "邮箱已被注册");
        }

        // 创建用户,密码 BCrypt 加密
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .build();

        user = userRepository.save(user);
        log.info("用户注册成功: id={}, username={}", user.getId(), user.getUsername());

        // 注册成功后自动登录,签发 token
        return buildAuthResponse(user);
    }

    /**
     * 用户登录
     * 支持用户名或邮箱登录
     *
     * @param request 登录请求(account, password)
     * @return 登录响应(含 token)
     */
    public AuthResponse login(LoginRequest request) {
        // 根据 account 查找用户(先按用户名查,再按邮箱查)
        User user = userRepository.findByUsername(request.getAccount())
                .or(() -> userRepository.findByEmail(request.getAccount()))
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND, "账号不存在"));

        // 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.PASSWORD_INCORRECT);
        }

        log.info("用户登录成功: id={}, username={}", user.getId(), user.getUsername());
        return buildAuthResponse(user);
    }

    /**
     * 获取当前登录用户信息
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    public AuthResponse.UserInfo getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
        return toUserInfo(user);
    }

    /**
     * 修改密码
     *
     * @param userId  用户 ID
     * @param request 修改密码请求(原密码, 新密码)
     */
    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));

        // 校验原密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.PASSWORD_INCORRECT, "原密码不正确");
        }

        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("用户修改密码成功: id={}", userId);
    }

    // ===== 私有方法 =====

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
                .build();
    }
}
