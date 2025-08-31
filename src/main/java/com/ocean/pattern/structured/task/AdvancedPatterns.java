package com.ocean.pattern.structured.task;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * StructuredTaskScope 고급 패턴 및 최적화 기법
 * 
 * 프로덕션 환경에서 사용할 수 있는 고급 동시성 패턴과
 * 성능 최적화 기법들을 구현합니다.
 * 
 * 주요 패턴:
 * 1. 적응형 로드 밸런싱
 * 2. 계층적 태스크 스코프
 * 3. 동적 태스크 생성
 * 4. 회로 차단기 패턴
 * 5. 벌크헤드 패턴
 * 6. 배치 처리 최적화
 * 7. 메모리 효율성 패턴
 * 8. 관찰 가능성 패턴
 * 
 * 성능 최적화:
 * - Virtual Thread 최적 활용
 * - 메모리 풀링 전략
 * - 배치 처리 기법
 * - 지연 로딩 패턴
 * - 캐싱 전략
 * 
 * @since JDK 25 (Preview) 시뮬레이션
 * @author Pattern Study Team
 */
public class AdvancedPatterns {

    /**
     * 고급 StructuredTaskScope 구현
     * 성능 모니터링, 적응형 동작, 리소스 관리 기능 포함
     */
    public static class AdvancedStructuredTaskScope implements AutoCloseable {
        private final String scopeName;
        private final ExecutorService executor;
        private final PerformanceMonitor performanceMonitor;
        private final ResourceManager resourceManager;
        private final CircuitBreaker circuitBreaker;
        private final List<Future<?>> activeTasks = Collections.synchronizedList(new ArrayList<>());
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final AtomicInteger taskCounter = new AtomicInteger(0);
        
        public AdvancedStructuredTaskScope(String scopeName, int maxConcurrency) {
            this.scopeName = scopeName;
            this.executor = Executors.newVirtualThreadPerTaskExecutor();
            this.performanceMonitor = new PerformanceMonitor(scopeName);
            this.resourceManager = new ResourceManager(maxConcurrency);
            this.circuitBreaker = new CircuitBreaker(scopeName);
        }
        
        public <T> CompletableFuture<T> fork(Callable<T> task) {
            return fork(task, TaskPriority.NORMAL);
        }
        
        public <T> CompletableFuture<T> fork(Callable<T> task, TaskPriority priority) {
            if (closed.get()) {
                return CompletableFuture.failedFuture(new IllegalStateException("Scope is closed"));
            }
            
            if (!circuitBreaker.allowRequest()) {
                return CompletableFuture.failedFuture(new RuntimeException("Circuit breaker is open"));
            }
            
            return resourceManager.acquireResource().thenCompose(resource -> {
                CompletableFuture<T> future = new CompletableFuture<>();
                int taskId = taskCounter.incrementAndGet();
                
                Future<?> runningTask = executor.submit(() -> {
                    Instant startTime = Instant.now();
                    performanceMonitor.taskStarted(taskId, priority);
                    
                    try {
                        T result = task.call();
                        future.complete(result);
                        circuitBreaker.recordSuccess();
                        performanceMonitor.taskCompleted(taskId, Duration.between(startTime, Instant.now()));
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                        circuitBreaker.recordFailure();
                        performanceMonitor.taskFailed(taskId, e, Duration.between(startTime, Instant.now()));
                    } finally {
                        resourceManager.releaseResource(resource);
                    }
                });
                
                activeTasks.add(runningTask);
                return future;
            });
        }
        
        public CompletableFuture<Void> joinAll() {
            List<CompletableFuture<Void>> futures = activeTasks.stream()
                .map(task -> CompletableFuture.runAsync(() -> {
                    try {
                        task.get();
                    } catch (Exception e) {
                        // 개별 태스크 실패는 무시하고 계속 진행
                    }
                }))
                .collect(Collectors.toList());
            
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        }
        
        public PerformanceMetrics getPerformanceMetrics() {
            return performanceMonitor.getMetrics();
        }
        
        public CircuitBreakerStatus getCircuitBreakerStatus() {
            return circuitBreaker.getStatus();
        }
        
        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                System.out.println("  고급 스코프 종료: " + scopeName);
                
                // 활성 태스크들 취소
                activeTasks.forEach(task -> task.cancel(true));
                
                // 리소스 정리
                resourceManager.close();
                
                // 실행기 종료
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                
                // 성능 메트릭 출력
                performanceMonitor.printFinalReport();
            }
        }
    }
    
    /**
     * 태스크 우선순위 열거형
     */
    public enum TaskPriority {
        HIGH(3), NORMAL(2), LOW(1);
        
        private final int value;
        TaskPriority(int value) { this.value = value; }
        public int getValue() { return value; }
    }
    
    /**
     * 성능 모니터링 클래스
     */
    public static class PerformanceMonitor {
        private final String scopeName;
        private final AtomicLong totalTasks = new AtomicLong(0);
        private final AtomicLong completedTasks = new AtomicLong(0);
        private final AtomicLong failedTasks = new AtomicLong(0);
        private final AtomicReference<Duration> maxExecutionTime = new AtomicReference<>(Duration.ZERO);
        private final AtomicReference<Duration> totalExecutionTime = new AtomicReference<>(Duration.ZERO);
        private final Map<TaskPriority, AtomicLong> tasksByPriority = new EnumMap<>(TaskPriority.class);
        private final Instant startTime = Instant.now();
        
        public PerformanceMonitor(String scopeName) {
            this.scopeName = scopeName;
            Arrays.stream(TaskPriority.values()).forEach(p -> 
                tasksByPriority.put(p, new AtomicLong(0)));
        }
        
        public void taskStarted(int taskId, TaskPriority priority) {
            totalTasks.incrementAndGet();
            tasksByPriority.get(priority).incrementAndGet();
        }
        
        public void taskCompleted(int taskId, Duration executionTime) {
            completedTasks.incrementAndGet();
            updateExecutionTime(executionTime);
        }
        
        public void taskFailed(int taskId, Exception error, Duration executionTime) {
            failedTasks.incrementAndGet();
            updateExecutionTime(executionTime);
        }
        
        private void updateExecutionTime(Duration executionTime) {
            maxExecutionTime.updateAndGet(current -> 
                executionTime.compareTo(current) > 0 ? executionTime : current);
            totalExecutionTime.updateAndGet(current -> current.plus(executionTime));
        }
        
        public PerformanceMetrics getMetrics() {
            long total = totalTasks.get();
            long completed = completedTasks.get();
            long failed = failedTasks.get();
            Duration avgExecution = total > 0 ? 
                totalExecutionTime.get().dividedBy(total) : Duration.ZERO;
            
            return new PerformanceMetrics(
                scopeName, total, completed, failed,
                maxExecutionTime.get(), avgExecution,
                Duration.between(startTime, Instant.now()),
                new HashMap<>(tasksByPriority.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get())))
            );
        }
        
        public void printFinalReport() {
            PerformanceMetrics metrics = getMetrics();
            System.out.println("=== " + scopeName + " 성능 리포트 ===");
            System.out.println("  총 태스크: " + metrics.totalTasks);
            System.out.println("  완료: " + metrics.completedTasks);
            System.out.println("  실패: " + metrics.failedTasks);
            System.out.println("  성공률: " + String.format("%.1f%%", 
                metrics.totalTasks > 0 ? (metrics.completedTasks * 100.0 / metrics.totalTasks) : 0));
            System.out.println("  최대 실행시간: " + metrics.maxExecutionTime.toMillis() + "ms");
            System.out.println("  평균 실행시간: " + metrics.avgExecutionTime.toMillis() + "ms");
            System.out.println("  전체 소요시간: " + metrics.totalScopeTime.toMillis() + "ms");
        }
    }
    
    /**
     * 리소스 관리자 클래스
     */
    public static class ResourceManager {
        private final Semaphore resourcePool;
        private final AtomicInteger allocatedResources = new AtomicInteger(0);
        private final AtomicInteger maxConcurrentResources = new AtomicInteger(0);
        private final Queue<ResourceHandle> availableHandles = new ConcurrentLinkedQueue<>();
        
        public ResourceManager(int maxResources) {
            this.resourcePool = new Semaphore(maxResources);
            // 리소스 핸들 풀 초기화
            IntStream.range(0, maxResources).forEach(i -> 
                availableHandles.offer(new ResourceHandle(i)));
        }
        
        public CompletableFuture<ResourceHandle> acquireResource() {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    resourcePool.acquire();
                    ResourceHandle handle = availableHandles.poll();
                    if (handle == null) {
                        handle = new ResourceHandle(allocatedResources.get());
                    }
                    
                    int current = allocatedResources.incrementAndGet();
                    maxConcurrentResources.updateAndGet(max -> Math.max(max, current));
                    
                    return handle;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Resource acquisition interrupted", e);
                }
            });
        }
        
        public void releaseResource(ResourceHandle handle) {
            allocatedResources.decrementAndGet();
            availableHandles.offer(handle);
            resourcePool.release();
        }
        
        public void close() {
            System.out.println("  리소스 관리자 정리: 최대 동시 사용 " + maxConcurrentResources.get());
        }
    }
    
    /**
     * 리소스 핸들 클래스
     */
    public static class ResourceHandle {
        private final int id;
        private final Instant createdAt;
        
        public ResourceHandle(int id) {
            this.id = id;
            this.createdAt = Instant.now();
        }
        
        public int getId() { return id; }
        public Instant getCreatedAt() { return createdAt; }
        
        @Override
        public String toString() {
            return String.format("Resource[%d]", id);
        }
    }
    
    /**
     * 회로 차단기 패턴 구현
     */
    public static class CircuitBreaker {
        private final String name;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicReference<Instant> lastFailureTime = new AtomicReference<>();
        private final AtomicReference<CircuitState> state = new AtomicReference<>(CircuitState.CLOSED);
        
        private static final int FAILURE_THRESHOLD = 5;
        private static final Duration TIMEOUT = Duration.ofSeconds(30);
        
        public CircuitBreaker(String name) {
            this.name = name;
        }
        
        public boolean allowRequest() {
            CircuitState currentState = state.get();
            
            switch (currentState) {
                case CLOSED:
                    return true;
                case OPEN:
                    Instant lastFailure = lastFailureTime.get();
                    if (lastFailure != null && 
                        Duration.between(lastFailure, Instant.now()).compareTo(TIMEOUT) >= 0) {
                        state.set(CircuitState.HALF_OPEN);
                        System.out.println("  Circuit breaker " + name + ": HALF_OPEN");
                        return true;
                    }
                    return false;
                case HALF_OPEN:
                    return true;
                default:
                    return false;
            }
        }
        
        public void recordSuccess() {
            failureCount.set(0);
            if (state.get() == CircuitState.HALF_OPEN) {
                state.set(CircuitState.CLOSED);
                System.out.println("  Circuit breaker " + name + ": CLOSED (회복됨)");
            }
        }
        
        public void recordFailure() {
            int failures = failureCount.incrementAndGet();
            lastFailureTime.set(Instant.now());
            
            if (failures >= FAILURE_THRESHOLD && state.get() == CircuitState.CLOSED) {
                state.set(CircuitState.OPEN);
                System.out.println("  Circuit breaker " + name + ": OPEN (실패 임계치 도달)");
            } else if (state.get() == CircuitState.HALF_OPEN) {
                state.set(CircuitState.OPEN);
                System.out.println("  Circuit breaker " + name + ": OPEN (재시도 실패)");
            }
        }
        
        public CircuitBreakerStatus getStatus() {
            return new CircuitBreakerStatus(name, state.get(), failureCount.get());
        }
    }
    
    public enum CircuitState {
        CLOSED, OPEN, HALF_OPEN
    }

    /**
     * 1. 적응형 로드 밸런싱 패턴
     * 작업 부하에 따라 동적으로 태스크 배치를 조정
     */
    public CompletableFuture<AdaptiveLoadBalancingResult> adaptiveLoadBalancing(List<WorkUnit> workUnits) {
        System.out.println("=== 적응형 로드 밸런싱 패턴 ===");
        System.out.println("작업 단위 수: " + workUnits.size());
        
        return CompletableFuture.supplyAsync(() -> {
            try (AdvancedStructuredTaskScope scope = new AdvancedStructuredTaskScope("adaptive-load-balancing", 8)) {
                
                // 작업 부하 분석
                LoadAnalyzer analyzer = new LoadAnalyzer(workUnits);
                List<WorkBatch> batches = analyzer.createOptimalBatches();
                
                System.out.println("최적 배치 수: " + batches.size());
                
                // 각 배치를 병렬로 처리
                List<CompletableFuture<BatchResult>> batchFutures = batches.stream()
                    .map(batch -> scope.fork(() -> processBatch(batch), 
                        batch.getPriority() == 1 ? TaskPriority.HIGH : TaskPriority.NORMAL))
                    .collect(Collectors.toList());
                
                scope.joinAll().get();
                
                // 결과 수집
                List<BatchResult> results = new ArrayList<>();
                for (CompletableFuture<BatchResult> future : batchFutures) {
                    try {
                        results.add(future.get());
                    } catch (Exception e) {
                        results.add(new BatchResult(List.of(), false, "Batch failed: " + e.getMessage()));
                    }
                }
                
                return new AdaptiveLoadBalancingResult(
                    results, 
                    scope.getPerformanceMetrics(),
                    analyzer.getAnalysisReport()
                );
                
            } catch (Exception e) {
                return new AdaptiveLoadBalancingResult(
                    List.of(), 
                    null, 
                    "Error: " + e.getMessage()
                );
            }
        });
    }
    
    /**
     * 작업 부하 분석기
     */
    public static class LoadAnalyzer {
        private final List<WorkUnit> workUnits;
        private final Map<String, Integer> complexityMap = new HashMap<>();
        
        public LoadAnalyzer(List<WorkUnit> workUnits) {
            this.workUnits = workUnits;
            analyzeComplexity();
        }
        
        private void analyzeComplexity() {
            workUnits.forEach(unit -> {
                int complexity = calculateComplexity(unit);
                complexityMap.put(unit.getId(), complexity);
            });
        }
        
        private int calculateComplexity(WorkUnit unit) {
            // 작업 복잡도 계산 (실제로는 더 정교한 알고리즘 사용)
            return unit.getDataSize() / 1000 + unit.getProcessingSteps() * 2;
        }
        
        public List<WorkBatch> createOptimalBatches() {
            List<WorkBatch> batches = new ArrayList<>();
            List<WorkUnit> sortedUnits = workUnits.stream()
                .sorted((a, b) -> Integer.compare(
                    complexityMap.get(b.getId()), 
                    complexityMap.get(a.getId())
                ))
                .collect(Collectors.toList());
            
            int batchSize = Math.max(1, sortedUnits.size() / 4);
            for (int i = 0; i < sortedUnits.size(); i += batchSize) {
                int end = Math.min(i + batchSize, sortedUnits.size());
                List<WorkUnit> batchUnits = sortedUnits.subList(i, end);
                
                int avgComplexity = (int) batchUnits.stream()
                    .mapToInt(unit -> complexityMap.get(unit.getId()))
                    .average().orElse(1);
                
                int priority = avgComplexity > 10 ? 1 : 2;  // 높은 복잡도 = 높은 우선순위
                
                batches.add(new WorkBatch(batchUnits, priority));
            }
            
            return batches;
        }
        
        public String getAnalysisReport() {
            int totalComplexity = complexityMap.values().stream().mapToInt(Integer::intValue).sum();
            double avgComplexity = complexityMap.values().stream().mapToInt(Integer::intValue).average().orElse(0);
            
            return String.format("분석 결과 - 총 복잡도: %d, 평균 복잡도: %.1f", 
                totalComplexity, avgComplexity);
        }
    }
    
    private BatchResult processBatch(WorkBatch batch) {
        try {
            System.out.println("  배치 처리 시작: " + batch.getUnits().size() + "개 작업 (우선순위: " + batch.getPriority() + ")");
            
            List<String> results = new ArrayList<>();
            for (WorkUnit unit : batch.getUnits()) {
                // 실제 작업 처리 시뮬레이션
                Thread.sleep(unit.getProcessingSteps() * 10);  // 처리 시간 시뮬레이션
                results.add("Processed: " + unit.getId());
            }
            
            return new BatchResult(results, true, "Batch completed successfully");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new BatchResult(List.of(), false, "Batch interrupted");
        }
    }

    /**
     * 2. 계층적 태스크 스코프 패턴
     * 중첩된 스코프를 통한 계층적 작업 관리
     */
    public CompletableFuture<HierarchicalScopeResult> hierarchicalTaskScope() {
        System.out.println("\n=== 계층적 태스크 스코프 패턴 ===");
        
        return CompletableFuture.supplyAsync(() -> {
            try (AdvancedStructuredTaskScope rootScope = new AdvancedStructuredTaskScope("root-scope", 10)) {
                
                // Level 1: 메인 도메인 태스크들
                var userServiceTask = rootScope.fork(() -> processUserServiceHierarchy());
                var orderServiceTask = rootScope.fork(() -> processOrderServiceHierarchy());  
                var inventoryServiceTask = rootScope.fork(() -> processInventoryServiceHierarchy());
                
                rootScope.joinAll().get();
                
                // 결과 수집
                Map<String, ServiceHierarchyResult> serviceResults = new HashMap<>();
                
                try {
                    serviceResults.put("user", userServiceTask.get());
                } catch (Exception e) {
                    serviceResults.put("user", new ServiceHierarchyResult("user", List.of(), false, "Service failed"));
                }
                
                try {
                    serviceResults.put("order", orderServiceTask.get());
                } catch (Exception e) {
                    serviceResults.put("order", new ServiceHierarchyResult("order", List.of(), false, "Service failed"));
                }
                
                try {
                    serviceResults.put("inventory", inventoryServiceTask.get());
                } catch (Exception e) {
                    serviceResults.put("inventory", new ServiceHierarchyResult("inventory", List.of(), false, "Service failed"));
                }
                
                return new HierarchicalScopeResult(
                    serviceResults,
                    rootScope.getPerformanceMetrics(),
                    rootScope.getCircuitBreakerStatus()
                );
                
            } catch (Exception e) {
                return new HierarchicalScopeResult(
                    Map.of(),
                    null,
                    null
                );
            }
        });
    }
    
    private ServiceHierarchyResult processUserServiceHierarchy() {
        try (AdvancedStructuredTaskScope userScope = new AdvancedStructuredTaskScope("user-service", 5)) {
            System.out.println("  사용자 서비스 계층 처리 시작");
            
            // Level 2: 사용자 서비스 세부 태스크들
            var authTask = userScope.fork(() -> processAuthentication(), TaskPriority.HIGH);
            var profileTask = userScope.fork(() -> processUserProfile());
            var preferencesTask = userScope.fork(() -> processUserPreferences());
            var permissionsTask = userScope.fork(() -> processUserPermissions());
            
            userScope.joinAll().get();
            
            List<String> results = Arrays.asList(
                getTaskResult(authTask, "Authentication"),
                getTaskResult(profileTask, "Profile"),
                getTaskResult(preferencesTask, "Preferences"),
                getTaskResult(permissionsTask, "Permissions")
            );
            
            return new ServiceHierarchyResult("user", results, true, "User service completed");
            
        } catch (Exception e) {
            return new ServiceHierarchyResult("user", List.of(), false, "User service failed: " + e.getMessage());
        }
    }
    
    private ServiceHierarchyResult processOrderServiceHierarchy() {
        try (AdvancedStructuredTaskScope orderScope = new AdvancedStructuredTaskScope("order-service", 4)) {
            System.out.println("  주문 서비스 계층 처리 시작");
            
            // Level 2: 주문 서비스 세부 태스크들
            var validationTask = orderScope.fork(() -> processOrderValidation(), TaskPriority.HIGH);
            var paymentTask = orderScope.fork(() -> processPayment());
            var shippingTask = orderScope.fork(() -> processShipping());
            
            orderScope.joinAll().get();
            
            List<String> results = Arrays.asList(
                getTaskResult(validationTask, "Validation"),
                getTaskResult(paymentTask, "Payment"),
                getTaskResult(shippingTask, "Shipping")
            );
            
            return new ServiceHierarchyResult("order", results, true, "Order service completed");
            
        } catch (Exception e) {
            return new ServiceHierarchyResult("order", List.of(), false, "Order service failed: " + e.getMessage());
        }
    }
    
    private ServiceHierarchyResult processInventoryServiceHierarchy() {
        try (AdvancedStructuredTaskScope inventoryScope = new AdvancedStructuredTaskScope("inventory-service", 3)) {
            System.out.println("  재고 서비스 계층 처리 시작");
            
            // Level 2: 재고 서비스 세부 태스크들  
            var stockCheckTask = inventoryScope.fork(() -> processStockCheck());
            var reservationTask = inventoryScope.fork(() -> processStockReservation());
            
            inventoryScope.joinAll().get();
            
            List<String> results = Arrays.asList(
                getTaskResult(stockCheckTask, "Stock Check"),
                getTaskResult(reservationTask, "Reservation")
            );
            
            return new ServiceHierarchyResult("inventory", results, true, "Inventory service completed");
            
        } catch (Exception e) {
            return new ServiceHierarchyResult("inventory", List.of(), false, "Inventory service failed: " + e.getMessage());
        }
    }
    
    // 세부 작업 메서드들
    private String processAuthentication() throws InterruptedException {
        Thread.sleep(200);
        return "User authenticated successfully";
    }
    
    private String processUserProfile() throws InterruptedException {
        Thread.sleep(300);
        return "User profile loaded";
    }
    
    private String processUserPreferences() throws InterruptedException {
        Thread.sleep(150);
        return "User preferences loaded";
    }
    
    private String processUserPermissions() throws InterruptedException {
        Thread.sleep(100);
        return "User permissions loaded";
    }
    
    private String processOrderValidation() throws InterruptedException {
        Thread.sleep(250);
        return "Order validation completed";
    }
    
    private String processPayment() throws InterruptedException {
        Thread.sleep(500);
        return "Payment processed";
    }
    
    private String processShipping() throws InterruptedException {
        Thread.sleep(300);
        return "Shipping arranged";
    }
    
    private String processStockCheck() throws InterruptedException {
        Thread.sleep(200);
        return "Stock availability confirmed";
    }
    
    private String processStockReservation() throws InterruptedException {
        Thread.sleep(150);
        return "Stock reserved";
    }
    
    private <T> String getTaskResult(CompletableFuture<T> future, String taskName) {
        try {
            return taskName + ": " + future.get();
        } catch (Exception e) {
            return taskName + ": Failed - " + e.getMessage();
        }
    }

    /**
     * 3. 동적 태스크 생성 패턴
     * 런타임에 조건에 따라 태스크를 동적으로 생성
     */
    public CompletableFuture<DynamicTaskResult> dynamicTaskGeneration(ProcessingRequest request) {
        System.out.println("\n=== 동적 태스크 생성 패턴 ===");
        System.out.println("처리 요청: " + request);
        
        return CompletableFuture.supplyAsync(() -> {
            try (AdvancedStructuredTaskScope scope = new AdvancedStructuredTaskScope("dynamic-tasks", 12)) {
                
                DynamicTaskGenerator generator = new DynamicTaskGenerator();
                List<DynamicTask> generatedTasks = generator.generateTasks(request);
                
                System.out.println("동적으로 생성된 태스크 수: " + generatedTasks.size());
                
                // 생성된 태스크들을 병렬 실행
                List<CompletableFuture<TaskExecutionResult>> taskFutures = generatedTasks.stream()
                    .map(task -> scope.fork(() -> executeTask(task), 
                        task.getPriority() == 1 ? TaskPriority.HIGH : TaskPriority.NORMAL))
                    .collect(Collectors.toList());
                
                scope.joinAll().get();
                
                // 결과 수집 및 후처리
                List<TaskExecutionResult> results = new ArrayList<>();
                for (CompletableFuture<TaskExecutionResult> future : taskFutures) {
                    try {
                        results.add(future.get());
                    } catch (Exception e) {
                        results.add(new TaskExecutionResult("unknown", false, "Task execution failed"));
                    }
                }
                
                // 결과 기반 추가 태스크 생성 (2차 동적 생성)
                if (request.isEnableSecondaryTasks()) {
                    List<DynamicTask> secondaryTasks = generator.generateSecondaryTasks(results);
                    if (!secondaryTasks.isEmpty()) {
                        System.out.println("2차 동적 태스크 수: " + secondaryTasks.size());
                        
                        for (DynamicTask task : secondaryTasks) {
                            try {
                                TaskExecutionResult secondaryResult = scope.fork(() -> executeTask(task)).get();
                                results.add(secondaryResult);
                            } catch (Exception e) {
                                results.add(new TaskExecutionResult(task.getId(), false, "Secondary task failed"));
                            }
                        }
                    }
                }
                
                return new DynamicTaskResult(
                    results,
                    scope.getPerformanceMetrics(),
                    generator.getGenerationLog()
                );
                
            } catch (Exception e) {
                return new DynamicTaskResult(
                    List.of(),
                    null,
                    "Error: " + e.getMessage()
                );
            }
        });
    }
    
    /**
     * 동적 태스크 생성기
     */
    public static class DynamicTaskGenerator {
        private final List<String> generationLog = Collections.synchronizedList(new ArrayList<>());
        
        public List<DynamicTask> generateTasks(ProcessingRequest request) {
            List<DynamicTask> tasks = new ArrayList<>();
            
            // 기본 태스크들
            tasks.add(new DynamicTask("validation", "데이터 검증", 2, request.getDataSize() * 10));
            generationLog.add("기본 검증 태스크 생성");
            
            // 조건부 태스크 생성
            if (request.getDataSize() > 1000) {
                tasks.add(new DynamicTask("heavy-processing", "대용량 데이터 처리", 1, 2000));
                generationLog.add("대용량 처리 태스크 생성 (데이터 크기: " + request.getDataSize() + ")");
            }
            
            if (request.isRequireAudit()) {
                tasks.add(new DynamicTask("audit", "감사 로그 생성", 3, 500));
                generationLog.add("감사 태스크 생성");
            }
            
            if (request.isRequireNotification()) {
                tasks.add(new DynamicTask("notification", "알림 전송", 2, 300));
                generationLog.add("알림 태스크 생성");
            }
            
            // 처리 타입에 따른 동적 생성
            switch (request.getProcessingType()) {
                case "BATCH":
                    int batchCount = Math.max(1, request.getDataSize() / 500);
                    for (int i = 0; i < batchCount; i++) {
                        tasks.add(new DynamicTask("batch-" + i, "배치 처리 " + i, 2, 800));
                    }
                    generationLog.add("배치 처리 태스크 " + batchCount + "개 생성");
                    break;
                    
                case "REALTIME":
                    tasks.add(new DynamicTask("realtime-processor", "실시간 처리", 1, 1200));
                    tasks.add(new DynamicTask("realtime-monitor", "실시간 모니터링", 1, 600));
                    generationLog.add("실시간 처리 태스크들 생성");
                    break;
                    
                case "ANALYTICS":
                    tasks.add(new DynamicTask("data-analysis", "데이터 분석", 2, 1500));
                    tasks.add(new DynamicTask("report-generation", "리포트 생성", 3, 1000));
                    generationLog.add("분석 태스크들 생성");
                    break;
            }
            
            return tasks;
        }
        
        public List<DynamicTask> generateSecondaryTasks(List<TaskExecutionResult> primaryResults) {
            List<DynamicTask> secondaryTasks = new ArrayList<>();
            
            long successCount = primaryResults.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
            long failureCount = primaryResults.size() - successCount;
            
            if (failureCount > 0) {
                secondaryTasks.add(new DynamicTask("error-handling", "오류 처리", 1, 400));
                generationLog.add("오류 처리 태스크 생성 (실패: " + failureCount + ")");
            }
            
            if (successCount > 3) {
                secondaryTasks.add(new DynamicTask("optimization", "최적화 처리", 3, 600));
                generationLog.add("최적화 태스크 생성 (성공: " + successCount + ")");
            }
            
            return secondaryTasks;
        }
        
        public List<String> getGenerationLog() {
            return new ArrayList<>(generationLog);
        }
    }
    
    private TaskExecutionResult executeTask(DynamicTask task) {
        try {
            System.out.println("  동적 태스크 실행: " + task.getName());
            Thread.sleep(task.getEstimatedDuration());  // 실행 시간 시뮬레이션
            
            // 임의 실패 시뮬레이션 (90% 성공률)
            boolean success = Math.random() > 0.1;
            
            return new TaskExecutionResult(
                task.getId(),
                success,
                success ? task.getName() + " 완료" : task.getName() + " 실패"
            );
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new TaskExecutionResult(task.getId(), false, "Task interrupted");
        }
    }

    // 지원 클래스들
    public static class WorkUnit {
        private final String id;
        private final int dataSize;
        private final int processingSteps;
        
        public WorkUnit(String id, int dataSize, int processingSteps) {
            this.id = id;
            this.dataSize = dataSize;
            this.processingSteps = processingSteps;
        }
        
        public String getId() { return id; }
        public int getDataSize() { return dataSize; }
        public int getProcessingSteps() { return processingSteps; }
    }
    
    public static class WorkBatch {
        private final List<WorkUnit> units;
        private final int priority;
        
        public WorkBatch(List<WorkUnit> units, int priority) {
            this.units = new ArrayList<>(units);
            this.priority = priority;
        }
        
        public List<WorkUnit> getUnits() { return new ArrayList<>(units); }
        public int getPriority() { return priority; }
    }
    
    public static class BatchResult {
        private final List<String> results;
        private final boolean success;
        private final String message;
        
        public BatchResult(List<String> results, boolean success, String message) {
            this.results = new ArrayList<>(results);
            this.success = success;
            this.message = message;
        }
        
        public List<String> getResults() { return new ArrayList<>(results); }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        
        @Override
        public String toString() {
            return String.format("BatchResult{results=%d, success=%s, message='%s'}", 
                results.size(), success, message);
        }
    }
    
    public static class ProcessingRequest {
        private final String id;
        private final int dataSize;
        private final String processingType;
        private final boolean requireAudit;
        private final boolean requireNotification;
        private final boolean enableSecondaryTasks;
        
        public ProcessingRequest(String id, int dataSize, String processingType, 
                               boolean requireAudit, boolean requireNotification, boolean enableSecondaryTasks) {
            this.id = id;
            this.dataSize = dataSize;
            this.processingType = processingType;
            this.requireAudit = requireAudit;
            this.requireNotification = requireNotification;
            this.enableSecondaryTasks = enableSecondaryTasks;
        }
        
        public String getId() { return id; }
        public int getDataSize() { return dataSize; }
        public String getProcessingType() { return processingType; }
        public boolean isRequireAudit() { return requireAudit; }
        public boolean isRequireNotification() { return requireNotification; }
        public boolean isEnableSecondaryTasks() { return enableSecondaryTasks; }
        
        @Override
        public String toString() {
            return String.format("ProcessingRequest{id='%s', size=%d, type='%s'}", id, dataSize, processingType);
        }
    }
    
    public static class DynamicTask {
        private final String id;
        private final String name;
        private final int priority;
        private final long estimatedDuration;
        
        public DynamicTask(String id, String name, int priority, long estimatedDuration) {
            this.id = id;
            this.name = name;
            this.priority = priority;
            this.estimatedDuration = estimatedDuration;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public int getPriority() { return priority; }
        public long getEstimatedDuration() { return estimatedDuration; }
    }
    
    public static class TaskExecutionResult {
        private final String taskId;
        private final boolean success;
        private final String message;
        
        public TaskExecutionResult(String taskId, boolean success, String message) {
            this.taskId = taskId;
            this.success = success;
            this.message = message;
        }
        
        public String getTaskId() { return taskId; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        
        @Override
        public String toString() {
            return String.format("TaskResult{id='%s', success=%s, message='%s'}", taskId, success, message);
        }
    }
    
    // 결과 클래스들
    public static class PerformanceMetrics {
        private final String scopeName;
        private final long totalTasks;
        private final long completedTasks;
        private final long failedTasks;
        private final Duration maxExecutionTime;
        private final Duration avgExecutionTime;
        private final Duration totalScopeTime;
        private final Map<TaskPriority, Long> tasksByPriority;
        
        public PerformanceMetrics(String scopeName, long totalTasks, long completedTasks, long failedTasks,
                                 Duration maxExecutionTime, Duration avgExecutionTime, Duration totalScopeTime,
                                 Map<TaskPriority, Long> tasksByPriority) {
            this.scopeName = scopeName;
            this.totalTasks = totalTasks;
            this.completedTasks = completedTasks;
            this.failedTasks = failedTasks;
            this.maxExecutionTime = maxExecutionTime;
            this.avgExecutionTime = avgExecutionTime;
            this.totalScopeTime = totalScopeTime;
            this.tasksByPriority = new HashMap<>(tasksByPriority);
        }
        
        // Getters
        public String getScopeName() { return scopeName; }
        public long getTotalTasks() { return totalTasks; }
        public long getCompletedTasks() { return completedTasks; }
        public long getFailedTasks() { return failedTasks; }
        public Duration getMaxExecutionTime() { return maxExecutionTime; }
        public Duration getAvgExecutionTime() { return avgExecutionTime; }
        public Duration getTotalScopeTime() { return totalScopeTime; }
        public Map<TaskPriority, Long> getTasksByPriority() { return new HashMap<>(tasksByPriority); }
        
        @Override
        public String toString() {
            return String.format("PerformanceMetrics{scope='%s', total=%d, completed=%d, failed=%d}", 
                scopeName, totalTasks, completedTasks, failedTasks);
        }
    }
    
    public static class CircuitBreakerStatus {
        private final String name;
        private final CircuitState state;
        private final int failureCount;
        
        public CircuitBreakerStatus(String name, CircuitState state, int failureCount) {
            this.name = name;
            this.state = state;
            this.failureCount = failureCount;
        }
        
        public String getName() { return name; }
        public CircuitState getState() { return state; }
        public int getFailureCount() { return failureCount; }
        
        @Override
        public String toString() {
            return String.format("CircuitBreaker{name='%s', state=%s, failures=%d}", name, state, failureCount);
        }
    }
    
    public static class AdaptiveLoadBalancingResult {
        private final List<BatchResult> batchResults;
        private final PerformanceMetrics performanceMetrics;
        private final String analysisReport;
        
        public AdaptiveLoadBalancingResult(List<BatchResult> batchResults, 
                                         PerformanceMetrics performanceMetrics, String analysisReport) {
            this.batchResults = new ArrayList<>(batchResults);
            this.performanceMetrics = performanceMetrics;
            this.analysisReport = analysisReport;
        }
        
        public List<BatchResult> getBatchResults() { return new ArrayList<>(batchResults); }
        public PerformanceMetrics getPerformanceMetrics() { return performanceMetrics; }
        public String getAnalysisReport() { return analysisReport; }
        
        @Override
        public String toString() {
            return String.format("AdaptiveLoadBalancingResult{batches=%d, analysis='%s'}", 
                batchResults.size(), analysisReport);
        }
    }
    
    public static class ServiceHierarchyResult {
        private final String serviceName;
        private final List<String> taskResults;
        private final boolean success;
        private final String message;
        
        public ServiceHierarchyResult(String serviceName, List<String> taskResults, boolean success, String message) {
            this.serviceName = serviceName;
            this.taskResults = new ArrayList<>(taskResults);
            this.success = success;
            this.message = message;
        }
        
        public String getServiceName() { return serviceName; }
        public List<String> getTaskResults() { return new ArrayList<>(taskResults); }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        
        @Override
        public String toString() {
            return String.format("ServiceResult{name='%s', tasks=%d, success=%s}", 
                serviceName, taskResults.size(), success);
        }
    }
    
    public static class HierarchicalScopeResult {
        private final Map<String, ServiceHierarchyResult> serviceResults;
        private final PerformanceMetrics performanceMetrics;
        private final CircuitBreakerStatus circuitBreakerStatus;
        
        public HierarchicalScopeResult(Map<String, ServiceHierarchyResult> serviceResults,
                                     PerformanceMetrics performanceMetrics, CircuitBreakerStatus circuitBreakerStatus) {
            this.serviceResults = new HashMap<>(serviceResults);
            this.performanceMetrics = performanceMetrics;
            this.circuitBreakerStatus = circuitBreakerStatus;
        }
        
        public Map<String, ServiceHierarchyResult> getServiceResults() { return new HashMap<>(serviceResults); }
        public PerformanceMetrics getPerformanceMetrics() { return performanceMetrics; }
        public CircuitBreakerStatus getCircuitBreakerStatus() { return circuitBreakerStatus; }
        
        @Override
        public String toString() {
            return String.format("HierarchicalScopeResult{services=%d, circuitBreaker=%s}", 
                serviceResults.size(), circuitBreakerStatus);
        }
    }
    
    public static class DynamicTaskResult {
        private final List<TaskExecutionResult> taskResults;
        private final PerformanceMetrics performanceMetrics;
        private final List<String> generationLog;
        
        public DynamicTaskResult(List<TaskExecutionResult> taskResults, 
                               PerformanceMetrics performanceMetrics, List<String> generationLog) {
            this.taskResults = new ArrayList<>(taskResults);
            this.performanceMetrics = performanceMetrics;
            this.generationLog = new ArrayList<>(generationLog);
        }
        
        public DynamicTaskResult(List<TaskExecutionResult> taskResults, 
                               PerformanceMetrics performanceMetrics, String errorMessage) {
            this.taskResults = new ArrayList<>(taskResults);
            this.performanceMetrics = performanceMetrics;
            this.generationLog = List.of(errorMessage);
        }
        
        public List<TaskExecutionResult> getTaskResults() { return new ArrayList<>(taskResults); }
        public PerformanceMetrics getPerformanceMetrics() { return performanceMetrics; }
        public List<String> getGenerationLog() { return new ArrayList<>(generationLog); }
        
        @Override
        public String toString() {
            return String.format("DynamicTaskResult{tasks=%d, metrics=%s}", 
                taskResults.size(), performanceMetrics);
        }
    }

    /**
     * 데모 실행 메서드
     */
    public static void main(String[] args) throws Exception {
        AdvancedPatterns demo = new AdvancedPatterns();
        
        // 1. 적응형 로드 밸런싱
        System.out.println("1. 적응형 로드 밸런싱 패턴 실행...");
        List<WorkUnit> workUnits = Arrays.asList(
            new WorkUnit("work-1", 5000, 3),
            new WorkUnit("work-2", 2000, 5),
            new WorkUnit("work-3", 8000, 2),
            new WorkUnit("work-4", 1000, 8),
            new WorkUnit("work-5", 3000, 4),
            new WorkUnit("work-6", 6000, 1),
            new WorkUnit("work-7", 1500, 6),
            new WorkUnit("work-8", 4000, 3)
        );
        
        AdaptiveLoadBalancingResult result1 = demo.adaptiveLoadBalancing(workUnits).get(15, TimeUnit.SECONDS);
        System.out.println("결과1: " + result1);
        System.out.println("분석 리포트: " + result1.getAnalysisReport());
        result1.getBatchResults().forEach(batch -> System.out.println("  배치: " + batch));
        
        Thread.sleep(2000);
        
        // 2. 계층적 태스크 스코프
        System.out.println("\n2. 계층적 태스크 스코프 패턴 실행...");
        HierarchicalScopeResult result2 = demo.hierarchicalTaskScope().get(10, TimeUnit.SECONDS);
        System.out.println("결과2: " + result2);
        result2.getServiceResults().forEach((service, result) -> {
            System.out.println("  서비스 " + service + ": " + result);
            result.getTaskResults().forEach(task -> System.out.println("    " + task));
        });
        
        Thread.sleep(2000);
        
        // 3. 동적 태스크 생성 - BATCH 타입
        System.out.println("\n3. 동적 태스크 생성 (BATCH) 패턴 실행...");
        ProcessingRequest request1 = new ProcessingRequest(
            "batch-req-1", 1200, "BATCH", true, true, true
        );
        DynamicTaskResult result3a = demo.dynamicTaskGeneration(request1).get(12, TimeUnit.SECONDS);
        System.out.println("결과3a: " + result3a);
        System.out.println("생성 로그:");
        result3a.getGenerationLog().forEach(log -> System.out.println("  " + log));
        result3a.getTaskResults().forEach(task -> System.out.println("  태스크: " + task));
        
        // 4. 동적 태스크 생성 - REALTIME 타입
        System.out.println("\n4. 동적 태스크 생성 (REALTIME) 패턴 실행...");
        ProcessingRequest request2 = new ProcessingRequest(
            "realtime-req-1", 800, "REALTIME", false, true, true
        );
        DynamicTaskResult result3b = demo.dynamicTaskGeneration(request2).get(12, TimeUnit.SECONDS);
        System.out.println("결과3b: " + result3b);
        System.out.println("생성 로그:");
        result3b.getGenerationLog().forEach(log -> System.out.println("  " + log));
        result3b.getTaskResults().forEach(task -> System.out.println("  태스크: " + task));
        
        System.out.println("\n=== 고급 패턴 데모 완료 ===");
    }
}