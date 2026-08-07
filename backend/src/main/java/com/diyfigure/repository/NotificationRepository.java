package com.diyfigure.repository;

import com.diyfigure.common.enums.NotificationChannel;
import com.diyfigure.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 通知 Repository
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 查询用户的站内消息(按时间倒序)
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 查询用户某渠道的通知
     */
    List<Notification> findByUserIdAndChannelOrderByCreatedAtDesc(Long userId, NotificationChannel channel);

    /**
     * 查询用户未读消息数(站内消息)
     */
    long countByUserIdAndChannelAndReadAtIsNull(Long userId, NotificationChannel channel);
}
