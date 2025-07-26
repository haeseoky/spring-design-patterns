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
```

### 테스트 커버리지
- ✅ 옵저버 패턴: 등록/제거, 알림 전송, 구독자별 기능, 예외 처리, REST API
- ✅ 전략 패턴: 결제 전략별 로직, 수수료 계산, 결제 내역 관리, REST API

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

### 실제 활용 사례
- **옵저버 패턴**: 이벤트 기반 시스템, MVC 아키텍처, 실시간 알림, UI 상태 관리
- **전략 패턴**: 결제 시스템, 정렬 알고리즘, 압축 알고리즘, 할인 정책

## 🔧 기술 스택

- **Backend**: Spring Boot 3.5.4, Spring WebFlux
- **Testing**: JUnit 5, Mockito
- **Build**: Gradle 8.x
- **Java**: 21
- **Logging**: SLF4J + Logback

## 📚 추가 예정 패턴

- [ ] 팩토리 패턴 (Factory Pattern)
- [ ] 싱글톤 패턴 (Singleton Pattern)
- [x] ~~전략 패턴 (Strategy Pattern)~~ ✅ 완료
- [ ] 데코레이터 패턴 (Decorator Pattern)
- [ ] 어댑터 패턴 (Adapter Pattern)

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