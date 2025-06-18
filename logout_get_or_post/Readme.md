# 로그아웃은 왜 GET 으로 하면 안될까?

&nbsp;스프링 시큐리티의 로그아웃은 POST 를 기본 요청으로 정의하고 있다. 
여기서 GET 으로 하지 않는 이유가 궁금해졌고, 이를 실습을 통해서 알아보고자 한다.

## 실습
&nbsp;이번 실습에서는 로그아웃 요청 방식을 POST 에서 GET 으로 바꿔가며 어떤 차이가 발생하는지 확인해봅니다.
홈페이지에 접속해서 나타나는 버튼들은 각각 로그인과 로그아웃 기능을 합니다. 
이미 POST 로의 로그인 흐름 검증을 마친 상황이며, GET 으로 변경해서 비교합니다.

&nbsp;아래와 같은 기존 POST 요청을(method) GET 으로 바꿔주기만 하면 됩니다.
```html
<!--<form th:action="@{/logout}" method="post">-->
<form th:action="@{/logout}" method="get">
    <input type="hidden"
           th:name="${_csrf.parameterName}"
           th:value="${_csrf.token}" />
    <button type="submit">로그아웃</button>
</form>
```

&nbsp;CSRF 토큰의 경우 GET 요청에 대해서는 불필요하다고 생각할 수 있지만 logout 을 GET 으로 임의변경했기 때문에 기존의 인증 방식이 유지됩니다.
```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/", "/favicon.ico",
                "/login", "/logout",
                "/css/**", "/js/**", "/images/**",
                "/error"
            ).permitAll()
            .anyRequest().authenticated()
        )
        .formLogin(AbstractAuthenticationFilterConfigurer::permitAll
        )
        .logout(logout->logout
            .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET")) // GET 으로 요청을 바꿈
            .permitAll()
        );
    return http.build();
}
```
&nbsp;그래서 메서드만 바뀌고 동작은 동일하게 유지되므로 토큰이 필요해집니다.

&nbsp;원래 의미에서는 토큰도 불필요한 요청이므로 시큐리티에 CSRF 를 제거해봅시다.
시큐리티의 기존 설정에 아래 코드를 추가합니다.

```java
.csrf(AbstractHttpConfigurer::disable)
```

&nbsp;기존 html 요청에서 토큰도 제거합니다.
```html
<!--            <input type="hidden"-->
<!--                   th:name="${_csrf.parameterName}"-->
<!--                   th:value="${_csrf.token}" />-->
```

&nbsp;이제부터는 일반적인 GET /logout 요청이 되었습니다. 
이 설정이 원래 의도한 GET 에 의한 로그아웃 과정인데요. 이게 왜 문제일까요?
의도하지 않은 상황에서 로그아웃 요청이 발생할 수 있기 때문입니다. 다음을 봅시다.

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8"/>
    <title>이미지 로그아웃 페이지</title>
</head>
<body>
<header>
    <h1>로그아웃 관련 페이지</h1>
    <nav>
        <a th:href="@{/}">Home</a>
    </nav>
</header>

<main>
    <h2>로그아웃 관련 이미지</h2>
    <img th:src="@{/logout}" alt="이미지로 로그아웃이 됩니다." />
</main>
</body>
</html>
```

&nbsp;아래 페이지는 언뜻보면 일반적일 수 있으나 img 태그를 보면 src 속성값이 /logout 경로로 되어있습니다. 
브라우저가 해당 페이지 파일을 읽으면서 자동으로 GET /logout 요청을 보내게 됩니다. 
그러므로 페이지에 접속만해도 의도치않게 로그아웃 요청이 처리되는 문제가 발생하게 되는겁니다.

&nbsp;이처럼 GET 방식으로 로그아웃이 처리되면 사용자의 의도와 관계없이 로그아웃 요청이 발생할 수 있습니다. 
이런 공격을 CSRF 공격이라고 하며, 만약 악의적인 사용자가 스크립트 삽입까지 가능하다면(XSS), 보다 다양한 방식으로 비슷한 문제가 발생할 수 있습니다. 
결국 로그아웃과 같이 인증 상태를 변경하는 요청은 반드시 POST 방식으로 제한해야 보안상 안전하다고 할 수 있습니다.

## RESTful 관점에서 본 로그아웃 요청 방식
&nbsp; REST 아키텍처 스타일에서는 각 메서드의 역할이 명확하게 구분되어 있습니다.
GET 은 리소스를 조회할 때만 사용해야 하며, 서버의 상태를 변경해야 하는 작업에는 사용해서는 안 됩니다.
이는 멱등성과 안정성이라는 REST 의 기본 원칙 때문입니다. 
멱등성이란, 같은 요청을 여러 번 보내더라도 시스템의 상태가 변하지 않는 속성을 의미합니다.
GET 요청은 멱등하고 안전해야 하므로, 반복 호출되어도 어떠한 변화가 발생해서는 안 됩니다.
이때, 서버 내부의 로그 기록 등은 이에 포함되지 않습니다.

&nbsp;반면, 로그아웃은 사용자의 인증 정보를 무효화라는 변화를 주는 행위입니다.
따라서 RESTful 하게 설계하려면, 이러한 상태 변경 작업에서 POST 또는 DELETE 같은 적절한 메서드를 사용합니다.

&nbsp;이상으로, 로그아웃 요청이 왜 POST 여야 하는지에 대해 보안과 RESTful 설계 원칙 관점으로 알아보았습니다.

