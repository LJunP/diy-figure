package com.diyfigure.notification;

import com.diyfigure.common.enums.NotificationChannel;
import com.diyfigure.common.enums.OrderStatus;
import com.diyfigure.common.exception.BusinessException;
import com.diyfigure.common.response.ResultCode;
import com.diyfigure.entity.Notification;
import com.diyfigure.entity.User;
import com.diyfigure.repository.NotificationRepository;
import com.diyfigure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 通知服务
 *
 * 站内消息(IN_APP):覆盖订单全生命周期的关键节点,随状态机转移产生
 * 邮件(EMAIL):仅在报价确认、需付尾款、已发货三个节点触发(02 文档 3.x 节)
 *
 * 设计约束:通知是旁路能力,任何异常都不能阻断订单主流程,
 * 因此状态机里调用本服务时会捕获异常,这里自身也不向外抛业务异常。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /** 订单状态 → 站内消息文案 */
    private static final Map<OrderStatus, String> STATUS_MESSAGE = Map.ofEntries(
            Map.entry(OrderStatus.REVIEWING, "你的设计已提交,正在等待人工终审"),
            Map.entry(OrderStatus.REVIEW_REJECTED, "终审未通过,请查看拒绝理由并修改设计后重新提交"),
            Map.entry(OrderStatus.QUOTED, "设计已通过终审,运营已给出报价,请尽快确认"),
            Map.entry(OrderStatus.CLOSED, "你已拒绝报价,订单已关闭,可重新打开后再次申请"),
            Map.entry(OrderStatus.LOTTERY_PENDING, "已接受报价,请执行抽选决定生产角色"),
            Map.entry(OrderStatus.LOTTERY_DONE, "抽选完成,请填写收货地址"),
            Map.entry(OrderStatus.DEPOSIT_PENDING, "请支付定金以锁定设计并安排生产"),
            Map.entry(OrderStatus.IN_PRODUCTION, "定金已支付,设计已锁定,正在生产中"),
            Map.entry(OrderStatus.QC_PENDING, "生产完成,平台质检中"),
            Map.entry(OrderStatus.BALANCE_PENDING, "质检通过,请支付尾款,付款后安排发货"),
            Map.entry(OrderStatus.SHIPPING_PENDING, "尾款已支付,准备发货"),
            Map.entry(OrderStatus.SHIPPED, "订单已发货,请注意查收。未中签角色可在 60 天内补购"),
            Map.entry(OrderStatus.COMPLETED, "订单已完成,感谢你的定制"),
            Map.entry(OrderStatus.CANCELLED, "订单已取消")
    );

    /** 需要同时发送邮件的三个关键节点 */
    private static final Map<OrderStatus, String> EMAIL_SUBJECT = Map.of(
            OrderStatus.QUOTED, "【DIY Figure】报价已确认,请查看",
            OrderStatus.BALANCE_PENDING, "【DIY Figure】质检通过,请支付尾款",
            OrderStatus.SHIPPED, "【DIY Figure】你的定制盲盒已发货"
    );

    /**
     * 订单状态变化时产生通知(由状态机调用)
     */
    @Transactional
    public void notifyOrderStatusChanged(Long orderId, Long userId, OrderStatus toStatus) {
        String content = STATUS_MESSAGE.get(toStatus);
        if (content == null) {
            return;
        }
        saveInApp(userId, orderId, content);

        String subject = EMAIL_SUBJECT.get(toStatus);
        if (subject != null) {
            String email = userRepository.findById(userId)
                    .map(User::getEmail)
                    .orElse(null);
            emailService.sendAfterCommit(email, subject, content + "\n\n订单编号:" + orderId);
        }
    }

    /**
     * 写入一条站内消息
     */
    @Transactional
    public Notification saveInApp(Long userId, Long orderId, String content) {
        Notification notification = Notification.builder()
                .userId(userId)
                .orderId(orderId)
                .content(content)
                .channel(NotificationChannel.IN_APP)
                .build();
        return notificationRepository.save(notification);
    }

    /**
     * 我的站内消息
     */
    public List<Notification> listMyNotifications(Long userId) {
        return notificationRepository.findByUserIdAndChannelOrderByCreatedAtDesc(
                userId, NotificationChannel.IN_APP);
    }

    /**
     * 未读数量
     */
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndChannelAndReadAtIsNull(
                userId, NotificationChannel.IN_APP);
    }

    /**
     * 标记单条已读(校验归属,不能替别人点已读)
     */
    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "消息不存在"));
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此消息");
        }
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    /**
     * 全部标记已读,返回更新条数
     */
    @Transactional
    public int markAllRead(Long userId) {
        List<Notification> unread = notificationRepository
                .findByUserIdAndChannelOrderByCreatedAtDesc(userId, NotificationChannel.IN_APP)
                .stream()
                .filter(n -> n.getReadAt() == null)
                .toList();
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(n -> n.setReadAt(now));
        notificationRepository.saveAll(unread);
        return unread.size();
    }
}
