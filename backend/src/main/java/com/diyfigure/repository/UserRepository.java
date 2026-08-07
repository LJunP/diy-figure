package com.diyfigure.repository;

import com.diyfigure.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户 Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户(登录时使用)
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户(邮箱注册/登录时使用)
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据手机号查找用户(手机号注册/登录时使用)
     */
    Optional<User> findByPhone(String phone);

    /**
     * 检查用户名是否已存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否已存在
     */
    boolean existsByEmail(String email);

    /**
     * 检查手机号是否已存在
     */
    boolean existsByPhone(String phone);
}
