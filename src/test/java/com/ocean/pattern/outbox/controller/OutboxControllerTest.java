package com.ocean.pattern.outbox.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocean.pattern.outbox.dto.OrderCreateRequest;
import com.ocean.pattern.outbox.entity.Order;
import com.ocean.pattern.outbox.repository.OrderRepository;
import com.ocean.pattern.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Transactional
public class OutboxControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    
    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        orderRepository.deleteAll();
    }
    
    @Test
    @DisplayName("POST /api/outbox/orders - 주문 생성 API 테스트")
    void testCreateOrder() throws Exception {
        // Given
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerName("홍길동");
        request.setCustomerEmail("hong@example.com");
        request.setProductName("스프링 부트 책");
        request.setQuantity(2);
        request.setPrice(BigDecimal.valueOf(25000));
        
        // When & Then
        mockMvc.perform(post("/api/outbox/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("홍길동"))
                .andExpect(jsonPath("$.customerEmail").value("hong@example.com"))
                .andExpect(jsonPath("$.productName").value("스프링 부트 책"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.price").value(25000))
                .andExpect(jsonPath("$.totalAmount").value(50000))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.orderNumber").exists())
                .andExpect(jsonPath("$.id").exists());
    }
    
    @Test
    @DisplayName("GET /api/outbox/orders - 모든 주문 조회 API 테스트")
    void testGetAllOrders() throws Exception {
        // Given
        OrderCreateRequest request1 = new OrderCreateRequest();
        request1.setCustomerName("사용자1");
        request1.setCustomerEmail("user1@example.com");
        request1.setProductName("책1");
        request1.setQuantity(1);
        request1.setPrice(BigDecimal.valueOf(20000));
        
        OrderCreateRequest request2 = new OrderCreateRequest();
        request2.setCustomerName("사용자2");
        request2.setCustomerEmail("user2@example.com");
        request2.setProductName("책2");
        request2.setQuantity(2);
        request2.setPrice(BigDecimal.valueOf(15000));
        
        // 주문 생성
        mockMvc.perform(post("/api/outbox/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)));
        
        mockMvc.perform(post("/api/outbox/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)));
        
        // When & Then
        mockMvc.perform(get("/api/outbox/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].customerName", anyOf(is("사용자1"), is("사용자2"))))
                .andExpect(jsonPath("$[1].customerName", anyOf(is("사용자1"), is("사용자2"))));
    }
    
    @Test
    @DisplayName("PUT /api/outbox/orders/{orderId}/confirm - 주문 확인 API 테스트")
    void testConfirmOrder() throws Exception {
        // Given
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerName("김철수");
        request.setCustomerEmail("kim@example.com");
        request.setProductName("자바 책");
        request.setQuantity(1);
        request.setPrice(BigDecimal.valueOf(30000));
        
        String response = mockMvc.perform(post("/api/outbox/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        Order createdOrder = objectMapper.readValue(response, Order.class);
        
        // When & Then
        mockMvc.perform(put("/api/outbox/orders/{orderId}/confirm", createdOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdOrder.getId()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
    
    @Test
    @DisplayName("GET /api/outbox/events - 아웃박스 이벤트 조회 API 테스트")
    void testGetAllOutboxEvents() throws Exception {
        // Given
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerName("이영희");
        request.setCustomerEmail("lee@example.com");
        request.setProductName("파이썬 책");
        request.setQuantity(1);
        request.setPrice(BigDecimal.valueOf(28000));
        
        mockMvc.perform(post("/api/outbox/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
        
        // When & Then
        mockMvc.perform(get("/api/outbox/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventType").value("ORDER_CREATED"))
                .andExpect(jsonPath("$[0].aggregateType").value("Order"))
                .andExpect(jsonPath("$[0].processed").value(false));
    }
    
    @Test
    @DisplayName("GET /api/outbox/events/unprocessed - 미처리 이벤트 조회 API 테스트")
    void testGetUnprocessedEvents() throws Exception {
        // Given
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerName("박민수");
        request.setCustomerEmail("park@example.com");
        request.setProductName("리액트 책");
        request.setQuantity(1);
        request.setPrice(BigDecimal.valueOf(35000));
        
        mockMvc.perform(post("/api/outbox/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
        
        // When & Then
        mockMvc.perform(get("/api/outbox/events/unprocessed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].processed").value(false));
    }
    
    @Test
    @DisplayName("GET /api/outbox/stats - 아웃박스 통계 API 테스트")
    void testGetOutboxStats() throws Exception {
        // Given
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerName("통계테스트");
        request.setCustomerEmail("stats@example.com");
        request.setProductName("통계책");
        request.setQuantity(1);
        request.setPrice(BigDecimal.valueOf(20000));
        
        mockMvc.perform(post("/api/outbox/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
        
        // When & Then
        mockMvc.perform(get("/api/outbox/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(1))
                .andExpect(jsonPath("$.unprocessedEvents").value(1))
                .andExpect(jsonPath("$.processedEvents").value(0))
                .andExpect(jsonPath("$.processedPercentage").value(0.0));
    }
}