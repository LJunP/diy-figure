package com.diyfigure.auth;

import com.diyfigure.common.enums.UserRole;
import com.diyfigure.entity.User;
import com.diyfigure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 受控的运营管理员初始化
 *
 * 背景:注册接口固定创建 USER 角色,仓库里没有管理员初始化入口,
 * 运营后台因此无法登录。直接放开注册接口授予管理员权限是危险的。
 *
 * 使用方式(默认关闭,必须显式开启):
 *   ADMIN_INIT_ENABLED=true \
 *   ADMIN_INIT_USERNAME=admin \
 *   ADMIN_INIT_PASSWORD=xxxx \
 *   ADMIN_INIT_EMAIL=admin@example.com \
 *   mvn spring-boot:run
 *
 * 安全约束:
 * 1. 只有 admin.init.enabled=true 时才执行
 * 2. 用户名或邮箱已存在时跳过,不覆盖既有账号
 * 3. 密码 BCrypt 加密存储,并打印一次性提示,要求首次登录后修改
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Value("${admin.init.enabled:false}")
    private boolean enabled;

    @Value("${admin.init.username:admin}")
    private String username;

    @Value("${admin.init.password:}")
    private String password;

    @Value("${admin.init.email:}")
    private String email;

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        if (password == null || password.isBlank()) {
            log.warn("已开启管理员初始化(admin.init.enabled=true),但未设置 ADMIN_INIT_PASSWORD,已跳过");
            return;
        }

        if (userRepository.existsByUsername(username)) {
            log.info("管理员账号已存在,跳过初始化: username={}", username);
            return;
        }

        String adminEmail = (email == null || email.isBlank())
                ? username + "@diyfigure.local"
                : email;
        if (userRepository.existsByEmail(adminEmail)) {
            log.warn("邮箱已被占用,跳过管理员初始化: email={}", adminEmail);
            return;
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User admin = User.builder()
                .username(username)
                .email(adminEmail)
                .passwordHash(encoder.encode(password))
                .role(UserRole.ADMIN)
                .emailVerified(true)
                .enabled(true)
                .build();
        userRepository.save(admin);

        log.warn("已初始化运营管理员: username={}。请尽快登录并在真实环境中轮换该密码", username);
    }
}
