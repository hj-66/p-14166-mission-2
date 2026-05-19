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
