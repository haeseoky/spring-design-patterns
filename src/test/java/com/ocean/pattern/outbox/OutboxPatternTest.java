package com.ocean.pattern.outbox;

import com.ocean.pattern.outbox.dto.OrderCreateRequest;
import com.ocean.pattern.outbox.entity.Order;
import com.ocean.pattern.outbox.entity.OutboxEvent;
import com.ocean.pattern.outbox.repository.OrderRepository;
import com.ocean.pattern.outbox.repository.OutboxEventRepository;
import com.ocean.pattern.outbox.service.OrderService;
import com.ocean.pattern.outbox.service.OutboxPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OutboxPatternTest {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private OutboxPublisher outboxPublisher;
    
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
    @DisplayName("주문 생성 시 아웃박스 이벤트가 함께 생성된다")
    void testOrderCreationWithOutboxEvent() {
        // Given
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerName("홍길동");
        request.setCustomerEmail("hong@example.com");
        request.setProductName("스프링 부트 책");
        request.setQuantity(2);
        request.setPrice(BigDecimal.valueOf(25000));
        
        // When
        Order createdOrder = orderService.createOrder(request);
        
        // Then
        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.getOrderNumber()).isNotBlank();
        assertThat(createdOrder.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
        assertThat(createdOrder.getTotalAmount()).isEqualTo(BigDecimal.valueOf(50000));
        
        // 아웃박스 이벤트 검증
        List<OutboxEvent> events = outboxEventRepository.findByAggregateIdAndAggregateType(
                createdOrder.getId().toString(), "Order");
        
        assertThat(events).hasSize(1);
        OutboxEvent event = events.get(0);
        assertThat(event.getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(event.getProcessed()).isFalse();
        assertThat(event.getEventData()).contains("홍길동");
    }
    
    @Test
    @DisplayName("주문 확인 시 아웃박스 이벤트가 생성된다")
    void testOrderConfirmationWithOutboxEvent() {
        // Given
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerName("김철수");
        request.setCustomerEmail("kim@example.com");
        request.setProductName("자바 책");
        request.setQuantity(1);
        request.setPrice(BigDecimal.valueOf(30000));
        
        Order order = orderService.createOrder(request);
        
        // When
        Order confirmedOrder = orderService.confirmOrder(order.getId());
        
        // Then
        assertThat(confirmedOrder.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
        
        // 아웃박스 이벤트 검증 (ORDER_CREATED + ORDER_CONFIRMED)
        List<OutboxEvent> events = outboxEventRepository.findByAggregateIdAndAggregateType(
                order.getId().toString(), "Order");
        
        assertThat(events).hasSize(2);
        assertThat(events.stream().map(OutboxEvent::getEventType))
                .containsExactlyInAnyOrder("ORDER_CREATED", "ORDER_CONFIRMED");
    }
    
    @Test
    @DisplayName("주문 취소 시 아웃박스 이벤트가 생성된다")
    void testOrderCancellationWithOutboxEvent() {
        // Given
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerName("이영희");
        request.setCustomerEmail("lee@example.com");
        request.setProductName("파이썬 책");
        request.setQuantity(1);
        request.setPrice(BigDecimal.valueOf(28000));
        
        Order order = orderService.createOrder(request);
        
        // When
        Order cancelledOrder = orderService.cancelOrder(order.getId());
        
        // Then
        assertThat(cancelledOrder.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        
        // 아웃박스 이벤트 검증
        List<OutboxEvent> events = outboxEventRepository.findByAggregateIdAndAggregateType(
                order.getId().toString(), "Order");
        
        assertThat(events).hasSize(2);
        assertThat(events.stream().map(OutboxEvent::getEventType))
                .containsExactlyInAnyOrder("ORDER_CREATED", "ORDER_CANCELLED");
    }
    
    @Test
    @DisplayName("아웃박스 이벤트 처리 시 processed 상태가 변경된다")
    void testOutboxEventProcessing() {
        // Given
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerName("박민수");
        request.setCustomerEmail("park@example.com");
        request.setProductName("리액트 책");
        request.setQuantity(1);
        request.setPrice(BigDecimal.valueOf(35000));
        
        Order order = orderService.createOrder(request);
        
        List<OutboxEvent> unprocessedEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();
        assertThat(unprocessedEvents).hasSize(1);
        
        OutboxEvent event = unprocessedEvents.get(0);
        
        // When
        outboxPublisher.processEvent(event);
        
        // Then
        OutboxEvent processedEvent = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(processedEvent.getProcessed()).isTrue();
        assertThat(processedEvent.getProcessedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("미처리 이벤트 조회가 정확히 동작한다")
    void testUnprocessedEventQuery() {
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
        request2.setQuantity(1);
        request2.setPrice(BigDecimal.valueOf(25000));
        
        Order order1 = orderService.createOrder(request1);
        Order order2 = orderService.createOrder(request2);
        
        // When
        List<OutboxEvent> unprocessedEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();
        
        // Then
        assertThat(unprocessedEvents).hasSize(2);
        assertThat(unprocessedEvents.stream().allMatch(event -> !event.getProcessed())).isTrue();
        
        // 첫 번째 이벤트 처리
        outboxPublisher.processEvent(unprocessedEvents.get(0));
        
        // 미처리 이벤트가 1개로 줄어드는지 확인
        List<OutboxEvent> remainingEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();
        assertThat(remainingEvents).hasSize(1);
    }
    
    @Test
    @DisplayName("아웃박스 패턴의 트랜잭션 보장을 확인한다")
    void testTransactionalConsistency() {
        // Given
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerName("테스트사용자");
        request.setCustomerEmail("test@example.com");
        request.setProductName("테스트상품");
        request.setQuantity(1);
        request.setPrice(BigDecimal.valueOf(10000));
        
        // When
        Order order = orderService.createOrder(request);
        
        // Then - 같은 트랜잭션에서 주문과 아웃박스 이벤트가 모두 생성되어야 함
        assertThat(orderRepository.findById(order.getId())).isPresent();
        
        List<OutboxEvent> events = outboxEventRepository.findByAggregateIdAndAggregateType(
                order.getId().toString(), "Order");
        assertThat(events).hasSize(1);
        
        // 이벤트 데이터에 주문 정보가 포함되어 있는지 확인
        OutboxEvent event = events.get(0);
        assertThat(event.getEventData()).contains("테스트사용자");
        assertThat(event.getEventData()).contains("테스트상품");
        assertThat(event.getEventData()).contains("10000");
    }
}