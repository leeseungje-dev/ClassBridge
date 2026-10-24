package com.classbridge.user;

import org.springframework.stereotype.Service;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.transaction.annotation.Transactional;

/*
 * 회원가입 처리를 담당 (가입한 이메일인지 확인 후, 비밀번호를 해시한 뒤 저장)
 * author : LEE SEUNGJE
 * date : 2026.10.13 ~ 2026.10.14
 */

// @Service : 업무 처리(중복 확인, 가입처리)를 담당하는 클래스의 객체를 Spring 이 만들어서 등록 (생성시 매개변수를 스스로 찾고, Controller 가 필요로하면 연결)
@Service
public class UserService {
	
	// field
	private final UserMapper userMapper;
	// PasswordEncoder : 비밀번호를 해시, 검증하는 도구객체
	private final PasswordEncoder passwordEncoder;
	
	// constructor
	public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
	}
	
	// method
	public boolean isEmailRegistered(String email) {
		User existingUser = userMapper.findUserByEmail(email);
		return existingUser != null;
	}
	
	@Transactional
	public Long registerUser(String email, String rawPassword) {
		// 필수 입력값 확인
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("メールアドレスを入力してください。");
		}
		if (rawPassword == null || rawPassword.isBlank()) {
			throw new IllegalArgumentException("パスワードを入力してください。");
		}
		
		// BCrypt 가 처리하는 비밀번호 길이 제한 확인
		if (rawPassword.getBytes().length > 72) {
			throw new IllegalArgumentException("パスワードは72バイト以内で入力してください。\r\n半角英数字のみの場合は72文字までです。");
		}
		
		// email 앞뒤 공백 제거
		String normalizedEmail = email.strip();
		
		// 이미 가입된 이메일이면 중단
		if (this.isEmailRegistered(normalizedEmail)) {
			throw new IllegalArgumentException("このメールアドレスは既に登録されています。");
		}
		
		// 원문 비밀번호를 해시로 변환
		String encodedPassword = passwordEncoder.encode(rawPassword);
		
		// 해시된 비밀번호로 강사 객체 생성
		User user = new User(normalizedEmail, encodedPassword);
		
		// DB에 저장
		userMapper.insertUser(user);
		
		// 발급된 강사 ID 반환
		return user.getId();
	}
	
	// email 로 강사 정보 조회
	// 로그인한 강사 email -> 강사 id 조회 -> id 로 학생 목록 조회 -> 학생 목록 화면 표시
	public User findUserByEmail(String email) {
		
		User user = userMapper.findUserByEmail(email);
		
		if (user == null) {
			throw new IllegalArgumentException("ユーザーが見つかりません。");
		}
		
		return user;
	}
	
}