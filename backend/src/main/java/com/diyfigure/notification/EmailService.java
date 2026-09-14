package com.diyfigure.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 邮件通知服务
 *
 * 只在三个关键节点发送(02 文档):报价确认、需付尾款、已发货。
 *
 * 降级策略:没有配置 SMTP(spring.mail.host 为空)时只记日志,不抛异常、不阻塞业务流程。
 *
 * ★ 注意:不能只依赖「JavaMailSender bean 存不存在」来判断是否配置。
 *   application.yml 里 spring.mail.host 写成 ${SPRING_MAIL_HOST:},未配时解析成空字符串,
 *   而 Spring Boot 的 MailSenderCondition 只判断属性「是否存在」,空字符串也算存在,
 *   于是 bean 照常创建、每次通知都会白连一次 SMTP 再失败。必须自己再判一次 host。
 *
 * 发送时机:注册为事务提交后的回调,避免"事务回滚了但邮件已经发出去"。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.host:}")
    private String host;

    @Value("${spring.mail.from:no-reply@diyfigure.local}")
    private String from;

    /**
     * 在事务提交后发送邮件;当前没有事务时立即发送
     */
    public void sendAfterCommit(String to, String subject, String content) {
        if (to == null || to.isBlank()) {
            log.info("收件人为空,跳过邮件: subject={}", subject);
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doSend(to, subject, content);
                }
            });
        } else {
            doSend(to, subject, content);
        }
    }

    private void doSend(String to, String subject, String content) {
        if (!smtpConfigured()) {
            log.info("邮件服务未配置(缺少 spring.mail.host),已降级为站内消息。收件人={}, 主题={}", to, subject);
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.info("邮件服务不可用(无 JavaMailSender),已降级为站内消息。收件人={}, 主题={}", to, subject);
            return;
        }
        try {
            org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("邮件已发送: to={}, subject={}", to, subject);
        } catch (Exception e) {
            // 邮件失败不能影响主流程,站内消息已经落库
            log.error("邮件发送失败: to={}, subject={}, error={}", to, subject, e.getMessage());
        }
    }

    /**
     * 邮件服务是否可用(供运维排查)
     */
    public boolean isEnabled() {
        return smtpConfigured() && mailSenderProvider.getIfAvailable() != null;
    }

    private boolean smtpConfigured() {
        return host != null && !host.isBlank();
    }
}
