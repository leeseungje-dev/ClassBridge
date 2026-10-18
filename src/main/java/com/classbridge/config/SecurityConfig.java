package com.classbridge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/*
 * <login Configuration>
 * 페이지 접근 제한 설정 (home 과 signup 은 누구나 접근 가능 / 나머지는 login 필요)
 * author : LEE SEUNGJE
 * date : 2026.10.14 ~ 2026.10.17
 */

// 서버 시작 시 메소드 까지 실행되어 메소드 반환 체인 객체까지 스프링에 등록해둠
// 브라우저 요청 시 -> 체인으로 보안 처리 -> 접근 허용 시 Controller 로 전달
@Configuration
public class SecurityConfig {

	// method
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		
		// 경로 요청별 접근 규칙 설정
		http.authorizeHttpRequests((auth) -> {
			auth.requestMatchers("/", "/signup", "/error").permitAll();
			auth.anyRequest().authenticated();
		});

		// 기본 폼 로그인 기능 설정
		http.formLogin(Customizer.withDefaults());
		
		// 로그아웃 시 "/"경로로 이동하도록 설정
		http.logout((logout) -> {
			logout.logoutSuccessUrl("/");
		});

		// 설정한 내용으로 보안 체인을 만들어 반환
		return http.build();
	}
}

/* 
 * HttpSecurity http : 전체 보안 설정 도구
 * auth(인증) : 그중 '요청별 접근 규칙' 등록 도구 (실제 타입은 표기를 생략한 매개변수)
 * 
 * [http 메소드 설명]
 * http . authorizeHpptRequests(...) : '경로별 접근 규칙' 작성
 * 		ㄴ> HttpSecurity 객체 반환
 * http . formLogin(...) : '폼 로그인 기능' 설정
 * 		ㄴ> HttpSecurity 객체 반환
 * http . build() : 지금까지 규칙으로 '필터 체인' 생성
 * 		ㄴ> DefaultSecurityFilterChain 객체 반환
 * 
 * [auth 메소드 설명]
 * auth . requestMatchers(...) : 규칙을 적용할 '경로 요청' 선택
 * auth . permitAll() : 모두 접근 허용
 * auth . anyRequest() : 이전 규칙 '이외의 나머지 요청' 선택
 * auth . authenticated() : 선택한 요청에 로그인 조건 지정
 */
 