package com.diyfigure.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 邮件服务测试
 *
 * 关键不变量:邮件是旁路能力,无论"没配 SMTP"还是"SMTP 报错",
 * 都不能把异常抛给订单主流程。
 */
class EmailServiceTest {

    private EmailService serviceWith(ObjectProvider<JavaMailSender> provider, String mailHost) {
        EmailService service = new EmailService(provider);
        ReflectionTestUtils.setField(service, "host", mailHost);
        return service;
    }

    @Test
    @DisplayName("未配置 SMTP 时降级:不抛异常,且 isEnabled=false")
    void noSmtpConfigured_degradesGracefully() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> empty = mock(ObjectProvider.class);
        EmailService service = serviceWith(empty, "smtp.example.com");

        assertFalse(service.isEnabled());
        assertDoesNotThrow(() -> service.sendAfterCommit("a@b.com", "主题", "内容"));
    }

    @Test
    @DisplayName("spring.mail.host 为空串时也要降级,不能白连一次 SMTP")
    void blankHost_degradesWithoutConnecting() {
        JavaMailSender sender = mock(JavaMailSender.class);
        // 即使 Spring 仍然创建了 JavaMailSender bean(host 属性"存在"但为空),
        // 也不该真的发起发送
        EmailService service = serviceWith(new FixedObjectProvider(sender), "");

        assertFalse(service.isEnabled());
        assertDoesNotThrow(() -> service.sendAfterCommit("a@b.com", "主题", "内容"));
        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("收件人为空时直接跳过")
    void blankRecipient_skipped() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService service = serviceWith(new FixedObjectProvider(sender), "smtp.example.com");

        service.sendAfterCommit(null, "主题", "内容");
        service.sendAfterCommit("  ", "主题", "内容");

        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("SMTP 报错时吞掉异常,不阻断主流程")
    void smtpFailure_swallowed() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new MailSendException("smtp down"))
                .when(sender).send(any(SimpleMailMessage.class));
        EmailService service = serviceWith(new FixedObjectProvider(sender), "smtp.example.com");

        assertTrue(service.isEnabled());
        assertDoesNotThrow(() -> service.sendAfterCommit("a@b.com", "主题", "内容"));
        verify(sender).send(any(SimpleMailMessage.class));
    }

    /** 简单的固定值 ObjectProvider */
    private record FixedObjectProvider(JavaMailSender sender) implements ObjectProvider<JavaMailSender> {
        @Override
        public JavaMailSender getObject(Object... args) {
            return sender;
        }

        @Override
        public JavaMailSender getIfAvailable() {
            return sender;
        }

        @Override
        public JavaMailSender getIfUnique() {
            return sender;
        }

        @Override
        public JavaMailSender getObject() {
            return sender;
        }
    }
}
