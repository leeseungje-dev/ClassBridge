package com.classbridge;

import com.classbridge.user.User;
import com.classbridge.student.Student;
import com.classbridge.student.StudentNotFoundException;
import com.classbridge.lesson.Lesson;
import com.classbridge.lesson.LessonMapper;
import com.classbridge.lesson.LessonService;
import com.classbridge.lesson.LessonNotFoundException;

// MyBatis
import com.classbridge.user.UserMapper;
import com.classbridge.student.StudentMapper;

// 회원가입
import com.classbridge.user.UserService;
// 학생 관리 CRUD
import com.classbridge.student.StudentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.UUID;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

// 원래 있던거.
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
// 원래 있던거.
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

/*
 * author : LEE SEUNGJE
 * date : 2026.10.10 ~ 2026.10.27
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
	
	@Autowired
	private StudentService studentService;
	
	@Autowired
	private LessonMapper lessonMapper;
	
	@Autowired
	private LessonService lessonService;
	
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
	
	// Service 메서드로 학생 등록
	// 이름 앞뒤 공백 제거 확인
	// 비어 있는 선택 입력값(learningStartDate, currentLevel, instructorMemo)이 null 로 저장되는지
	@Test
	@Transactional
	void registerStudentNormalizesInput() {
		// 강사 준비
		User instructor = new User(
				"teacher-" + UUID.randomUUID() + "@example.com",
				"test-only-hash-placeholder"
			);
		userMapper.insertUser(instructor);
		
		// Service 로 등록 : 이름에는 앞뒤 공백, 선택 항목은 비워서 전달
		// strip()으로 ""이 됨 -> isEmpty() 가 참 => null 저장 예정
		Long studentId = studentService.registerStudent(
				instructor.getId(),
				"  山田太郎   ",
				null,
				"           ",
				""
			);
		assertNotNull(studentId);
		
		// 실제 DB에 저장된 결과 조회
		Student savedStudent = studentMapper.findStudentById(studentId, instructor.getId());
		
		assertNotNull(savedStudent);
		assertEquals("山田太郎", savedStudent.getName());
		assertNull(savedStudent.getLearningStartDate());
		assertNull(savedStudent.getCurrentLevel());
		assertNull(savedStudent.getInstructorMemo());
		
	}
	
	// 필수값(name) 입력하지 않으면 등록을 거절하는지
	// 공백만 입력한 name 은 등록 거절
	@Test
	@Transactional
	void rejectBlankStudentName() {
		// 강사 준비
		User instructor = new User(
				"teacher-" + UUID.randomUUID() + "@example.com", 
				"test-only-hash-placeholder"
			);
		userMapper.insertUser(instructor);
		
		// 공백만 있는 이름으로 등록 시도
		boolean rejected = false;
		
		try {
			studentService.registerStudent(
					instructor.getId(), 
					"     ", 
					null, 
					null, 
					null
				);
		} catch (IllegalArgumentException e) {
			rejected = true;
			
			// 이름 누락에 대한 안내인지 확인
			assertEquals("学生名を入力してください。", e.getMessage());
		}
			
		// 예외가 발생해서 등록이 거절됐는지
		assertTrue(rejected);
			
		// 실제로 DB 에도 학생이 저장되지 않았는지
		List<Student> students = studentMapper.findStudentsByInstructorId(instructor.getId());
		
		assertTrue(students.isEmpty());
		
	}
	
	// 학생 name 100자는 허용하고 101자는 실패하는지
	@Test
	@Transactional
	void checksStudentNameLengthLimit() {
		// 강사 준비
		User instructor = new User(
				"teacher-" + UUID.randomUUID() + "@example.com",
				"test-only-hash-placeholder"
			);
		userMapper.insertUser(instructor);
		
		// 검사 1 : 제한인 100자는 등록 가능한지
		String allowedName = "잉".repeat(100);
		
		Long studentId = studentService.registerStudent(
				instructor.getId(),
				allowedName,
				null,
				null,
				null
			);
		
		Student savedStudent = studentMapper.findStudentById(studentId, instructor.getId());
		
		assertNotNull(savedStudent);
		assertEquals(allowedName, savedStudent.getName());
		
		// 검사 2 : 제한을 넘는 101자는 등록 거절인지
		String tooLongName = "잉".repeat(101);
		
		boolean rejected = false;
		
		try {
			studentService.registerStudent(
					instructor.getId(),
					tooLongName,
					null,
					null,
					null
				);
		} catch (IllegalArgumentException e) {
			rejected = true;
			
			// 이름 초과에 대한 안내인지 확인
			assertEquals("学生名は100文字以内で入力してください。", e.getMessage());
		}
		
		// 예외가 발생해서 등록이 거절 됐는지
		assertTrue(rejected);
		
		// 처음 성공한 학생 한 명이 남아있는지
		List<Student> students = studentMapper.findStudentsByInstructorId(instructor.getId());
		
		assertEquals(1, students.size());
		assertEquals(studentId, students.get(0).getId());
		
	}
	
	// studentLevel 지정된 네가지 학습 레벨은 허용, 그 외는 거절
	@Test
	@Transactional
	void checkStudentLevelAllowedValues() {
		// 강사 준비
		User instructor = new User(
				"teacher-" + UUID.randomUUID() + "@example.com",
				"test-only-hash-placeholder"
			);
		userMapper.insertUser(instructor);
		
		// 검사 1 : 네 가지 학습 레벨 모두 등록 가능
		String[] allowedLevels = {"入門", "初級", "中級", "上級"};
		
		for (int i = 0; i < allowedLevels.length; i++) {
			Long studentId = studentService.registerStudent(
					instructor.getId(),
					"山田太郎",
					null,
					allowedLevels[i],
					null
				);

			Student savedStudent = studentMapper.findStudentById(studentId, instructor.getId());
		
			assertNotNull(savedStudent);
			assertEquals(allowedLevels[i], savedStudent.getCurrentLevel());
		}

		// 검사 2 : 목록에 없는 학습 레벨은 등록 거절
		boolean rejected = false;
		
		try {
			studentService.registerStudent(
					instructor.getId(),
					"鈴木花子",
					null,
					"未定",
					null
				);
			
		} catch (IllegalArgumentException e) {
			rejected = true;
			
			assertEquals("学習レベルは入門・初級・中級・上級から選択してください。", e.getMessage());
		}
		
		assertTrue(rejected);
		
		// 허용된 레벨로 등록한 네 명 저장되어 있는지
		List<Student> students = studentService.findStudentsByInstructorId(instructor.getId());
		
		assertEquals(4, students.size());		
	}
	
	// studentMemo 최대 1000자 허용하고 1001자 실패하는지
	@Test
	@Transactional
	void checkStudentMemoLengthLimit() {
		// 강사 준비
		User instructor = new User(
				"teacher-" + UUID.randomUUID() + "@example.com",
				"test-only-hash-placeholder"
			);
		userMapper.insertUser(instructor);
		
		// 검사 1 : 1000자 등록 가능
		String allowedMemo = "잉".repeat(1000);
		
		Long studentId = studentService.registerStudent(
				instructor.getId(),
				"山田太郎",
				null,
				null,
				allowedMemo
			);
		
		Student savedStudent = studentMapper.findStudentById(studentId, instructor.getId());
		
		assertNotNull(savedStudent);
		assertEquals(allowedMemo, savedStudent.getInstructorMemo());
		
		// 검사 2 : 1001자 등록 거절
		boolean rejected = false;
		
		try {
			studentService.registerStudent(
					instructor.getId(),
					"佐藤花子",
					null,
					null,
					"잉".repeat(1001)
				);
		} catch (IllegalArgumentException e) {
			rejected = true;
			
			assertEquals("メモは1000文字以内で入力してください。", e.getMessage());
		}
		
		assertTrue(rejected);
		
		// 처음 성공한 학생 한 명 저장 확인
		List<Student> students = studentMapper.findStudentsByInstructorId(instructor.getId());
		
		assertEquals(1, students.size());
		assertEquals(studentId, students.get(0).getId());
		
	}
	
	// Service 에서 update 시 입력값을 정리한 뒤 수정하는지 검사
	// 정상 수정 시 공백 정리와 null 처리가 DB에 반영되는지
	@Test
	@Transactional
	void updatesStudentNormalizesInput() {
		// 강사와 학생 준비
		User instructor = new User(
				"teacher-" + UUID.randomUUID() + "@example.com",
				"test-only-hash-placeholder"
			);
		userMapper.insertUser(instructor);
		
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				LocalDate.of(2026, 10, 21),
				"初級",
				"変更前のメモ"
			);
		studentMapper.insertStudent(student);
		
		// Service 로 수정 요청
		studentService.updateStudent(
				student.getId(),
				instructor.getId(),
				"   山田花子     ",
				null,
				"",
				"     "
			);
		
		// DB에서 수정 결과 확인
		Student updated = studentMapper.findStudentById(student.getId(), instructor.getId());
		
		assertNotNull(updated);
		assertEquals("山田花子", updated.getName());
		assertNull(updated.getLearningStartDate());
		assertNull(updated.getCurrentLevel());
		assertNull(updated.getInstructorMemo());
		
	}
	
	// Service 에서 update 시 다른 강사의 수정 요청은 예외로 거절되고 기존 정보가 유지되는지
	@Test
	@Transactional
	void rejectStudentUpdateByOtherInstructor() {
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
		
		// 첫 번째 강사의 학생 등록
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				LocalDate.of(2026, 10, 21),
				"初級",
				"変更前のメモ"
			);
		studentMapper.insertStudent(student);
		
		// 다른 강사 id 로 수정 요청
		boolean rejected = false;
		
		try {
			studentService.updateStudent(
					student.getId(),
					otherInstructor.getId(),
					"変更されてはいけない名前",
					null,
					null,
					null
				);
		} catch(StudentNotFoundException e) {
			rejected = true;
			
			assertEquals("学生が見つかりません。", e.getMessage());
		}
		
		assertTrue(rejected);
		
		// 기존 정보가 유지되는지 확인
		Student unchanged = studentMapper.findStudentById(student.getId(), instructor.getId());
		
		assertNotNull(unchanged);
		assertEquals("山田太郎", unchanged.getName());
		assertEquals(LocalDate.of(2026, 10, 21), unchanged.getLearningStartDate());
		assertEquals("初級", unchanged.getCurrentLevel());
		assertEquals("変更前のメモ", unchanged.getInstructorMemo());
		
	}
	
	// 다른 강사의 삭제 요청은 예외로 거절되는지
	// 소유 강사의 삭제 요청은 정상 처리 되는지
	@Test
	@Transactional
	void deleteStudentThroughServiceOnlyForOwner() {
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
				null,
				null,
				null
			);
		studentMapper.insertStudent(student);
		
		// 검사 1 : 다른 강사의 삭제 요청은 실패하는지
		boolean rejected = false;
		
		try {
			studentService.deleteStudent(
					student.getId(),
					otherInstructor.getId()
				);
			
		} catch(StudentNotFoundException e) {
			rejected = true;
			
			assertEquals("学生が見つかりません。", e.getMessage());
		}
		assertTrue(rejected);
		
		// 학생이 DB 에 남아 있는지 확인
		Student remainingStudent = studentMapper.findStudentById(student.getId(), instructor.getId());
		
		assertNotNull(remainingStudent);
		
		// 검사 2 : 소유 강사는 삭제 시도
		studentService.deleteStudent(student.getId(), instructor.getId());
		
		// 학생이 DB 에 없는지 확인
		Student deletedStudent = studentMapper.findStudentById(student.getId(), instructor.getId());
		
		assertNull(deletedStudent);
		
	}
	
	// 수업 저장, 조회 및 다른 강사의 조회 차단 확인
	@Test
	@Transactional
	void saveAndFindLessonOnlyForOwner() {
		
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
		
		// 첫 번째 강사의 학생 등록
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				null,
				null,
				null
			);
		studentMapper.insertStudent(student);
		
		// 해당 학생의 수업 등록
		LocalDateTime lessonAt = LocalDateTime.of(2026, 11, 1, 15, 30);
		
		Lesson lesson = new Lesson(student.getId(), lessonAt);
		
		int insertedRows = lessonMapper.insertLesson(lesson);
		
		assertEquals(1, insertedRows);
		assertNotNull(lesson.getId());
		
		// 소유 강사로 조회
		Lesson foundLesson = lessonMapper.findLessonById(lesson.getId(), instructor.getId());
		
		assertNotNull(foundLesson);
		assertEquals(lesson.getId(), foundLesson.getId());
		assertEquals(student.getId(), foundLesson.getStudentId());
		assertEquals(lessonAt, foundLesson.getLessonAt());
		
		// DB 기본값과 선택 항목 확인
		assertEquals("SCHEDULED", foundLesson.getStatus());
		assertNull(foundLesson.getStatusMemo());
		assertNotNull(foundLesson.getCreatedAt());
		
		// 다른 강사로 조회하면 결과가 없는지 확인
		Lesson inaccessibleLesson = lessonMapper.findLessonById(lesson.getId(), otherInstructor.getId());
		
		assertNull(inaccessibleLesson);
		
	}
	
	// 학생별 수업 목록이 올바르게 조회되는지 Test
	// 1. 수업이 없으면 빈 목록(LIst)인지
	// 2. 등록 순서와 관계없이 수업 일시순으로 나오는지
	// 3. 다른 강사가 검색하면 빈 목록인지
	@Test
	@Transactional
	void findLessonsByStudentOnlyForOwner() {
		
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
		
		// 같은 강사의 학생 두 명 준비
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				null,
				null,
				null
			);
		studentMapper.insertStudent(student);

		Student otherStudent = new Student(
				instructor.getId(),
				"鈴木花子",
				null,
				null,
				null
			);
		studentMapper.insertStudent(otherStudent);
		
		// 검사 1 : 수업이 없으면 빈 목록
		List<Lesson> emptyLessons = lessonMapper.findLessonsByStudentId(student.getId(), instructor.getId());
		assertTrue(emptyLessons.isEmpty());
		
		// 늦은 수업을 먼저 등록
		Lesson laterLesson = new Lesson(
				student.getId(), 
				LocalDateTime.of(2026, 11, 10, 15, 30)
			);
		lessonMapper.insertLesson(laterLesson);
		
		// 이른 수업을 나중에 등록
		Lesson earlierLesson = new Lesson(
				student.getId(), 
				LocalDateTime.of(2026, 11, 1, 15, 30)
			);
		lessonMapper.insertLesson(earlierLesson);
		
		// 다른 학생의 수업도 등록 (직접 비교하지는 않지만 해당 강사 id 로 조회시 검색 안되는지 확인 가능함)
		Lesson otherStudentLesson = new Lesson(
				otherStudent.getId(), 
				LocalDateTime.of(2026, 11, 5, 15, 30)
			);
		lessonMapper.insertLesson(otherStudentLesson);
		
		// 검사 2 : 해당 학생의 수업만 일시순으로 조회 (student, instructor)
		List<Lesson> lessons = lessonMapper.findLessonsByStudentId(student.getId(), instructor.getId());
		
		assertEquals(2, lessons.size());
		assertEquals(earlierLesson.getId(), lessons.get(0).getId());
		assertEquals(laterLesson.getId(), lessons.get(1).getId());
		
		// 검사 3 : 다른 강사로 조회하면 빈 목록
		List<Lesson> inaccessibleLessons = lessonMapper.findLessonsByStudentId(student.getId(), otherInstructor.getId());
		
		assertTrue(inaccessibleLessons.isEmpty());
		
	}
	
	// 수업 날짜 변경 및 다른 강사의 변경 차단 Test
	@Test
	@Transactional
	void updateLessonDateTimeOnlyForOwner() {
		
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
		
		// 첫 번째 강사의 학생 등록
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				null,
				null,
				null
			);
		studentMapper.insertStudent(student);
		
		// 수업 등록
		LocalDateTime originalDateTime = LocalDateTime.of(2026, 11, 1, 15, 30);
		
		Lesson lesson = new Lesson(student.getId(), originalDateTime);
		lessonMapper.insertLesson(lesson);
		
		// DB 에서 등록 시각 등 기존 정보 확보
		Lesson original = lessonMapper.findLessonById(lesson.getId(), instructor.getId());
		
		assertNotNull(original);
		
		// 변경할 수업 일시
		LocalDateTime newDateTime = LocalDateTime.of(2026, 11, 2, 16, 0);
		
		// 검사 1 : 다른 강사의 변경은 거절
		int rejectedRows = lessonMapper.updateLessonDateTime(
				lesson.getId(),
				otherInstructor.getId(),
				newDateTime
			);
		
		assertEquals(0, rejectedRows);
		
		// DB 의 수업 일시가 그대로인지 확인
		Lesson unchanged = lessonMapper.findLessonById(lesson.getId(), instructor.getId());
		
		assertNotNull(unchanged);
		assertEquals(originalDateTime, unchanged.getLessonAt());
		
		// 검사 2 : 소유 강사의 변경은 성공
		int updatedRows = lessonMapper.updateLessonDateTime(
				lesson.getId(),
				instructor.getId(),
				newDateTime
			);
		
		assertEquals(1, updatedRows);
		
		// DB 에서 다시 조회해서 변경 결과 확인
		Lesson updated = lessonMapper.findLessonById(lesson.getId(), instructor.getId());
		
		assertNotNull(updated);
		assertEquals(newDateTime, updated.getLessonAt());
		
		// 수정 대상이 아닌 값은 그대로인지 확인
		assertEquals(original.getId(), updated.getId());
		assertEquals(original.getStudentId(), updated.getStudentId());
		assertEquals(original.getStatus(), updated.getStatus());
		assertEquals(original.getStatusMemo(), updated.getStatusMemo());
		assertEquals(original.getCreatedAt(), updated.getCreatedAt());
	}
	
	// 수업 상태, 메모 변경 및 소유 강사 확인 Test
	@Test
	@Transactional
	void updateLessonStatusOnlyForOwner() {
		
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
		
		// 첫 번째 강사의 학생과 수업 등록
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				null,
				null,
				null
			);
		studentMapper.insertStudent(student);
		
		LocalDateTime lessonAt = LocalDateTime.of(2026, 11, 1, 15, 30);
		
		Lesson lesson = new Lesson(student.getId(), lessonAt);
		lessonMapper.insertLesson(lesson);
		
		// DB 에서 기존 정보 확보
		Lesson original = lessonMapper.findLessonById(lesson.getId(), instructor.getId());
		
		assertNotNull(original);
		
		// 검사 1 : 다른 강사의 상태, 메모 변경은 거절
		int rejectedRows = lessonMapper.updateLessonStatus(
				lesson.getId(),
				otherInstructor.getId(),
				"CANCELLED",
				"変更されてはいけないメモ"
			);
		
		assertEquals(0, rejectedRows);
		
		// DB 의 상태와 메모가 그대로인지 확인
		Lesson unchanged = lessonMapper.findLessonById(lesson.getId(), instructor.getId());
		
		assertNotNull(unchanged);
		assertEquals(original.getStatus(), unchanged.getStatus());
		assertEquals(original.getStatusMemo(), unchanged.getStatusMemo());
		
		// 검사 2 : 소유 강사는 취소 상태와 사유를 저장 가능한지 확인
		int updatedRows = lessonMapper.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"CANCELLED",
				"体調不良のためキャンセル"
			);
		assertEquals(1, updatedRows);
		
		Lesson updated = lessonMapper.findLessonById(lesson.getId(), instructor.getId());
		
		assertNotNull(updated);
		assertEquals("CANCELLED", updated.getStatus());
		assertEquals("体調不良のためキャンセル", updated.getStatusMemo());
		
		// 검사 3 : 상태를 예정으로 되돌리며 기존 메모 삭제
		int clearedRows = lessonMapper.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"SCHEDULED",
				null
			);
		assertEquals(1, clearedRows);
		
		Lesson cleared = lessonMapper.findLessonById(lesson.getId(), instructor.getId());
		
		assertNotNull(cleared);
		assertEquals("SCHEDULED", cleared.getStatus());
		assertNull(cleared.getStatusMemo());
		
		// 두 번의 수정 모두 다른 열을 변경하지 않았는지 확인
		assertEquals(original.getId(), updated.getId());
		assertEquals(original.getStudentId(), updated.getStudentId());
		assertEquals(original.getLessonAt(), updated.getLessonAt());
		assertEquals(original.getCreatedAt(), updated.getCreatedAt());

		assertEquals(original.getId(), cleared.getId());
		assertEquals(original.getStudentId(), cleared.getStudentId());
		assertEquals(original.getLessonAt(), cleared.getLessonAt());
		assertEquals(original.getCreatedAt(), cleared.getCreatedAt());
	}
	
	/*
	 * LessonService Test
	 */
	// 본인 학생의 수업 등록 성공
	@Test
	@Transactional
	void registerLessonForOwner() {
		// 강사와 해당 강사의 학생 준비
		User instructor = new User(
				"teacher-" + UUID.randomUUID() + "@example.com",
				"test-only-hash-placeholder"
			);
		userMapper.insertUser(instructor);
		
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				null,
				null,
				null
			);
		studentMapper.insertStudent(student);
		
		LocalDateTime lessonAt = LocalDateTime.of(2026, 11, 1, 15, 30);
		
		// LessonService 를 통한 수업 등록
		Long lessonId = lessonService.registerLesson(
				student.getId(),
				instructor.getId(),
				lessonAt
			);
		
		// 반환된 id 로 실제 저장 결과 확인
		assertNotNull(lessonId);
		
		Lesson savedLesson = lessonMapper.findLessonById(
				lessonId,
				instructor.getId()
			);
		
		assertNotNull(savedLesson);
		assertEquals(student.getId(), savedLesson.getStudentId());
		assertEquals(lessonAt, savedLesson.getLessonAt());
		assertEquals("SCHEDULED", savedLesson.getStatus());
	}
	
	// 다른 강사 학생의 수업 등록 거절
	@Test
	@Transactional
	void rejectLessonRegisterationForOtherInstructor() {
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
		
		// 첫 번째 강사의 학생 준비
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				null,
				null,
				null
			);
		studentMapper.insertStudent(student);
		
		// 다른 강사 id 로 수업 등록 시도
		boolean rejected = false;
		
		try {
			lessonService.registerLesson(
					student.getId(),
					otherInstructor.getId(),
					LocalDateTime.of(2026, 11, 1, 15, 30)
				);
			
		} catch (StudentNotFoundException e) {
			rejected = true;
		}
		
		// 등록이 거절됐는지 확인
		assertTrue(rejected);
		
		// 실제 소유 강사 기준으로 조회해도 수업이 없는지 확인
		assertTrue(lessonMapper.findLessonsByStudentId(student.getId(), instructor.getId()).isEmpty());
	}
	
	// 수업 일시가 없으면 등록 거절되는지
	@Test
	@Transactional
	void rejectLessonRegistrationWithoutDateTime() {
		// 강사와 해당 강사의 학생 준비
		User instructor = new User(
				"teacher-" + UUID.randomUUID() + "@example.com",
				"test-only-hash-placeholder"
			);
		userMapper.insertUser(instructor);
		
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				null,
				null,
				null
			);
		studentMapper.insertStudent(student);
		
		// 수업 일시 없이 등록 시도
		boolean rejected = false;
		
		try {
			lessonService.registerLesson(
					student.getId(), 
					instructor.getId(), 
					null
				);
			
		} catch (IllegalArgumentException e) {
			rejected = true;
			
			assertEquals("授業日時を入力してください。", e.getMessage());
		}
		
		// 필수 입력값 없어서 거절됐는지 확인
		assertTrue(rejected);
		
		// 실제로 수업이 저장되지 않았는지 확인
		assertTrue(lessonMapper.findLessonsByStudentId(student.getId(), instructor.getId()).isEmpty());
	}
	
	// 자기 수업의 일시 수정 성공하는지
	@Test
	@Transactional
	void updateLessonDateTimeThroughService() {
		// 강사와 학생 준비
		User instructor = new User(
				"teacher-" + UUID.randomUUID() + "@example.com",
				"test-only-hash-placeholder"
			);
		userMapper.insertUser(instructor);
		
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				null,
				null,
				null
			);
		studentMapper.insertStudent(student);
		
		// 수정할 수업 준비
		Lesson lesson = new Lesson(student.getId(), LocalDateTime.of(2026, 11, 1, 15, 30));
		lessonMapper.insertLesson(lesson);
		
		// 등록 시각을 포함한 기존 DB 값 확보
		Lesson original = lessonMapper.findLessonById(lesson.getId(), instructor.getId());
		
		assertNotNull(original);
		
		LocalDateTime newDateTime = LocalDateTime.of(2026, 11, 2, 16, 0);
		
		// LessonService 를 통해 일시 수정
		lessonService.updateLessonDateTime(
				lesson.getId(), 
				instructor.getId(), 
				newDateTime
			);
		
		// DB 의 일시가 변경됐는지 확인
		Lesson updated = lessonMapper.findLessonById(lesson.getId(), instructor.getId());
		
		assertNotNull(updated);
		assertEquals(newDateTime, updated.getLessonAt());
		
		// 수정 대상이 아닌 값은 그대로인지 확인
		assertEquals(original.getId(), updated.getId());
		assertEquals(original.getStudentId(), updated.getStudentId());
		assertEquals(original.getStatus(), updated.getStatus());
		assertEquals(original.getStatusMemo(), updated.getStatusMemo());
		assertEquals(original.getCreatedAt(), updated.getCreatedAt());
	}
	
	// 다른 강사의 수정 요청은 거절되는지 확인
	@Test
	@Transactional
	void rejectOtherInstructorLessonDateTimeUpdateThroughService() {
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
		
		// 첫 번째 강사의 학생 준비와 수업 준비
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				null,
				null,
				null
			);
		studentMapper.insertStudent(student);
		
		LocalDateTime originalDateTime = LocalDateTime.of(2026, 11, 1, 15, 30);
		
		Lesson lesson = new Lesson(student.getId(), originalDateTime);
		lessonMapper.insertLesson(lesson);
		
		// 다른 강사 id 로 수정 시도
		boolean rejected = false;
		
		try {
			lessonService.updateLessonDateTime(
					lesson.getId(), 
					otherInstructor.getId(), 
					LocalDateTime.of(2026, 11, 2, 16, 0)
				);
			
		} catch (LessonNotFoundException e) {
			rejected = true;
		}
		
		assertTrue(rejected);
		
		// 실제 소유 강사 기준으로 조회 후 변경되지 않았는지 확인
		Lesson unchanged = lessonMapper.findLessonById(lesson.getId(), instructor.getId());
		
		assertNotNull(unchanged);
		assertEquals(originalDateTime, unchanged.getLessonAt());
	}
	
	// 수업 일시가 없으면 수정 거절하는지
	@Test
	@Transactional
	void rejectNullLessonDateTimeUpdateThroughService() {
		// 강사와 학생 준비
		User instructor = new User(
				"teacher-" + UUID.randomUUID() + "@example.com",
				"test-only-hash-placeholder"
			);
		userMapper.insertUser(instructor);
		
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				null,
				null,
				null
			);
		studentMapper.insertStudent(student);
		
		// 기존 일시가 있는 수업 준비
		LocalDateTime originalDateTime = LocalDateTime.of(2026, 11, 1, 15, 30);
		
		Lesson lesson = new Lesson(student.getId(), originalDateTime);
		lessonMapper.insertLesson(lesson);
		
		// 수업 일시를 null 로 수정 시도
		boolean rejected = false;
		
		try {
			lessonService.updateLessonDateTime(
					lesson.getId(), 
					instructor.getId(), 
					null
				);
			
		} catch (IllegalArgumentException e) {
			rejected = true;
			
			assertEquals("授業日時を入力してください。", e.getMessage());
		}
		
		assertTrue(rejected);
		
		// DB 의 원래 일시가 유지되는지 확인
		Lesson unchanged = lessonMapper.findLessonById(lesson.getId(), instructor.getId());
		
		assertNotNull(unchanged);
		assertEquals(originalDateTime, unchanged.getLessonAt());
	}
	
	// status 변경, statusMemo 공백 정리와 삭제 확인
	@Test
	@Transactional
	void updateLessonStatusThroughService() {
		// 강사와 학생 준비
		User instructor = new User(
				"teacher-" + UUID.randomUUID() + "@example.com",
				"test-only-hash-placeholder"
			);
		userMapper.insertUser(instructor);
		
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				null,
				null,
				null
			);
		studentMapper.insertStudent(student);
		
		// 수업 준비
		Lesson lesson = new Lesson(student.getId(), LocalDateTime.of(2026, 11, 1, 15, 30));
		lessonMapper.insertLesson(lesson);
		
		// 변경 전 DB 정보 확보
		Lesson original = lessonMapper.findLessonById(lesson.getId(), instructor.getId());
		
		assertNotNull(original);
		
		// 검사 1 : "CANCELLED" status 와 공백이 있는 statusMemo 입력
		lessonService.updateLessonStatus(
				lesson.getId(), 
				instructor.getId(), 
				"CANCELLED",
				"   体調不良のためキャンセル    "
			);
		
		Lesson updated = lessonMapper.findLessonById(lesson.getId(), instructor.getId());
		
		assertNotNull(updated);
		assertEquals("CANCELLED", updated.getStatus());
		assertEquals("体調不良のためキャンセル", updated.getStatusMemo());
		
		// 검사 2 : "SCHEDULED" 로 공백만 있는 statusMemo 입력
		lessonService.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"SCHEDULED",
				"               "
			);
		
		Lesson cleared = lessonMapper.findLessonById(lesson.getId(), instructor.getId());
		
		assertNotNull(cleared);
		assertEquals("SCHEDULED", cleared.getStatus());
		assertNull(cleared.getStatusMemo());
		
		// 두 번의 변경 모두 수정 대상이 아닌 값은 그대로인지 확인
		assertEquals(original.getId(), updated.getId());
		assertEquals(original.getStudentId(), updated.getStudentId());
		assertEquals(original.getLessonAt(), updated.getLessonAt());
		assertEquals(original.getCreatedAt(), updated.getCreatedAt());
		
		assertEquals(original.getId(), cleared.getId());
		assertEquals(original.getStudentId(), cleared.getStudentId());
		assertEquals(original.getLessonAt(), cleared.getLessonAt());
		assertEquals(original.getCreatedAt(), cleared.getCreatedAt());
	}
	
	// 다른 강사의 수업 상태 변경 거절되는지
	@Test
	@Transactional
	void rejectOtherInstructorLessonStatusUpdateThroughService() {
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
		
		// 첫 번째 강사의 학생 준비와 수업 준비
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				null,
				null,
				null
			);
		studentMapper.insertStudent(student);
		
		Lesson lesson = new Lesson(student.getId(), LocalDateTime.of(2026, 11, 1, 15, 30));
		lessonMapper.insertLesson(lesson);
		
		// 다른 강사 id 로 status 변경 시도
		boolean rejected = false;
		
		try {
			lessonService.updateLessonStatus(
					lesson.getId(),
					otherInstructor.getId(),
					"CANCELLED",
					"変更されてはいけないメモ"
				);
			
		} catch (LessonNotFoundException e) {
			rejected = true;
		}
		
		// 요청이 거절됐는지 확인
		assertTrue(rejected);
		
		// 실제 담당 강사 기준으로 DB 조회
		Lesson unchanged = lessonMapper.findLessonById(lesson.getId(), instructor.getId());
		
		// 원래 상태와 메모가 그대로인지 확인
		assertNotNull(unchanged);
		assertEquals("SCHEDULED", unchanged.getStatus());
		assertNull(unchanged.getStatusMemo());
	}
	
	// 네 가지 status 가 아닌 status 는 거절되는지 확인
	@Test
	@Transactional
	void rejectInvalidLessonStatusThroughService() {
		// 강사와 학생, 수업 준비
		User instructor = new User(
				"teacher-" + UUID.randomUUID() + "@example.com",
				"test-only-hash-placeholder"
			);
		userMapper.insertUser(instructor);
		
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				null,
				null,
				null
			);
		studentMapper.insertStudent(student);
		
		Lesson lesson = new Lesson(
				student.getId(),
				LocalDateTime.of(2026, 11, 1, 15, 30)
			);
		lessonMapper.insertLesson(lesson);
		
		// 네 가지 status 이외의 상태값으로 변경 시도
		boolean rejected = false;
		
		try {
			lessonService.updateLessonStatus(
					lesson.getId(),
					instructor.getId(),
					"UNKNOWN",
					"変更されてはいけないメモ"
				);
			
		} catch (IllegalArgumentException e) {
			rejected = true;
			
			assertEquals("授業の状態が正しくありません。", e.getMessage());
		}
		
		assertTrue(rejected);
		
		// 상태와 메모 전부 변경되지 않았는지 확인
		Lesson unchanged = lessonMapper.findLessonById(lesson.getId(), instructor.getId());
		
		assertNotNull(unchanged);
		assertEquals("SCHEDULED", unchanged.getStatus());
		assertNull(unchanged.getStatusMemo());
	}
	
	// 1000자를 넘는 statusMemo 는 거절되는지 확인
	@Test
	@Transactional
	void rejectTooLongLessonStatusMemoThroughService() {
		// 강사와 학생, 수업 준비
		User instructor = new User(
				"teacher-" + UUID.randomUUID() + "@example.com",
				"test-only-hash-placeholder"
			);
		userMapper.insertUser(instructor);
		
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				null,
				null,
				null
			);
		studentMapper.insertStudent(student);
		
		Lesson lesson = new Lesson(
				student.getId(),
				LocalDateTime.of(2026, 11, 1, 15, 30)
			);
		lessonMapper.insertLesson(lesson);
		
		// 1,001자 statusMemo 준비
		String longMemo = "잉".repeat(1001);
		
		// 긴 statusMemo 로 변경 시도
		boolean rejected = false;
		
		try {
			lessonService.updateLessonStatus(
					lesson.getId(),
					instructor.getId(),
					"CANCELLED",
					longMemo
				);
			
		} catch (IllegalArgumentException e) {
			rejected = true;
			
			assertEquals("状態メモは1000文字以内で入力してください。", e.getMessage());
		}
		
		assertTrue(rejected);
		
		// statusMemo 이외의 상태도 변경되지 않았는지 확인
		Lesson unchanged = lessonMapper.findLessonById(lesson.getId(), instructor.getId());
		
		assertNotNull(unchanged);
		assertEquals("SCHEDULED", unchanged.getStatus());
		assertNull(unchanged.getStatusMemo());
	}
	
}