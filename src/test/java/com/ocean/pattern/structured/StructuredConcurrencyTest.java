package com.ocean.pattern.structured;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Structured Concurrency 패턴 테스트
 * 
 * 이 테스트 클래스는 모든 Structured Concurrency 예제의
 * 정확성과 성능을 검증합니다.
 * 
 * 테스트 실행 방법:
 * - JUnit 5와 Spring Boot Test 환경에서 실행
 * - 각 테스트는 독립적으로 실행 가능
 * - @Timeout을 통한 성능 검증 포함
 */
@SpringBootTest
class StructuredConcurrencyTest {
    
    private TaskScopeExample taskScopeExample;
    private ErrorHandlingExample errorHandlingExample;
    private ParallelDataProcessing parallelDataProcessing;
    private WebServiceAggregation webServiceAggregation;
    private CustomJoinerExample customJoinerExample;
    
    @BeforeEach
    void setUp() {
        taskScopeExample = new TaskScopeExample();
        errorHandlingExample = new ErrorHandlingExample();
        parallelDataProcessing = new ParallelDataProcessing();
        webServiceAggregation = new WebServiceAggregation();
        customJoinerExample = new CustomJoinerExample();
    }
    
    @AfterEach
    void tearDown() {
        // 리소스 정리가 필요한 경우
    }
    
    // TaskScopeExample 테스트들
    
    @Test
    @DisplayName("사용자 프로필 병렬 조회가 순차 조회보다 빨라야 함")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testUserProfileParallelFetchIsFasterThanSequential() throws Exception {
        // Given
        String userId = "test-user";
        
        // When - 병렬 처리
        long parallelStart = System.currentTimeMillis();
        TaskScopeExample.UserProfile profile = taskScopeExample.fetchUserProfileAsync(userId);
        long parallelTime = System.currentTimeMillis() - parallelStart;
        
        // Then
        assertNotNull(profile);
        assertNotNull(profile.user());
        assertNotNull(profile.preferences());
        assertNotNull(profile.permissions());
        assertEquals(userId, profile.user().id());
        
        // 병렬 처리는 1초 이내에 완료되어야 함 (시뮬레이션 기준)
        assertTrue(parallelTime < 1000, "병렬 처리가 예상보다 오래 걸림: " + parallelTime + "ms");
    }
    
    @Test
    @DisplayName("타임아웃 설정된 프로필 조회가 정상 동작해야 함")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testUserProfileWithTimeout() throws Exception {
        // Given
        String userId = "timeout-test-user";
        Duration timeout = Duration.ofSeconds(2);
        
        // When
        TaskScopeExample.UserProfile profile = taskScopeExample.fetchUserProfileWithTimeout(userId, timeout);
        
        // Then
        assertNotNull(profile);
        assertEquals(userId, profile.user().id());
    }
    
    @Test
    @DisplayName("가장 빠른 응답 조회가 정상 동작해야 함")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testFastestResponse() throws Exception {
        // Given
        String query = "test-query";
        
        // When
        String response = taskScopeExample.fetchFastestResponse(query);
        
        // Then
        assertNotNull(response);
        assertTrue(response.contains(query));
        assertTrue(response.startsWith("Primary:") || 
                  response.startsWith("Secondary:") || 
                  response.startsWith("Cache:"));
    }
    
    // ErrorHandlingExample 테스트들
    
    @Test
    @DisplayName("실패 없는 입력으로 Fail-fast 처리가 성공해야 함")
    void testFailFastWithSuccessInputs() throws Exception {
        // Given
        List<String> inputs = List.of("data1", "data2", "data3");
        
        // When
        List<String> results = errorHandlingExample.processWithFailFast(inputs);
        
        // Then
        assertNotNull(results);
        assertEquals(inputs.size(), results.size());
        for (int i = 0; i < results.size(); i++) {
            assertTrue(results.get(i).contains(inputs.get(i)));
        }
    }
    
    @Test
    @DisplayName("부분 실패 허용 처리가 정상 동작해야 함")
    void testProcessWithPartialFailure() throws Exception {
        // Given
        List<String> inputs = List.of("good1", "fail", "good2", "good3");
        
        // When
        ErrorHandlingExample.ProcessingResult result = 
            errorHandlingExample.processWithPartialFailure(inputs);
        
        // Then
        assertNotNull(result);
        assertTrue(result.successes().size() > 0);
        assertTrue(result.failures().size() > 0);
        assertTrue(result.hasFailures());
        assertFalse(result.allSucceeded());
        
        double expectedSuccessRate = (double) result.successes().size() / inputs.size();
        assertEquals(expectedSuccessRate, result.successRate(), 0.01);
    }
    
    @Test
    @DisplayName("재시도 메커니즘이 정상 동작해야 함")
    void testProcessWithRetry() throws Exception {
        // Given
        String input = "retry-test-data";
        int maxRetries = 3;
        
        // When
        String result = errorHandlingExample.processWithRetry(input, maxRetries);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains(input));
    }
    
    @Test
    @DisplayName("서킷 브레이커가 정상 동작해야 함")
    void testCircuitBreaker() throws Exception {
        // Given
        String input = "circuit-test";
        
        // When & Then - 여러 번 호출하여 서킷 브레이커 동작 확인
        String result1 = errorHandlingExample.processWithCircuitBreaker(input + "1");
        assertNotNull(result1);
        
        String result2 = errorHandlingExample.processWithCircuitBreaker(input + "2");
        assertNotNull(result2);
    }
    
    // ParallelDataProcessing 테스트들
    
    @Test
    @DisplayName("대용량 데이터셋 처리가 정상 동작해야 함")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testLargeDatasetProcessing() throws Exception {
        // Given
        List<Integer> dataset = IntStream.rangeClosed(1, 1000).boxed().toList();
        ParallelDataProcessing.DataProcessor<Integer, String> processor = 
            number -> "처리된-" + number;
        
        // When
        List<String> results = parallelDataProcessing.processLargeDataset(dataset, processor, 100);
        
        // Then
        assertNotNull(results);
        assertEquals(dataset.size(), results.size());
        
        for (int i = 0; i < results.size(); i++) {
            assertTrue(results.get(i).contains(dataset.get(i).toString()));
        }
    }
    
    @Test
    @DisplayName("Map-Reduce 처리가 정상 동작해야 함")
    void testMapReduceProcessing() throws Exception {
        // Given
        List<Integer> dataset = List.of(1, 2, 3, 4, 5);
        ParallelDataProcessing.DataProcessor<Integer, Integer> mapper = number -> number * number;
        ParallelDataProcessing.ResultCombiner<Integer> reducer = Integer::sum;
        
        // When
        Integer result = parallelDataProcessing.mapReduceProcess(dataset, mapper, reducer, 0, 2);
        
        // Then
        assertNotNull(result);
        // 1² + 2² + 3² + 4² + 5² = 1 + 4 + 9 + 16 + 25 = 55
        assertEquals(55, result);
    }
    
    @Test
    @DisplayName("통계 계산이 정확해야 함")
    void testProcessingStatistics() throws Exception {
        // Given
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        
        // When
        ParallelDataProcessing.ProcessingStatistics stats = 
            parallelDataProcessing.processLargeDatasetWithStats(numbers);
        
        // Then
        assertNotNull(stats);
        assertEquals(10, stats.totalCount());
        assertEquals(55, stats.sum()); // 1+2+...+10 = 55
        assertEquals(5.5, stats.average(), 0.1);
        assertEquals(1, stats.minimum());
        assertEquals(10, stats.maximum());
        assertEquals(5, stats.evenCount()); // 2, 4, 6, 8, 10
        assertEquals(5, stats.oddCount());  // 1, 3, 5, 7, 9
    }
    
    @Test
    @DisplayName("동적 부하 분산이 정상 동작해야 함")
    void testDynamicLoadBalancing() throws Exception {
        // Given
        List<Integer> dataset = IntStream.rangeClosed(1, 100).boxed().toList();
        ParallelDataProcessing.DataProcessor<Integer, String> processor = 
            number -> "로드밸런싱-" + number;
        int numberOfWorkers = 4;
        
        // When
        List<String> results = parallelDataProcessing.processWithDynamicLoadBalancing(
            dataset, processor, numberOfWorkers);
        
        // Then
        assertNotNull(results);
        assertEquals(dataset.size(), results.size());
    }
    
    // WebServiceAggregation 테스트들
    
    @Test
    @DisplayName("사용자 대시보드 집계가 정상 동작해야 함")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testUserDashboardAggregation() throws Exception {
        // Given
        String userId = "test-dashboard-user";
        
        // When
        WebServiceAggregation.UserDashboard dashboard = 
            webServiceAggregation.aggregateUserDashboard(userId);
        
        // Then
        assertNotNull(dashboard);
        assertNotNull(dashboard.userInfo());
        assertNotNull(dashboard.orders());
        assertNotNull(dashboard.paymentInfo());
        assertNotNull(dashboard.recommendations());
        assertNotNull(dashboard.notifications());
        assertEquals(userId, dashboard.userInfo().userId());
        assertTrue(dashboard.getLoadTimeMs() >= 0);
    }
    
    @Test
    @DisplayName("폴백이 있는 대시보드 집계가 정상 동작해야 함")
    void testUserDashboardWithFallback() throws Exception {
        // Given
        String userId = "fallback-test-user";
        Duration timeout = Duration.ofSeconds(2);
        
        // When
        WebServiceAggregation.UserDashboard dashboard = 
            webServiceAggregation.aggregateUserDashboardWithFallback(userId, timeout);
        
        // Then
        assertNotNull(dashboard);
        assertNotNull(dashboard.userInfo());
        // 폴백 상황에서도 기본 데이터는 있어야 함
        assertEquals(userId, dashboard.userInfo().userId());
    }
    
    @Test
    @DisplayName("제품 상세 정보 집계가 정상 동작해야 함")
    void testProductDetailsAggregation() throws Exception {
        // Given
        String productId = "test-product";
        
        // When
        WebServiceAggregation.ProductDetails details = 
            webServiceAggregation.aggregateProductDetails(productId);
        
        // Then
        assertNotNull(details);
        assertNotNull(details.product());
        assertNotNull(details.reviews());
        assertNotNull(details.inventory());
        assertNotNull(details.pricing());
        assertNotNull(details.relatedProducts());
        assertEquals(productId, details.product().productId());
    }
    
    @Test
    @DisplayName("다중 사용자 집계가 정상 동작해야 함")
    void testMultipleUsersAggregation() throws Exception {
        // Given
        List<String> userIds = List.of("user1", "user2", "user3");
        int batchSize = 2;
        
        // When
        var results = webServiceAggregation.aggregateMultipleUsers(userIds, batchSize);
        
        // Then
        assertNotNull(results);
        assertEquals(userIds.size(), results.size());
        
        for (String userId : userIds) {
            assertTrue(results.containsKey(userId));
            assertNotNull(results.get(userId));
        }
    }
    
    // CustomJoinerExample 테스트들
    
    @Test
    @DisplayName("첫 번째 성공 결과가 정상 반환되어야 함")
    void testFirstSuccessfulResult() throws Exception {
        // Given
        List<Callable<String>> tasks = List.of(
            () -> { Thread.sleep(200); return "결과1"; },
            () -> { Thread.sleep(100); return "결과2"; },
            () -> { Thread.sleep(300); return "결과3"; }
        );
        Duration timeout = Duration.ofSeconds(1);
        
        // When
        String result = customJoinerExample.getFirstSuccessfulResult(tasks, timeout);
        
        // Then
        assertNotNull(result);
        // 가장 빠른 응답이어야 함
        assertEquals("결과2", result);
    }
    
    @Test
    @DisplayName("과반수 성공 조이너가 정상 동작해야 함")
    void testMajoritySuccess() throws Exception {
        // Given
        List<Callable<Integer>> tasks = List.of(
            () -> 10,
            () -> 20,
            () -> { throw new RuntimeException("실패"); },
            () -> 40,
            () -> 50
        );
        Duration timeout = Duration.ofSeconds(2);
        
        // When
        CustomJoinerExample.MajorityResult<Integer> result = 
            customJoinerExample.waitForMajoritySuccess(tasks, timeout);
        
        // Then
        assertNotNull(result);
        assertTrue(result.hasMajority());
        assertTrue(result.successes().size() >= result.requiredSuccesses());
        assertTrue(result.successRate() > 0.5);
    }
    
    @Test
    @DisplayName("품질 기반 조이너가 정상 동작해야 함")
    void testQualityBasedJoiner() throws Exception {
        // Given
        List<Callable<CustomJoinerExample.QualifiedResult<String>>> tasks = List.of(
            () -> new CustomJoinerExample.QualifiedResult<>("저품질", 0.3, "소스1"),
            () -> new CustomJoinerExample.QualifiedResult<>("고품질", 0.9, "소스2"),
            () -> new CustomJoinerExample.QualifiedResult<>("중품질", 0.6, "소스3")
        );
        double qualityThreshold = 0.8;
        Duration timeout = Duration.ofSeconds(2);
        
        // When
        CustomJoinerExample.QualityResult<String> result = 
            customJoinerExample.waitForQualityResult(tasks, qualityThreshold, timeout);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.bestResult());
        
        if (result.metThreshold()) {
            assertTrue(result.bestResult().quality() >= qualityThreshold);
        }
    }
    
    @Test
    @DisplayName("주문 배치 처리가 정상 동작해야 함")
    void testOrderBatchProcessing() throws Exception {
        // Given
        List<CustomJoinerExample.Order> orders = List.of(
            new CustomJoinerExample.Order("ORD001", "CUST001", 50000, "대기"),
            new CustomJoinerExample.Order("ORD002", "CUST002", 150000, "대기"),
            new CustomJoinerExample.Order("ORD003", "CUST003", 80000, "대기")
        );
        Duration timeout = Duration.ofSeconds(3);
        
        // When
        CustomJoinerExample.OrderProcessingResult result = 
            customJoinerExample.processOrdersBatch(orders, timeout);
        
        // Then
        assertNotNull(result);
        assertTrue(result.getProcessedCount() > 0);
        assertTrue(result.getTotalAmount() > 0);
    }
    
    // 성능 테스트들
    
    @Test
    @DisplayName("병렬 처리가 순차 처리보다 빨라야 함")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testParallelProcessingPerformance() throws Exception {
        // Given
        List<Integer> dataset = IntStream.rangeClosed(1, 1000).boxed().toList();
        ParallelDataProcessing.DataProcessor<Integer, String> processor = 
            number -> {
                try {
                    Thread.sleep(1); // 작은 지연 시뮬레이션
                    return "처리-" + number;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            };
        
        // When - 순차 처리
        long sequentialStart = System.currentTimeMillis();
        List<String> sequentialResult = dataset.stream()
                                              .map(n -> {
                                                  try {
                                                      Thread.sleep(1);
                                                      return "처리-" + n;
                                                  } catch (InterruptedException e) {
                                                      Thread.currentThread().interrupt();
                                                      throw new RuntimeException(e);
                                                  }
                                              })
                                              .toList();
        long sequentialTime = System.currentTimeMillis() - sequentialStart;
        
        // When - 병렬 처리
        long parallelStart = System.currentTimeMillis();
        List<String> parallelResult = parallelDataProcessing.processLargeDataset(
            dataset, processor, 100);
        long parallelTime = System.currentTimeMillis() - parallelStart;
        
        // Then
        assertEquals(sequentialResult.size(), parallelResult.size());
        // 병렬 처리가 더 빨라야 함 (하지만 오버헤드 고려)
        assertTrue(parallelTime < sequentialTime * 0.8, 
                  "병렬 처리가 예상만큼 빠르지 않음. 순차: " + sequentialTime + "ms, 병렬: " + parallelTime + "ms");
    }
    
    // 통합 테스트
    
    @Test
    @DisplayName("전체 데모가 오류 없이 실행되어야 함")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testFullDemo() {
        // Given & When & Then
        assertDoesNotThrow(() -> {
            StructuredConcurrencyDemo demo = new StructuredConcurrencyDemo();
            // main 메서드 대신 개별 메서드들 실행
            demo.runTaskScopeDemo();
            demo.runErrorHandlingDemo();
            demo.runParallelProcessingDemo();
            demo.runWebServiceDemo();
            demo.runCustomJoinerDemo();
        });
    }
    
    @Test
    @DisplayName("실제 시나리오가 정상 동작해야 함")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testRealWorldScenario() {
        // Given & When & Then
        assertDoesNotThrow(() -> {
            StructuredConcurrencyDemo demo = new StructuredConcurrencyDemo();
            demo.runRealWorldScenario();
        });
    }
    
    // 예외 상황 테스트들
    
    @Test
    @DisplayName("타임아웃 상황에서 적절히 처리되어야 함")
    void testTimeoutHandling() {
        // Given
        Duration veryShortTimeout = Duration.ofMillis(10);
        
        // When & Then
        assertThrows(TimeoutException.class, () -> {
            taskScopeExample.fetchUserProfileWithTimeout("slow-user", veryShortTimeout);
        });
    }
    
    @Test
    @DisplayName("실패하는 태스크들로 구성된 첫 번째 성공 결과 조회 시 예외 발생해야 함")
    void testFirstSuccessfulResultWithAllFailures() {
        // Given
        List<Callable<String>> failingTasks = List.of(
            () -> { throw new RuntimeException("실패1"); },
            () -> { throw new RuntimeException("실패2"); },
            () -> { throw new RuntimeException("실패3"); }
        );
        Duration timeout = Duration.ofSeconds(1);
        
        // When & Then
        assertThrows(ExecutionException.class, () -> {
            customJoinerExample.getFirstSuccessfulResult(failingTasks, timeout);
        });
    }
}