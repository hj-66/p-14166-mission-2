# SBB 프로젝트 전체 코드 분석

SBB는 Spring Boot로 만든 질문/답변 게시판 애플리케이션이다. 사용자는 회원가입과 로그인을 할 수 있고, 로그인한 사용자는 질문과 답변을 작성, 수정, 삭제, 추천할 수 있다. 질문 목록은 페이징과 키워드 검색을 지원하며, 질문과 답변 본문은 Markdown으로 작성한 뒤 HTML로 렌더링된다.

이 README는 현재 저장소의 실제 코드를 기준으로 프로젝트 구조, 실행 방법, 주요 기능, 계층별 책임, 관련 Spring 개념을 정리한다.

## 기술 스택

- Java 25
- Spring Boot 4.0.6
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Thymeleaf
- Thymeleaf Layout Dialect
- Thymeleaf Spring Security Extras
- Bean Validation
- H2 Database
- Lombok
- CommonMark
- Gradle Kotlin DSL
- Bootstrap

## 프로젝트 구조

```text
src/main/java/com/mysite/sbb
├── SbbApplication.java
├── MainController.java
├── SecurityConfig.java
├── CommonUtil.java
├── DataNotFoundException.java
├── question
│   ├── Question.java
│   ├── QuestionController.java
│   ├── QuestionService.java
│   ├── QuestionRepository.java
│   └── QuestionForm.java
├── answer
│   ├── Answer.java
│   ├── AnswerController.java
│   ├── AnswerService.java
│   ├── AnswerRepository.java
│   └── AnswerForm.java
└── user
    ├── SiteUser.java
    ├── UserController.java
    ├── UserService.java
    ├── UserSecurityService.java
    ├── UserRepository.java
    ├── UserCreateForm.java
    └── UserRole.java
```

```text
src/main/resources
├── application.yaml
├── application.properties
├── templates
│   ├── layout.html
│   ├── navbar.html
│   ├── question_list.html
│   ├── question_detail.html
│   ├── question_form.html
│   ├── answer_form.html
│   ├── login_form.html
│   ├── signup_form.html
│   └── form_errors.html
└── static
    ├── bootstrap.min.css
    ├── bootstrap.min.js
    └── style.css
```

## 실행 방법

현재 `gradlew` 파일에 실행 권한이 없을 수 있다. 이 경우 먼저 권한을 부여한다.

```bash
chmod +x gradlew
```

애플리케이션 실행:

```bash
./gradlew bootRun
```

브라우저에서 접속:

```text
http://localhost:8080/question/list
```

H2 콘솔:

```text
http://localhost:8080/h2-console
```

H2 접속 정보:

```text
JDBC URL: jdbc:h2:~/local
User Name: sa
Password:
```

## 현재 테스트 상태

현재 `./gradlew test`는 컴파일 단계에서 실패한다.

원인은 `src/test/java/com/mysite/sbb/SbbApplicationTests.java`의 테스트 코드가 다음처럼 `QuestionService.create(subject, content)`를 호출하기 때문이다.

```java
this.questionService.create(subject, content);
```

하지만 실제 `QuestionService`의 메서드는 다음 시그니처를 가진다.

```java
public void create(String subject, String content, SiteUser user)
```

즉, 질문 작성자 `SiteUser` 인자가 추가되었는데 테스트 코드가 갱신되지 않았다. 테스트를 살리려면 테스트용 사용자를 생성하거나, 테스트 전용으로 작성자를 넘기도록 수정해야 한다.

## 전체 아키텍처

이 프로젝트는 전형적인 Spring MVC 계층 구조를 따른다.

```text
Browser
  ↓
Controller
  ↓
Service
  ↓
Repository
  ↓
JPA Entity
  ↓
H2 Database
```

각 계층의 역할은 다음과 같다.

- Controller: HTTP 요청을 받고, 입력값을 검증하며, 화면 이름 또는 리다이렉트 경로를 반환한다.
- Service: 비즈니스 로직을 처리한다. 생성일, 수정일, 작성자 연결, 추천 처리 등이 여기에 있다.
- Repository: 데이터베이스 접근을 담당한다. Spring Data JPA가 구현체를 자동 생성한다.
- Entity: 데이터베이스 테이블과 매핑되는 객체다.
- Template: Controller가 Model에 담은 데이터를 HTML로 렌더링한다.

## 도메인 모델

### Question

`Question`은 질문 게시글을 나타내는 JPA 엔티티다.

주요 필드:

- `id`: 기본키
- `subject`: 질문 제목
- `content`: 질문 본문
- `createDate`: 작성일
- `modifyDate`: 수정일
- `answerList`: 이 질문에 달린 답변 목록
- `author`: 질문 작성자
- `voter`: 질문을 추천한 사용자 집합

관계:

```java
@OneToMany(mappedBy = "question", cascade = CascadeType.REMOVE)
private List<Answer> answerList;

@ManyToOne
private SiteUser author;

@ManyToMany
Set<SiteUser> voter;
```

`Question`과 `Answer`는 일대다 관계다. 질문 하나에는 여러 답변이 달릴 수 있다. `cascade = CascadeType.REMOVE`가 있으므로 질문을 삭제하면 연결된 답변도 함께 삭제된다.

`Question`과 `SiteUser`는 작성자 기준으로 다대일 관계다. 여러 질문을 한 사용자가 작성할 수 있다.

추천자는 `ManyToMany`다. 하나의 질문은 여러 사용자에게 추천될 수 있고, 한 사용자는 여러 질문을 추천할 수 있다.

### Answer

`Answer`는 답변을 나타내는 JPA 엔티티다.

주요 필드:

- `id`: 기본키
- `content`: 답변 본문
- `createDate`: 작성일
- `modifyDate`: 수정일
- `question`: 답변이 달린 질문
- `author`: 답변 작성자
- `voter`: 답변을 추천한 사용자 집합

답변은 반드시 어떤 질문에 속한다.

```java
@ManyToOne
private Question question;
```

답변 작성자도 사용자와 다대일 관계다.

```java
@ManyToOne
private SiteUser author;
```

### SiteUser

`SiteUser`는 애플리케이션 사용자 엔티티다.

주요 필드:

- `id`: 기본키
- `username`: 로그인 ID, 유니크
- `password`: BCrypt로 암호화된 비밀번호
- `email`: 이메일, 유니크

`username`과 `email`에는 `unique = true`가 적용되어 중복 저장을 막는다.

## 주요 기능 흐름

### 질문 목록

요청 경로:

```text
GET /question/list?page=0&kw=검색어
```

처리 흐름:

```text
QuestionController.list
→ QuestionService.getList(page, kw)
→ QuestionRepository.findAllByKeyword(kw, pageable)
→ question_list.html
```

목록은 최신순으로 정렬되고 한 페이지에 10개씩 표시된다. 검색어는 질문 제목, 질문 내용, 질문 작성자, 답변 내용, 답변 작성자에 대해 JPQL로 검색된다.

### 질문 상세

요청 경로:

```text
GET /question/detail/{id}
```

처리 흐름:

```text
QuestionController.detail
→ QuestionService.getQuestion(id)
→ CommonUtil.markdown(question.content)
→ 각 Answer.content도 Markdown 변환
→ question_detail.html
```

본문은 CommonMark를 이용해 Markdown에서 HTML로 변환된다. 템플릿에서는 `th:utext`로 변환된 HTML을 출력한다.

### 질문 작성

요청 경로:

```text
GET  /question/create
POST /question/create
```

질문 작성은 로그인 사용자만 가능하다.

```java
@PreAuthorize("isAuthenticated()")
```

POST 요청에서는 `QuestionForm`을 Bean Validation으로 검증한다.

```java
@NotEmpty(message="제목은 필수항목입니다.")
@Size(max=200)
private String subject;

@NotEmpty(message="내용은 필수항목입니다.")
private String content;
```

검증에 성공하면 현재 로그인 사용자를 `Principal`에서 얻고, `UserService.getUser`로 `SiteUser`를 조회한 뒤 질문을 저장한다.

### 질문 수정과 삭제

질문 수정과 삭제는 로그인 사용자 중에서도 질문 작성자만 가능하다.

```java
if (!question.getAuthor().getUsername().equals(principal.getName())) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
}
```

수정 시에는 `modifyDate`가 현재 시각으로 저장된다.

삭제 시에는 `Question`에 설정된 `CascadeType.REMOVE` 때문에 연결된 답변도 함께 삭제된다.

### 답변 작성

요청 경로:

```text
POST /answer/create/{questionId}
```

답변 작성도 로그인 사용자만 가능하다. 컨트롤러는 먼저 질문을 조회하고, 현재 로그인 사용자를 조회한 다음 답변을 저장한다.

```text
AnswerController.createAnswer
→ QuestionService.getQuestion(id)
→ UserService.getUser(principal.getName())
→ AnswerService.create(question, content, user)
```

### 답변 수정과 삭제

답변 수정과 삭제도 작성자만 가능하다. 수정 후에는 해당 답변 위치로 이동하기 위해 앵커를 포함한 URL로 리다이렉트한다.

```text
/question/detail/{questionId}#answer_{answerId}
```

### 추천 기능

질문 추천:

```text
GET /question/vote/{id}
```

답변 추천:

```text
GET /answer/vote/{id}
```

추천 기능은 `Question.voter`, `Answer.voter`에 현재 사용자를 추가하는 방식이다.

```java
question.getVoter().add(siteUser);
answer.getVoter().add(siteUser);
```

`voter`가 `Set<SiteUser>`이므로 같은 사용자를 중복 추천자로 담는 것을 컬렉션 차원에서 방지하려는 의도가 있다.

## 인증과 인가

보안 설정은 `SecurityConfig`에 있다.

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig
```

현재 HTTP 요청 자체는 전체 허용되어 있다.

```java
.requestMatchers(mvc.matcher("/**")).permitAll()
```

대신 로그인 필요 기능은 컨트롤러 메서드의 `@PreAuthorize("isAuthenticated()")`로 보호한다.

로그인 설정:

```java
.formLogin((formLogin) -> formLogin
    .loginPage("/user/login")
    .defaultSuccessUrl("/question/list", true))
```

로그아웃 설정:

```java
.logout((logout) -> logout
    .logoutRequestMatcher(mvc.matcher("/user/logout"))
    .logoutSuccessUrl("/question/list")
    .invalidateHttpSession(true))
```

비밀번호는 BCrypt로 저장된다.

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

`UserSecurityService`는 Spring Security의 `UserDetailsService`를 구현한다. 로그인 시 입력한 username으로 `SiteUser`를 찾고, Spring Security가 이해할 수 있는 `UserDetails` 객체로 변환한다.

권한은 username이 `admin`이면 `ROLE_ADMIN`, 그 외에는 `ROLE_USER`로 부여된다.

## 화면 구성

### layout.html

공통 레이아웃이다. Bootstrap CSS, 프로젝트 CSS, 네비게이션 바, 공통 스크립트 위치를 제공한다.

각 화면은 다음처럼 레이아웃을 사용한다.

```html
<html layout:decorate="~{layout}">
```

본문은 다음 fragment에 들어간다.

```html
<div layout:fragment="content">
```

페이지별 JavaScript는 다음 fragment에 들어간다.

```html
<script layout:fragment="script">
```

### navbar.html

네비게이션 바 fragment다. 로그인 상태에 따라 로그인/로그아웃 링크를 다르게 보여준다.

```html
<a sec:authorize="isAnonymous()" th:href="@{/user/login}">로그인</a>
<a sec:authorize="isAuthenticated()" th:href="@{/user/logout}">로그아웃</a>
```

### question_list.html

질문 목록 화면이다.

주요 기능:

- 질문 등록 버튼
- 검색 입력창
- 질문 목록 테이블
- 답변 수 표시
- 페이징
- 검색과 페이지 이동을 hidden form으로 처리

페이징 번호는 현재 페이지 기준 앞뒤 5개만 보여준다.

### question_detail.html

질문 상세 화면이다.

주요 기능:

- 질문 제목과 본문 표시
- Markdown 변환 결과 출력
- 작성자, 작성일, 수정일 표시
- 질문 추천, 수정, 삭제
- 답변 목록 표시
- 답변 추천, 수정, 삭제
- 답변 작성 폼
- 삭제/추천 확인 JavaScript

작성자만 수정/삭제 버튼을 볼 수 있도록 Thymeleaf Security 표현식을 사용한다.

### question_form.html

질문 등록과 질문 수정에 함께 쓰이는 폼이다. `QuestionForm` 객체와 바인딩된다.

### answer_form.html

답변 수정 화면이다. `AnswerForm` 객체와 바인딩된다.

### signup_form.html

회원가입 화면이다. `UserCreateForm` 객체와 바인딩된다.

### login_form.html

Spring Security form login과 연결된 로그인 화면이다. 로그인 실패 시 `param.error`를 확인해서 오류 메시지를 보여준다.

### form_errors.html

폼 검증 오류를 공통으로 표시하는 fragment다.

```html
<div th:each="err : ${#fields.allErrors()}" th:text="${err}" />
```

## Repository 분석

### QuestionRepository

`JpaRepository<Question, Integer>`를 상속한다. 기본 CRUD 메서드는 Spring Data JPA가 자동 제공한다.

예:

- `findById`
- `findAll`
- `save`
- `delete`

메서드 이름 기반 쿼리도 정의되어 있다.

```java
Question findBySubject(String subject);
Question findBySubjectAndContent(String subject, String content);
List<Question> findBySubjectLike(String subject);
```

키워드 검색은 직접 JPQL을 작성한다.

```java
@Query("select distinct q from Question q ... where ...")
Page<Question> findAllByKeyword(@Param("kw") String kw, Pageable pageable);
```

이 쿼리는 질문, 질문 작성자, 답변, 답변 작성자를 left join해서 검색한다.

### AnswerRepository

`JpaRepository<Answer, Integer>`만 상속한다. 현재 별도 쿼리 메서드는 없다.

### UserRepository

`JpaRepository<SiteUser, Long>`를 상속한다. username으로 사용자를 찾기 위한 메서드가 있다.

```java
Optional<SiteUser> findByusername(String username);
```

Spring Data JPA 메서드 네이밍 관점에서는 보통 `findByUsername`처럼 필드명 대소문자를 맞춰 쓰는 편이 자연스럽다.

## Service 분석

### QuestionService

질문 관련 비즈니스 로직을 담당한다.

주요 메서드:

- `getQuestion`: id로 질문 조회, 없으면 `DataNotFoundException`
- `create`: 제목, 내용, 작성자를 받아 질문 저장
- `getList`: 페이징과 검색 적용
- `modify`: 제목과 내용을 수정하고 수정일 저장
- `delete`: 질문 삭제
- `vote`: 추천자 추가

`search`라는 `Specification<Question>` 생성 메서드도 있지만, 현재 목록 조회에서는 사용하지 않고 `QuestionRepository.findAllByKeyword`를 사용한다.

### AnswerService

답변 관련 비즈니스 로직을 담당한다.

주요 메서드:

- `create`: 질문, 내용, 작성자를 받아 답변 저장
- `getAnswer`: id로 답변 조회, 없으면 `DataNotFoundException`
- `modify`: 내용을 수정하고 수정일 저장
- `delete`: 답변 삭제
- `vote`: 추천자 추가

### UserService

회원 관련 비즈니스 로직을 담당한다.

주요 메서드:

- `create`: 사용자 생성, 비밀번호 BCrypt 암호화
- `getUser`: username으로 사용자 조회

### UserSecurityService

Spring Security 로그인 과정에서 사용자 정보를 제공한다.

`UserDetailsService`를 구현하면 Spring Security가 로그인 시 `loadUserByUsername`을 호출한다. 이 메서드는 DB의 `SiteUser`를 Spring Security의 `UserDetails`로 변환한다.

## 설정 파일

`application.yaml`과 `application.properties`가 모두 존재하며 같은 성격의 설정이 중복되어 있다. Spring Boot는 둘 다 읽을 수 있으므로 운영 관점에서는 하나로 통일하는 것이 좋다.

주요 설정:

```yaml
spring:
  h2:
    console:
      enabled: true
      path: /h2-console
  datasource:
    url: jdbc:h2:~/local
    driver-class-name: org.h2.Driver
    username: sa
  jpa:
    hibernate:
      ddl-auto: update
```

`ddl-auto: update`는 엔티티 변경 사항을 데이터베이스 스키마에 자동 반영한다. 학습과 개발에는 편리하지만 운영 환경에서는 의도하지 않은 스키마 변경 위험이 있으므로 보통 Flyway, Liquibase 같은 마이그레이션 도구를 사용한다.

## 관련 개념 정리

### Spring Boot

Spring 애플리케이션을 빠르게 만들기 위한 프레임워크다. 자동 설정, 내장 서버, 스타터 의존성을 제공한다. 이 프로젝트의 시작점은 `SbbApplication`이다.

```java
@SpringBootApplication
public class SbbApplication {
    public static void main(String[] args) {
        SpringApplication.run(SbbApplication.class, args);
    }
}
```

### Spring MVC

HTTP 요청을 Controller가 받고, Model에 데이터를 담고, View 템플릿을 렌더링하는 웹 MVC 구조다.

예:

```java
@GetMapping("/list")
public String list(Model model) {
    model.addAttribute("paging", paging);
    return "question_list";
}
```

반환 문자열 `question_list`는 `templates/question_list.html`을 의미한다.

### Controller

URL과 Java 메서드를 연결한다. `@GetMapping`, `@PostMapping`, `@PathVariable`, `@RequestParam` 등이 사용된다.

이 프로젝트에서는 `QuestionController`, `AnswerController`, `UserController`, `MainController`가 Controller 역할을 한다.

### Service

Controller에서 바로 Repository를 호출하지 않고 Service를 거치는 이유는 비즈니스 규칙을 한곳에 모으기 위해서다. 예를 들어 질문 작성 시 생성일과 작성자를 함께 저장하는 로직은 `QuestionService.create`에 있다.

### Repository

데이터 접근 계층이다. `JpaRepository`를 상속하면 반복적인 CRUD 구현을 직접 작성하지 않아도 된다.

### JPA Entity

Java 객체를 데이터베이스 테이블과 매핑한다. `@Entity`, `@Id`, `@GeneratedValue`, `@Column`, `@ManyToOne`, `@OneToMany`, `@ManyToMany` 등이 사용된다.

### DTO/Form 객체

`QuestionForm`, `AnswerForm`, `UserCreateForm`은 화면 입력값을 받기 위한 객체다. Entity를 직접 폼에 노출하지 않고 별도의 Form 객체를 두면 검증 규칙과 화면 입력 구조를 분리할 수 있다.

### Bean Validation

`@NotEmpty`, `@Size`, `@Email` 같은 애너테이션으로 입력값 검증을 선언한다. Controller에서 `@Valid`와 `BindingResult`를 함께 사용해 검증 결과를 처리한다.

```java
public String signup(@Valid UserCreateForm userCreateForm, BindingResult bindingResult)
```

`BindingResult`는 반드시 검증 대상 바로 뒤에 위치해야 한다.

### Thymeleaf

서버 사이드 HTML 템플릿 엔진이다. Controller가 Model에 담은 데이터를 HTML에 삽입한다.

자주 쓰는 문법:

- `th:text`: 텍스트 출력
- `th:utext`: escape하지 않은 HTML 출력
- `th:if`: 조건부 렌더링
- `th:each`: 반복
- `th:href`: URL 생성
- `th:field`: 폼 필드 바인딩
- `th:object`: 폼 객체 지정
- `th:replace`: fragment 삽입

### Thymeleaf Layout Dialect

공통 레이아웃을 재사용하기 위한 Thymeleaf 확장이다. `layout.html`에 공통 뼈대를 두고, 각 페이지는 `layout:fragment`로 본문만 제공한다.

### Spring Security

인증과 인가를 담당한다.

- 인증: 사용자가 누구인지 확인하는 것
- 인가: 해당 사용자가 어떤 기능을 사용할 수 있는지 확인하는 것

이 프로젝트에서는 로그인, 로그아웃, 비밀번호 암호화, 로그인 필요 기능 제한, 로그인 상태별 화면 표시가 구현되어 있다.

### Principal

현재 로그인한 사용자의 이름을 얻기 위해 Controller 메서드 인자로 사용한다.

```java
Principal principal
principal.getName()
```

`principal.getName()`은 로그인한 username을 반환한다.

### BCrypt

비밀번호를 안전하게 저장하기 위한 단방향 해시 알고리즘이다. 같은 비밀번호라도 매번 다른 해시가 만들어지도록 salt가 포함된다.

### JPQL

JPA 엔티티 객체를 대상으로 작성하는 쿼리 언어다. SQL이 테이블을 대상으로 한다면 JPQL은 Entity와 필드를 대상으로 한다.

이 프로젝트의 검색 쿼리는 `Question`, `Answer`, `SiteUser` 엔티티를 조인해서 키워드를 찾는다.

### Pageable과 Page

Spring Data의 페이징 abstraction이다.

```java
Pageable pageable = PageRequest.of(page, 10, Sort.by(sorts));
Page<Question> paging = questionRepository.findAllByKeyword(kw, pageable);
```

`Page` 객체는 현재 페이지 데이터뿐 아니라 전체 페이지 수, 이전/다음 페이지 여부, 전체 데이터 개수 같은 메타데이터도 제공한다.

### Markdown 렌더링

`CommonUtil`은 CommonMark 라이브러리를 사용해 Markdown 문자열을 HTML로 바꾼다.

```java
Parser parser = Parser.builder().build();
Node document = parser.parse(markdown);
HtmlRenderer renderer = HtmlRenderer.builder().build();
return renderer.render(document);
```

템플릿에서는 변환된 HTML을 `th:utext`로 출력한다. 사용자 입력을 HTML로 출력하는 구조이므로 실제 서비스에서는 XSS 방지를 위해 HTML sanitizer 적용을 고려해야 한다.

## 현재 코드상 주의점

1. 테스트 코드가 현재 Service 시그니처와 맞지 않아 컴파일되지 않는다.
2. `application.yaml`과 `application.properties`에 설정이 중복되어 있어 하나로 정리하는 것이 좋다.
3. `QuestionService.search` 메서드는 현재 사용되지 않는다. JPQL 검색 방식과 Specification 검색 방식 중 하나로 통일할 수 있다.
4. Markdown HTML을 `th:utext`로 출력하므로 XSS 방어를 강화할 필요가 있다.
5. 추천 기능은 GET 요청으로 상태를 변경한다. REST 관점에서는 POST 요청이 더 적절하다.
6. 답변 상세 템플릿의 답변 작성자 표시 부분에서 `answer.author`가 아니라 `question.author`를 참조하는 코드가 있다. 답변 작성자를 표시하려면 `answer.author` 기준으로 수정하는 것이 자연스럽다.
7. 답변 수정일 표시 조건도 `question.modifyDate != null`을 보고 있다. 답변 수정일을 표시하려면 `answer.modifyDate != null`을 확인해야 한다.
8. `UserRepository.findByusername`은 동작할 수 있지만 Java 네이밍 관례상 `findByUsername`으로 쓰는 것이 더 명확하다.
9. `style.css`는 현재 비어 있다.
10. 전체 URL은 `permitAll`이고, 실제 제한은 메서드 보안에 의존한다. 기능이 늘어나면 URL 단위 정책과 메서드 단위 정책의 책임을 명확히 나누는 것이 좋다.

## 주요 URL 요약

| 기능 | Method | URL | 로그인 필요 |
| --- | --- | --- | --- |
| 질문 목록 | GET | `/question/list` | 아니오 |
| 질문 상세 | GET | `/question/detail/{id}` | 아니오 |
| 질문 작성 화면 | GET | `/question/create` | 예 |
| 질문 작성 처리 | POST | `/question/create` | 예 |
| 질문 수정 화면 | GET | `/question/modify/{id}` | 예 |
| 질문 수정 처리 | POST | `/question/modify/{id}` | 예 |
| 질문 삭제 | GET | `/question/delete/{id}` | 예 |
| 질문 추천 | GET | `/question/vote/{id}` | 예 |
| 답변 작성 | POST | `/answer/create/{questionId}` | 예 |
| 답변 수정 화면 | GET | `/answer/modify/{id}` | 예 |
| 답변 수정 처리 | POST | `/answer/modify/{id}` | 예 |
| 답변 삭제 | GET | `/answer/delete/{id}` | 예 |
| 답변 추천 | GET | `/answer/vote/{id}` | 예 |
| 회원가입 화면 | GET | `/user/signup` | 아니오 |
| 회원가입 처리 | POST | `/user/signup` | 아니오 |
| 로그인 | GET/POST | `/user/login` | 아니오 |
| 로그아웃 | GET | `/user/logout` | 예 |
| H2 콘솔 | GET | `/h2-console` | 아니오 |

## 학습 포인트

이 프로젝트를 통해 다음 개념을 한 번에 학습할 수 있다.

- Spring Boot 프로젝트 구성
- MVC 요청 처리 흐름
- Controller, Service, Repository 계층 분리
- JPA Entity 설계와 연관관계 매핑
- Spring Data JPA 쿼리 메서드와 JPQL
- 페이징과 정렬
- Bean Validation 기반 폼 검증
- Thymeleaf 서버 사이드 렌더링
- Thymeleaf Layout Dialect 기반 공통 레이아웃
- Spring Security 로그인/로그아웃
- `UserDetailsService` 기반 사용자 인증
- BCrypt 비밀번호 암호화
- 로그인 사용자 기반 작성자 권한 검사
- Markdown 렌더링
- H2 데이터베이스와 개발용 JPA 설정
