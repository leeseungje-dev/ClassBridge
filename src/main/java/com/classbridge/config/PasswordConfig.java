package com.classbridge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/*
 * 비밀번호 처리(해시 처리, 검증) 설정 클래스를 Spring 에 등록
 * author : LEE SEUNGJE
 * date : 2026.10.14 ~
 */

// Configuration : 설정(인코더 객체) 등록
@Configuration
public class PasswordConfig {
	
	// method
	// Bean : 메서드가 반환한 객체를 Spring 에 등록
	@Bean
	// PasswordEncoder : (interface) 비밀번호 해시, 검증 메소드의 규칙 정의
	// BCryptPasswordEncoder : (class) 메소드의 실제 기능 구현 클래스, Polymorphism
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
}