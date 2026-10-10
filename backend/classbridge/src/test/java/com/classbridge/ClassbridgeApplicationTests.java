package com.classbridge;

/*
Writer : LEE SEUNGJE
Date : 2026.10.10 ~ 
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

// 원래 있던거.
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
// 원래 있던거.
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.classbridge.user.User;
import com.classbridge.user.UserRepository;

import jakarta.persistence.EntityManager;

// 원래 있던 양식
@SpringBootTest
class ClassbridgeApplicationTests {

	// field
	// Spring이 준비한 객체를 필드에 넣음
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private EntityManager entityManager;
	
	// method
	// (원래 있던 양식) Test로 실행할 메소드
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
		
		// 저장 요청 후 실제 INSERT 실행 (저장을 요청하고 대기중인 SQL을 DB에 실행)
		userRepository.saveAndFlush(user);
		
		Long savedId = user.getId();
		assertNotNull(savedId);
		
		// 메모리의 객체를 그대로 반환하지 않도록 JPA 관리 상태 비우기 (JPA가 관리하던 객체를 분리, DB 데이터를 삭제하는 것은 아님)
		entityManager.clear();
		
		// DB에서 다시 조회
		// findById() : ID로 조회
		// orElseThrow() : 조회 결과가 없으면 예외를 Throw해 테스트 실패
		User foundUser = userRepository.findById(savedId).orElseThrow();

		// assertEquals : 기대값과 실제값이 같은지 검사
        assertEquals(savedId, foundUser.getId());
        assertEquals(email, foundUser.getEmail());
        assertEquals(passwordValue, foundUser.getPassword());
        assertEquals("INSTRUCTOR", foundUser.getRole());
        // assertNotNull() : 값이 NULL이 아닌지 검사
        assertNotNull(foundUser.getCreatedAt());
	}
}