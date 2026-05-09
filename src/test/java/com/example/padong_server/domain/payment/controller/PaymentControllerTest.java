package com.example.padong_server.domain.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.padong_server.domain.oauth.entity.CustomUserDetails;
import com.example.padong_server.domain.oauth.entity.Role;
import com.example.padong_server.domain.oauth.entity.User;
import com.example.padong_server.domain.payment.dto.PaymentPrepareResponse;
import com.example.padong_server.domain.payment.dto.PaymentResponse;
import com.example.padong_server.domain.payment.entity.PaymentStatus;
import com.example.padong_server.domain.payment.service.PaymentService;
import com.example.padong_server.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("결제 준비 API는 준비 응답을 반환한다")
    void prepare_returnsPaymentPrepareResponse() throws Exception {
        User user = authenticatedUser(1L);
        PaymentPrepareResponse response = new PaymentPrepareResponse(
                31L,
                "payment-123",
                11000L,
                "제육덮밥",
                "결제테스터",
                true
        );
        given(paymentService.prepare(same(user), any())).willReturn(response);

        mockMvc.perform(post("/api/payments/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "groupOrderId": 7,
                                  "orderMenus": [
                                    { "menuId": 11, "quantity": 2 }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(31))
                .andExpect(jsonPath("$.data.paymentId").value("payment-123"))
                .andExpect(jsonPath("$.data.amount").value(11000))
                .andExpect(jsonPath("$.data.customerName").value("결제테스터"))
                .andExpect(jsonPath("$.data.isTest").value(true));

        verify(paymentService).prepare(same(user), any());
    }

    @Test
    @DisplayName("결제 승인 API는 결제 응답을 반환한다")
    void confirm_returnsPaymentResponse() throws Exception {
        User user = authenticatedUser(1L);
        PaymentResponse response = new PaymentResponse(
                "payment-123",
                31L,
                PaymentStatus.PAID,
                11000L,
                true,
                "pg-123",
                null,
                null,
                null
        );
        given(paymentService.confirm(same(user), any())).willReturn(response);

        mockMvc.perform(post("/api/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentId": "payment-123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentId").value("payment-123"))
                .andExpect(jsonPath("$.data.orderId").value(31))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.totalAmount").value(11000));

        verify(paymentService).confirm(same(user), any());
    }

    @Test
    @DisplayName("결제 조회 API는 결제 응답을 반환한다")
    void getPayment_returnsPaymentResponse() throws Exception {
        User user = authenticatedUser(1L);
        PaymentResponse response = new PaymentResponse(
                "payment-123",
                31L,
                PaymentStatus.READY,
                11000L,
                true,
                null,
                null,
                null,
                null
        );
        given(paymentService.getPayment(same(user), eq("payment-123"))).willReturn(response);

        mockMvc.perform(get("/api/payments/payment-123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentId").value("payment-123"))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.totalAmount").value(11000));

        verify(paymentService).getPayment(same(user), eq("payment-123"));
    }

    private User authenticatedUser(Long id) {
        User user = User.builder()
                .kakaoId(1000L + id)
                .email("tester" + id + "@example.com")
                .nickname("결제테스터")
                .role(Role.USER)
                .registered(true)
                .approved(true)
                .build();
        ReflectionTestUtils.setField(user, "id", id);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return user;
    }
}
