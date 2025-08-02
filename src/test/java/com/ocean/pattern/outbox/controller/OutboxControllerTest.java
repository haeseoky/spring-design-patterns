package com.ocean.pattern.outbox.controller;

import com.ocean.pattern.outbox.dto.OrderCreateRequest;
import com.ocean.pattern.outbox.entity.Order;
import com.ocean.pattern.outbox.entity.OutboxEvent;
import com.ocean.pattern.outbox.repository.OrderRepository;
import com.ocean.pattern.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class OutboxControllerTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    
    private HttpHeaders headers;
    
    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        orderRepository.deleteAll();
        
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }
    
    @Test
    @DisplayName("POST /api/outbox/orders - 주문 생성 API 테스트")
    void testCreateOrder() {
        // Given
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerName("홍길동");
        request.setCustomerEmail("hong@example.com");
        request.setProductName("스프링 부트 책");
        request.setQuantity(2);
        request.setPrice(BigDecimal.valueOf(25000));
        
        HttpEntity<OrderCreateRequest> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<Order> response = restTemplate.exchange(
                "/api/outbox/orders", HttpMethod.POST, entity, Order.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCustomerName()).isEqualTo("홍길동");
        assertThat(response.getBody().getCustomerEmail()).isEqualTo("hong@example.com");
        assertThat(response.getBody().getProductName()).isEqualTo("스프링 부트 책");
        assertThat(response.getBody().getQuantity()).isEqualTo(2);
        assertThat(response.getBody().getPrice()).isEqualTo(BigDecimal.valueOf(25000));
        assertThat(response.getBody().getTotalAmount()).isEqualTo(BigDecimal.valueOf(50000));
        assertThat(response.getBody().getStatus()).isEqualTo(Order.OrderStatus.PENDING);
        assertThat(response.getBody().getOrderNumber()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
    }
    
    @Test
    @DisplayName("GET /api/outbox/orders - 모든 주문 조회 API 테스트")
    void testGetAllOrders() {
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
        restTemplate.exchange("/api/outbox/orders", HttpMethod.POST, 
                new HttpEntity<>(request1, headers), Order.class);
        restTemplate.exchange("/api/outbox/orders", HttpMethod.POST, 
                new HttpEntity<>(request2, headers), Order.class);
        
        // When
        ResponseEntity<List<Order>> response = restTemplate.exchange(
                "/api/outbox/orders", HttpMethod.GET, null, 
                new ParameterizedTypeReference<List<Order>>() {});
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }
    
    @Test
    @DisplayName("PUT /api/outbox/orders/{orderId}/confirm - 주문 확인 API 테스트")
    void testConfirmOrder() {
        // Given
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerName("김철수");
        request.setCustomerEmail("kim@example.com");
        request.setProductName("자바 책");
        request.setQuantity(1);
        request.setPrice(BigDecimal.valueOf(30000));
        
        ResponseEntity<Order> createResponse = restTemplate.exchange(
                "/api/outbox/orders", HttpMethod.POST, 
                new HttpEntity<>(request, headers), Order.class);
        Long orderId = createResponse.getBody().getId();
        
        // When
        ResponseEntity<Order> response = restTemplate.exchange(
                "/api/outbox/orders/" + orderId + "/confirm", HttpMethod.PUT, 
                null, Order.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(orderId);
        assertThat(response.getBody().getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
    }
    
    @Test
    @DisplayName("GET /api/outbox/events - 아웃박스 이벤트 조회 API 테스트")
    void testGetAllOutboxEvents() {
        // Given
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerName("이영희");
        request.setCustomerEmail("lee@example.com");
        request.setProductName("파이썬 책");
        request.setQuantity(1);
        request.setPrice(BigDecimal.valueOf(28000));
        
        restTemplate.exchange("/api/outbox/orders", HttpMethod.POST, 
                new HttpEntity<>(request, headers), Order.class);
        
        // When
        ResponseEntity<List<OutboxEvent>> response = restTemplate.exchange(
                "/api/outbox/events", HttpMethod.GET, null, 
                new ParameterizedTypeReference<List<OutboxEvent>>() {});
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(response.getBody().get(0).getAggregateType()).isEqualTo("Order");
        assertThat(response.getBody().get(0).getProcessed()).isFalse();
    }
    
    @Test
    @DisplayName("GET /api/outbox/events/unprocessed - 미처리 이벤트 조회 API 테스트")
    void testGetUnprocessedEvents() {
        // Given
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerName("박민수");
        request.setCustomerEmail("park@example.com");
        request.setProductName("리액트 책");
        request.setQuantity(1);
        request.setPrice(BigDecimal.valueOf(35000));
        
        restTemplate.exchange("/api/outbox/orders", HttpMethod.POST, 
                new HttpEntity<>(request, headers), Order.class);
        
        // When
        ResponseEntity<List<OutboxEvent>> response = restTemplate.exchange(
                "/api/outbox/events/unprocessed", HttpMethod.GET, null, 
                new ParameterizedTypeReference<List<OutboxEvent>>() {});
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getProcessed()).isFalse();
    }
    
    @Test
    @DisplayName("GET /api/outbox/stats - 아웃박스 통계 API 테스트")
    void testGetOutboxStats() {
        // Given
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerName("통계테스트");
        request.setCustomerEmail("stats@example.com");
        request.setProductName("통계책");
        request.setQuantity(1);
        request.setPrice(BigDecimal.valueOf(20000));
        
        restTemplate.exchange("/api/outbox/orders", HttpMethod.POST, 
                new HttpEntity<>(request, headers), Order.class);
        
        // When
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/outbox/stats", HttpMethod.GET, null, 
                new ParameterizedTypeReference<Map<String, Object>>() {});
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("totalEvents")).isEqualTo(1);
        assertThat(response.getBody().get("unprocessedEvents")).isEqualTo(1);
        assertThat(response.getBody().get("processedEvents")).isEqualTo(0);
        assertThat(response.getBody().get("processedPercentage")).isEqualTo(0.0);
    }
}