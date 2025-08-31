package com.ocean.pattern.structured;

import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

/**
 * Structured Concurrency 통합 데모
 * 
 * 이 클래스는 모든 Structured Concurrency 예제를 통합하여
 * 실행할 수 있는 종합 데모를 제공합니다.
 * 
 * 실행 방법:
 * 1. JDK 21 이상에서 실행 (가상 스레드 지원)
 * 2. JDK 25에서는 실제 Structured Concurrency API 사용 가능
 * 3. --enable-preview 플래그와 함께 실행
 * 
 * 학습 목표:
 * 1. 모든 패턴의 통합 이해
 * 2. 성능 특성 비교
 * 3. 실제 사용 사례 적용
 */
public class StructuredConcurrencyDemo {
    
    private static final Logger logger = Logger.getLogger(StructuredConcurrencyDemo.class.getName());
    
    public static void main(String[] args) {
        logger.info("Structured Concurrency 통합 데모 시작");
        
        StructuredConcurrencyDemo demo = new StructuredConcurrencyDemo();
        
        // 각 예제를 순차적으로 실행
        demo.runTaskScopeDemo();
        demo.runErrorHandlingDemo();
        demo.runParallelProcessingDemo();
        demo.runWebServiceDemo();
        demo.runCustomJoinerDemo();
        
        logger.info("모든 데모 완료");
    }
    
    public void runTaskScopeDemo() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("1. 기본 Task Scope 데모");
        System.out.println("=".repeat(60));
        
        try {
            TaskScopeExample example = new TaskScopeExample();
            
            // 기본 병렬 처리
            TaskScopeExample.UserProfile profile = example.fetchUserProfileAsync("demo-user");
            System.out.println("✓ 사용자 프로필: " + profile.user().name());
            
            // 타임아웃 처리
            TaskScopeExample.UserProfile timeoutProfile = example.fetchUserProfileWithTimeout(
                "timeout-user", Duration.ofSeconds(1)
            );
            System.out.println("✓ 타임아웃 프로필 로드 완료");
            
            // 빠른 응답
            String fastResponse = example.fetchFastestResponse("demo-query");
            System.out.println("✓ 빠른 응답: " + fastResponse);
            
        } catch (Exception e) {
            System.err.println("✗ Task Scope 데모 실패: " + e.getMessage());
        }
    }
    
    public void runErrorHandlingDemo() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("2. 에러 처리 데모");
        System.out.println("=".repeat(60));
        
        try {
            ErrorHandlingExample example = new ErrorHandlingExample();
            
            // 성공 케이스
            List<String> successInputs = List.of("data1", "data2", "data3");
            List<String> successResults = example.processWithFailFast(successInputs);
            System.out.println("✓ Fail-fast 성공: " + successResults.size() + "개 처리");
            
            // 부분 실패 허용
            List<String> partialInputs = List.of("good1", "fail", "good2");
            ErrorHandlingExample.ProcessingResult partialResult = 
                example.processWithPartialFailure(partialInputs);
            
            System.out.println("✓ 부분 실패 허용 - 성공: " + partialResult.successes().size() + 
                             ", 실패: " + partialResult.failures().size());
            System.out.println("  성공률: " + String.format("%.1f%%", partialResult.successRate() * 100));
            
            // 재시도 메커니즘
            String retryResult = example.processWithRetry("retry-data", 2);
            System.out.println("✓ 재시도 성공: " + retryResult);
            
        } catch (Exception e) {
            System.err.println("✗ 에러 처리 데모 실패: " + e.getMessage());
        }
    }
    
    public void runParallelProcessingDemo() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("3. 병렬 데이터 처리 데모");
        System.out.println("=".repeat(60));
        
        try {
            ParallelDataProcessing processor = new ParallelDataProcessing();
            
            // 대용량 데이터 생성
            List<Integer> testData = generateTestData(1000);
            
            // 단순 변환 처리
            List<String> transformed = processor.processLargeDataset(
                testData,
                number -> "처리-" + number,
                100
            );
            System.out.println("✓ 대용량 변환: " + transformed.size() + "개 처리");
            
            // Map-Reduce 처리
            Integer sum = processor.mapReduceProcess(
                testData.subList(0, 100),
                number -> number * number,
                Integer::sum,
                0,
                4
            );
            System.out.println("✓ Map-Reduce 제곱합: " + sum);
            
            // 통계 계산
            ParallelDataProcessing.ProcessingStatistics stats = 
                processor.processLargeDatasetWithStats(testData.subList(0, 500));
            
            System.out.println("✓ 통계 계산:");
            System.out.println("  - 개수: " + stats.totalCount());
            System.out.println("  - 평균: " + String.format("%.1f", stats.average()));
            System.out.println("  - 합계: " + stats.sum());
            
        } catch (Exception e) {
            System.err.println("✗ 병렬 처리 데모 실패: " + e.getMessage());
        }
    }
    
    public void runWebServiceDemo() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("4. 웹서비스 집계 데모");
        System.out.println("=".repeat(60));
        
        try {
            WebServiceAggregation aggregator = new WebServiceAggregation();
            
            // 기본 대시보드 집계
            WebServiceAggregation.UserDashboard dashboard = 
                aggregator.aggregateUserDashboard("demo-user");
            
            System.out.println("✓ 대시보드 집계 완료:");
            System.out.println("  - 사용자: " + dashboard.userInfo().name());
            System.out.println("  - 주문 개수: " + dashboard.orders().size());
            System.out.println("  - 추천 개수: " + dashboard.recommendations().size());
            System.out.println("  - 로드 시간: " + dashboard.getLoadTimeMs() + "ms");
            
            // 폴백이 있는 집계
            WebServiceAggregation.UserDashboard fallbackDashboard = 
                aggregator.aggregateUserDashboardWithFallback("fallback-user", Duration.ofSeconds(1));
            System.out.println("✓ 폴백 대시보드 집계 완료");
            
            // 제품 상세 정보
            WebServiceAggregation.ProductDetails product = 
                aggregator.aggregateProductDetails("DEMO-PRODUCT");
            
            System.out.println("✓ 제품 상세 정보:");
            System.out.println("  - 제품명: " + product.product().name());
            System.out.println("  - 리뷰 수: " + product.reviews().size());
            System.out.println("  - 재고: " + product.inventory().stock());
            
            // 다중 사용자 집계
            List<String> userIds = List.of("user1", "user2", "user3");
            var multiResults = aggregator.aggregateMultipleUsers(userIds, 2);
            System.out.println("✓ 다중 사용자 집계: " + multiResults.size() + "명 완료");
            
        } catch (Exception e) {
            System.err.println("✗ 웹서비스 집계 데모 실패: " + e.getMessage());
        }
    }
    
    public void runCustomJoinerDemo() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("5. 커스텀 조이너 데모");
        System.out.println("=".repeat(60));
        
        try {
            CustomJoinerExample example = new CustomJoinerExample();
            
            // 첫 번째 성공 결과
            List<java.util.concurrent.Callable<String>> fastTasks = List.of(
                () -> { Thread.sleep(200); return "첫번째 응답"; },
                () -> { Thread.sleep(100); return "두번째 응답"; },
                () -> { Thread.sleep(300); return "세번째 응답"; }
            );
            
            String firstResult = example.getFirstSuccessfulResult(fastTasks, Duration.ofSeconds(1));
            System.out.println("✓ 첫 번째 성공: " + firstResult);
            
            // 과반수 성공
            List<java.util.concurrent.Callable<Integer>> majorityTasks = List.of(
                () -> 10,
                () -> 20,
                () -> { throw new RuntimeException("의도적 실패"); },
                () -> 40,
                () -> 50
            );
            
            CustomJoinerExample.MajorityResult<Integer> majorityResult = 
                example.waitForMajoritySuccess(majorityTasks, Duration.ofSeconds(1));
            
            System.out.println("✓ 과반수 성공:");
            System.out.println("  - 성공 개수: " + majorityResult.successes().size());
            System.out.println("  - 과반수 달성: " + majorityResult.hasMajority());
            System.out.println("  - 성공률: " + String.format("%.1f%%", majorityResult.successRate() * 100));
            
            // 품질 기반 결과
            List<java.util.concurrent.Callable<CustomJoinerExample.QualifiedResult<String>>> qualityTasks = List.of(
                () -> new CustomJoinerExample.QualifiedResult<>("저품질 결과", 0.3, "소스1"),
                () -> new CustomJoinerExample.QualifiedResult<>("고품질 결과", 0.9, "소스2"),
                () -> new CustomJoinerExample.QualifiedResult<>("중간 결과", 0.6, "소스3")
            );
            
            CustomJoinerExample.QualityResult<String> qualityResult = 
                example.waitForQualityResult(qualityTasks, 0.8, Duration.ofSeconds(1));
            
            System.out.println("✓ 품질 기반 완료:");
            System.out.println("  - 임계치 달성: " + qualityResult.metThreshold());
            if (qualityResult.bestResult() != null) {
                System.out.println("  - 최고 품질: " + qualityResult.bestResult().quality());
                System.out.println("  - 결과: " + qualityResult.bestResult().result());
            }
            
            // 주문 처리
            List<CustomJoinerExample.Order> orders = List.of(
                new CustomJoinerExample.Order("ORD001", "CUST001", 50000, "대기"),
                new CustomJoinerExample.Order("ORD002", "CUST002", 150000, "대기"),
                new CustomJoinerExample.Order("ORD003", "CUST003", 200000, "대기")
            );
            
            CustomJoinerExample.OrderProcessingResult orderResult = 
                example.processOrdersBatch(orders, Duration.ofSeconds(2));
            
            System.out.println("✓ 주문 처리 완료:");
            System.out.println("  - 처리 개수: " + orderResult.getProcessedCount());
            System.out.println("  - 총 금액: " + orderResult.getTotalAmount());
            System.out.println("  - 고액 주문: " + orderResult.getHighValueOrders().size());
            
        } catch (Exception e) {
            System.err.println("✗ 커스텀 조이너 데모 실패: " + e.getMessage());
        }
    }
    
    private List<Integer> generateTestData(int size) {
        return java.util.stream.IntStream.rangeClosed(1, size)
                                        .boxed()
                                        .collect(java.util.ArrayList::new, 
                                               java.util.ArrayList::add, 
                                               java.util.ArrayList::addAll);
    }
    
    /**
     * 성능 비교 데모
     * 순차 처리 vs 병렬 처리 성능 측정
     */
    public void runPerformanceComparison() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("성능 비교: 순차 vs 병렬 처리");
        System.out.println("=".repeat(60));
        
        List<Integer> testData = generateTestData(10000);
        
        try {
            // 순차 처리
            long sequentialStart = System.currentTimeMillis();
            List<String> sequentialResult = testData.stream()
                                                   .map(n -> "처리-" + n)
                                                   .toList();
            long sequentialTime = System.currentTimeMillis() - sequentialStart;
            
            // 병렬 처리
            ParallelDataProcessing processor = new ParallelDataProcessing();
            long parallelStart = System.currentTimeMillis();
            List<String> parallelResult = processor.processLargeDataset(
                testData,
                number -> "처리-" + number,
                1000
            );
            long parallelTime = System.currentTimeMillis() - parallelStart;
            
            System.out.println("순차 처리:");
            System.out.println("  - 결과 수: " + sequentialResult.size());
            System.out.println("  - 소요 시간: " + sequentialTime + "ms");
            
            System.out.println("병렬 처리 (Structured Concurrency):");
            System.out.println("  - 결과 수: " + parallelResult.size());
            System.out.println("  - 소요 시간: " + parallelTime + "ms");
            
            double speedup = (double) sequentialTime / parallelTime;
            System.out.println("성능 향상: " + String.format("%.2fx", speedup));
            
        } catch (Exception e) {
            System.err.println("성능 비교 실패: " + e.getMessage());
        }
    }
    
    /**
     * 실제 사용 시나리오 데모
     */
    public void runRealWorldScenario() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("실제 사용 시나리오: 전자상거래 주문 처리");
        System.out.println("=".repeat(60));
        
        try {
            // 1. 사용자 정보 조회
            TaskScopeExample taskExample = new TaskScopeExample();
            TaskScopeExample.UserProfile userProfile = taskExample.fetchUserProfileAsync("customer-123");
            System.out.println("1. 사용자 정보 조회 완료: " + userProfile.user().name());
            
            // 2. 제품 정보 집계
            WebServiceAggregation webService = new WebServiceAggregation();
            WebServiceAggregation.ProductDetails product = webService.aggregateProductDetails("PRODUCT-456");
            System.out.println("2. 제품 정보 집계 완료: " + product.product().name());
            
            // 3. 주문 처리 (커스텀 조이너 사용)
            CustomJoinerExample customJoiner = new CustomJoinerExample();
            List<CustomJoinerExample.Order> orders = List.of(
                new CustomJoinerExample.Order("ORD-001", userProfile.user().id(), 
                                             product.pricing().salePrice(), "신규")
            );
            
            CustomJoinerExample.OrderProcessingResult orderResult = 
                customJoiner.processOrdersBatch(orders, Duration.ofSeconds(5));
            System.out.println("3. 주문 처리 완료: " + orderResult.getProcessedCount() + "개");
            
            // 4. 최종 대시보드 업데이트
            WebServiceAggregation.UserDashboard finalDashboard = 
                webService.aggregateUserDashboardWithFallback(userProfile.user().id(), Duration.ofSeconds(3));
            System.out.println("4. 대시보드 업데이트 완료");
            
            System.out.println("\n전자상거래 주문 처리 플로우 성공적으로 완료!");
            
        } catch (Exception e) {
            System.err.println("실제 시나리오 실행 실패: " + e.getMessage());
            
            // 에러 처리 데모
            System.out.println("에러 복구 시나리오 실행...");
            try {
                ErrorHandlingExample errorHandler = new ErrorHandlingExample();
                List<String> recoveryData = List.of("recovery-data-1", "recovery-data-2");
                ErrorHandlingExample.ProcessingResult recovery = 
                    errorHandler.processWithPartialFailure(recoveryData);
                    
                System.out.println("복구 완료: " + recovery.successes().size() + "개 성공");
            } catch (Exception recoveryError) {
                System.err.println("복구도 실패: " + recoveryError.getMessage());
            }
        }
    }
}