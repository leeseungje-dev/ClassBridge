package com.classbridge;

import com.classbridge.user.User;
import com.classbridge.student.Student;

// MyBatis
import com.classbridge.user.UserMapper;
import com.classbridge.student.StudentMapper;

// 회원가입
import com.classbridge.user.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.UUID;
import java.util.List;
import java.time.LocalDate;

// 원래 있던거.
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
// 원래 있던거.
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

/*
 * author : LEE SEUNGJE
 * date : 2026.10.10 ~ 2026.10.19
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
	
	@Autowired
	private StudentMapper studentMapper;
	
	// method
	// Test 로 실행할 메소드
	@Test
	void contextLoads() {
	}
	
	// 이 Test 의 DB 작업을 하나로 묶고, 종료 후 기본적으로 롤백
	@Test
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
	void rejectDuplicateEmailRegistration() {
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
	
	// 학생 1명이 저장되는지
	// 등록한 강사 id 로 조회하면 학생 정보가 나오는지
	// 다른 강사 id 로 조회하면 null 나오는지
	@Test
	@Transactional
	void saveAndFindStudent() {
		// 서로 다른 강사 두명 준비
		User instructor = new User(
				"teacher-" + UUID.randomUUID() + "@example.com",
				"test-only-hash-placeholder"
			);
		userMapper.insertUser(instructor);
		
		User otherInstructor = new User(
				"other-" + UUID.randomUUID() + "@example.com",
				"test-only-hash-placeholder"
			);
		userMapper.insertUser(otherInstructor);
		
		// 첫 번째 강사의 학생 등록
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				LocalDate.of(2026, 10, 18),
				"初級",
				"日常会話を中心に学習"
			);
		
		int insertedRows = studentMapper.insertStudent(student);
		assertEquals(1, insertedRows);
		assertNotNull(student.getId());
		
		// 해당 강사 id 로 조회 시 학생이 나오는지
		Student foundStudent = studentMapper.findStudentById(student.getId(), instructor.getId());
		
		assertNotNull(foundStudent);
		// 각각 필드 대조
		assertEquals(student.getId(), foundStudent.getId());
		assertEquals(instructor.getId(), foundStudent.getInstructorId());
		assertEquals("山田太郎", foundStudent.getName());
		assertEquals(LocalDate.of(2026, 10, 18), foundStudent.getLearningStartDate());
		assertEquals("初級", foundStudent.getCurrentLevel());
		assertEquals("日常会話を中心に学習", foundStudent.getInstructorMemo());
		assertNotNull(foundStudent.getCreatedAt());
		
		// 다른 강사 id 로 조회 시 학생이 안 나오는지
		Student inaccessibleStudent = studentMapper.findStudentById(student.getId(), otherInstructor.getId());
		assertNull(inaccessibleStudent);
	}
	
	// 강사 두 명, 학생 한 명 등록
	// Setter 로 객체 수정 내용 설정
	// 다른 강사 업데이트 시도 -> 실패 -> DB 그대로인지 검사
	// 해당 강사 업데이트 시도 -> 성공 -> DB 변화 있는지 검사
	// 수정 대상 아닌 값은 그대로인지 검사
	@Test
	@Transactional
	void updateStudentOnlyForOwner() {
		// 강사 두 명 준비
		User instructor = new User(
				"teacher-" + UUID.randomUUID() + "@example.com",
				"test-only-hash-placeholder"
			);
		userMapper.insertUser(instructor);
		
		User otherInstructor = new User(
				"other-" + UUID.randomUUID() + "@example.com",
				"test-only-hash-placeholder"
				);
		userMapper.insertUser(otherInstructor);
		
		// 첫 번째 강사 학생 등록
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				LocalDate.of(2026, 10, 18),
				"初級",
				"変更前のメモ"
			);
		studentMapper.insertStudent(student);
		
		// Java 객체에 수정할 내용 설정
		student.setName("山田花子");
		student.setLearningStartDate(LocalDate.of(2026, 10, 19));
		student.setCurrentLevel("中級");
		student.setInstructorMemo("会話練習を増やす");
		
		// 검사 1 : 다른 강사 id 로 수정 요정 -> 거절 예정
		int rejectedRows = studentMapper.updateStudent(
				student,
				otherInstructor.getId()
			);
		assertEquals(0, rejectedRows);
		
		// DB 변화 없는지
		Student unchanged = studentMapper.findStudentById(
				student.getId(),
				instructor.getId()
			);
		
		assertNotNull(unchanged);
		assertEquals("山田太郎", unchanged.getName());
		assertEquals(LocalDate.of(2026, 10, 18), unchanged.getLearningStartDate());
		assertEquals("初級", unchanged.getCurrentLevel());
		assertEquals("変更前のメモ", unchanged.getInstructorMemo());
		
		// 검사 2 : 소유 강사 id 로 같은 수정 요청 -> 성공 예정
		int updatedRows = studentMapper.updateStudent(
				student,
				instructor.getId()
			);
		assertEquals(1,updatedRows);
		
		// DB update 성공인지
		Student updated = studentMapper.findStudentById(
				student.getId(),
				instructor.getId()
			);
		
		assertNotNull(updated);
		assertNotNull(updated.getCreatedAt());
		assertEquals("山田花子", updated.getName());
		assertEquals(LocalDate.of(2026, 10, 19), updated.getLearningStartDate());
		assertEquals("中級", updated.getCurrentLevel());
		assertEquals("会話練習を増やす", updated.getInstructorMemo());
		
		// 수정 대상이 아닌 값(학생 id, 소유 강사 id, 등록 시각)은 그대로인지
		assertEquals(student.getId(), updated.getId());
		assertEquals(instructor.getId(), updated.getInstructorId());
		assertEquals(unchanged.getCreatedAt(), updated.getCreatedAt());
		
	}
	
	// 강사 두 명, 학생 한 명 등록
	// 다른 강사 삭제 시도 -> 거절 -> 학생 남아 있는지 검사
	// 소유 강사 삭제 시도 -> 성공 -> 학생 사라졌는지 검사
	@Test
	@Transactional
	void deleteStudentOnlyForOwner() {
		// 강사 두 명 준비
		User instructor = new User(
				"teacher-" + UUID.randomUUID() + "@example.com",
				"test-only-hash-placeholder"
			);
		userMapper.insertUser(instructor);
		
		User otherInstructor = new User(
				"other-" + UUID.randomUUID() + "@example.com",
				"test-only-hash-placeholder"
				);
		userMapper.insertUser(otherInstructor);
		
		// 첫 번째 강사 학생 등록
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				LocalDate.of(2026, 10, 19),
				"初級",
				"削除テスト用のメモ"
			);
		studentMapper.insertStudent(student);
		
		// 검사 1 : 다른 강사 id 로 삭제 요청 -> 거절 예정
		int rejectedRows = studentMapper.deleteStudent(
				student.getId(), 
				otherInstructor.getId()
			);
		assertEquals(0, rejectedRows);
		
		// DB 학생 남아 있는지
		Student remainingStudent = studentMapper.findStudentById(
				student.getId(),
				instructor.getId()
			);
		assertNotNull(remainingStudent);
		
		// 검사 2 : 소유 강사 id 로 삭제 요청 -> 성공 예정
		int deletedRows = studentMapper.deleteStudent(
				student.getId(),
				instructor.getId()
			);
		assertEquals(1, deletedRows);
		
		// DB 학생 삭제됐는지
		Student deletedStudent = studentMapper.findStudentById(
				student.getId(),
				instructor.getId()
			);
		assertNull(deletedStudent);
		
	}
	
	// 학생이 없으면 빈 목록인지 검사
	// 강사 두 명의 학생을 각각 등록
	// 내 학생만 id 내림차순 조회되는지 검사
	@Test
	@Transactional
	void findStudentsOnlyForOwner() {
		// 강사 두 명 준비
		User instructor = new User(
				"teacher-" + UUID.randomUUID() + "@example.com",
				"test-only-hash-placeholder"
			);
		userMapper.insertUser(instructor);

		User otherInstructor = new User(
				"other-" + UUID.randomUUID() + "@example.com",
				"test-only-hash-placeholder"
				);
		userMapper.insertUser(otherInstructor);
		
		// 검사 1 : 아직 학생이 없음 -> 빈 목록
		List<Student> emptyStudents = studentMapper.findStudentsByInstructorId(instructor.getId());
		
		assertNotNull(emptyStudents);
		assertTrue(emptyStudents.isEmpty());
		
		// 내 학생 두 명 등록
		Student firstStudent = new Student(
				instructor.getId(),
				"山田太郎",
				null,
				null,
				null
			);
		studentMapper.insertStudent(firstStudent);

		Student secondStudent = new Student(
				instructor.getId(),
				"佐藤花子",
				null,
				null,
				null
				);
		studentMapper.insertStudent(secondStudent);
		
		// 다른 강사 학생 한 명 등록
		Student otherStudent = new Student(
				otherInstructor.getId(),
				"鈴木ゆき",
				null,
				null,
				null
				);
		studentMapper.insertStudent(otherStudent);
		
		// 내 학생 목록 조회
		List<Student> students = studentMapper.findStudentsByInstructorId(instructor.getId());
		
		// 검사 2 : 내 학생만 조회되는지 (두 명)
		assertEquals(2, students.size());
		
		// 검사 3 : id 가 큰 두 번째 학생부터 조회되는지
		assertEquals(secondStudent.getId(), students.get(0).getId());
		assertEquals(firstStudent.getId(), students.get(1).getId());
		
	}
	
}