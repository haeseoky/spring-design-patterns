package com.ocean.pattern.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocean.pattern.observer.NewsAgency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 옵저버 패턴 컨트롤러 테스트
 */
@WebFluxTest(ObserverPatternController.class)
@DisplayName("옵저버 패턴 컨트롤러 테스트")
class ObserverPatternControllerTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private NewsAgency newsAgency;
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }
    
    @Test
    @DisplayName("뉴스 발행 API 테스트 - 성공")
    void testPublishNews_Success() throws Exception {
        // Given
        when(newsAgency.getObserverCount()).thenReturn(3);
        when(newsAgency.getObserverNames()).thenReturn(List.of("KBS", "SBS", "MBC"));
        
        Map<String, String> requestBody = Map.of("news", "테스트 뉴스입니다");
        
        // When & Then
        webTestClient.post()
                .uri("/api/observer/news")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("뉴스가 성공적으로 발행되었습니다.")
                .jsonPath("$.news").isEqualTo("테스트 뉴스입니다")
                .jsonPath("$.subscriberCount").isEqualTo(3)
                .jsonPath("$.subscribers").isArray();
        
        verify(newsAgency).publishNews("테스트 뉴스입니다");
    }
    
    @Test
    @DisplayName("뉴스 발행 API 테스트 - 빈 내용으로 실패")
    void testPublishNews_EmptyNews() throws Exception {
        // Given
        Map<String, String> requestBody = Map.of("news", "");
        
        // When & Then
        webTestClient.post()
                .uri("/api/observer/news")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("뉴스 내용이 필요합니다.");
        
        verify(newsAgency, never()).publishNews(anyString());
    }
    
    @Test
    @DisplayName("뉴스 발행 API 테스트 - 뉴스 필드 누락")
    void testPublishNews_MissingNewsField() throws Exception {
        // Given
        Map<String, String> requestBody = Map.of("title", "제목만 있음");
        
        // When & Then
        webTestClient.post()
                .uri("/api/observer/news")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("뉴스 내용이 필요합니다.");
    }
    
    @Test
    @DisplayName("뉴스 채널 구독 API 테스트 - 성공")
    void testAddNewsChannel_Success() throws Exception {
        // Given
        when(newsAgency.getObserverCount()).thenReturn(1);
        
        Map<String, String> requestBody = Map.of("channelName", "KBS 뉴스");
        
        // When & Then
        webTestClient.post()
                .uri("/api/observer/subscribe/channel")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("뉴스 채널이 성공적으로 구독되었습니다.")
                .jsonPath("$.channelName").isEqualTo("KBS 뉴스")
                .jsonPath("$.totalSubscribers").isEqualTo(1);
        
        verify(newsAgency).addObserver(any());
    }
    
    @Test
    @DisplayName("이메일 구독 API 테스트 - 성공")
    void testAddEmailSubscriber_Success() throws Exception {
        // Given
        when(newsAgency.getObserverCount()).thenReturn(1);
        
        Map<String, String> requestBody = Map.of(
                "name", "김철수",
                "email", "test@example.com"
        );
        
        // When & Then
        webTestClient.post()
                .uri("/api/observer/subscribe/email")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("이메일 구독자가 성공적으로 등록되었습니다.")
                .jsonPath("$.name").isEqualTo("김철수")
                .jsonPath("$.email").isEqualTo("test@example.com")
                .jsonPath("$.totalSubscribers").isEqualTo(1);
        
        verify(newsAgency).addObserver(any());
    }
    
    @Test
    @DisplayName("이메일 구독 API 테스트 - 필수 필드 누락")
    void testAddEmailSubscriber_MissingFields() throws Exception {
        // Given
        Map<String, String> requestBody = Map.of("name", "김철수");
        
        // When & Then
        webTestClient.post()
                .uri("/api/observer/subscribe/email")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("이름과 이메일이 모두 필요합니다.");
        
        verify(newsAgency, never()).addObserver(any());
    }
    
    @Test
    @DisplayName("모바일 앱 구독 API 테스트 - 성공")
    void testAddMobileApp_Success() throws Exception {
        // Given
        when(newsAgency.getObserverCount()).thenReturn(1);
        
        Map<String, String> requestBody = Map.of(
                "appName", "뉴스앱",
                "deviceId", "device123"
        );
        
        // When & Then
        webTestClient.post()
                .uri("/api/observer/subscribe/mobile")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("모바일 앱이 성공적으로 구독되었습니다.")
                .jsonPath("$.appName").isEqualTo("뉴스앱")
                .jsonPath("$.deviceId").isEqualTo("device123")
                .jsonPath("$.totalSubscribers").isEqualTo(1);
        
        verify(newsAgency).addObserver(any());
    }
    
    @Test
    @DisplayName("구독자 목록 조회 API 테스트")
    void testGetSubscribers() throws Exception {
        // Given
        when(newsAgency.getObserverCount()).thenReturn(2);
        when(newsAgency.getObserverNames()).thenReturn(List.of("KBS 뉴스", "test@example.com"));
        when(newsAgency.getLatestNews()).thenReturn("최신 뉴스");
        
        // When & Then
        webTestClient.get()
                .uri("/api/observer/subscribers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalCount").isEqualTo(2)
                .jsonPath("$.subscribers").isArray()
                .jsonPath("$.latestNews").isEqualTo("최신 뉴스");
    }
    
    @Test
    @DisplayName("데모 실행 API 테스트")
    void testRunDemo() throws Exception {
        // Given
        when(newsAgency.getObserverCount()).thenReturn(4);
        when(newsAgency.getObserverNames()).thenReturn(
                List.of("KBS 뉴스", "SBS 뉴스", "김철수 (user@example.com)", "뉴스앱 (Device: device123)")
        );
        
        // When & Then
        webTestClient.post()
                .uri("/api/observer/demo")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("데모가 성공적으로 실행되었습니다.")
                .jsonPath("$.subscribersAdded").isEqualTo(4)
                .jsonPath("$.newsPublished").isEqualTo(1)
                .jsonPath("$.subscribers").isArray();
        
        verify(newsAgency, times(4)).addObserver(any());
        verify(newsAgency).publishNews(anyString());
    }
    
    @Test
    @DisplayName("리셋 API 테스트")
    void testReset() throws Exception {
        // Given
        when(newsAgency.getObserverCount()).thenReturn(0);
        
        // When & Then
        webTestClient.delete()
                .uri("/api/observer/reset")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("모든 구독자가 제거되었습니다.")
                .jsonPath("$.totalSubscribers").isEqualTo(0);
    }
    
    @Test
    @DisplayName("구독 해지 API 테스트 - 존재하지 않는 ID")
    void testUnsubscribe_NotFound() throws Exception {
        // When & Then
        webTestClient.delete()
                .uri("/api/observer/unsubscribe/nonexistent")
                .exchange()
                .expectStatus().isNotFound();
        
        verify(newsAgency, never()).removeObserver(any());
    }
    
    @Test
    @DisplayName("구독자 상세 정보 조회 API 테스트 - 존재하지 않는 ID")
    void testGetSubscriberDetails_NotFound() throws Exception {
        // When & Then
        webTestClient.get()
                .uri("/api/observer/subscriber/nonexistent")
                .exchange()
                .expectStatus().isNotFound();
    }
}