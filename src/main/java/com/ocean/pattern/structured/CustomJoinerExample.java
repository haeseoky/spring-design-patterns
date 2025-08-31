package com.ocean.pattern.structured;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.function.Predicate;

/**
 * Structured Concurrency의 커스텀 조이너 패턴
 * 
 * 이 클래스는 JDK 25의 StructuredTaskScope에서 사용할 수 있는
 * 커스텀 조이너(Joiner) 패턴을 보여줍니다. 조이너는 여러 태스크의
 * 완료 조건을 정의하는 중요한 컴포넌트입니다.
 * 
 * 주요 학습 목표:
 * 1. 내장 조이너 (allSuccessfulOrThrow, anySuccessfulResultOrThrow)
 * 2. 커스텀 조이너 구현
 * 3. 비즈니스 로직 기반 완료 조건
 * 4. 성능 최적화 조이너 패턴
 */
public class CustomJoinerExample {
    
    private static final Logger logger = Logger.getLogger(CustomJoinerExample.class.getName());
    
    /**
     * JDK 25 Structured Concurrency의 커스텀 조이너 (이론적 구현)
     * 
     * 실제 JDK 25에서는 다음과 같이 사용됩니다:
     * 
     * try (var scope = StructuredTaskScope.open(customJoiner)) {
     *     Subtask<String> task1 = scope.fork(() -> operation1());
     *     Subtask<String> task2 = scope.fork(() -> operation2());
     *     Subtask<String> task3 = scope.fork(() -> operation3());
     *     
     *     scope.join(); // 커스텀 조이너의 조건에 따라 완료
     *     
     *     return customJoiner.result();
     * }
     * 
     * 내장 조이너들:
     * - StructuredTaskScope.allSuccessfulOrThrow() : 모든 태스크 성공 필요
     * - StructuredTaskScope.anySuccessfulResultOrThrow() : 하나라도 성공하면 완료
     */
    
    /**
     * 첫 번째 성공 결과 반환 패턴 (anySuccessfulResultOrThrow 시뮬레이션)
     * 여러 데이터 소스 중 가장 빠른 응답을 사용
     */
    public <T> T getFirstSuccessfulResult(List<Callable<T>> tasks, Duration timeout) 
            throws InterruptedException, ExecutionException, TimeoutException {
        
        logger.info("첫 번째 성공 결과 조회 시작 - 태스크 수: " + tasks.size());
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            CompletionService<T> completionService = new ExecutorCompletionService<>(executor);
            List<Future<T>> futures = new ArrayList<>();
            
            // 모든 태스크 시작
            for (Callable<T> task : tasks) {
                futures.add(completionService.submit(task));
            }
            
            // 첫 번째 성공 결과를 기다림
            Exception lastException = null;
            int completedTasks = 0;
            
            while (completedTasks < tasks.size()) {
                try {
                    Future<T> completed = completionService.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
                    
                    if (completed == null) {
                        // 타임아웃 - 남은 태스크들 취소
                        futures.forEach(f -> f.cancel(true));
                        throw new TimeoutException("첫 번째 성공 결과 타임아웃");
                    }
                    
                    T result = completed.get();
                    
                    // 성공 - 남은 태스크들 취소
                    futures.forEach(f -> f.cancel(true));
                    logger.info("첫 번째 성공 결과 획득");
                    return result;
                    
                } catch (ExecutionException e) {
                    lastException = (Exception) e.getCause();
                    completedTasks++;
                    logger.warning("태스크 실패: " + lastException.getMessage());
                }
            }
            
            // 모든 태스크가 실패한 경우
            logger.severe("모든 태스크 실패");
            throw new ExecutionException("모든 태스크 실패", lastException);
        }
    }
    
    /**
     * 과반수 성공 조이너
     * 전체 태스크의 과반수가 성공하면 완료
     */
    public <T> MajorityResult<T> waitForMajoritySuccess(List<Callable<T>> tasks, Duration timeout) 
            throws InterruptedException {
        
        logger.info("과반수 성공 대기 시작 - 태스크 수: " + tasks.size());
        int requiredSuccesses = (tasks.size() / 2) + 1;
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            List<Future<T>> futures = new ArrayList<>();
            List<T> successResults = Collections.synchronizedList(new ArrayList<>());
            List<Exception> failures = Collections.synchronizedList(new ArrayList<>());
            
            // 모든 태스크 시작
            for (Callable<T> task : tasks) {
                futures.add(executor.submit(task));
            }
            
            // 각 태스크 결과를 개별적으로 처리
            CompletableFuture<Void> allCompleted = CompletableFuture.allOf(
                futures.stream()
                       .map(future -> CompletableFuture.runAsync(() -> {
                           try {
                               T result = future.get();
                               successResults.add(result);
                               
                               // 과반수 달성 시 나머지 태스크 취소
                               if (successResults.size() >= requiredSuccesses) {
                                   futures.forEach(f -> f.cancel(true));
                               }
                           } catch (Exception e) {
                               failures.add(e);
                           }
                       }, executor))
                       .toArray(CompletableFuture[]::new)
            );
            
            try {
                allCompleted.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                futures.forEach(f -> f.cancel(true));
                logger.warning("과반수 성공 타임아웃");
            } catch (ExecutionException e) {
                // 개별 태스크 예외는 이미 처리됨
            }
            
            MajorityResult<T> result = new MajorityResult<>(
                successResults, failures, requiredSuccesses, tasks.size()
            );
            
            logger.info("과반수 성공 완료 - 성공: " + successResults.size() + 
                       ", 필요: " + requiredSuccesses);
            
            return result;
        }
    }
    
    /**
     * 품질 기반 조이너
     * 결과의 품질이 임계치를 넘는 첫 번째 결과를 반환
     */
    public <T> QualityResult<T> waitForQualityResult(List<Callable<QualifiedResult<T>>> tasks,
                                                    double qualityThreshold,
                                                    Duration timeout) throws InterruptedException {
        
        logger.info("품질 기반 결과 대기 시작 - 임계치: " + qualityThreshold);
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            CompletionService<QualifiedResult<T>> completionService = new ExecutorCompletionService<>(executor);
            List<Future<QualifiedResult<T>>> futures = new ArrayList<>();
            List<QualifiedResult<T>> allResults = new ArrayList<>();
            
            // 모든 태스크 시작
            for (Callable<QualifiedResult<T>> task : tasks) {
                futures.add(completionService.submit(task));
            }
            
            int completedTasks = 0;
            QualifiedResult<T> bestResult = null;
            
            while (completedTasks < tasks.size()) {
                try {
                    Future<QualifiedResult<T>> completed = completionService.poll(
                        timeout.toMillis() / tasks.size(), TimeUnit.MILLISECONDS
                    );
                    
                    if (completed == null) {
                        break; // 부분 타임아웃
                    }
                    
                    QualifiedResult<T> result = completed.get();
                    allResults.add(result);
                    completedTasks++;
                    
                    // 품질 임계치를 넘는 첫 번째 결과 반환
                    if (result.quality() >= qualityThreshold) {
                        futures.forEach(f -> f.cancel(true));
                        logger.info("품질 임계치 달성: " + result.quality());
                        return new QualityResult<>(result, allResults, true);
                    }
                    
                    // 최고 품질 결과 추적
                    if (bestResult == null || result.quality() > bestResult.quality()) {
                        bestResult = result;
                    }
                    
                } catch (ExecutionException e) {
                    completedTasks++;
                    logger.warning("품질 태스크 실패: " + e.getCause().getMessage());
                }
            }
            
            // 임계치를 넘는 결과가 없으면 최고 품질 결과 반환
            futures.forEach(f -> f.cancel(true));
            
            QualityResult<T> result = new QualityResult<>(bestResult, allResults, false);
            logger.info("품질 기반 완료 - 최고 품질: " + 
                       (bestResult != null ? bestResult.quality() : "없음"));
            
            return result;
        }
    }
    
    /**
     * 리소스 기반 조이너
     * 사용 가능한 리소스에 따라 동적으로 완료 조건 조정
     */
    public <T> ResourceAwareResult<T> processWithResourceConstraints(
            List<Callable<T>> tasks, 
            ResourceMonitor resourceMonitor,
            Duration timeout) throws InterruptedException {
        
        logger.info("리소스 제약 처리 시작 - 태스크 수: " + tasks.size());
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            List<Future<T>> activeTasks = new ArrayList<>();
            List<T> completedResults = Collections.synchronizedList(new ArrayList<>());
            List<Exception> errors = Collections.synchronizedList(new ArrayList<>());
            
            Instant startTime = Instant.now();
            int taskIndex = 0;
            int maxConcurrentTasks = resourceMonitor.getMaxConcurrentTasks();
            
            // 리소스 상황에 따라 동적으로 태스크 시작
            while ((taskIndex < tasks.size() || !activeTasks.isEmpty()) && 
                   Duration.between(startTime, Instant.now()).compareTo(timeout) < 0) {
                
                // 완료된 태스크 확인 및 정리
                activeTasks.removeIf(future -> {
                    if (future.isDone()) {
                        try {
                            T result = future.get();
                            completedResults.add(result);
                            logger.info("태스크 완료, 총 완료: " + completedResults.size());
                        } catch (Exception e) {
                            errors.add(e);
                            logger.warning("태스크 실패: " + e.getMessage());
                        }
                        return true;
                    }
                    return false;
                });
                
                // 리소스 상황 확인
                ResourceStatus status = resourceMonitor.getCurrentStatus();
                
                // 리소스가 부족하면 더 적은 태스크로 조정
                if (status == ResourceStatus.HIGH_USAGE) {
                    maxConcurrentTasks = Math.max(1, maxConcurrentTasks / 2);
                    logger.warning("리소스 사용량 높음, 동시 태스크 수 감소: " + maxConcurrentTasks);
                } else if (status == ResourceStatus.LOW_USAGE) {
                    maxConcurrentTasks = Math.min(tasks.size(), maxConcurrentTasks * 2);
                    logger.info("리소스 사용량 낮음, 동시 태스크 수 증가: " + maxConcurrentTasks);
                }
                
                // 새 태스크 시작 (리소스 허용 범위 내에서)
                while (activeTasks.size() < maxConcurrentTasks && taskIndex < tasks.size()) {
                    final int currentIndex = taskIndex++;
                    Future<T> future = executor.submit(tasks.get(currentIndex));
                    activeTasks.add(future);
                    logger.info("태스크 " + currentIndex + " 시작");
                }
                
                // 잠시 대기
                Thread.sleep(100);
                
                // 조기 완료 조건 확인
                if (shouldCompleteEarly(completedResults, errors, tasks.size(), resourceMonitor)) {
                    logger.info("조기 완료 조건 충족");
                    break;
                }
            }
            
            // 남은 태스크 취소
            activeTasks.forEach(f -> f.cancel(true));
            
            ResourceAwareResult<T> result = new ResourceAwareResult<>(
                completedResults, errors, resourceMonitor.getCurrentStatus()
            );
            
            logger.info("리소스 제약 처리 완료 - 성공: " + completedResults.size() + 
                       ", 실패: " + errors.size());
            
            return result;
        }
    }
    
    /**
     * 비즈니스 로직 기반 조이너
     * 도메인 특화된 완료 조건을 적용
     */
    public OrderProcessingResult processOrdersBatch(List<Order> orders, Duration timeout) 
            throws InterruptedException {
        
        logger.info("주문 배치 처리 시작: " + orders.size() + "개 주문");
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            List<Future<OrderResult>> futures = new ArrayList<>();
            List<OrderResult> processedOrders = Collections.synchronizedList(new ArrayList<>());
            
            // 각 주문을 개별 태스크로 처리
            for (Order order : orders) {
                futures.add(executor.submit(() -> processOrder(order)));
            }
            
            // 비즈니스 완료 조건 모니터링
            CompletableFuture<Void> monitor = CompletableFuture.runAsync(() -> {
                while (!futures.stream().allMatch(Future::isDone)) {
                    try {
                        // 완료된 주문 수집
                        for (Future<OrderResult> future : futures) {
                            if (future.isDone() && !future.isCancelled()) {
                                try {
                                    OrderResult result = future.get();
                                    if (!processedOrders.contains(result)) {
                                        processedOrders.add(result);
                                    }
                                } catch (Exception e) {
                                    // 이미 처리된 예외는 무시
                                }
                            }
                        }
                        
                        // 비즈니스 조건 확인
                        if (shouldStopOrderProcessing(processedOrders)) {
                            logger.info("비즈니스 조건으로 주문 처리 중단");
                            futures.forEach(f -> f.cancel(true));
                            break;
                        }
                        
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }, executor);
            
            try {
                monitor.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                logger.warning("주문 처리 타임아웃");
                futures.forEach(f -> f.cancel(true));
            } catch (ExecutionException e) {
                logger.severe("주문 처리 중 오류: " + e.getCause().getMessage());
            }
            
            OrderProcessingResult result = new OrderProcessingResult(processedOrders);
            logger.info("주문 배치 처리 완료: " + result.getProcessedCount() + "개 처리");
            
            return result;
        }
    }
    
    // 헬퍼 메서드들
    
    private <T> boolean shouldCompleteEarly(List<T> results, List<Exception> errors, 
                                           int totalTasks, ResourceMonitor resourceMonitor) {
        // 50% 이상 완료되고 리소스 사용량이 높으면 조기 완료
        double completionRate = (double) (results.size() + errors.size()) / totalTasks;
        return completionRate >= 0.5 && resourceMonitor.getCurrentStatus() == ResourceStatus.HIGH_USAGE;
    }
    
    private OrderResult processOrder(Order order) throws Exception {
        // 주문 처리 시뮬레이션
        Thread.sleep(100 + (int)(Math.random() * 500));
        
        // 10% 확률로 실패
        if (Math.random() < 0.1) {
            throw new RuntimeException("주문 처리 실패: " + order.id());
        }
        
        return new OrderResult(order.id(), "처리완료", order.amount());
    }
    
    private boolean shouldStopOrderProcessing(List<OrderResult> processedOrders) {
        // 비즈니스 규칙: 고액 주문 3개 이상 처리되면 중단
        long highValueOrders = processedOrders.stream()
                                             .filter(order -> order.amount() > 100000)
                                             .count();
        return highValueOrders >= 3;
    }
    
    // 데이터 클래스들과 인터페이스들
    
    public record QualifiedResult<T>(T result, double quality, String source) {}
    
    public record MajorityResult<T>(List<T> successes, List<Exception> failures, 
                                  int requiredSuccesses, int totalTasks) {
        public boolean hasMajority() {
            return successes.size() >= requiredSuccesses;
        }
        
        public double successRate() {
            return (double) successes.size() / totalTasks;
        }
    }
    
    public record QualityResult<T>(QualifiedResult<T> bestResult, List<QualifiedResult<T>> allResults, 
                                 boolean metThreshold) {}
    
    public record ResourceAwareResult<T>(List<T> results, List<Exception> errors, ResourceStatus resourceStatus) {}
    
    public record Order(String id, String customerId, int amount, String status) {}
    
    public record OrderResult(String orderId, String status, int amount) {}
    
    public record OrderProcessingResult(List<OrderResult> processedOrders) {
        public int getProcessedCount() {
            return processedOrders.size();
        }
        
        public int getTotalAmount() {
            return processedOrders.stream().mapToInt(OrderResult::amount).sum();
        }
        
        public List<OrderResult> getHighValueOrders() {
            return processedOrders.stream()
                                 .filter(order -> order.amount() > 100000)
                                 .toList();
        }
    }
    
    public enum ResourceStatus {
        LOW_USAGE, NORMAL_USAGE, HIGH_USAGE, CRITICAL_USAGE
    }
    
    public interface ResourceMonitor {
        ResourceStatus getCurrentStatus();
        int getMaxConcurrentTasks();
    }
    
    // 시뮬레이션을 위한 리소스 모니터 구현
    public static class SimpleResourceMonitor implements ResourceMonitor {
        private final Random random = new Random();
        
        @Override
        public ResourceStatus getCurrentStatus() {
            double usage = random.nextDouble();
            if (usage < 0.3) return ResourceStatus.LOW_USAGE;
            if (usage < 0.7) return ResourceStatus.NORMAL_USAGE;
            if (usage < 0.9) return ResourceStatus.HIGH_USAGE;
            return ResourceStatus.CRITICAL_USAGE;
        }
        
        @Override
        public int getMaxConcurrentTasks() {
            return switch (getCurrentStatus()) {
                case LOW_USAGE -> 8;
                case NORMAL_USAGE -> 4;
                case HIGH_USAGE -> 2;
                case CRITICAL_USAGE -> 1;
            };
        }
    }
    
    // 실행 예제
    public static void main(String[] args) {
        CustomJoinerExample example = new CustomJoinerExample();
        
        try {
            // 첫 번째 성공 결과 테스트
            System.out.println("=== 첫 번째 성공 결과 테스트 ===");
            List<Callable<String>> fastTasks = List.of(
                () -> { Thread.sleep(300); return "빠른 응답 1"; },
                () -> { Thread.sleep(100); return "빠른 응답 2"; },
                () -> { Thread.sleep(500); return "빠른 응답 3"; }
            );
            
            String firstResult = example.getFirstSuccessfulResult(fastTasks, Duration.ofSeconds(2));
            System.out.println("첫 번째 결과: " + firstResult);
            
            // 과반수 성공 테스트
            System.out.println("\n=== 과반수 성공 테스트 ===");
            List<Callable<Integer>> majorityTasks = List.of(
                () -> 10,
                () -> 20,
                () -> { throw new RuntimeException("실패"); },
                () -> 40,
                () -> { throw new RuntimeException("실패"); }
            );
            
            MajorityResult<Integer> majorityResult = example.waitForMajoritySuccess(
                majorityTasks, Duration.ofSeconds(2)
            );
            
            System.out.println("과반수 달성: " + majorityResult.hasMajority());
            System.out.println("성공 개수: " + majorityResult.successes().size());
            System.out.println("성공률: " + String.format("%.1f%%", majorityResult.successRate() * 100));
            
            // 품질 기반 결과 테스트
            System.out.println("\n=== 품질 기반 결과 테스트 ===");
            List<Callable<QualifiedResult<String>>> qualityTasks = List.of(
                () -> new QualifiedResult<>("결과1", 0.7, "소스1"),
                () -> new QualifiedResult<>("결과2", 0.9, "소스2"),
                () -> new QualifiedResult<>("결과3", 0.5, "소스3")
            );
            
            QualityResult<String> qualityResult = example.waitForQualityResult(
                qualityTasks, 0.8, Duration.ofSeconds(2)
            );
            
            System.out.println("임계치 달성: " + qualityResult.metThreshold());
            if (qualityResult.bestResult() != null) {
                System.out.println("최고 품질: " + qualityResult.bestResult().quality());
                System.out.println("결과: " + qualityResult.bestResult().result());
            }
            
            // 주문 처리 테스트
            System.out.println("\n=== 주문 배치 처리 테스트 ===");
            List<Order> orders = List.of(
                new Order("ORD001", "CUST001", 50000, "대기"),
                new Order("ORD002", "CUST002", 150000, "대기"),
                new Order("ORD003", "CUST003", 80000, "대기"),
                new Order("ORD004", "CUST004", 200000, "대기"),
                new Order("ORD005", "CUST005", 30000, "대기")
            );
            
            OrderProcessingResult orderResult = example.processOrdersBatch(orders, Duration.ofSeconds(3));
            System.out.println("처리된 주문: " + orderResult.getProcessedCount());
            System.out.println("총 금액: " + orderResult.getTotalAmount());
            System.out.println("고액 주문: " + orderResult.getHighValueOrders().size());
            
        } catch (Exception e) {
            System.err.println("커스텀 조이너 테스트 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
}