package com.diyfigure.entity;

import com.diyfigure.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 用户表(对应 03-技术设计说明书.md 第 5.1 节)
 *
 * V1 运营账号也存在此表,用 role 字段区分(USER / ADMIN),不单独建管理员表
 * 登录凭证预留 username / phone / email 三个字段,具体注册方式待定
 */
@Entity
@Table(name = "user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户名(登录凭证之一) */
    @Column(unique = true)
    private String username;

    /** 手机号(登录凭证之一) */
    @Column(unique = true)
    private String phone;

    /** 邮箱(登录凭证之一) */
    @Column(unique = true)
    private String email;

    /** 密码哈希(BCrypt) */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** 角色:USER / ADMIN */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
