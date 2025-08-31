package com.ocean.pattern.structured.task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ScopedValue 상속 메커니즘 및 컨텍스트 전파 예제
 * 
 * JEP 506: Scoped Values (Preview) 기능을 시뮬레이션하여
 * StructuredTaskScope에서 스레드 간 컨텍스트 전파를 구현합니다.
 * 
 * 주요 패턴:
 * 1. 보안 컨텍스트 상속
 * 2. 트랜잭션 경계 전파
 * 3. 로깅 컨텍스트 관리
 * 4. 성능 메트릭 수집
 * 
 * JDK 25 Preview API:
 * - ScopedValue<T> 바인딩과 상속
 * - StructuredTaskScope와 통합
 * - 암시적 컨텍스트 전파
 * 
 * @since JDK 25 (Preview) 시뮬레이션
 * @author Pattern Study Team
 */
public class ScopedValueInheritance {

    /**
     * 현재 Java 버전에서 ScopedValue 시뮬레이션
     * JDK 25에서는 실제 ScopedValue API 사용
     */
    public static class SimulatedScopedValue<T> {
        private static final ThreadLocal<Map<String, Object>> CONTEXT = 
            ThreadLocal.withInitial(HashMap::new);
        
        private final String key;
        
        public SimulatedScopedValue(String key) {
            this.key = key;
        }
        
        @SuppressWarnings("unchecked")
        public T get() {
            return (T) CONTEXT.get().get(key);
        }
        
        public void runWhere(T value, Runnable operation) {
            Map<String, Object> currentContext = CONTEXT.get();
            Map<String, Object> newContext = new HashMap<>(currentContext);
            newContext.put(key, value);
            
            CONTEXT.set(newContext);
            try {
                operation.run();
            } finally {
                CONTEXT.set(currentContext);
            }
        }
        
        public <U> U callWhere(T value, Callable<U> operation) throws Exception {
            Map<String, Object> currentContext = CONTEXT.get();
            Map<String, Object> newContext = new HashMap<>(currentContext);
            newContext.put(key, value);
            
            CONTEXT.set(newContext);
            try {
                return operation.call();
            } finally {
                CONTEXT.set(currentContext);
            }
        }
        
        public static void inheritContext(Runnable task) {
            Map<String, Object> inheritedContext = CONTEXT.get();
            CompletableFuture.runAsync(() -> {
                CONTEXT.set(new HashMap<>(inheritedContext));
                try {
                    task.run();
                } finally {
                    CONTEXT.remove();
                }
            });
        }
    }
    
    // 다양한 컨텍스트 타입들
    private static final SimulatedScopedValue<SecurityContext> SECURITY_CONTEXT = 
        new SimulatedScopedValue<>("security");
    
    private static final SimulatedScopedValue<TransactionContext> TRANSACTION_CONTEXT = 
        new SimulatedScopedValue<>("transaction");
    
    private static final SimulatedScopedValue<LoggingContext> LOGGING_CONTEXT = 
        new SimulatedScopedValue<>("logging");
    
    private static final SimulatedScopedValue<MetricsContext> METRICS_CONTEXT = 
        new SimulatedScopedValue<>("metrics");

    /**
     * 보안 컨텍스트 정의
     */
    public static class SecurityContext {
        private final String userId;
        private final String sessionId;
        private final Set<String> roles;
        private final LocalDateTime timestamp;
        
        public SecurityContext(String userId, String sessionId, Set<String> roles) {
            this.userId = userId;
            this.sessionId = sessionId;
            this.roles = new HashSet<>(roles);
            this.timestamp = LocalDateTime.now();
        }
        
        // Getters
        public String getUserId() { return userId; }
        public String getSessionId() { return sessionId; }
        public Set<String> getRoles() { return roles; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        public boolean hasRole(String role) {
            return roles.contains(role);
        }
        
        @Override
        public String toString() {
            return String.format("SecurityContext{userId='%s', sessionId='%s', roles=%s}", 
                userId, sessionId, roles);
        }
    }
    
    /**
     * 트랜잭션 컨텍스트 정의
     */
    public static class TransactionContext {
        private final String transactionId;
        private final String correlationId;
        private final LocalDateTime startTime;
        private final boolean readOnly;
        
        public TransactionContext(String transactionId, String correlationId, boolean readOnly) {
            this.transactionId = transactionId;
            this.correlationId = correlationId;
            this.readOnly = readOnly;
            this.startTime = LocalDateTime.now();
        }
        
        // Getters
        public String getTransactionId() { return transactionId; }
        public String getCorrelationId() { return correlationId; }
        public LocalDateTime getStartTime() { return startTime; }
        public boolean isReadOnly() { return readOnly; }
        
        @Override
        public String toString() {
            return String.format("TransactionContext{txId='%s', correlationId='%s', readOnly=%b}", 
                transactionId, correlationId, readOnly);
        }
    }
    
    /**
     * 로깅 컨텍스트 정의
     */
    public static class LoggingContext {
        private final String requestId;
        private final String operationName;
        private final Map<String, String> additionalFields;
        
        public LoggingContext(String requestId, String operationName) {
            this.requestId = requestId;
            this.operationName = operationName;
            this.additionalFields = new HashMap<>();
        }
        
        public LoggingContext withField(String key, String value) {
            additionalFields.put(key, value);
            return this;
        }
        
        // Getters
        public String getRequestId() { return requestId; }
        public String getOperationName() { return operationName; }
        public Map<String, String> getAdditionalFields() { return additionalFields; }
        
        @Override
        public String toString() {
            return String.format("LoggingContext{requestId='%s', operation='%s', fields=%s}", 
                requestId, operationName, additionalFields);
        }
    }
    
    /**
     * 성능 메트릭 컨텍스트 정의
     */
    public static class MetricsContext {
        private final String operationName;
        private final LocalDateTime startTime;
        private final AtomicInteger operationCount = new AtomicInteger(0);
        private final List<String> metrics = Collections.synchronizedList(new ArrayList<>());
        
        public MetricsContext(String operationName) {
            this.operationName = operationName;
            this.startTime = LocalDateTime.now();
        }
        
        public void incrementOperation() {
            operationCount.incrementAndGet();
        }
        
        public void addMetric(String metric) {
            metrics.add(metric);
        }
        
        // Getters
        public String getOperationName() { return operationName; }
        public LocalDateTime getStartTime() { return startTime; }
        public int getOperationCount() { return operationCount.get(); }
        public List<String> getMetrics() { return new ArrayList<>(metrics); }
        
        @Override
        public String toString() {
            return String.format("MetricsContext{operation='%s', count=%d, metrics=%d}", 
                operationName, operationCount.get(), metrics.size());
        }
    }

    /**
     * 1. 기본 ScopedValue 상속 예제
     * 부모 스레드에서 설정한 컨텍스트가 자식 태스크로 상속됨
     */
    public CompletableFuture<String> basicScopedValueInheritance() {
        System.out.println("=== 기본 ScopedValue 상속 패턴 ===");
        
        SecurityContext securityContext = new SecurityContext(
            "user123", "session456", Set.of("USER", "READ_ONLY")
        );
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return SECURITY_CONTEXT.callWhere(securityContext, () -> {
                    System.out.println("Parent 스레드 컨텍스트: " + SECURITY_CONTEXT.get());
                    
                    // 자식 태스크들 생성 (컨텍스트 상속)
                    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                        Future<String> task1 = executor.submit(() -> processUserData());
                        Future<String> task2 = executor.submit(() -> validatePermissions());
                        Future<String> task3 = executor.submit(() -> logActivity());
                        
                        String result1 = task1.get();
                        String result2 = task2.get();
                        String result3 = task3.get();
                        
                        return String.format("결과: [%s, %s, %s]", result1, result2, result3);
                    }
                });
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        });
    }
    
    private String processUserData() {
        SecurityContext context = SECURITY_CONTEXT.get();
        System.out.println("  Task1에서 상속받은 컨텍스트: " + context.getUserId());
        return "UserData processed for " + context.getUserId();
    }
    
    private String validatePermissions() {
        SecurityContext context = SECURITY_CONTEXT.get();
        boolean hasAccess = context.hasRole("USER");
        System.out.println("  Task2 권한 검증: " + hasAccess);
        return "Permission validation: " + hasAccess;
    }
    
    private String logActivity() {
        SecurityContext context = SECURITY_CONTEXT.get();
        System.out.println("  Task3 활동 로그: " + context.getSessionId());
        return "Activity logged for session " + context.getSessionId();
    }

    /**
     * 2. 다중 컨텍스트 상속 예제
     * 여러 ScopedValue를 동시에 상속하는 패턴
     */
    public CompletableFuture<BusinessResult> multipleContextInheritance() {
        System.out.println("\n=== 다중 컨텍스트 상속 패턴 ===");
        
        SecurityContext security = new SecurityContext(
            "admin789", "session999", Set.of("ADMIN", "READ_WRITE")
        );
        
        TransactionContext transaction = new TransactionContext(
            "tx-" + UUID.randomUUID().toString().substring(0, 8),
            "corr-" + System.currentTimeMillis(),
            false
        );
        
        LoggingContext logging = new LoggingContext(
            "req-" + System.currentTimeMillis(), "multi-context-operation"
        ).withField("component", "business-service");
        
        MetricsContext metrics = new MetricsContext("multi-context-processing");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return SECURITY_CONTEXT.callWhere(security, () -> {
                    try {
                        return TRANSACTION_CONTEXT.callWhere(transaction, () -> {
                            try {
                                return LOGGING_CONTEXT.callWhere(logging, () -> {
                                    try {
                                        return METRICS_CONTEXT.callWhere(metrics, () -> {
                                            return processBusinessLogic();
                                        });
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                return new BusinessResult("Error: " + e.getMessage(), false);
            }
        });
    }
    
    private BusinessResult processBusinessLogic() {
        System.out.println("=== 모든 컨텍스트가 상속된 비즈니스 로직 실행 ===");
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // 각 태스크에서 모든 컨텍스트에 접근 가능
            Future<String> dataTask = executor.submit(this::processDataWithContext);
            Future<String> auditTask = executor.submit(this::performAuditWithContext);
            Future<String> notifyTask = executor.submit(this::sendNotificationWithContext);
            
            String dataResult = dataTask.get();
            String auditResult = auditTask.get();
            String notifyResult = notifyTask.get();
            
            return new BusinessResult(
                String.format("Business processing completed: [%s, %s, %s]", 
                    dataResult, auditResult, notifyResult),
                true
            );
        } catch (Exception e) {
            return new BusinessResult("Business logic failed: " + e.getMessage(), false);
        }
    }
    
    private String processDataWithContext() {
        SecurityContext security = SECURITY_CONTEXT.get();
        TransactionContext transaction = TRANSACTION_CONTEXT.get();
        LoggingContext logging = LOGGING_CONTEXT.get();
        MetricsContext metrics = METRICS_CONTEXT.get();
        
        System.out.println("  Data Task - Security: " + security.getUserId());
        System.out.println("  Data Task - Transaction: " + transaction.getTransactionId());
        System.out.println("  Data Task - Logging: " + logging.getRequestId());
        
        metrics.incrementOperation();
        metrics.addMetric("data-processed");
        
        return "Data processed";
    }
    
    private String performAuditWithContext() {
        SecurityContext security = SECURITY_CONTEXT.get();
        TransactionContext transaction = TRANSACTION_CONTEXT.get();
        LoggingContext logging = LOGGING_CONTEXT.get();
        MetricsContext metrics = METRICS_CONTEXT.get();
        
        System.out.println("  Audit Task - User: " + security.getUserId());
        System.out.println("  Audit Task - Transaction: " + transaction.getTransactionId());
        System.out.println("  Audit Task - Request: " + logging.getRequestId());
        
        metrics.incrementOperation();
        metrics.addMetric("audit-performed");
        
        return "Audit completed";
    }
    
    private String sendNotificationWithContext() {
        SecurityContext security = SECURITY_CONTEXT.get();
        LoggingContext logging = LOGGING_CONTEXT.get();
        MetricsContext metrics = METRICS_CONTEXT.get();
        
        System.out.println("  Notification Task - Target: " + security.getUserId());
        System.out.println("  Notification Task - Request: " + logging.getRequestId());
        
        metrics.incrementOperation();
        metrics.addMetric("notification-sent");
        
        return "Notification sent";
    }

    /**
     * 3. 조건부 컨텍스트 상속 예제
     * 특정 조건에 따라 컨텍스트를 선택적으로 상속
     */
    public CompletableFuture<ProcessingResult> conditionalContextInheritance(boolean isPrivilegedOperation) {
        System.out.println("\n=== 조건부 컨텍스트 상속 패턴 ===");
        System.out.println("특권 작업 여부: " + isPrivilegedOperation);
        
        SecurityContext baseContext = new SecurityContext(
            "user456", "session123", Set.of("USER")
        );
        
        SecurityContext privilegedContext = new SecurityContext(
            "admin123", "admin-session", Set.of("ADMIN", "PRIVILEGED")
        );
        
        SecurityContext selectedContext = isPrivilegedOperation ? privilegedContext : baseContext;
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return SECURITY_CONTEXT.callWhere(selectedContext, () -> {
                    System.out.println("선택된 컨텍스트: " + SECURITY_CONTEXT.get());
                    
                    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                        List<Future<String>> tasks = new ArrayList<>();
                        
                        // 기본 작업들
                        tasks.add(executor.submit(() -> performBasicOperation()));
                        tasks.add(executor.submit(() -> performDataProcessing()));
                        
                        // 조건부 특권 작업
                        if (isPrivilegedOperation) {
                            tasks.add(executor.submit(() -> performPrivilegedOperation()));
                            tasks.add(executor.submit(() -> performSystemOperation()));
                        }
                        
                        List<String> results = new ArrayList<>();
                        for (Future<String> task : tasks) {
                            results.add(task.get());
                        }
                        
                        return new ProcessingResult(results, true);
                    }
                });
            } catch (Exception e) {
                return new ProcessingResult(List.of("Error: " + e.getMessage()), false);
            }
        });
    }
    
    private String performBasicOperation() {
        SecurityContext context = SECURITY_CONTEXT.get();
        System.out.println("  기본 작업 수행 - User: " + context.getUserId());
        return "Basic operation completed";
    }
    
    private String performDataProcessing() {
        SecurityContext context = SECURITY_CONTEXT.get();
        System.out.println("  데이터 처리 - Session: " + context.getSessionId());
        return "Data processing completed";
    }
    
    private String performPrivilegedOperation() {
        SecurityContext context = SECURITY_CONTEXT.get();
        if (context.hasRole("PRIVILEGED")) {
            System.out.println("  특권 작업 수행 - Admin: " + context.getUserId());
            return "Privileged operation completed";
        } else {
            System.out.println("  특권 작업 거부 - 권한 부족");
            return "Privileged operation denied";
        }
    }
    
    private String performSystemOperation() {
        SecurityContext context = SECURITY_CONTEXT.get();
        if (context.hasRole("ADMIN")) {
            System.out.println("  시스템 작업 수행 - Admin: " + context.getUserId());
            return "System operation completed";
        } else {
            System.out.println("  시스템 작업 거부 - 관리자 권한 필요");
            return "System operation denied";
        }
    }

    /**
     * 4. 컨텍스트 수정 및 전파 예제
     * 자식 태스크에서 컨텍스트를 수정하고 형제 태스크로 전파
     */
    public CompletableFuture<AggregatedResult> contextModificationAndPropagation() {
        System.out.println("\n=== 컨텍스트 수정 및 전파 패턴 ===");
        
        MetricsContext initialMetrics = new MetricsContext("context-modification");
        LoggingContext initialLogging = new LoggingContext(
            "req-" + System.currentTimeMillis(), "context-modification"
        );
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return METRICS_CONTEXT.callWhere(initialMetrics, () -> {
                    try {
                        return LOGGING_CONTEXT.callWhere(initialLogging, () -> {
                            return processWithContextModification();
                        });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                return new AggregatedResult(List.of("Error: " + e.getMessage()), false);
            }
        });
    }
    
    private AggregatedResult processWithContextModification() {
        System.out.println("초기 메트릭스: " + METRICS_CONTEXT.get());
        System.out.println("초기 로깅: " + LOGGING_CONTEXT.get());
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Phase 1: 컨텍스트 수집 태스크들
            Future<String> collectMetrics = executor.submit(() -> {
                MetricsContext metrics = METRICS_CONTEXT.get();
                LoggingContext logging = LOGGING_CONTEXT.get();
                
                metrics.incrementOperation();
                metrics.addMetric("phase1-metric-collection");
                
                System.out.println("  Phase1 - 메트릭 수집 완료: " + metrics.getOperationCount());
                return "Metrics collected";
            });
            
            Future<String> collectLogs = executor.submit(() -> {
                MetricsContext metrics = METRICS_CONTEXT.get();
                LoggingContext logging = LOGGING_CONTEXT.get();
                
                metrics.incrementOperation();
                logging.withField("phase", "1").withField("task", "log-collection");
                
                System.out.println("  Phase1 - 로그 수집 완료: " + logging.getAdditionalFields());
                return "Logs collected";
            });
            
            // Phase 1 완료 대기
            String metricsResult = collectMetrics.get();
            String logsResult = collectLogs.get();
            
            // Phase 2: 수정된 컨텍스트로 추가 처리
            Future<String> processData = executor.submit(() -> {
                MetricsContext metrics = METRICS_CONTEXT.get();
                LoggingContext logging = LOGGING_CONTEXT.get();
                
                metrics.incrementOperation();
                metrics.addMetric("phase2-data-processing");
                logging.withField("phase", "2");
                
                System.out.println("  Phase2 - 데이터 처리: " + metrics.getOperationCount());
                System.out.println("  Phase2 - 로깅 필드: " + logging.getAdditionalFields());
                
                return "Data processed with modified context";
            });
            
            Future<String> generateReport = executor.submit(() -> {
                MetricsContext metrics = METRICS_CONTEXT.get();
                LoggingContext logging = LOGGING_CONTEXT.get();
                
                metrics.incrementOperation();
                metrics.addMetric("phase2-report-generation");
                logging.withField("report", "generated");
                
                System.out.println("  Phase2 - 보고서 생성: 총 " + metrics.getOperationCount() + "개 작업");
                
                return "Report generated with aggregated metrics";
            });
            
            String dataResult = processData.get();
            String reportResult = generateReport.get();
            
            return new AggregatedResult(
                List.of(metricsResult, logsResult, dataResult, reportResult),
                true
            );
            
        } catch (Exception e) {
            return new AggregatedResult(List.of("Processing failed: " + e.getMessage()), false);
        }
    }

    // 결과 클래스들
    public static class BusinessResult {
        private final String message;
        private final boolean success;
        
        public BusinessResult(String message, boolean success) {
            this.message = message;
            this.success = success;
        }
        
        public String getMessage() { return message; }
        public boolean isSuccess() { return success; }
        
        @Override
        public String toString() {
            return String.format("BusinessResult{message='%s', success=%s}", message, success);
        }
    }
    
    public static class ProcessingResult {
        private final List<String> results;
        private final boolean success;
        
        public ProcessingResult(List<String> results, boolean success) {
            this.results = new ArrayList<>(results);
            this.success = success;
        }
        
        public List<String> getResults() { return new ArrayList<>(results); }
        public boolean isSuccess() { return success; }
        
        @Override
        public String toString() {
            return String.format("ProcessingResult{results=%s, success=%s}", results, success);
        }
    }
    
    public static class AggregatedResult {
        private final List<String> operations;
        private final boolean completed;
        
        public AggregatedResult(List<String> operations, boolean completed) {
            this.operations = new ArrayList<>(operations);
            this.completed = completed;
        }
        
        public List<String> getOperations() { return new ArrayList<>(operations); }
        public boolean isCompleted() { return completed; }
        
        @Override
        public String toString() {
            return String.format("AggregatedResult{operations=%s, completed=%s}", operations, completed);
        }
    }

    /**
     * 데모 실행 메서드
     */
    public static void main(String[] args) throws Exception {
        ScopedValueInheritance demo = new ScopedValueInheritance();
        
        // 1. 기본 상속 패턴
        System.out.println("1. 기본 ScopedValue 상속 패턴 실행...");
        String result1 = demo.basicScopedValueInheritance().get(5, TimeUnit.SECONDS);
        System.out.println("결과1: " + result1);
        
        Thread.sleep(1000);
        
        // 2. 다중 컨텍스트 상속
        System.out.println("\n2. 다중 컨텍스트 상속 패턴 실행...");
        BusinessResult result2 = demo.multipleContextInheritance().get(10, TimeUnit.SECONDS);
        System.out.println("결과2: " + result2);
        
        Thread.sleep(1000);
        
        // 3. 조건부 상속 - 일반 사용자
        System.out.println("\n3. 조건부 컨텍스트 상속 (일반 사용자)...");
        ProcessingResult result3a = demo.conditionalContextInheritance(false).get(5, TimeUnit.SECONDS);
        System.out.println("결과3a: " + result3a);
        
        // 4. 조건부 상속 - 특권 사용자  
        System.out.println("\n4. 조건부 컨텍스트 상속 (특권 사용자)...");
        ProcessingResult result3b = demo.conditionalContextInheritance(true).get(5, TimeUnit.SECONDS);
        System.out.println("결과3b: " + result3b);
        
        Thread.sleep(1000);
        
        // 5. 컨텍스트 수정 및 전파
        System.out.println("\n5. 컨텍스트 수정 및 전파 패턴 실행...");
        AggregatedResult result4 = demo.contextModificationAndPropagation().get(10, TimeUnit.SECONDS);
        System.out.println("결과4: " + result4);
        
        System.out.println("\n=== ScopedValue 상속 패턴 데모 완료 ===");
    }
}