package com.diyfigure.integration;

import com.diyfigure.common.enums.*;
import com.diyfigure.common.util.HmacSigner;
import com.diyfigure.entity.*;
import com.diyfigure.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("支付回调白名单与验签")
class PaymentCallbackIntegrationTest {

    private static final String WECHAT_KEY = "test-wechat-callback-key";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;
    @Autowired private org.flywaydb.core.Flyway flyway;
    @Autowired private UserRepository userRepository;
    @Autowired private SeriesRepository seriesRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentRepository paymentRepository;

    private Payment pending;

    @BeforeEach
    void seed() {
        flyway.clean();
        flyway.migrate();
        String s = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .username("p" + s).email("p" + s + "@t.l")
                .passwordHash("x").role(UserRole.USER).build());
        Series series = seriesRepository.save(Series.builder()
                .userId(user.getId()).name("支付回调系列")
                .specTier(SpecTier.LIGHT).sizeTier("STANDARD")
                .designStatus(DesignStatus.READY_FOR_QUOTE).build());
        OrderEntity order = orderRepository.save(OrderEntity.builder()
                .seriesId(series.getId()).userId(user.getId())
                .orderType(OrderType.MAIN).status(OrderStatus.DEPOSIT_PENDING)
                .quotedPrice(new BigDecimal("1000.00"))
                .depositAmount(new BigDecimal("500.00"))
                .balanceAmount(new BigDecimal("500.00"))
                .build());
        pending = paymentRepository.save(Payment.builder()
                .orderId(order.getId()).type(PaymentType.DEPOSIT)
                .channel(PaymentChannel.WECHAT)
                .amount(new BigDecimal("500.00"))
                .status(PaymentStatus.PENDING)
                .build());
    }

    @Test
    @DisplayName("未签名回调失败关闭,不 ACK success,也不改订单")
    void unsignedCallbackRejected() throws Exception {
        mockMvc.perform(post("/payments/callback/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"out_trade_no\":\"" + pending.getId() + "\",\"amount\":\"500.00\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("FAIL")));

        assertEquals(PaymentStatus.PENDING,
                paymentRepository.findById(pending.getId()).orElseThrow().getStatus());
        assertEquals(OrderStatus.DEPOSIT_PENDING,
                orderRepository.findById(pending.getOrderId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("签名正确且金额一致时入账,重复回调幂等")
    void signedCallbackPaysOnce() throws Exception {
        String body = signedBody(pending.getId(), "500.00", "wx-txn-1");
        mockMvc.perform(post("/payments/callback/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SUCCESS")));

        assertEquals(PaymentStatus.SUCCESS,
                paymentRepository.findById(pending.getId()).orElseThrow().getStatus());
        assertEquals(OrderStatus.IN_PRODUCTION,
                orderRepository.findById(pending.getOrderId()).orElseThrow().getStatus());

        mockMvc.perform(post("/payments/callback/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SUCCESS")));
    }

    @Test
    @DisplayName("金额不一致拒绝入账")
    void wrongAmountRejected() throws Exception {
        mockMvc.perform(post("/payments/callback/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signedBody(pending.getId(), "1.00", "wx-bad")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("FAIL")));
        assertEquals(PaymentStatus.PENDING,
                paymentRepository.findById(pending.getId()).orElseThrow().getStatus());
    }

    private String signedBody(Long paymentId, String amount, String txn) throws Exception {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("amount", amount);
        fields.put("out_trade_no", String.valueOf(paymentId));
        fields.put("transaction_id", txn);
        fields.put("sign", HmacSigner.sign(WECHAT_KEY, HmacSigner.canonicalQuery(fields)));
        return json.writeValueAsString(fields);
    }
}
