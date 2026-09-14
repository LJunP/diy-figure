package com.diyfigure.notification;

import com.diyfigure.common.enums.NotificationChannel;
import com.diyfigure.common.enums.OrderStatus;
import com.diyfigure.common.exception.BusinessException;
import com.diyfigure.common.response.ResultCode;
import com.diyfigure.entity.Notification;
import com.diyfigure.entity.User;
import com.diyfigure.repository.NotificationRepository;
import com.diyfigure.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 站内消息测试
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    private Notification unread(Long id, Long userId) {
        return Notification.builder()
                .id(id)
                .userId(userId)
                .orderId(1L)
                .content("测试消息")
                .channel(NotificationChannel.IN_APP)
                .build();
    }

    @Test
    @DisplayName("终审通过(QUOTED)会写站内消息,并触发邮件")
    void quoted_writesInAppAndEmail() {
        when(userRepository.findById(5L))
                .thenReturn(Optional.of(User.builder().id(5L).email("u@diy.com").build()));

        notificationService.notifyOrderStatusChanged(1L, 5L, OrderStatus.QUOTED);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(NotificationChannel.IN_APP, captor.getValue().getChannel());
        assertEquals(5L, captor.getValue().getUserId());
        verify(emailService).sendAfterCommit(any(), any(), any());
    }

    @Test
    @DisplayName("非关键节点只写站内消息,不发邮件")
    void nonKeyStatus_noEmail() {
        notificationService.notifyOrderStatusChanged(1L, 5L, OrderStatus.IN_PRODUCTION);

        verify(notificationRepository).save(any(Notification.class));
        verify(emailService, never()).sendAfterCommit(any(), any(), any());
    }

    @Test
    @DisplayName("没有文案的状态不产生消息(如待提交报价)")
    void unmappedStatus_noNotification() {
        notificationService.notifyOrderStatusChanged(1L, 5L, OrderStatus.DRAFT_SUBMIT_PENDING);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("不能把别人的消息标记为已读")
    void markReadOtherUsersNotification_forbidden() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(unread(1L, 5L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.markRead(1L, 999L));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("标记已读会写入 readAt")
    void markRead_setsReadAt() {
        Notification n = unread(1L, 5L);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        notificationService.markRead(1L, 5L);

        assertNotNull(n.getReadAt());
        verify(notificationRepository).save(n);
    }

    @Test
    @DisplayName("全部已读只处理未读项,已读的不重复更新")
    void markAllRead_onlyUnread() {
        Notification a = unread(1L, 5L);
        Notification b = Notification.builder()
                .id(2L).userId(5L).channel(NotificationChannel.IN_APP)
                .content("已读过的").readAt(LocalDateTime.now()).build();
        when(notificationRepository.findByUserIdAndChannelOrderByCreatedAtDesc(5L, NotificationChannel.IN_APP))
                .thenReturn(List.of(a, b));

        int updated = notificationService.markAllRead(5L);

        assertEquals(1, updated);
        assertNotNull(a.getReadAt());
    }

    @Test
    @DisplayName("未读数量走专用计数查询")
    void countUnread_usesCountQuery() {
        when(notificationRepository.countByUserIdAndChannelAndReadAtIsNull(5L, NotificationChannel.IN_APP))
                .thenReturn(3L);

        assertEquals(3L, notificationService.countUnread(5L));
        assertNull(null);
    }
}
