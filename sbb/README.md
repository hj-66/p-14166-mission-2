# AntPathRequestMatcher 오류 원인과 해결법

## 발생한 오류

현재 프로젝트에서 `./gradlew compileJava`를 실행하면 `SecurityConfig.java`의 아래 import 구문에서 컴파일 오류가 발생한다.

```java
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
```

오류 메시지는 다음과 같다.

```text
error: cannot find symbol
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
                                                    ^
  symbol:   class AntPathRequestMatcher
  location: package org.springframework.security.web.util.matcher
```

## 원인

`build.gradle.kts`에서 Spring Boot 버전이 `4.0.6`으로 설정되어 있다.

```kotlin
id("org.springframework.boot") version "4.0.6"
```

Spring Boot 4는 Spring Security 7 계열을 사용한다. Spring Security 7에서는 기존에 사용하던 `AntPathRequestMatcher`와 `MvcRequestMatcher`가 더 이상 지원되지 않고 제거되었다. 그래서 예전 Spring Security 5 또는 6 기준 예제처럼 다음 코드를 사용하면 컴파일 단계에서 클래스를 찾지 못한다.

```java
.requestMatchers(new AntPathRequestMatcher("/**")).permitAll()
```

공식 문서에서도 Spring Security 7에서 `AntPathRequestMatcher`와 `MvcRequestMatcher`가 제거되었고, `PathPatternRequestMatcher` 사용을 안내한다.

- https://docs.spring.io/spring-security/reference/7.0/whats-new.html
- https://docs.spring.io/spring-security/reference/migration-7/web.html

## 해결법

### 1. 단순 경로 허용은 문자열 matcher를 사용한다

현재 설정처럼 모든 요청을 허용하려는 목적이라면 별도의 `AntPathRequestMatcher` 객체를 만들 필요가 없다. `requestMatchers`에 문자열 패턴을 직접 넘기면 된다.

```java
http
    .authorizeHttpRequests((authorizeHttpRequests) -> authorizeHttpRequests
        .requestMatchers("/**").permitAll()
    );
```

이 경우 `AntPathRequestMatcher` import도 제거해야 한다.

```java
// 제거 대상
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
```

### 2. RequestMatcher 객체가 꼭 필요하면 PathPatternRequestMatcher를 사용한다

문자열 matcher가 아니라 `RequestMatcher` 객체를 직접 만들어야 하는 상황이라면 Spring Security 7에서는 `PathPatternRequestMatcher`를 사용한다.

예시는 다음과 같은 방향이다.

```java
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

PathPatternRequestMatcher.Builder mvc = PathPatternRequestMatcher.withDefaults();

http
    .authorizeHttpRequests((authorizeHttpRequests) -> authorizeHttpRequests
        .requestMatchers(mvc.matcher("/**")).permitAll()
    );
```

단순히 특정 URL을 허용하는 정도라면 1번 방식이 더 간단하다.

### 3. 구버전 강의나 책을 따라가는 경우 버전을 맞춘다

학습 자료가 Spring Boot 3 또는 Spring Security 6 기준이라면, 프로젝트의 Spring Boot 버전을 해당 자료와 맞추는 방법도 있다. 다만 새 프로젝트라면 Spring Boot 4 / Spring Security 7 방식으로 코드를 고치는 편이 좋다.

## 정리

이 오류는 코드 문법 문제가 아니라 Spring Security 버전 차이 때문에 발생한다. 현재 프로젝트는 Spring Boot 4를 사용하므로 `AntPathRequestMatcher`를 사용할 수 없다.

가장 간단한 해결은 다음 두 가지다.

1. `AntPathRequestMatcher` import를 제거한다.
2. `.requestMatchers(new AntPathRequestMatcher("/**"))`를 `.requestMatchers("/**")`로 바꾼다.

# 답변 수정 시간이 화면에 기록되지 않는 문제

## 테스트 결과

테스트용 메모리 H2 DB로 애플리케이션을 실행한 뒤 다음 순서로 확인했다.

1. 회원가입
2. 로그인
3. 질문 생성
4. 답변 생성
5. `/question/detail/1` 접속
6. `/answer/modify/1`에서 답변 수정
7. 다시 `/question/detail/1` 접속

답변 수정 요청 자체는 정상 처리되었다. 서버 로그에서도 답변 테이블을 업데이트하는 SQL이 실행되었다.

```text
Hibernate: update answer set author_id=?,content=?,create_date=?,modify_date=?,question_id=? where id=?
```

즉, `AnswerService.modify`에서는 답변의 수정 시간을 실제로 저장하고 있다.

```java
public void modify(Answer answer, String content) {
    answer.setContent(content);
    answer.setModifyDate(LocalDateTime.now());
    this.answerRepository.save(answer);
}
```

하지만 `/question/detail/1` 화면에는 답변 수정 시간이 표시되지 않았다.

## 원인

문제는 저장 로직이 아니라 `question_detail.html`의 답변 표시 영역에 있다.

현재 답변 반복 구간에서는 `answer`를 반복하고 있지만, 수정 시간 조건과 출력 값이 모두 `question.modifyDate`를 보고 있다.

```html
<div th:if="${question.modifyDate != null}" class="badge bg-light text-dark p-2 text-start mx-3">
    <div class="mb-2">modified at</div>
    <div th:text="${#temporals.format(question.modifyDate, 'yyyy-MM-dd HH:mm')}"></div>
</div>
```

이 코드는 답변의 수정 시간이 아니라 질문의 수정 시간을 확인한다. 그래서 답변을 수정해도 질문을 수정하지 않았다면 `question.modifyDate`는 `null`이고, 답변의 `modifyDate`가 저장되어 있어도 화면에는 보이지 않는다.

추가로 답변 작성자 표시도 답변 기준이 아니라 질문 기준으로 되어 있다.

```html
<span th:if="${question.author != null}" th:text="${question.author.username}"></span>
```

답변 영역에서는 `question.author`가 아니라 `answer.author`를 사용해야 한다.

## 해결법

`question_detail.html`의 답변 반복 영역에서 질문 객체가 아니라 답변 객체를 기준으로 수정 시간과 작성자를 출력해야 한다.

수정 시간 표시 코드는 다음처럼 바꾸는 것이 맞다.

```html
<div th:if="${answer.modifyDate != null}" class="badge bg-light text-dark p-2 text-start mx-3">
    <div class="mb-2">modified at</div>
    <div th:text="${#temporals.format(answer.modifyDate, 'yyyy-MM-dd HH:mm')}"></div>
</div>
```

답변 작성자 표시도 다음처럼 바꾸는 것이 맞다.

```html
<span th:if="${answer.author != null}" th:text="${answer.author.username}"></span>
```

## 정리

답변 수정 시간이 저장되지 않는 것이 아니라, 화면에서 잘못된 필드를 보고 있어서 표시되지 않는 문제다.

- 저장 로직: `answer.modifyDate`를 정상 저장함
- 화면 로직: 답변 영역에서 `question.modifyDate`를 보고 있음
- 해결 방향: 답변 영역에서는 `answer.modifyDate`와 `answer.author`를 사용
