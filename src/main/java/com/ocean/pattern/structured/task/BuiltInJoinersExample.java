package com.ocean.pattern.structured.task;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * StructuredTaskScope 내장 조이너 활용 예제
 * 
 * JDK 25에서 제공하는 내장 조이너들의 특성과 사용법을 상세히 보여줍니다:
 * 
 * 1. allSuccessfulOrThrow() - 모든 태스크가 성공해야 완료
 * 2. anySuccessfulResultOrThrow() - 첫 번째 성공 결과로 완료
 * 3. awaitAllSuccessfulOrThrow() - 모든 성공을 대기, void 반환
 * 4. awaitAll() - 모든 완료를 대기 (부분 실패 허용)
 * 5. allUntil(Predicate) - 조건부 완료 로직
 * 
 * JDK 25 실제 사용법:
 * ```java
 * try (var scope = StructuredTaskScope.open(StructuredTaskScope.allSuccessfulOrThrow())) {
 *     Subtask<String> task1 = scope.fork(() -> operation1());
 *     Subtask<String> task2 = scope.fork(() -> operation2());
 *     
 *     Stream<Subtask<String>> completedTasks = scope.join();
 *     return completedTasks.map(Subtask::get).toList();
 * }
 * ```
 * 
 * 학습 목표:
 * 1. 각 조이너의 동작 특성 이해
 * 2. 적절한 조이너 선택 기준
 * 3. 성능과 안정성 트레이드오프
 * 4. 실제 사용 시나리오 적용
 */
public class BuiltInJoinersExample {
    
    private static final Logger logger = Logger.getLogger(BuiltInJoinersExample.class.getName());
    
    /**
     * allSuccessfulOrThrow() 조이너 시뮬레이션
     * 모든 태스크가 성공해야 완료되며, 하나라도 실패하면 전체 실패
     */
    public static class AllSuccessfulJoiner {
        
        /**
         * JDK 25 실제 구현 (이론적)
         * 
         * public List<String> processAllSuccessful() throws InterruptedException {
         *     try (var scope = StructuredTaskScope.open(StructuredTaskScope.allSuccessfulOrThrow())) {
         *         Subtask<String> task1 = scope.fork(() -> criticalOperation1());
         *         Subtask<String> task2 = scope.fork(() -> criticalOperation2());
         *         Subtask<String> task3 = scope.fork(() -> criticalOperation3());
         *         
         *         Stream<Subtask<String>> completedTasks = scope.join(); // 모든 성공 or 예외
         *         return completedTasks.map(Subtask::get).collect(toList());
         *     }
         * }
         */
        
        /**
         * 시뮬레이션: 모든 성공 필요 패턴
         * 금융 거래, 중요 데이터 동기화 등에 적합
         */
        public AllSuccessfulResult processAllSuccessfulSimulation(boolean simulateFailure) 
                throws InterruptedException, ExecutionException {
            
            logger.info("allSuccessfulOrThrow 시뮬레이션 - 실패 시뮬레이션: " + simulateFailure);
            Instant startTime = Instant.now();
            
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                // 모든 태스크가 성공해야 하는 중요한 작업들
                Future<String> criticalTask1 = executor.submit(() -> criticalOperation("결제-검증", simulateFailure && Math.random() < 0.3));
                Future<String> criticalTask2 = executor.submit(() -> criticalOperation("재고-확인", simulateFailure && Math.random() < 0.3));
                Future<String> criticalTask3 = executor.submit(() -> criticalOperation("배송-예약", simulateFailure && Math.random() < 0.3));
                
                List<String> results = new ArrayList<>();
                List<Exception> failures = new ArrayList<>();
                
                // allSuccessfulOrThrow 동작 시뮬레이션
                try {
                    String result1 = criticalTask1.get();
                    results.add(result1);
                } catch (ExecutionException e) {
                    failures.add((Exception) e.getCause());
                }
                
                try {
                    String result2 = criticalTask2.get();
                    results.add(result2);
                } catch (ExecutionException e) {
                    failures.add((Exception) e.getCause());
                }
                
                try {
                    String result3 = criticalTask3.get();
                    results.add(result3);
                } catch (ExecutionException e) {
                    failures.add((Exception) e.getCause());
                }
                
                Duration duration = Duration.between(startTime, Instant.now());
                
                // allSuccessfulOrThrow는 하나라도 실패하면 전체 실패
                if (!failures.isEmpty()) {
                    logger.warning("일부 중요 작업 실패, 전체 취소");
                    return new AllSuccessfulResult(false, new ArrayList<>(), failures, duration);
                }
                
                logger.info("모든 중요 작업 성공");
                return new AllSuccessfulResult(true, results, failures, duration);
            }
        }
        
        /**
         * 트랜잭션 일관성이 중요한 시나리오
         */
        public TransactionResult processTransactionalOperations() throws InterruptedException, ExecutionException {
            logger.info("트랜잭션 작업 처리 시작");
            
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                // 모두 성공하거나 모두 롤백해야 하는 작업들
                Future<String> dbUpdate1 = executor.submit(() -> simulateDbUpdate("사용자-정보"));
                Future<String> dbUpdate2 = executor.submit(() -> simulateDbUpdate("주문-정보"));
                Future<String> dbUpdate3 = executor.submit(() -> simulateDbUpdate("결제-정보"));
                Future<String> cacheUpdate = executor.submit(() -> simulateCacheUpdate("세션-정보"));
                
                List<String> operations = new ArrayList<>();
                boolean allSuccessful = true;
                
                try {
                    operations.add(dbUpdate1.get());
                    operations.add(dbUpdate2.get());
                    operations.add(dbUpdate3.get());
                    operations.add(cacheUpdate.get());
                    
                } catch (ExecutionException e) {
                    allSuccessful = false;
                    logger.warning("트랜잭션 작업 실패, 롤백 필요: " + e.getCause().getMessage());
                    
                    // 실제로는 보상 트랜잭션 실행
                    operations.add("ROLLBACK: 모든 변경사항 취소");
                }
                
                return new TransactionResult(allSuccessful, operations);
            }
        }
        
        private String criticalOperation(String operationType, boolean shouldFail) throws Exception {
            Thread.sleep(100 + (int)(Math.random() * 200));
            
            if (shouldFail) {
                throw new Exception(operationType + " 중요 작업 실패");
            }
            
            return operationType + " 성공";
        }
        
        private String simulateDbUpdate(String table) throws Exception {
            Thread.sleep(150 + (int)(Math.random() * 100));
            
            // 10% 확률로 실패
            if (Math.random() < 0.1) {
                throw new Exception("DB 업데이트 실패: " + table);
            }
            
            return "DB_UPDATE: " + table;
        }
        
        private String simulateCacheUpdate(String key) throws Exception {
            Thread.sleep(50 + (int)(Math.random() * 50));
            
            // 5% 확률로 실패
            if (Math.random() < 0.05) {
                throw new Exception("캐시 업데이트 실패: " + key);
            }
            
            return "CACHE_UPDATE: " + key;
        }
    }
    
    /**
     * anySuccessfulResultOrThrow() 조이너 시뮬레이션
     * 첫 번째 성공 결과를 반환하고 나머지 태스크는 취소
     */
    public static class AnySuccessfulJoiner {
        
        /**
         * JDK 25 실제 구현 (이론적)
         * 
         * public String getFirstSuccessfulResult() throws InterruptedException {
         *     try (var scope = StructuredTaskScope.open(StructuredTaskScope.anySuccessfulResultOrThrow())) {
         *         Subtask<String> primarySource = scope.fork(() -> fetchFromPrimary());
         *         Subtask<String> backupSource = scope.fork(() -> fetchFromBackup());
         *         Subtask<String> cacheSource = scope.fork(() -> fetchFromCache());
         *         
         *         String result = scope.join(); // 첫 번째 성공 결과
         *         return result;
         *     }
         * }
         */
        
        /**
         * 시뮬레이션: 첫 번째 성공 결과 패턴
         * 여러 데이터 소스 중 가장 빠른 응답 활용
         */
        public AnySuccessfulResult getFirstSuccessfulSimulation(List<DataSource> sources) 
                throws InterruptedException, ExecutionException {
            
            logger.info("anySuccessfulResultOrThrow 시뮬레이션 - 데이터 소스: " + sources.size());
            Instant startTime = Instant.now();
            
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                // anySuccessfulResultOrThrow 동작: 첫 번째 성공을 기다림
                CompletionService<DataResponse> completionService = new ExecutorCompletionService<>(executor);
                List<Future<DataResponse>> futures = new ArrayList<>();
                
                // 모든 데이터 소스에 동시 요청
                for (DataSource source : sources) {
                    Future<DataResponse> future = completionService.submit(() -> fetchFromDataSource(source));
                    futures.add(future);
                }
                
                int attempts = 0;
                Exception lastException = null;
                
                while (attempts < futures.size()) {
                    try {
                        Future<DataResponse> completed = completionService.take();
                        DataResponse result = completed.get();
                        
                        // 첫 번째 성공 - 나머지 모든 태스크 취소
                        futures.forEach(f -> f.cancel(true));
                        
                        Duration duration = Duration.between(startTime, Instant.now());
                        
                        logger.info("첫 번째 성공 결과 획득: " + result.source() + 
                                   " (시도 " + (attempts + 1) + "/" + futures.size() + ")");
                        
                        return new AnySuccessfulResult(true, result, attempts + 1, duration, null);
                        
                    } catch (ExecutionException e) {
                        attempts++;
                        lastException = (Exception) e.getCause();
                        logger.warning("데이터 소스 실패 (시도 " + attempts + "): " + lastException.getMessage());
                    }
                }
                
                // 모든 소스가 실패한 경우
                Duration duration = Duration.between(startTime, Instant.now());
                logger.severe("모든 데이터 소스 실패");
                
                return new AnySuccessfulResult(false, null, attempts, duration, lastException);
            }
        }
        
        /**
         * 서비스 가용성 패턴 - 여러 서비스 인스턴스 중 응답 가능한 것 사용
         */
        public ServiceResult getAvailableService(List<ServiceEndpoint> endpoints) 
                throws InterruptedException, ExecutionException {
            
            logger.info("서비스 가용성 확인: " + endpoints.size() + "개 엔드포인트");
            
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                CompletionService<ServiceResponse> completionService = new ExecutorCompletionService<>(executor);
                
                // 모든 서비스 엔드포인트에 헬스체크
                for (ServiceEndpoint endpoint : endpoints) {
                    completionService.submit(() -> healthCheck(endpoint));
                }
                
                List<Exception> failures = new ArrayList<>();
                
                for (int i = 0; i < endpoints.size(); i++) {
                    try {
                        Future<ServiceResponse> completed = completionService.take();
                        ServiceResponse response = completed.get();
                        
                        if (response.isHealthy()) {
                            logger.info("가용한 서비스 발견: " + response.endpoint().url());
                            return new ServiceResult(true, response, failures);
                        }
                        
                    } catch (ExecutionException e) {
                        failures.add((Exception) e.getCause());
                    }
                }
                
                logger.warning("모든 서비스 인스턴스 사용 불가");
                return new ServiceResult(false, null, failures);
            }
        }
        
        private DataResponse fetchFromDataSource(DataSource source) throws Exception {
            Thread.sleep(source.latency() + (int)(Math.random() * 100));
            
            if (Math.random() < source.failureRate()) {
                throw new Exception("데이터 소스 실패: " + source.name());
            }
            
            return new DataResponse(source.name(), "데이터-" + System.currentTimeMillis());
        }
        
        private ServiceResponse healthCheck(ServiceEndpoint endpoint) throws Exception {
            Thread.sleep(endpoint.timeout());
            
            boolean healthy = Math.random() > endpoint.failureRate();
            
            if (!healthy) {
                throw new Exception("서비스 헬스체크 실패: " + endpoint.url());
            }
            
            return new ServiceResponse(endpoint, healthy, "OK");
        }
    }
    
    /**
     * awaitAll() 조이너 시뮬레이션
     * 모든 태스크 완료를 대기하되 부분 실패 허용
     */
    public static class AwaitAllJoiner {
        
        /**
         * JDK 25 실제 구현 (이론적)
         * 
         * public void processAllTasks() throws InterruptedException {
         *     try (var scope = StructuredTaskScope.open(StructuredTaskScope.awaitAll())) {
         *         Subtask<Void> task1 = scope.fork(() -> { sideEffectOperation1(); return null; });
         *         Subtask<Void> task2 = scope.fork(() -> { sideEffectOperation2(); return null; });
         *         Subtask<Void> task3 = scope.fork(() -> { sideEffectOperation3(); return null; });
         *         
         *         scope.join(); // 모든 완료 대기 (성공/실패 무관)
         *         
         *         // 개별적으로 결과 확인
         *         checkTaskResult(task1);
         *         checkTaskResult(task2);
         *         checkTaskResult(task3);
         *     }
         * }
         */
        
        /**
         * 시뮬레이션: 모든 완료 대기 패턴 (부분 실패 허용)
         * 로그 수집, 통계 처리, 백그라운드 작업 등에 적합
         */
        public AwaitAllResult processAllWithPartialFailure(List<BackgroundTask> tasks) 
                throws InterruptedException {
            
            logger.info("awaitAll 시뮬레이션 - 백그라운드 작업: " + tasks.size());
            Instant startTime = Instant.now();
            
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                List<Future<TaskResult>> futures = new ArrayList<>();
                
                // 모든 백그라운드 작업 시작
                for (BackgroundTask task : tasks) {
                    Future<TaskResult> future = executor.submit(() -> executeBackgroundTask(task));
                    futures.add(future);
                }
                
                // awaitAll: 모든 완료 대기 (성공/실패 관계없이)
                List<TaskResult> results = new ArrayList<>();
                List<Exception> failures = new ArrayList<>();
                
                for (int i = 0; i < futures.size(); i++) {
                    try {
                        TaskResult result = futures.get(i).get();
                        results.add(result);
                        logger.info("백그라운드 작업 완료: " + result.taskName());
                    } catch (ExecutionException e) {
                        Exception cause = (Exception) e.getCause();
                        failures.add(cause);
                        logger.warning("백그라운드 작업 실패: " + tasks.get(i).name() + " - " + cause.getMessage());
                        
                        // 실패한 작업도 결과에 포함 (부분 실패 허용)
                        results.add(new TaskResult(tasks.get(i).name(), "FAILED", cause.getMessage()));
                    }
                }
                
                Duration duration = Duration.between(startTime, Instant.now());
                
                int successCount = (int) results.stream().filter(r -> r.status().equals("SUCCESS")).count();
                
                AwaitAllResult result = new AwaitAllResult(results, failures, successCount, duration);
                
                logger.info("모든 작업 완료 - 성공: " + successCount + "/" + tasks.size());
                return result;
            }
        }
        
        /**
         * 통계 수집 패턴 - 여러 메트릭 소스에서 데이터 수집
         */
        public MetricsResult collectAllMetrics(List<MetricSource> sources) throws InterruptedException {
            logger.info("메트릭 수집: " + sources.size() + "개 소스");
            
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                List<Future<MetricData>> futures = new ArrayList<>();
                
                for (MetricSource source : sources) {
                    Future<MetricData> future = executor.submit(() -> collectMetric(source));
                    futures.add(future);
                }
                
                List<MetricData> allMetrics = new ArrayList<>();
                List<String> failedSources = new ArrayList<>();
                
                // 모든 메트릭 수집 대기 (실패 허용)
                for (int i = 0; i < futures.size(); i++) {
                    try {
                        MetricData metric = futures.get(i).get();
                        allMetrics.add(metric);
                    } catch (ExecutionException e) {
                        String sourceName = sources.get(i).name();
                        failedSources.add(sourceName);
                        logger.warning("메트릭 수집 실패: " + sourceName + " - " + e.getCause().getMessage());
                    }
                }
                
                return new MetricsResult(allMetrics, failedSources);
            }
        }
        
        private TaskResult executeBackgroundTask(BackgroundTask task) throws Exception {
            Thread.sleep(task.duration());
            
            if (Math.random() < task.failureRate()) {
                throw new Exception("백그라운드 작업 실패: " + task.name());
            }
            
            return new TaskResult(task.name(), "SUCCESS", "완료됨");
        }
        
        private MetricData collectMetric(MetricSource source) throws Exception {
            Thread.sleep(source.collectDuration());
            
            if (Math.random() < source.unavailabilityRate()) {
                throw new Exception("메트릭 소스 사용 불가: " + source.name());
            }
            
            double value = 50 + Math.random() * 100; // 시뮬레이션 값
            return new MetricData(source.name(), value, source.unit());
        }
    }
    
    // 데이터 클래스들
    
    public record AllSuccessfulResult(
        boolean allSucceeded,
        List<String> results,
        List<Exception> failures,
        Duration duration
    ) {}
    
    public record TransactionResult(boolean committed, List<String> operations) {}
    
    public record DataSource(String name, int latency, double failureRate) {}
    
    public record DataResponse(String source, String data) {}
    
    public record AnySuccessfulResult(
        boolean success,
        DataResponse result,
        int attemptsUntilSuccess,
        Duration duration,
        Exception lastFailure
    ) {}
    
    public record ServiceEndpoint(String url, int timeout, double failureRate) {}
    
    public record ServiceResponse(ServiceEndpoint endpoint, boolean isHealthy, String status) {}
    
    public record ServiceResult(boolean available, ServiceResponse response, List<Exception> failures) {}
    
    public record BackgroundTask(String name, int duration, double failureRate) {}
    
    public record TaskResult(String taskName, String status, String message) {}
    
    public record AwaitAllResult(
        List<TaskResult> results,
        List<Exception> failures,
        int successCount,
        Duration totalDuration
    ) {}
    
    public record MetricSource(String name, int collectDuration, double unavailabilityRate, String unit) {}
    
    public record MetricData(String sourceName, double value, String unit) {}
    
    public record MetricsResult(List<MetricData> metrics, List<String> failedSources) {}
    
    // 실행 예제
    public static void main(String[] args) {
        AllSuccessfulJoiner allSuccessful = new AllSuccessfulJoiner();
        AnySuccessfulJoiner anySuccessful = new AnySuccessfulJoiner();
        AwaitAllJoiner awaitAll = new AwaitAllJoiner();
        
        try {
            // allSuccessfulOrThrow 시뮬레이션 (성공)
            System.out.println("=== allSuccessfulOrThrow (성공 케이스) ===");
            AllSuccessfulResult successCase = allSuccessful.processAllSuccessfulSimulation(false);
            System.out.println("결과: " + successCase);
            
            // allSuccessfulOrThrow 시뮬레이션 (실패)
            System.out.println("\n=== allSuccessfulOrThrow (실패 케이스) ===");
            AllSuccessfulResult failureCase = allSuccessful.processAllSuccessfulSimulation(true);
            System.out.println("결과: " + failureCase);
            
            // 트랜잭션 처리
            System.out.println("\n=== 트랜잭션 처리 ===");
            TransactionResult txResult = allSuccessful.processTransactionalOperations();
            System.out.println("트랜잭션 커밋: " + txResult.committed());
            txResult.operations().forEach(System.out::println);
            
            // anySuccessfulResultOrThrow
            System.out.println("\n=== anySuccessfulResultOrThrow ===");
            List<DataSource> dataSources = List.of(
                new DataSource("Primary-DB", 200, 0.3),
                new DataSource("Cache", 50, 0.1),
                new DataSource("Backup-DB", 300, 0.2)
            );
            
            AnySuccessfulResult anyResult = anySuccessful.getFirstSuccessfulSimulation(dataSources);
            System.out.println("첫 번째 성공: " + anyResult);
            
            // 서비스 가용성 확인
            System.out.println("\n=== 서비스 가용성 확인 ===");
            List<ServiceEndpoint> endpoints = List.of(
                new ServiceEndpoint("http://service1.com", 100, 0.4),
                new ServiceEndpoint("http://service2.com", 150, 0.2),
                new ServiceEndpoint("http://service3.com", 80, 0.1)
            );
            
            ServiceResult serviceResult = anySuccessful.getAvailableService(endpoints);
            System.out.println("서비스 가용성: " + serviceResult);
            
            // awaitAll (부분 실패 허용)
            System.out.println("\n=== awaitAll (부분 실패 허용) ===");
            List<BackgroundTask> bgTasks = List.of(
                new BackgroundTask("로그-정리", 200, 0.1),
                new BackgroundTask("캐시-갱신", 150, 0.2),
                new BackgroundTask("통계-계산", 300, 0.15),
                new BackgroundTask("백업-실행", 500, 0.05)
            );
            
            AwaitAllResult awaitResult = awaitAll.processAllWithPartialFailure(bgTasks);
            System.out.println("백그라운드 작업 결과: 성공 " + awaitResult.successCount() + 
                             "/" + awaitResult.results().size());
            
            // 메트릭 수집
            System.out.println("\n=== 메트릭 수집 ===");
            List<MetricSource> metricSources = List.of(
                new MetricSource("CPU-Usage", 100, 0.05, "%"),
                new MetricSource("Memory-Usage", 120, 0.1, "GB"),
                new MetricSource("Disk-IO", 80, 0.15, "MB/s"),
                new MetricSource("Network-Traffic", 90, 0.08, "Mbps")
            );
            
            MetricsResult metricsResult = awaitAll.collectAllMetrics(metricSources);
            System.out.println("수집된 메트릭: " + metricsResult.metrics().size());
            System.out.println("실패한 소스: " + metricsResult.failedSources());
            
            for (MetricData metric : metricsResult.metrics()) {
                System.out.printf("  %s: %.2f %s%n", metric.sourceName(), metric.value(), metric.unit());
            }
            
        } catch (Exception e) {
            System.err.println("내장 조이너 예제 실행 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
}