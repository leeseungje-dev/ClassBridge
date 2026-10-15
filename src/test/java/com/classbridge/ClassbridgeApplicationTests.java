package com.classbridge;

// MyBatis
import com.classbridge.user.UserMapper;
// 회원가입
import com.classbridge.user.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.UUID;

// 원래 있던거.
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
// 원래 있던거.
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.classbridge.user.User;

/*
 * author : LEE SEUNGJE
 * date : 2026.10.10 ~ 2026.10.15
 */

// Test 에 필요한 Spring 환경(DB 연결, Repository 등)을 준비하라는 표시
@SpringBootTest
class ClassbridgeApplicationTests {

	// field
	// proxy : 추상 인터페이스지만 MyBatis가 이 인터페이스를 구현하는 객체를 자동으로 만들어줌
	@Autowired
	private UserMapper userMapper;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	// method
	// Test로 실행할 메소드
	@Test
	void contextLoads() {
	}
	
	@Test
	// 이 Test의 DB 작업을 하나로 묶고, 종료 후 기본적으로 롤백
	@Transactional
	void saveAndFindUser() {
		// Test 마다 다른 이메일을 사용
		String email = "test-" + UUID.randomUUID() + "@example.com";
		
		// 저장, 조회 확인용 가짜 값, 실제 pw나 hash 값은 아님
		String passwordValue = "test-only-hash-placeholder";
		
		User user = new User(email, passwordValue);
		
		// MyBatis로 저장하고, 추가된 행 수 확인
		int insertedRows = userMapper.insertUser(user);
		assertEquals(1, insertedRows);
		
		// selectkey가 User 객체에 넣은 ID 확인
		Long savedId = user.getId();
		assertNotNull(savedId);
		
		// MyBatis로 DB에서 다시 조회
		User foundUser = userMapper.findUserById(savedId);
		// JUnit(테스트 도구)의 기능 : null인지 검사 / null이면 테스트 실패
		assertNotNull(foundUser);

		// assertEquals : 저장값이 조회값과 같은지 검사
        assertEquals(savedId, foundUser.getId());
        assertEquals(email, foundUser.getEmail());
        assertEquals(passwordValue, foundUser.getPassword());
        assertEquals("INSTRUCTOR", foundUser.getRole());
        assertNotNull(foundUser.getCreatedAt());
        
        // 이메일로 다시 조회
        User foundByEmail = userMapper.findUserByEmail(email);
        
        // 조회결과가 null이 아닌지, 저장한 강사가 맞는지 확인
        assertNotNull(foundByEmail);
        assertEquals(savedId, foundByEmail.getId());
        assertEquals(email, foundByEmail.getEmail());
	}
	
	@Test
	@Transactional
	void checkEmailRegistration() {
		String email = "test-" + UUID.randomUUID() + "@example.com";
		
		// 저장 전 : 아직 가입되지 않은 이메일 검사
		assertFalse(userService.isEmailRegistered(email));
		
		// Test용 강사 저장
		User user = new User(email, "test-only-hash-placeholder");
		userMapper.insertUser(user);
		
		// 저장 후 : 이미 가입된 이메일 검사
		assertTrue(userService.isEmailRegistered(email));
	}
	
	@Test
	@Transactional
	void registerUserStoresHashedPassword() {
		String email = "test-" + UUID.randomUUID() + "@example.com";
		String rawPassword = "Test-password-123!";
		
		// 실제 회원가입 처리 호출
		Long userId = userService.registerUser(email, rawPassword);
		assertNotNull(userId);
		
		// 저장된 강사 조회
		User savedUser = userMapper.findUserById(userId);
		assertNotNull(savedUser);
		assertEquals(email, savedUser.getEmail());
		
		// password 가 해시처리가 됐는지
		assertNotEquals(rawPassword, savedUser.getPassword());
		
		// 저장된 해시가 입력한 비밀번호와 일치하는지 확인
		assertTrue(passwordEncoder.matches(rawPassword, savedUser.getPassword()));
	}
	
	// email 중복 가입 거절 예외 테스트
	@Test
	@Transactional
	void rejectDuplicateEmailResgistration() {
		String email = "Test-" + UUID.randomUUID() + "@example.com";
		
		// 첫번째 가입 : 성공
		userService.registerUser(email, "First-password-123!");
		
		// 두번째 가입 : 예외 발생
		boolean duplicateRejected = false;
		
		try {
			// 같은 이메일로 가입 시도
			userService.registerUser(email, "Second-password-456!");
		} catch (IllegalArgumentException e){
			// 예외가 발생하면 거절로 표시
			duplicateRejected = true;
		}
		
		assertTrue(duplicateRejected);
	}
}