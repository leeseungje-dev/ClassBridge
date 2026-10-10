package com.classbridge.user;

/*
Writer : LEE SEUNGJE
Date : 2026.10.08 ~ 2026.10.10
 */

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;	// PK 값을 자동으로 생성하도록 지정
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Column;	// EMAIL, PASSWORD field 지정 시 필요
import java.time.LocalDateTime;	// CREATED_AT field 지정 시 필요

@Entity	// JPA가 관리하는 클래스로 지정
@Table(name = "APP_USERS")	// 연결할 테이블 지정

public class User {

	// 이 Entity를 식별하는 PK = id field 입니다.
	@Id
	// PK 생성 방식 지정 (ID를 직접 입력하지 않고 지정한 방식으로 발급받아 사용)
	@GeneratedValue(
			strategy = GenerationType.SEQUENCE,
			generator = "userIdGenerator"
	)
	// 위의 SEQUENCE의 구체적인 설정 (Java 에서 이 생성 설정을 부를 이름, Oracle에서 만들어둔 시퀀스 이름, ID를 한 개씩 할당)
	@SequenceGenerator(
			name = "userIdGenerator",
			sequenceName = "APP_USERS_SEQ",
			allocationSize = 1
	)
	
	// field
	private Long id;
	
	@Column(name = "EMAIL", nullable = false, length = 255, unique = true)
	private String email;
	
	@Column(name = "PASSWORD", nullable = false, length = 255)
	private String password;
	
	@Column(name = "ROLE", nullable = false, length = 20)
	private String role = "INSTRUCTOR";
	
	@Column(name = "CREATED_AT", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;
	
	// constructor
	protected User() {
	}
	
	public User(String email, String password) {
		this.email = email;
		this.password = password;
	}
	
	// method
	// Getter
	public Long getId() {
		return id;
	}
	
	public String getEmail() {
		return email;
	}
	
	public String getPassword() {
		return password;
	}

	public String getRole() {
		return role;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	
}