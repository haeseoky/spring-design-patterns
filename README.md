# 🎯 Spring Boot 디자인 패턴 학습 프로젝트

Spring Boot를 활용한 디자인 패턴 구현 및 학습을 위한 프로젝트입니다.

## 📋 프로젝트 개요

- **프레임워크**: Spring Boot 3.5.4
- **Java 버전**: 21
- **빌드 도구**: Gradle
- **주요 의존성**: WebFlux, Thymeleaf, Lombok, JUnit 5

## 🎨 구현된 디자인 패턴

### 1. 옵저버 패턴 (Observer Pattern)

실시간 뉴스 알림 시스템을 통해 옵저버 패턴을 구현했습니다.

#### 📁 구조
```
src/main/java/com/ocean/pattern/observer/
├── Observer.java              # 옵저버 인터페이스
├── Subject.java              # 서브젝트 인터페이스
├── NewsAgency.java           # 뉴스 발행자 (Subject 구현)
├── NewsChannel.java          # TV 뉴스 채널 (Observer)
├── EmailSubscriber.java      # 이메일 구독자 (Observer)
└── MobileApp.java           # 모바일 앱 (Observer)
```

#### 🔧 주요 특징

- **느슨한 결합**: Subject와 Observer 간 독립적인 관계
- **동적 구독관리**: 런타임에 구독자 추가/제거 가능
- **다양한 알림 방식**: TV 방송, 이메일, 모바일 푸시 알림
- **Thread-Safe**: CopyOnWriteArrayList를 활용한 안전한 동시성 처리

#### 🚀 실행 방법

1. **애플리케이션 시작**
```bash
./gradlew bootRun
```

2. **데모 실행** (기본 구독자들 자동 등록)
```bash
curl -X POST http://localhost:8080/api/observer/demo
```

3. **뉴스 발행**
```bash
curl -X POST http://localhost:8080/api/observer/news \
  -H "Content-Type: application/json" \
  -d '{"news":"긴급 속보: 새로운 기술이 발표되었습니다!"}'
```

4. **구독자 목록 조회**
```bash
curl http://localhost:8080/api/observer/subscribers
```

## 🌐 API 엔드포인트

### 뉴스 관리
- `POST /api/observer/news` - 뉴스 발행
- `GET /api/observer/subscribers` - 구독자 목록 조회

### 구독자 관리
- `POST /api/observer/subscribe/channel` - 뉴스 채널 구독
- `POST /api/observer/subscribe/email` - 이메일 구독
- `POST /api/observer/subscribe/mobile` - 모바일 앱 구독
- `DELETE /api/observer/unsubscribe/{observerId}` - 구독 해지

### 유틸리티
- `POST /api/observer/demo` - 데모 실행
- `DELETE /api/observer/reset` - 모든 구독자 제거

## 📝 사용 예시

### 뉴스 채널 구독
```bash
curl -X POST http://localhost:8080/api/observer/subscribe/channel \
  -H "Content-Type: application/json" \
  -d '{"channelName":"MBC 뉴스"}'
```

### 이메일 구독자 등록
```bash
curl -X POST http://localhost:8080/api/observer/subscribe/email \
  -H "Content-Type: application/json" \
  -d '{"name":"홍길동", "email":"hong@example.com"}'
```

### 모바일 앱 등록
```bash
curl -X POST http://localhost:8080/api/observer/subscribe/mobile \
  -H "Content-Type: application/json" \
  -d '{"appName":"뉴스알리미", "deviceId":"android123"}'
```

## 🧪 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 옵저버 패턴 테스트만 실행
./gradlew test --tests "*ObserverPattern*"
```

### 테스트 커버리지
- ✅ 옵저버 등록/제거 테스트
- ✅ 뉴스 발행 및 알림 전송 테스트
- ✅ 각 구독자 타입별 기능 테스트
- ✅ 예외 상황 처리 테스트
- ✅ REST API 컨트롤러 테스트

## 🎯 학습 포인트

### 옵저버 패턴의 장점
1. **확장성**: 새로운 Observer 타입을 쉽게 추가 가능
2. **재사용성**: Subject 하나를 여러 Observer가 공유
3. **유지보수성**: Subject와 Observer가 독립적으로 변경 가능
4. **실시간 반응**: 상태 변화에 즉시 반응하는 시스템 구축

### 실제 활용 사례
- 이벤트 기반 시스템 (Event-Driven Architecture)
- 모델-뷰 아키텍처 (MVC Pattern)
- 실시간 알림 시스템
- UI 상태 관리 (React, Vue 등)

### 2. 전략 패턴 (Strategy Pattern)

다양한 결제 방식을 통해 전략 패턴을 구현했습니다.

#### 📁 구조
```
src/main/java/com/ocean/pattern/strategy/
├── PaymentStrategy.java         # 결제 전략 인터페이스
├── PaymentResult.java          # 결제 결과 클래스
├── PaymentProcessor.java       # Context 클래스 (결제 처리기)
├── CreditCardPayment.java      # 신용카드 결제 전략
├── CashPayment.java           # 현금 결제 전략
├── PayPalPayment.java         # PayPal 결제 전략
└── BankTransferPayment.java   # 계좌이체 결제 전략
```

#### 🔧 주요 특징

- **유연한 알고리즘 교체**: 런타임에 결제 방식 변경 가능
- **개방-폐쇄 원칙**: 새로운 결제 방식 추가 시 기존 코드 수정 없음
- **단일 책임 원칙**: 각 결제 방식이 독립적인 로직 처리
- **다양한 수수료 정책**: 결제 방식별 차별화된 수수료 체계

#### 🚀 실행 방법

1. **신용카드 결제**
```bash
curl -X POST http://localhost:8080/api/strategy/payment/creditcard \
  -H "Content-Type: application/json" \
  -d '{
    "amount": "50000",
    "cardNumber": "1234-5678-9012-3456",
    "holderName": "김철수",
    "cvv": "123",
    "expiryDate": "12/25",
    "creditLimit": "1000000"
  }'
```

2. **현금 결제**
```bash
curl -X POST http://localhost:8080/api/strategy/payment/cash \
  -H "Content-Type: application/json" \
  -d '{
    "amount": "30000",
    "availableCash": "500000"
  }'
```

3. **PayPal 결제**
```bash
curl -X POST http://localhost:8080/api/strategy/payment/paypal \
  -H "Content-Type: application/json" \
  -d '{
    "amount": "40000",
    "email": "user@example.com",
    "password": "password123",
    "balance": "300000",
    "isVerified": "true"
  }'
```

4. **계좌이체 결제**
```bash
curl -X POST http://localhost:8080/api/strategy/payment/banktransfer \
  -H "Content-Type: application/json" \
  -d '{
    "amount": "80000",
    "bankName": "국민은행",
    "accountNumber": "123-456-789012",
    "accountHolder": "이영희",
    "balance": "2000000",
    "pin": "123456"
  }'
```

5. **전략 패턴 데모 실행**
```bash
curl -X POST http://localhost:8080/api/strategy/demo
```

#### 💳 결제 방식별 특징

| 결제 방식 | 수수료 | 처리 시간 | 특징 |
|----------|--------|----------|------|
| 신용카드 | 2.5% | 1초 | 신용한도 확인, 카드 유효성 검증 |
| 현금 | 0% | 즉시 | 수수료 없음, 보유 현금 확인 |
| PayPal | 3.4% + 35원 | 1.5초 | 계정 인증 필요, 온라인 결제 |
| 계좌이체 | 1,000원 고정 | 2초 | PIN 인증, 일일 한도 5백만원 |

## 🌐 API 엔드포인트

### 옵저버 패턴 - 뉴스 관리
- `POST /api/observer/news` - 뉴스 발행
- `GET /api/observer/subscribers` - 구독자 목록 조회
- `POST /api/observer/subscribe/channel` - 뉴스 채널 구독
- `POST /api/observer/subscribe/email` - 이메일 구독
- `POST /api/observer/subscribe/mobile` - 모바일 앱 구독
- `DELETE /api/observer/unsubscribe/{observerId}` - 구독 해지
- `POST /api/observer/demo` - 데모 실행
- `DELETE /api/observer/reset` - 모든 구독자 제거

### 전략 패턴 - 결제 관리
- `POST /api/strategy/payment/creditcard` - 신용카드 결제
- `POST /api/strategy/payment/cash` - 현금 결제
- `POST /api/strategy/payment/paypal` - PayPal 결제
- `POST /api/strategy/payment/banktransfer` - 계좌이체 결제
- `POST /api/strategy/calculate-fee` - 수수료 계산
- `GET /api/strategy/payment-history` - 결제 내역 조회
- `GET /api/strategy/statistics` - 결제 통계 조회
- `DELETE /api/strategy/payment-history` - 결제 내역 초기화
- `POST /api/strategy/demo` - 전략 패턴 데모 실행

## 🧪 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 특정 패턴 테스트 실행
./gradlew test --tests "*ObserverPattern*"
./gradlew test --tests "*StrategyPattern*"
./gradlew test --tests "*StructuredConcurrency*"
```

### 테스트 커버리지
- ✅ **옵저버 패턴**: 등록/제거, 알림 전송, 구독자별 기능, 예외 처리, REST API
- ✅ **전략 패턴**: 결제 전략별 로직, 수수료 계산, 결제 내역 관리, REST API
- ✅ **StructuredTaskScope 패턴**: 
  - 기본 스코프 생명주기 (open/fork/join/close)
  - 내장 조이너 (allSuccessful, anySuccessful, awaitAll, allUntil)
  - 커스텀 조이너 (품질 기반, 다수결, 리소스 제약 등)
  - Subtask 상태 관리 (UNAVAILABLE → SUCCESS/FAILED/CANCELLED)
  - 스코프 값 상속 (보안, 트랜잭션, 로깅 컨텍스트)
  - 타임아웃 및 취소 처리 (글로벌/개별, 조건부 취소)
  - 고급 패턴 (적응형 로드밸런싱, 회로차단기, 계층적 스코프)
  - 실전 시나리오 (E-Commerce, 데이터파이프라인, 마이크로서비스, 장애복구)

## 🎯 학습 포인트

### 옵저버 패턴의 장점
1. **확장성**: 새로운 Observer 타입을 쉽게 추가 가능
2. **재사용성**: Subject 하나를 여러 Observer가 공유
3. **유지보수성**: Subject와 Observer가 독립적으로 변경 가능
4. **실시간 반응**: 상태 변화에 즉시 반응하는 시스템 구축

### 전략 패턴의 장점
1. **유연성**: 런타임에 알고리즘 교체 가능
2. **확장성**: 새로운 전략 추가 시 기존 코드 수정 불필요
3. **단일 책임**: 각 전략이 독립적인 책임을 가짐
4. **테스트 용이성**: 각 전략을 독립적으로 테스트 가능

### StructuredTaskScope 패턴의 장점
1. **구조화된 동시성**: 명확한 태스크 생명주기로 복잡성 감소
2. **자동 리소스 관리**: try-with-resources를 통한 안전한 정리
3. **가상 스레드 최적화**: 경량 스레드로 높은 처리량과 낮은 메모리 사용
4. **실시간 제어**: 타임아웃, 취소, 조건부 완료 지원
5. **컨텍스트 전파**: ScopedValue를 통한 자동 메타데이터 상속
6. **프로덕션 견고성**: 장애 복구, 회로차단기, 적응형 로드밸런싱

### 실제 활용 사례
- **옵저버 패턴**: 이벤트 기반 시스템, MVC 아키텍처, 실시간 알림, UI 상태 관리
- **전략 패턴**: 결제 시스템, 정렬 알고리즘, 압축 알고리즘, 할인 정책
- **StructuredTaskScope 패턴**: 마이크로서비스 오케스트레이션, 대용량 데이터 처리, E-Commerce 주문 파이프라인, 실시간 집계 시스템, 병렬 API 호출, IoT 데이터 수집

## 🔧 기술 스택

- **Backend**: Spring Boot 3.5.4, Spring WebFlux
- **Testing**: JUnit 5, Mockito
- **Build**: Gradle 8.x
- **Java**: 21
- **Logging**: SLF4J + Logback

### 3. StructuredTaskScope 패턴 (JDK 25 Preview)

JEP 505: Structured Concurrency를 기반으로 한 고급 동시성 패턴을 구현했습니다.

#### 📁 구조
```
src/main/java/com/ocean/pattern/structured/
├── package-info.java                    # 전체 패키지 문서화
├── TaskScopeExample.java               # 기본 사용 예제
├── ErrorHandlingExample.java           # 에러 처리 패턴
├── ParallelDataProcessing.java         # 병렬 데이터 처리
├── WebServiceAggregation.java          # 웹서비스 집계
├── CustomJoinerExample.java            # 커스텀 조이너
├── StructuredConcurrencyDemo.java      # 종합 데모
└── task/                               # 심층 분석 패키지
    ├── package-info.java               # Task 패키지 문서
    ├── BasicScopeOperations.java       # 기본 스코프 운영
    ├── BuiltInJoinersExample.java      # 내장 조이너
    ├── CustomJoinerImplementation.java # 고급 커스텀 조이너
    ├── SubtaskLifecycleExample.java    # Subtask 생명주기
    ├── ScopedValueInheritance.java     # 스코프 값 상속
    ├── TimeoutAndCancellation.java     # 타임아웃 & 취소
    ├── AdvancedPatterns.java           # 고급 패턴
    └── TaskScopeIntegrationTest.java   # 통합 테스트
```

#### 🔧 주요 특징

- **구조화된 동시성**: 명확한 태스크 생명주기 관리 (open → fork → join → close)
- **가상 스레드 활용**: Project Loom의 가상 스레드 최적 활용
- **다양한 조이너 패턴**: allSuccessful, anySuccessful, awaitAll, 커스텀 조이너
- **컨텍스트 전파**: ScopedValue를 통한 보안 컨텍스트 및 메타데이터 상속
- **견고성 패턴**: 타임아웃, 취소, 장애 복구 메커니즘
- **고급 최적화**: 적응형 로드밸런싱, 계층적 스코프, 회로차단기 패턴

#### 🚀 실행 방법

1. **기본 데모 실행**
```bash
# 기본 TaskScope 예제
java com.ocean.pattern.structured.TaskScopeExample

# 에러 처리 예제
java com.ocean.pattern.structured.ErrorHandlingExample

# 병렬 데이터 처리
java com.ocean.pattern.structured.ParallelDataProcessing

# 웹서비스 집계
java com.ocean.pattern.structured.WebServiceAggregation
```

2. **심층 분석 예제 실행**
```bash
# 기본 스코프 운영
java com.ocean.pattern.structured.task.BasicScopeOperations

# 내장 조이너 패턴
java com.ocean.pattern.structured.task.BuiltInJoinersExample

# 커스텀 조이너 구현
java com.ocean.pattern.structured.task.CustomJoinerImplementation

# Subtask 생명주기 관리
java com.ocean.pattern.structured.task.SubtaskLifecycleExample

# 스코프 값 상속
java com.ocean.pattern.structured.task.ScopedValueInheritance

# 타임아웃 및 취소 처리
java com.ocean.pattern.structured.task.TimeoutAndCancellation

# 고급 패턴 (프로덕션 레벨)
java com.ocean.pattern.structured.task.AdvancedPatterns
```

3. **통합 테스트 실행**
```bash
# 전체 통합 테스트 스위트
java com.ocean.pattern.structured.task.TaskScopeIntegrationTest

# 특정 시나리오 테스트
./gradlew test --tests "*StructuredConcurrency*"
```

#### ⚡ 패턴별 특징

| 패턴 | 설명 | 사용 사례 |
|------|------|-----------|
| **BasicScope** | 기본 open/fork/join/close 생명주기 | 간단한 병렬 작업 관리 |
| **BuiltInJoiners** | allSuccessful, anySuccessful 등 내장 조이너 | 표준 완료 조건 처리 |
| **CustomJoiner** | 비즈니스 로직 기반 커스텀 조이너 | 복잡한 완료 조건 (품질, 다수결 등) |
| **SubtaskLifecycle** | Subtask 상태 관리 및 생명주기 | 세밀한 태스크 제어 |
| **ScopedValue** | 스레드 간 컨텍스트 전파 | 보안, 트랜잭션, 로깅 컨텍스트 |
| **Timeout/Cancel** | 타임아웃 및 취소 전략 | 안정적인 리소스 관리 |
| **AdvancedPatterns** | 적응형 밸런싱, 회로차단기 | 프로덕션 환경 최적화 |

#### 🎯 실전 시나리오 (통합 테스트)

1. **E-Commerce 주문 처리**
   - 사용자 인증, 재고 확인, 결제 처리, 배송 준비 병렬 수행
   - 타임아웃 처리 및 부분 실패 허용

2. **대용량 데이터 파이프라인**
   - 1000건 데이터의 적응형 배치 처리
   - 복잡도 기반 로드 밸런싱

3. **마이크로서비스 오케스트레이션**
   - 계층적 서비스 호출 (인증 → 비즈니스 → 집계)
   - 서비스별 타임아웃 및 회로차단기

4. **장애 복구 및 복원력**
   - 부분 실패 시나리오, 타임아웃 복구
   - 리소스 부족, 연쇄 실패 방지

#### 📊 성능 특징

- **가상 스레드 활용**: 수백만 개의 경량 스레드 지원
- **메모리 효율성**: 기존 플랫폼 스레드 대비 1/1000 메모리 사용
- **구조화된 리소스 관리**: 자동 정리를 통한 메모리 누수 방지
- **성능 모니터링**: 실시간 메트릭 수집 및 분석

## 📚 추가 예정 패턴

- [ ] 팩토리 패턴 (Factory Pattern)
- [ ] 싱글톤 패턴 (Singleton Pattern)
- [x] ~~전략 패턴 (Strategy Pattern)~~ ✅ 완료
- [x] ~~StructuredTaskScope 패턴 (JDK 25 Preview)~~ ✅ 완료
- [ ] 데코레이터 패턴 (Decorator Pattern)
- [ ] 어댑터 패턴 (Adapter Pattern)
- [ ] CQRS 패턴 (Command Query Responsibility Segregation)
- [ ] 이벤트 소싱 패턴 (Event Sourcing)
- [ ] 사가 패턴 (Saga Pattern)

## 🤝 기여하기

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이센스

이 프로젝트는 학습 목적으로 작성되었습니다.

---

**Happy Learning! 🚀**