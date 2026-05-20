# `question_detail.html` 전체 코드 분석

이 문서는 `src/main/resources/templates/question_detail.html` 파일을 이해하기 위한 설명이다. 이 파일은 SBB 게시판에서 질문 상세 화면을 렌더링하는 Thymeleaf 템플릿이다.

이 화면은 하나의 질문, 그 질문에 달린 답변 목록, 답변 작성 폼, 추천/수정/삭제 버튼, 삭제 및 추천 확인 스크립트로 구성된다.

## 이 파일의 역할

`question_detail.html`은 사용자가 `/question/detail/{id}` 주소로 접속했을 때 보여지는 화면이다.

예를 들어 `/question/detail/3`으로 접속하면 `QuestionController.detail` 메서드가 실행되고, 컨트롤러는 질문 객체와 마크다운 변환 결과를 모델에 담아서 이 템플릿으로 전달한다.

관련 컨트롤러 흐름은 다음과 같다.

```java
@GetMapping(value = "/detail/{id}")
public String detail(Model model, @PathVariable("id") Integer id, AnswerForm answerForm) {
    Question question = this.questionService.getQuestion(id);
    String questionContentHtml = commonUtil.markdown(question.getContent());
    Map<Integer, String> answerContentHtml = question.getAnswerList().stream()
            .collect(Collectors.toMap(
                    Answer::getId,
                    answer -> commonUtil.markdown(answer.getContent())
            ));

    model.addAttribute("questionContentHtml", questionContentHtml);
    model.addAttribute("answerContentHtml", answerContentHtml);
    model.addAttribute("question", question);
    return "question_detail";
}
```

이 템플릿에서 주로 사용하는 모델 데이터는 다음과 같다.

- `question`: 현재 상세 화면에 표시할 질문 객체
- `questionContentHtml`: 질문 본문을 마크다운에서 HTML로 변환한 문자열
- `answerContentHtml`: 답변 id를 key로 하고, 답변 본문 HTML을 value로 가지는 Map
- `answerForm`: 답변 작성 폼에서 사용하는 입력 객체

## 전체 구조

파일의 큰 구조는 다음과 같다.

```html
<html layout:decorate="~{layout}">
<div layout:fragment="content" class="container my-3">
    질문 표시 영역
    답변 개수 표시 영역
    답변 목록 반복 영역
    답변 작성 폼
</div>
<script layout:fragment="script" type='text/javascript'>
    삭제 확인 스크립트
    추천 확인 스크립트
</script>
</html>
```

즉, 이 파일은 완전한 HTML 문서를 직접 만드는 것이 아니라 공통 레이아웃인 `layout.html` 안에 끼워 넣을 `content` 영역과 `script` 영역을 정의한다.

## 1. 레이아웃 적용

```html
<html layout:decorate="~{layout}">
```

이 코드는 Thymeleaf Layout Dialect 문법이다. 현재 템플릿이 `layout.html`을 기반으로 렌더링된다는 뜻이다.

`~{layout}`은 `templates/layout.html` 파일을 의미한다. 보통 `layout.html`에는 공통 `<head>`, 네비게이션 바, Bootstrap CSS/JS, 공통 스크립트 위치 등이 들어간다.

이 개념을 이해하려면 다음을 알아야 한다.

- Thymeleaf 템플릿은 서버에서 HTML을 만들어 브라우저에 전달한다.
- Layout Dialect는 여러 화면에서 반복되는 레이아웃 코드를 공통 파일로 분리하게 해준다.
- `layout:decorate`는 현재 파일이 어떤 레이아웃을 사용할지 지정한다.
- `layout:fragment`는 레이아웃 안의 특정 영역에 삽입될 조각을 정의한다.

## 2. 본문 영역 fragment

```html
<div layout:fragment="content" class="container my-3">
```

이 `div` 안의 내용은 `layout.html`의 `content` fragment 위치에 삽입된다.

`class="container my-3"`는 Bootstrap 클래스다.

- `container`: 화면 좌우 여백을 가진 중앙 정렬 레이아웃
- `my-3`: margin y축, 즉 위아래 여백을 3단계로 설정

여기서 중요한 점은 `layout:fragment="content"`가 화면 구조를 결정하고, `class="container my-3"`는 화면 스타일을 결정한다는 것이다.

## 3. 질문 제목 출력

```html
<h2 class="border-bottom py-2" th:text="${question.subject}"></h2>
```

이 코드는 질문 제목을 출력한다.

`th:text="${question.subject}"`는 Thymeleaf 표현식이다. 컨트롤러에서 모델에 담은 `question` 객체의 `subject` 값을 꺼내서 `<h2>`의 텍스트로 넣는다.

예를 들어 질문 제목이 `스프링 질문입니다`라면 최종 HTML은 다음처럼 된다.

```html
<h2 class="border-bottom py-2">스프링 질문입니다</h2>
```

`th:text`는 HTML 태그를 해석하지 않고 문자열로 출력한다. 따라서 사용자가 제목에 `<script>` 같은 값을 넣어도 HTML로 실행되지 않고 텍스트로 표시된다.

## 4. 질문 카드 영역

```html
<div class="card my-3">
    <div class="card-body">
        ...
    </div>
</div>
```

이 부분은 Bootstrap 카드 컴포넌트다. 질문 본문, 작성자, 작성일, 수정일, 추천/수정/삭제 버튼을 하나의 카드 안에 보여준다.

- `card`: Bootstrap 카드 스타일
- `my-3`: 카드 위아래 여백
- `card-body`: 카드 내부 본문 영역

## 5. 질문 본문 마크다운 출력

```html
<div class="card-text" th:utext="${questionContentHtml}"></div>
```

이 코드는 질문 본문을 출력한다. 여기서는 `th:text`가 아니라 `th:utext`를 사용한다.

둘의 차이는 중요하다.

- `th:text`: HTML 태그를 escape해서 텍스트로 출력
- `th:utext`: HTML 태그를 escape하지 않고 실제 HTML로 출력

질문 본문은 컨트롤러에서 이미 `commonUtil.markdown(question.getContent())`를 통해 HTML로 변환되어 있다. 예를 들어 사용자가 다음처럼 작성했다고 하자.

```markdown
## 제목

- 항목 1
- 항목 2
```

마크다운 변환 후에는 대략 다음과 같은 HTML이 된다.

```html
<h2>제목</h2>
<ul>
<li>항목 1</li>
<li>항목 2</li>
</ul>
```

이 HTML을 실제 HTML로 렌더링하려면 `th:utext`가 필요하다.

주의할 점도 있다. `th:utext`는 HTML을 그대로 출력하므로 XSS 보안 위험이 생길 수 있다. CommonMark 기본 렌더러가 모든 위험한 HTML을 자동으로 안전하게 정리해주는 것은 아니다. 사용자 입력을 HTML로 출력하는 구조라면 실제 서비스에서는 HTML sanitizer 적용을 검토해야 한다.

## 6. 질문 수정일 표시

```html
<div th:if="${question.modifyDate != null}" class="badge bg-light text-dark p-2 text-start mx-3">
    <div class="mb-2">modified at</div>
    <div th:text="${#temporals.format(question.modifyDate, 'yyyy-MM-dd HH:mm')}"></div>
</div>
```

이 부분은 질문이 수정된 적이 있을 때만 수정 시간을 보여준다.

`th:if="${question.modifyDate != null}"`는 조건부 렌더링이다. 조건이 참이면 HTML에 포함되고, 거짓이면 아예 렌더링되지 않는다.

`#temporals.format(...)`은 Thymeleaf에서 날짜와 시간 객체를 포맷할 때 사용하는 유틸리티 객체다.

```html
th:text="${#temporals.format(question.modifyDate, 'yyyy-MM-dd HH:mm')}"
```

예를 들어 `question.modifyDate`가 `2026-05-20T13:45`라면 화면에는 다음처럼 보인다.

```text
2026-05-20 13:45
```

## 7. 질문 작성자 표시

```html
<span th:if="${question.author != null}" th:text="${question.author.username}"></span>
```

이 코드는 질문 작성자가 존재할 때 작성자 username을 출력한다.

`question.author != null` 조건을 두는 이유는 기존 데이터 중 작성자가 없는 질문이 있을 수 있기 때문이다. 만약 `author`가 `null`인데 `question.author.username`을 바로 접근하면 오류가 날 수 있다.

이런 방어 코드는 게시판 예제에서 자주 사용된다. 로그인 기능을 나중에 추가한 경우, 예전에 만들어진 질문에는 작성자 정보가 없을 수 있기 때문이다.

## 8. 질문 작성일 표시

```html
<div class="badge bg-light text-dark p-2 text-start">
    <div th:text="${#temporals.format(question.createDate, 'yyyy-MM-dd HH:mm')}"></div>
</div>
```

질문 생성일을 표시한다. 수정일과 달리 생성일은 항상 존재한다고 가정하고 `th:if`를 사용하지 않는다.

이 코드가 정상 동작하려면 `Question` 엔티티에 `createDate` 값이 저장되어 있어야 한다.

## 9. 질문 추천 버튼

```html
<a href="javascript:void(0);" class="recommend btn btn-sm btn-outline-secondary"
   th:data-uri="@{|/question/vote/${question.id}|}">
    추천
    <span class="badge rounded-pill bg-success" th:text="${#lists.size(question.voter)}"></span>
</a>
```

이 코드는 질문 추천 버튼이다.

`href="javascript:void(0);"`는 링크를 클릭해도 기본 이동을 하지 않게 만든다. 실제 이동은 아래쪽 JavaScript에서 처리한다.

`th:data-uri`는 HTML의 `data-uri` 속성을 만든다.

```html
th:data-uri="@{|/question/vote/${question.id}|}"
```

질문 id가 3이면 최종 HTML은 다음처럼 된다.

```html
data-uri="/question/vote/3"
```

아래 스크립트에서 사용자가 추천 확인 창에 동의하면 `location.href = this.dataset.uri;`를 실행해서 해당 주소로 이동한다.

추천 수는 다음 코드로 표시한다.

```html
th:text="${#lists.size(question.voter)}"
```

`question.voter`는 이 질문을 추천한 사용자 목록 또는 집합이다. `#lists.size(...)`는 컬렉션의 크기를 구한다.

## 10. 질문 수정 버튼

```html
<a th:href="@{|/question/modify/${question.id}|}" class="btn btn-sm btn-outline-secondary"
   sec:authorize="isAuthenticated()"
   th:if="${question.author != null and #authentication.getPrincipal().getUsername() == question.author.username}"
   th:text="수정"></a>
```

이 버튼은 로그인한 사용자 중에서도 질문 작성자에게만 보인다.

여기에는 두 종류의 조건이 함께 쓰인다.

첫 번째는 Spring Security Thymeleaf 확장 문법이다.

```html
sec:authorize="isAuthenticated()"
```

현재 사용자가 로그인한 경우에만 이 요소가 렌더링된다.

두 번째는 작성자 검사다.

```html
th:if="${question.author != null and #authentication.getPrincipal().getUsername() == question.author.username}"
```

현재 로그인한 사용자의 username과 질문 작성자의 username이 같을 때만 버튼을 보여준다.

이 코드는 화면에서 버튼을 숨기는 역할이다. 실제 보안은 컨트롤러에서도 다시 검사해야 한다. 현재 `QuestionController.questionModify`에서도 작성자 검사를 하고 있으므로, 화면과 서버 양쪽에서 권한을 확인하는 구조다.

## 11. 질문 삭제 버튼

```html
<a href="javascript:void(0);" th:data-uri="@{|/question/delete/${question.id}|}"
   class="delete btn btn-sm btn-outline-secondary" sec:authorize="isAuthenticated()"
   th:if="${question.author != null and #authentication.getPrincipal().getUsername() == question.author.username}"
   th:text="삭제"></a>
```

삭제 버튼도 수정 버튼과 같은 권한 조건을 사용한다.

차이는 `th:href`를 바로 사용하지 않고 `th:data-uri`에 삭제 URL을 저장한다는 점이다. 이유는 삭제 전에 JavaScript 확인 창을 띄우기 위해서다.

사용자가 삭제 버튼을 클릭하면 아래 스크립트가 실행된다.

```javascript
if(confirm("정말로 삭제하시겠습니까?")) {
    location.href = this.dataset.uri;
}
```

확인을 누르면 `/question/delete/{id}` 주소로 이동하고, 취소하면 아무 일도 일어나지 않는다.

## 12. 답변 개수 표시

```html
<h5 class="border-bottom my-3 py-2"
    th:text="|${#lists.size(question.answerList)}개의 답변이 있습니다.|"></h5>
```

이 코드는 질문에 달린 답변 개수를 보여준다.

`question.answerList`는 질문 엔티티와 연결된 답변 목록이다. `#lists.size(question.answerList)`는 그 목록의 크기를 구한다.

여기서 사용된 `|...|` 문법은 Thymeleaf 리터럴 치환 문법이다.

```html
th:text="|${count}개의 답변이 있습니다.|"
```

이 문법을 사용하면 문자열 중간에 변수를 쉽게 넣을 수 있다.

## 13. 답변 목록 반복

```html
<div class="card my-3" th:each="answer : ${question.answerList}">
```

이 코드는 `question.answerList`에 들어 있는 답변들을 하나씩 반복하면서 답변 카드를 만든다.

`th:each`는 Thymeleaf의 반복문이다.

```html
th:each="answer : ${question.answerList}"
```

의미는 다음과 같다.

- `question.answerList`에서 답변을 하나씩 꺼낸다.
- 꺼낸 답변 하나를 `answer`라는 이름으로 사용한다.
- 이 `div` 전체를 답변 개수만큼 반복 렌더링한다.

예를 들어 답변이 3개라면 `<div class="card my-3">...</div>`가 3번 만들어진다.

## 14. 답변 앵커

```html
<a th:id="|answer_${answer.id}|"></a>
```

이 코드는 각 답변 위치로 바로 이동할 수 있는 HTML id를 만든다.

답변 id가 5이면 최종 HTML은 다음처럼 된다.

```html
<a id="answer_5"></a>
```

이 id는 답변 수정 또는 추천 후 해당 답변 위치로 돌아올 때 사용된다.

`AnswerController`에는 다음과 같은 redirect 코드가 있다.

```java
return String.format("redirect:/question/detail/%s#answer_%s", answer.getQuestion().getId(), answer.getId());
```

URL 뒤의 `#answer_5`는 브라우저에게 페이지 로딩 후 `id="answer_5"` 요소 위치로 스크롤하라는 뜻이다.

## 15. 답변 본문 마크다운 출력

```html
<div class="card-text" th:utext="${answerContentHtml[answer.id]}"></div>
```

이 코드는 현재 반복 중인 답변의 본문을 출력한다.

`answerContentHtml`은 컨트롤러에서 만들어진 Map이다.

```java
Map<Integer, String> answerContentHtml = question.getAnswerList().stream()
        .collect(Collectors.toMap(
                Answer::getId,
                answer -> commonUtil.markdown(answer.getContent())
        ));
```

즉, 구조는 대략 다음과 같다.

```text
{
  1: "<p>첫 번째 답변</p>",
  2: "<p>두 번째 답변</p>"
}
```

템플릿에서는 현재 답변의 id로 Map에서 값을 꺼낸다.

```html
${answerContentHtml[answer.id]}
```

답변도 질문처럼 마크다운을 HTML로 변환한 결과를 출력해야 하므로 `th:text`가 아니라 `th:utext`를 사용한다.

## 16. 답변 수정일 표시 부분의 주의점

현재 코드는 다음과 같다.

```html
<div th:if="${question.modifyDate != null}" class="badge bg-light text-dark p-2 text-start mx-3">
    <div class="mb-2">modified at</div>
    <div th:text="${#temporals.format(answer.modifyDate, 'yyyy-MM-dd HH:mm')}"></div>
</div>
```

이 코드는 출력값은 `answer.modifyDate`를 사용하지만, 표시 여부 조건은 `question.modifyDate`를 사용한다.

즉, 답변이 수정되어 `answer.modifyDate`가 있어도 질문이 수정된 적이 없어서 `question.modifyDate`가 `null`이면 답변 수정일이 표시되지 않는다.

의도상으로는 다음처럼 답변 기준 조건을 쓰는 것이 자연스럽다.

```html
<div th:if="${answer.modifyDate != null}" class="badge bg-light text-dark p-2 text-start mx-3">
```

이 문서는 코드 분석용이므로 실제 코드는 수정하지 않았다.

## 17. 답변 작성자 표시 부분의 주의점

현재 코드는 다음과 같다.

```html
<span th:if="${question.author != null}" th:text="${question.author.username}"></span>
```

이 코드는 답변 카드 안에 있지만 질문 작성자를 표시한다.

답변 영역에서는 일반적으로 답변 작성자를 보여주는 것이 맞다. 따라서 의도상으로는 다음처럼 `answer.author`를 사용하는 것이 자연스럽다.

```html
<span th:if="${answer.author != null}" th:text="${answer.author.username}"></span>
```

현재 코드 상태에서는 모든 답변에 질문 작성자 이름이 표시될 수 있다.

## 18. 답변 작성일 표시

```html
<div class="badge bg-light text-dark p-2 text-start">
    <div th:text="${#temporals.format(answer.createDate, 'yyyy-MM-dd HH:mm')}"></div>
</div>
```

현재 반복 중인 답변의 생성일을 출력한다.

답변 생성일은 `answer.createDate`에서 가져온다. 날짜 포맷 방식은 질문 작성일과 동일하게 `#temporals.format`을 사용한다.

## 19. 답변 추천 버튼

```html
<a href="javascript:void(0);" class="recommend btn btn-sm btn-outline-secondary"
   th:data-uri="@{|/answer/vote/${answer.id}|}">
    추천
    <span class="badge rounded-pill bg-success" th:text="${#lists.size(answer.voter)}"></span>
</a>
```

답변 추천 버튼이다.

질문 추천과 구조는 거의 같다. 차이는 URL이 `/question/vote/{id}`가 아니라 `/answer/vote/{id}`라는 점이다.

추천 수 역시 질문의 추천자가 아니라 답변의 추천자 목록을 기준으로 한다.

```html
th:text="${#lists.size(answer.voter)}"
```

## 20. 답변 수정 버튼

```html
<a th:href="@{|/answer/modify/${answer.id}|}" class="btn btn-sm btn-outline-secondary"
   sec:authorize="isAuthenticated()"
   th:if="${answer.author != null and #authentication.getPrincipal().getUsername() == answer.author.username}"
   th:text="수정"></a>
```

답변 수정 버튼은 로그인한 사용자이면서 해당 답변의 작성자인 경우에만 보인다.

질문 수정 버튼과 비교하면 객체만 다르다.

- 질문 수정: `question.author`
- 답변 수정: `answer.author`

이 버튼을 누르면 `/answer/modify/{answer.id}`로 이동한다.

## 21. 답변 삭제 버튼

```html
<a href="javascript:void(0);" th:data-uri="@{|/answer/delete/${answer.id}|}"
   class="delete btn btn-sm btn-outline-secondary" sec:authorize="isAuthenticated()"
   th:if="${answer.author != null and #authentication.getPrincipal().getUsername() == answer.author.username}"
   th:text="삭제"></a>
```

답변 삭제 버튼이다.

질문 삭제 버튼과 마찬가지로 직접 이동하지 않고 `data-uri`에 삭제 URL을 저장한다. 삭제 버튼에는 `delete` 클래스가 있으므로 아래 JavaScript의 삭제 확인 로직이 적용된다.

## 22. 답변 작성 폼

```html
<form th:action="@{|/answer/create/${question.id}|}" th:object="${answerForm}" method="post" class="my-3">
```

이 폼은 새 답변을 작성할 때 사용한다.

`th:action`은 폼 제출 주소를 만든다.

```html
@{|/answer/create/${question.id}|}
```

질문 id가 3이면 최종 action은 다음과 같다.

```html
/answer/create/3
```

폼이 제출되면 `AnswerController.createAnswer`가 실행된다.

```java
@PostMapping("/create/{id}")
public String createAnswer(Model model, @PathVariable("id") Integer id, @Valid AnswerForm answerForm, BindingResult bindingResult, Principal principal) {
    ...
}
```

`th:object="${answerForm}"`는 이 폼이 `answerForm` 객체와 연결되어 있다는 뜻이다. 내부의 `th:field="*{content}"`는 `answerForm.content` 필드와 연결된다.

## 23. 폼 오류 표시

```html
<div th:replace="~{form_errors :: formErrorsFragment}"></div>
```

이 코드는 `form_errors.html` 템플릿의 `formErrorsFragment` 조각을 현재 위치에 삽입한다.

입력값 검증에 실패했을 때 에러 메시지를 보여주는 공통 조각으로 보인다.

이 개념을 이해하려면 다음을 알아야 한다.

- `@Valid`는 폼 객체의 검증 애너테이션을 검사한다.
- `BindingResult`는 검증 실패 결과를 담는다.
- Thymeleaf fragment는 반복되는 HTML 조각을 재사용하게 해준다.
- `th:replace`는 현재 태그를 다른 fragment의 내용으로 교체한다.

## 24. 비로그인 사용자용 textarea

```html
<textarea sec:authorize="isAnonymous()" disabled th:field="*{content}" class="form-control" rows="10"></textarea>
```

이 textarea는 비로그인 사용자에게 보여진다.

`sec:authorize="isAnonymous()"`는 현재 사용자가 로그인하지 않은 경우에만 렌더링한다.

`disabled`가 있으므로 입력할 수 없다. 비로그인 사용자는 답변을 작성할 수 없다는 것을 화면에서 표현한다.

`th:field="*{content}"`는 현재 폼 객체인 `answerForm`의 `content` 필드와 연결된다.

## 25. 로그인 사용자용 textarea

```html
<textarea sec:authorize="isAuthenticated()" th:field="*{content}" class="form-control" rows="10"></textarea>
```

이 textarea는 로그인한 사용자에게 보여진다.

비로그인 사용자용 textarea와 달리 `disabled`가 없다. 따라서 로그인한 사용자는 내용을 입력하고 답변을 등록할 수 있다.

## 26. 답변 등록 버튼

```html
<input type="submit" value="답변등록" class="btn btn-primary my-2">
```

폼 제출 버튼이다.

클릭하면 `form`의 `method="post"`와 `th:action`에 따라 `/answer/create/{question.id}`로 POST 요청이 전송된다.

## 27. 삭제 확인 JavaScript

```javascript
const delete_elements = document.getElementsByClassName("delete");
Array.from(delete_elements).forEach(function(element) {
    element.addEventListener('click', function() {
        if(confirm("정말로 삭제하시겠습니까?")) {
            location.href = this.dataset.uri;
        };
    });
});
```

이 스크립트는 `delete` 클래스를 가진 모든 요소에 클릭 이벤트를 등록한다.

대상 요소는 질문 삭제 버튼과 답변 삭제 버튼이다.

동작 순서는 다음과 같다.

1. `document.getElementsByClassName("delete")`로 삭제 버튼들을 찾는다.
2. `Array.from(...)`으로 반복 가능한 배열 형태로 바꾼다.
3. 각 요소에 클릭 이벤트를 등록한다.
4. 클릭되면 `confirm` 창을 띄운다.
5. 사용자가 확인을 누르면 `this.dataset.uri` 주소로 이동한다.

`this.dataset.uri`는 HTML의 `data-uri` 속성 값을 읽는다.

예를 들어 HTML이 다음과 같다면

```html
<a data-uri="/question/delete/3" class="delete">삭제</a>
```

JavaScript의 `this.dataset.uri` 값은 `/question/delete/3`이다.

## 28. 추천 확인 JavaScript

```javascript
const recommend_elements = document.getElementsByClassName("recommend");
Array.from(recommend_elements).forEach(function(element) {
    element.addEventListener('click', function() {
        if(confirm("정말로 추천하시겠습니까?")) {
            location.href = this.dataset.uri;
        };
    });
});
```

이 스크립트는 `recommend` 클래스를 가진 모든 요소에 클릭 이벤트를 등록한다.

대상 요소는 질문 추천 버튼과 답변 추천 버튼이다.

삭제 스크립트와 구조는 같고, 확인 메시지만 다르다.

## 핵심 Thymeleaf 개념 정리

이 파일을 이해하려면 다음 Thymeleaf 문법을 알아야 한다.

| 문법 | 의미 |
| --- | --- |
| `th:text` | 값을 HTML escape 후 텍스트로 출력 |
| `th:utext` | 값을 escape하지 않고 HTML로 출력 |
| `th:if` | 조건이 참일 때만 렌더링 |
| `th:each` | 컬렉션 반복 |
| `th:href` | 동적으로 href 속성 생성 |
| `th:action` | 동적으로 form action 생성 |
| `th:field` | 폼 필드를 객체 속성과 바인딩 |
| `th:object` | 폼에서 사용할 객체 지정 |
| `th:data-uri` | `data-uri` 같은 HTML data 속성 생성 |
| `th:replace` | 다른 fragment로 현재 태그 교체 |
| `${...}` | 모델 객체의 값 접근 |
| `*{...}` | `th:object` 기준으로 필드 접근 |
| `@{...}` | URL 생성 |
| `|...|` | 문자열 안에 변수 삽입 |

## Spring Security Thymeleaf 개념

이 파일에는 `sec:authorize`가 사용된다.

```html
sec:authorize="isAuthenticated()"
sec:authorize="isAnonymous()"
```

이 문법은 `thymeleaf-extras-springsecurity6` 의존성이 있어야 사용할 수 있다.

- `isAuthenticated()`: 로그인한 사용자
- `isAnonymous()`: 로그인하지 않은 사용자
- `#authentication`: 현재 인증 정보 객체
- `#authentication.getPrincipal().getUsername()`: 현재 로그인한 사용자의 username

화면에서 버튼을 숨기는 것은 사용자 경험을 위한 것이다. 실제 권한 보호는 컨트롤러의 `@PreAuthorize`와 작성자 검증으로 처리해야 한다.

## Bootstrap 개념

이 파일은 Bootstrap 클래스를 많이 사용한다.

| 클래스 | 의미 |
| --- | --- |
| `container` | 중앙 정렬된 반응형 컨테이너 |
| `my-3` | 위아래 margin |
| `py-2` | 위아래 padding |
| `card` | 카드 컴포넌트 |
| `card-body` | 카드 내부 본문 |
| `card-text` | 카드 본문 텍스트 |
| `d-flex` | flexbox 적용 |
| `justify-content-end` | 오른쪽 정렬 |
| `badge` | 작은 배지 스타일 |
| `bg-light` | 밝은 배경 |
| `text-dark` | 어두운 글자 |
| `btn` | 버튼 스타일 |
| `btn-sm` | 작은 버튼 |
| `btn-primary` | 주요 버튼 색상 |
| `btn-outline-secondary` | 외곽선 보조 버튼 |
| `form-control` | 입력 필드 스타일 |

## 마크다운 렌더링 개념

질문과 답변 본문은 사용자가 작성한 마크다운 원문을 DB에 저장하고, 상세 화면을 보여줄 때 HTML로 변환한다.

변환은 `CommonUtil.markdown`에서 수행한다.

```java
public String markdown(String markdown) {
    Parser parser = Parser.builder().build();
    Node document = parser.parse(markdown);
    HtmlRenderer renderer = HtmlRenderer.builder().build();
    return renderer.render(document);
}
```

흐름은 다음과 같다.

1. 사용자가 textarea에 마크다운 문법으로 글을 작성한다.
2. 서버는 그 내용을 문자열 그대로 저장한다.
3. 상세 화면 요청이 들어오면 컨트롤러가 문자열을 HTML로 변환한다.
4. 템플릿은 `th:utext`로 변환된 HTML을 출력한다.

마크다운을 렌더링할 때 `th:utext`를 사용하는 이유는 변환 결과가 이미 HTML이기 때문이다.

## 컨트롤러와 템플릿의 관계

템플릿은 혼자 동작하지 않는다. 컨트롤러가 모델에 어떤 이름으로 데이터를 담아주는지가 중요하다.

현재 `question_detail.html`은 최소한 다음 모델 값을 기대한다.

```java
model.addAttribute("question", question);
model.addAttribute("questionContentHtml", questionContentHtml);
model.addAttribute("answerContentHtml", answerContentHtml);
```

템플릿에서 `${question.subject}`를 사용한다는 것은 모델 안에 `question`이라는 이름의 객체가 있어야 한다는 뜻이다.

템플릿에서 `${answerContentHtml[answer.id]}`를 사용한다는 것은 모델 안에 `answerContentHtml`이라는 Map이 있어야 한다는 뜻이다.

## 현재 코드에서 주의할 점

현재 파일을 읽을 때 특히 주의할 부분은 세 가지다.

첫째, 답변 수정일 표시 조건이 질문 기준이다.

```html
th:if="${question.modifyDate != null}"
```

답변 수정일을 표시하려는 의도라면 `answer.modifyDate`를 기준으로 조건을 확인해야 자연스럽다.

둘째, 답변 작성자 표시가 질문 작성자 기준이다.

```html
th:text="${question.author.username}"
```

답변 카드 안에서는 일반적으로 `answer.author.username`을 보여주는 것이 맞다.

셋째, `AnswerController.createAnswer`에서 검증 오류가 발생하면 `question_detail`을 바로 반환한다.

```java
if (bindingResult.hasErrors()) {
    model.addAttribute("question", question);
    return "question_detail";
}
```

그런데 현재 템플릿은 `questionContentHtml`과 `answerContentHtml`도 필요로 한다. 검증 오류 시 이 값들이 모델에 없으면 상세 화면 렌더링 중 문제가 생길 수 있다. 이 문제를 해결하려면 오류로 `question_detail`을 반환하는 경로에서도 동일한 모델 데이터를 넣어줘야 한다.

이 문서는 분석만 수행하며 실제 코드는 수정하지 않았다.

## 요약

`question_detail.html`은 질문 상세 화면의 중심 템플릿이다.

이 파일은 컨트롤러에서 전달받은 `question`, `questionContentHtml`, `answerContentHtml`, `answerForm`을 사용해서 질문과 답변을 화면에 출력한다. Thymeleaf 문법으로 조건문, 반복문, URL 생성, 폼 바인딩을 처리하고, Spring Security 확장 문법으로 로그인 여부와 작성자 권한에 따라 버튼 표시를 제어한다.

질문과 답변 본문은 마크다운을 HTML로 변환한 뒤 `th:utext`로 출력한다. 이 구조를 이해하려면 Thymeleaf의 모델 접근 방식, `th:text`와 `th:utext`의 차이, Spring MVC의 컨트롤러-모델-템플릿 흐름, Spring Security의 인증 객체 접근, Bootstrap의 화면 구성 클래스를 함께 이해해야 한다.
