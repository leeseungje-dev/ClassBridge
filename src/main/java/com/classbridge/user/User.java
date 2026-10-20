package com.classbridge.user;

import lombok.Getter;

import java.time.LocalDateTime;	// CREATED_AT field 지정 시 field type (= Java 객체 안에 날짜와 시간을 담기)

/*
 * author : LEE SEUNGJE
 * date : 2026.10.08 ~ 2026.10.10
 */

@Getter	// lombok이 getter 자동 생성
public class User {

	// field
	private Long id;
	private String email;
	private String password;
	private String role = "INSTRUCTOR";
	private LocalDateTime createdAt;
	
	// constructor
	protected User() {
	}
	
	public User(String email, String password) {
		this.email = email;
		this.password = password;
	}
	
}