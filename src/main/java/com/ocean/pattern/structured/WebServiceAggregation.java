package com.ocean.pattern.structured;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Structured Concurrency를 활용한 웹서비스 집계 패턴
 * 
 * 이 클래스는 마이크로서비스 환경에서 여러 서비스를 동시 호출하여
 * 결과를 집계하는 실용적인 패턴을 보여줍니다.
 * 
 * 주요 학습 목표:
 * 1. 마이크로서비스 집계 패턴
 * 2. API 응답 시간 최적화
 * 3. 부분 실패 허용 전략
 * 4. 캐시와 폴백 메커니즘
 */
public class WebServiceAggregation {
    
    private static final Logger logger = Logger.getLogger(WebServiceAggregation.class.getName());
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, String> cache;
    
    public WebServiceAggregation() {
        this.httpClient = HttpClient.newBuilder()
                                   .connectTimeout(Duration.ofSeconds(5))
                                   .build();
        this.objectMapper = new ObjectMapper();
        this.cache = new ConcurrentHashMap<>();
    }
    
    /**
     * JDK 25 Structured Concurrency를 사용한 서비스 집계 (이론적 구현)
     * 
     * try (var scope = StructuredTaskScope.open()) {
     *     Subtask<UserInfo> userTask = scope.fork(() -> fetchUserInfo(userId));
     *     Subtask<List<Order>> orderTask = scope.fork(() -> fetchUserOrders(userId));
     *     Subtask<PaymentInfo> paymentTask = scope.fork(() -> fetchPaymentInfo(userId));
     *     
     *     scope.join();
     *     
     *     return new UserDashboard(
     *         userTask.get(),
     *         orderTask.get(),
     *         paymentTask.get()
     *     );
     * }
     */
    
    /**
     * 사용자 대시보드 데이터 집계
     * 여러 마이크로서비스에서 사용자 관련 정보를 동시에 조회
     */
    public UserDashboard aggregateUserDashboard(String userId) throws InterruptedException, ExecutionException {
        logger.info("사용자 대시보드 집계 시작: " + userId);
        Instant startTime = Instant.now();
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // 병렬로 여러 서비스 호출
            Future<UserInfo> userTask = executor.submit(() -> fetchUserInfo(userId));
            Future<List<Order>> ordersTask = executor.submit(() -> fetchUserOrders(userId));
            Future<PaymentInfo> paymentTask = executor.submit(() -> fetchPaymentInfo(userId));
            Future<List<Recommendation>> recommendationsTask = executor.submit(() -> fetchRecommendations(userId));
            Future<NotificationSummary> notificationsTask = executor.submit(() -> fetchNotificationSummary(userId));
            
            // 모든 결과 수집
            UserInfo userInfo = userTask.get();
            List<Order> orders = ordersTask.get();
            PaymentInfo paymentInfo = paymentTask.get();
            List<Recommendation> recommendations = recommendationsTask.get();
            NotificationSummary notifications = notificationsTask.get();
            
            UserDashboard dashboard = new UserDashboard(
                userInfo, orders, paymentInfo, recommendations, notifications,
                Duration.between(startTime, Instant.now())
            );
            
            logger.info("사용자 대시보드 집계 완료: " + dashboard.getLoadTimeMs() + "ms");
            return dashboard;
        }
    }
    
    /**
     * 부분 실패를 허용하는 대시보드 집계
     * 일부 서비스가 실패해도 사용 가능한 데이터로 대시보드 구성
     */
    public UserDashboard aggregateUserDashboardWithFallback(String userId, Duration timeout) throws InterruptedException {
        logger.info("폴백 기능이 있는 사용자 대시보드 집계 시작: " + userId);
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // 각 서비스별로 개별 타임아웃과 폴백 적용
            CompletableFuture<UserInfo> userTask = CompletableFuture.supplyAsync(() -> {
                try {
                    return fetchUserInfoWithFallback(userId);
                } catch (Exception e) {
                    logger.warning("사용자 정보 조회 실패, 폴백 사용: " + e.getMessage());
                    return createFallbackUserInfo(userId);
                }
            }, executor);
            
            CompletableFuture<List<Order>> ordersTask = CompletableFuture.supplyAsync(() -> {
                try {
                    return fetchUserOrdersWithFallback(userId);
                } catch (Exception e) {
                    logger.warning("주문 정보 조회 실패, 빈 목록 반환: " + e.getMessage());
                    return new ArrayList<>();
                }
            }, executor);
            
            CompletableFuture<PaymentInfo> paymentTask = CompletableFuture.supplyAsync(() -> {
                try {
                    return fetchPaymentInfoWithFallback(userId);
                } catch (Exception e) {
                    logger.warning("결제 정보 조회 실패, 폴백 사용: " + e.getMessage());
                    return createFallbackPaymentInfo(userId);
                }
            }, executor);
            
            CompletableFuture<List<Recommendation>> recommendationsTask = CompletableFuture.supplyAsync(() -> {
                try {
                    return fetchRecommendationsWithFallback(userId);
                } catch (Exception e) {
                    logger.warning("추천 정보 조회 실패, 빈 목록 반환: " + e.getMessage());
                    return new ArrayList<>();
                }
            }, executor);
            
            CompletableFuture<NotificationSummary> notificationsTask = CompletableFuture.supplyAsync(() -> {
                try {
                    return fetchNotificationSummaryWithFallback(userId);
                } catch (Exception e) {
                    logger.warning("알림 정보 조회 실패, 폴백 사용: " + e.getMessage());
                    return createFallbackNotificationSummary();
                }
            }, executor);
            
            // 전체 타임아웃 적용하여 결과 수집
            CompletableFuture<UserDashboard> combinedTask = CompletableFuture.allOf(
                userTask, ordersTask, paymentTask, recommendationsTask, notificationsTask
            ).thenApply(ignored -> {
                try {
                    return new UserDashboard(
                        userTask.join(),
                        ordersTask.join(),
                        paymentTask.join(),
                        recommendationsTask.join(),
                        notificationsTask.join(),
                        Duration.ofMillis(0) // 실제로는 측정된 시간
                    );
                } catch (Exception e) {
                    logger.severe("대시보드 조합 중 오류: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            });
            
            try {
                UserDashboard dashboard = combinedTask.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                logger.info("폴백 대시보드 집계 완료");
                return dashboard;
            } catch (TimeoutException e) {
                logger.warning("대시보드 집계 타임아웃, 부분 결과 사용");
                
                // 타임아웃 시 완료된 결과만 사용
                return new UserDashboard(
                    userTask.isDone() ? userTask.join() : createFallbackUserInfo(userId),
                    ordersTask.isDone() ? ordersTask.join() : new ArrayList<>(),
                    paymentTask.isDone() ? paymentTask.join() : createFallbackPaymentInfo(userId),
                    recommendationsTask.isDone() ? recommendationsTask.join() : new ArrayList<>(),
                    notificationsTask.isDone() ? notificationsTask.join() : createFallbackNotificationSummary(),
                    timeout
                );
            } catch (ExecutionException e) {
                throw new RuntimeException(e.getCause());
            }
        }
    }
    
    /**
     * 제품 상세 정보 집계
     * 제품 정보, 리뷰, 재고, 가격 등을 동시에 조회
     */
    public ProductDetails aggregateProductDetails(String productId) throws InterruptedException, ExecutionException {
        logger.info("제품 상세 정보 집계: " + productId);
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // 제품 관련 여러 정보를 병렬로 조회
            Future<ProductInfo> productTask = executor.submit(() -> fetchProductInfo(productId));
            Future<List<Review>> reviewsTask = executor.submit(() -> fetchProductReviews(productId));
            Future<InventoryInfo> inventoryTask = executor.submit(() -> fetchInventoryInfo(productId));
            Future<PricingInfo> pricingTask = executor.submit(() -> fetchPricingInfo(productId));
            Future<List<ProductInfo>> relatedTask = executor.submit(() -> fetchRelatedProducts(productId));
            
            // 결과 수집
            ProductInfo product = productTask.get();
            List<Review> reviews = reviewsTask.get();
            InventoryInfo inventory = inventoryTask.get();
            PricingInfo pricing = pricingTask.get();
            List<ProductInfo> relatedProducts = relatedTask.get();
            
            ProductDetails details = new ProductDetails(
                product, reviews, inventory, pricing, relatedProducts
            );
            
            logger.info("제품 상세 정보 집계 완료");
            return details;
        }
    }
    
    /**
     * 캐시를 활용한 고성능 집계
     * 자주 요청되는 데이터는 캐시에서 조회하여 성능 최적화
     */
    public UserDashboard aggregateWithCache(String userId, boolean forceRefresh) throws InterruptedException, ExecutionException {
        logger.info("캐시 활용 집계: " + userId + ", 강제 갱신: " + forceRefresh);
        
        String cacheKey = "dashboard:" + userId;
        
        // 캐시 확인 (강제 갱신이 아닌 경우)
        if (!forceRefresh && cache.containsKey(cacheKey)) {
            logger.info("캐시에서 대시보드 반환");
            // 실제로는 JSON 역직렬화
            return createFallbackDashboard(userId); 
        }
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // 병렬로 데이터 조회
            List<Future<Object>> futures = new ArrayList<>();
            
            futures.add(executor.submit(() -> (Object) fetchUserInfo(userId)));
            futures.add(executor.submit(() -> (Object) fetchUserOrders(userId)));
            futures.add(executor.submit(() -> (Object) fetchPaymentInfo(userId)));
            futures.add(executor.submit(() -> (Object) fetchRecommendations(userId)));
            futures.add(executor.submit(() -> (Object) fetchNotificationSummary(userId)));
            
            // 결과 수집
            UserInfo userInfo = (UserInfo) futures.get(0).get();
            @SuppressWarnings("unchecked")
            List<Order> orders = (List<Order>) futures.get(1).get();
            PaymentInfo paymentInfo = (PaymentInfo) futures.get(2).get();
            @SuppressWarnings("unchecked")
            List<Recommendation> recommendations = (List<Recommendation>) futures.get(3).get();
            NotificationSummary notifications = (NotificationSummary) futures.get(4).get();
            
            UserDashboard dashboard = new UserDashboard(
                userInfo, orders, paymentInfo, recommendations, notifications,
                Duration.ofMillis(0)
            );
            
            // 캐시에 저장 (실제로는 JSON 직렬화)
            cache.put(cacheKey, "cached_dashboard_" + userId);
            logger.info("대시보드를 캐시에 저장");
            
            return dashboard;
        }
    }
    
    /**
     * 배치 처리를 통한 다중 사용자 집계
     */
    public Map<String, UserDashboard> aggregateMultipleUsers(List<String> userIds, int batchSize) throws InterruptedException, ExecutionException {
        logger.info("다중 사용자 대시보드 집계: " + userIds.size() + "명, 배치 크기: " + batchSize);
        
        Map<String, UserDashboard> results = new ConcurrentHashMap<>();
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            List<Future<Void>> batchTasks = new ArrayList<>();
            
            // 사용자 ID를 배치로 분할하여 처리
            for (int i = 0; i < userIds.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, userIds.size());
                List<String> batch = userIds.subList(i, endIndex);
                final int batchIndex = i / batchSize;
                
                Future<Void> batchTask = executor.submit(() -> {
                    logger.info("배치 " + batchIndex + " 처리 시작: " + batch.size() + "명");
                    
                    List<Future<Map.Entry<String, UserDashboard>>> userTasks = new ArrayList<>();
                    
                    // 배치 내 각 사용자를 병렬 처리
                    for (String userId : batch) {
                        userTasks.add(executor.submit(() -> {
                            try {
                                UserDashboard dashboard = aggregateUserDashboardWithFallback(userId, Duration.ofSeconds(5));
                                return Map.entry(userId, dashboard);
                            } catch (Exception e) {
                                logger.warning("사용자 " + userId + " 처리 실패: " + e.getMessage());
                                return Map.entry(userId, createFallbackDashboard(userId));
                            }
                        }));
                    }
                    
                    // 배치 결과 수집
                    for (Future<Map.Entry<String, UserDashboard>> userTask : userTasks) {
                        try {
                            Map.Entry<String, UserDashboard> entry = userTask.get();
                            results.put(entry.getKey(), entry.getValue());
                        } catch (Exception e) {
                            logger.severe("배치 " + batchIndex + " 내 사용자 처리 오류: " + e.getMessage());
                        }
                    }
                    
                    logger.info("배치 " + batchIndex + " 처리 완료");
                    return null;
                });
                
                batchTasks.add(batchTask);
            }
            
            // 모든 배치 완료 대기
            for (Future<Void> batchTask : batchTasks) {
                batchTask.get();
            }
            
            logger.info("다중 사용자 집계 완료: " + results.size() + "개 결과");
            return results;
        }
    }
    
    // 실제 서비스 호출 시뮬레이션 메서드들
    
    private UserInfo fetchUserInfo(String userId) {
        simulateNetworkCall(100, 300);
        return new UserInfo(userId, "사용자 " + userId, "user" + userId + "@example.com", "활성");
    }
    
    private UserInfo fetchUserInfoWithFallback(String userId) {
        try {
            return fetchUserInfo(userId);
        } catch (Exception e) {
            return createFallbackUserInfo(userId);
        }
    }
    
    private List<Order> fetchUserOrders(String userId) {
        simulateNetworkCall(150, 400);
        return List.of(
            new Order("ORD001", userId, "주문 1", 50000, "배송중"),
            new Order("ORD002", userId, "주문 2", 30000, "완료")
        );
    }
    
    private List<Order> fetchUserOrdersWithFallback(String userId) {
        try {
            return fetchUserOrders(userId);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    private PaymentInfo fetchPaymentInfo(String userId) {
        simulateNetworkCall(80, 200);
        return new PaymentInfo(userId, 80000, "원", "카드", true);
    }
    
    private PaymentInfo fetchPaymentInfoWithFallback(String userId) {
        try {
            return fetchPaymentInfo(userId);
        } catch (Exception e) {
            return createFallbackPaymentInfo(userId);
        }
    }
    
    private List<Recommendation> fetchRecommendations(String userId) {
        simulateNetworkCall(200, 500);
        return List.of(
            new Recommendation("REC001", "추천 상품 1", 4.5),
            new Recommendation("REC002", "추천 상품 2", 4.2)
        );
    }
    
    private List<Recommendation> fetchRecommendationsWithFallback(String userId) {
        try {
            return fetchRecommendations(userId);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    private NotificationSummary fetchNotificationSummary(String userId) {
        simulateNetworkCall(50, 150);
        return new NotificationSummary(userId, 5, 2, 1);
    }
    
    private NotificationSummary fetchNotificationSummaryWithFallback(String userId) {
        try {
            return fetchNotificationSummary(userId);
        } catch (Exception e) {
            return createFallbackNotificationSummary();
        }
    }
    
    private ProductInfo fetchProductInfo(String productId) {
        simulateNetworkCall(100, 250);
        return new ProductInfo(productId, "제품 " + productId, "제품 설명", 25000);
    }
    
    private List<Review> fetchProductReviews(String productId) {
        simulateNetworkCall(200, 400);
        return List.of(
            new Review("REV001", productId, 5, "훌륭합니다!", "user1"),
            new Review("REV002", productId, 4, "좋아요", "user2")
        );
    }
    
    private InventoryInfo fetchInventoryInfo(String productId) {
        simulateNetworkCall(80, 180);
        return new InventoryInfo(productId, 100, "서울창고");
    }
    
    private PricingInfo fetchPricingInfo(String productId) {
        simulateNetworkCall(60, 120);
        return new PricingInfo(productId, 25000, 20000, true);
    }
    
    private List<ProductInfo> fetchRelatedProducts(String productId) {
        simulateNetworkCall(150, 300);
        return List.of(
            new ProductInfo("REL001", "관련 제품 1", "설명", 30000),
            new ProductInfo("REL002", "관련 제품 2", "설명", 35000)
        );
    }
    
    // 폴백 데이터 생성 메서드들
    
    private UserInfo createFallbackUserInfo(String userId) {
        return new UserInfo(userId, "사용자", "unknown@example.com", "알 수 없음");
    }
    
    private PaymentInfo createFallbackPaymentInfo(String userId) {
        return new PaymentInfo(userId, 0, "원", "알 수 없음", false);
    }
    
    private NotificationSummary createFallbackNotificationSummary() {
        return new NotificationSummary("unknown", 0, 0, 0);
    }
    
    private UserDashboard createFallbackDashboard(String userId) {
        return new UserDashboard(
            createFallbackUserInfo(userId),
            new ArrayList<>(),
            createFallbackPaymentInfo(userId),
            new ArrayList<>(),
            createFallbackNotificationSummary(),
            Duration.ofMillis(0)
        );
    }
    
    private void simulateNetworkCall(int minMs, int maxMs) {
        try {
            int delay = minMs + (int)(Math.random() * (maxMs - minMs));
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
    
    // 데이터 클래스들
    
    public record UserInfo(String userId, String name, String email, String status) {}
    
    public record Order(String orderId, String userId, String productName, int amount, String status) {}
    
    public record PaymentInfo(String userId, int totalAmount, String currency, String paymentMethod, boolean isActive) {}
    
    public record Recommendation(String id, String title, double rating) {}
    
    public record NotificationSummary(String userId, int totalCount, int unreadCount, int urgentCount) {}
    
    public record ProductInfo(String productId, String name, String description, int price) {}
    
    public record Review(String reviewId, String productId, int rating, String comment, String userId) {}
    
    public record InventoryInfo(String productId, int stock, String warehouse) {}
    
    public record PricingInfo(String productId, int originalPrice, int salePrice, boolean onSale) {}
    
    public record UserDashboard(
        UserInfo userInfo,
        List<Order> orders,
        PaymentInfo paymentInfo,
        List<Recommendation> recommendations,
        NotificationSummary notifications,
        Duration loadTime
    ) {
        public long getLoadTimeMs() {
            return loadTime.toMillis();
        }
    }
    
    public record ProductDetails(
        ProductInfo product,
        List<Review> reviews,
        InventoryInfo inventory,
        PricingInfo pricing,
        List<ProductInfo> relatedProducts
    ) {}
    
    // 실행 예제
    public static void main(String[] args) {
        WebServiceAggregation aggregator = new WebServiceAggregation();
        
        try {
            // 기본 사용자 대시보드 집계
            System.out.println("=== 기본 대시보드 집계 ===");
            UserDashboard dashboard = aggregator.aggregateUserDashboard("user123");
            System.out.println("사용자: " + dashboard.userInfo().name());
            System.out.println("주문 개수: " + dashboard.orders().size());
            System.out.println("로드 시간: " + dashboard.getLoadTimeMs() + "ms");
            
            // 폴백이 있는 집계
            System.out.println("\n=== 폴백 대시보드 집계 ===");
            UserDashboard fallbackDashboard = aggregator.aggregateUserDashboardWithFallback(
                "user456", Duration.ofSeconds(2)
            );
            System.out.println("폴백 대시보드 로드 완료");
            
            // 제품 상세 정보 집계
            System.out.println("\n=== 제품 상세 정보 집계 ===");
            ProductDetails product = aggregator.aggregateProductDetails("PROD001");
            System.out.println("제품명: " + product.product().name());
            System.out.println("리뷰 개수: " + product.reviews().size());
            System.out.println("재고: " + product.inventory().stock());
            
            // 다중 사용자 집계
            System.out.println("\n=== 다중 사용자 집계 ===");
            List<String> userIds = List.of("user1", "user2", "user3", "user4", "user5");
            Map<String, UserDashboard> multiResults = aggregator.aggregateMultipleUsers(userIds, 2);
            System.out.println("집계된 사용자 수: " + multiResults.size());
            
        } catch (Exception e) {
            System.err.println("집계 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}