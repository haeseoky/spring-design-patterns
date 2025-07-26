package com.ocean.pattern.observer;

/**
 * 옵저버 인터페이스 - Observer Pattern의 핵심 인터페이스
 * 
 * <p>상태 변화를 통지받을 객체들이 구현해야 하는 인터페이스입니다.
 * Observer Pattern을 통해 Subject의 상태 변화를 자동으로 감지하고
 * 적절한 반응을 수행할 수 있습니다.</p>
 * 
 * <h3>구현 클래스:</h3>
 * <ul>
 *   <li>{@link NewsChannel} - TV 뉴스 채널</li>
 *   <li>{@link EmailSubscriber} - 이메일 구독자</li>
 *   <li>{@link MobileApp} - 모바일 앱 알림</li>
 * </ul>
 * 
 * <h3>사용 예시:</h3>
 * <pre>{@code
 * NewsAgency agency = new NewsAgency();
 * Observer newsChannel = new NewsChannel("KBS 뉴스");
 * Observer emailSubscriber = new EmailSubscriber("user@example.com");
 * 
 * agency.addObserver(newsChannel);
 * agency.addObserver(emailSubscriber);
 * agency.publishNews("새로운 뉴스입니다!");
 * }</pre>
 * 
 * <h3>패턴의 장점:</h3>
 * <ul>
 *   <li>느슨한 결합: Subject와 Observer 간의 의존성 최소화</li>
 *   <li>동적 관계: 런타임에 Observer 추가/제거 가능</li>
 *   <li>확장성: 새로운 Observer 타입 쉽게 추가</li>
 *   <li>일대다 통신: 하나의 Subject가 여러 Observer에게 알림</li>
 * </ul>
 * 
 * @author Ocean Pattern Team
 * @version 1.0
 * @since 1.0
 * @see Subject
 * @see NewsAgency
 */
public interface Observer {
    
    /**
     * Subject로부터 알림을 받을 때 호출되는 메서드
     * 
     * <p>Subject의 상태가 변경되었을 때 자동으로 호출됩니다.
     * 각 Observer는 이 메서드를 구현하여 변경된 상태에 대한
     * 적절한 반응을 정의해야 합니다.</p>
     * 
     * <h4>구현 시 고려사항:</h4>
     * <ul>
     *   <li>메서드 실행 시간을 최소화하여 다른 Observer의 알림을 지연시키지 않음</li>
     *   <li>예외 발생 시 적절한 예외 처리로 전체 알림 체인이 중단되지 않도록 함</li>
     *   <li>필요시 비동기 처리를 통해 성능 최적화</li>
     * </ul>
     * 
     * @param message Subject로부터 전달받은 메시지 (null이 아님)
     * @throws RuntimeException 메시지 처리 중 오류가 발생한 경우
     */
    void update(String message);
    
    /**
     * 옵저버의 고유 이름을 반환
     * 
     * <p>Observer를 식별하기 위한 고유한 이름을 반환합니다.
     * 로깅, 디버깅, 관리 목적으로 사용됩니다.</p>
     * 
     * <h4>이름 규칙:</h4>
     * <ul>
     *   <li>Observer 타입과 식별자를 포함하는 것을 권장</li>
     *   <li>예: "NewsChannel-KBS", "EmailSubscriber-user@example.com"</li>
     *   <li>같은 타입의 Observer라도 고유한 이름을 가져야 함</li>
     * </ul>
     * 
     * @return Observer의 고유 이름 (null이거나 빈 문자열이 아님)
     */
    String getName();
}