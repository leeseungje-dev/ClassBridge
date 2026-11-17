package com.classbridge;

import com.classbridge.user.User;
import com.classbridge.student.Student;
import com.classbridge.student.StudentNotFoundException;
import com.classbridge.lesson.Lesson;
import com.classbridge.lesson.LessonMapper;
import com.classbridge.lesson.LessonService;
import com.classbridge.lesson.LessonNotFoundException;
import com.classbridge.report.LessonReport;
import com.classbridge.report.LessonReportMapper;
import com.classbridge.report.LessonReportService;
import com.classbridge.report.ObservationInput;
import com.classbridge.report.ObservationUpdateInput;
import com.classbridge.report.LessonReportNotFoundException;
import com.classbridge.observation.LearningObservation;
import com.classbridge.observation.LearningObservationMapper;
import com.classbridge.lesson.HomeLesson;

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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.YearMonth;

// 원래 있던거.
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
// 원래 있던거.
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

/*
 * author : LEE SEUNGJE
 * date : 2026.10.10 ~ 2026.11.15
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
	
	@Autowired
	private LessonReportMapper lessonReportMapper;
	
	@Autowired
	private LearningObservationMapper learningObservationMapper;
	
	@Autowired
	private LessonReportService lessonReportService;
	
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
	
	// 일보 등록, 조회 및 다른 강사의 조회 거절 Test
	@Test
	@Transactional
	void saveAndFindLessonReportOnlyForOwner() {
		
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
		
		// 수업을 등록하고 출석 상태로 변경
		Lesson lesson = new Lesson(student.getId(), LocalDateTime.of(2026, 11, 1, 15, 30));
		lessonMapper.insertLesson(lesson);
		
		lessonMapper.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"ATTENDED",
				null
			);
		
		// 검사 1 : 일보 등록과 id 발급되는지
		LessonReport report = new LessonReport(
				lesson.getId(),
				"過去形を練習しました。",
				"次回は過去形を復習する。"
			);
		
		int insertedRows = lessonReportMapper.insertLessonReport(report);
		
		assertEquals(1, insertedRows);
		assertNotNull(report.getId());
		
		// 검사 2 : 소유 강사는 저장된 일보 조회 가능한지
		LessonReport found = lessonReportMapper.findLessonReportByLessonId(lesson.getId(), instructor.getId());
		
		assertNotNull(found);
		assertEquals(report.getId(), found.getId());
		assertEquals(lesson.getId(), found.getLessonId());
		assertEquals("過去形を練習しました。", found.getContent());
		assertEquals("次回は過去形を復習する。", found.getNextCheck());
		assertNotNull(found.getCreatedAt());
		assertNotNull(found.getUpdatedAt());
		
		// 검사 3 : 다른 강사는 같은 수업의 일보가 검색되지 않는지
		LessonReport inaccessible = lessonReportMapper.findLessonReportByLessonId(lesson.getId(), otherInstructor.getId());
		
		assertNull(inaccessible);
	}
	
	// 관찰 기록 여러 건의 등록, 조회 및 다른 강사의 조회 거절 Test
	@Test
	@Transactional
	void saveAndFindObservationsOnlyForOwner() {
		
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
		
		// 수업 등록 및 출석 처리
		Lesson lesson = new Lesson(
				student.getId(),
				LocalDateTime.of(2026, 11, 1, 15, 30)
			);
		lessonMapper.insertLesson(lesson);
		
		lessonMapper.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"ATTENDED",
				null
			);
		
		// 수업일보 등록
		LessonReport report = new LessonReport(
				lesson.getId(),
				"過去形を練習しました。",
				"次回は過去形を復習する。"
			);
		lessonReportMapper.insertLessonReport(report);
		
		// 검사 1 : 같은 일보에 관찰 기록 두 건 등록
		LearningObservation grammar = new LearningObservation(
				report.getId(),
				"GRAMMAR",
				"過去形",
				 "過去形を使うことができた。"
			);
		assertEquals(1, learningObservationMapper.insertLearningObservation(grammar));
		
		LearningObservation pronunciation = new LearningObservation(
				report.getId(),
				"PRONUNCIATION",
				"発音",
				"ゆっくり話すと正しく発音できた。"
			);
		assertEquals(1, learningObservationMapper.insertLearningObservation(pronunciation));
		
		assertNotNull(grammar.getId());
		assertNotNull(pronunciation.getId());
		assertNotEquals(grammar.getId(), pronunciation.getId());
		
		// 검사 2 : 담당 강사는 두 기록을 id 오름차순으로 조회되는지
		List<LearningObservation> found = learningObservationMapper.findObservationsByLessonReportId(report.getId(), instructor.getId());
		assertEquals(2, found.size());
		
		LearningObservation first = found.get(0);
		
		assertEquals(grammar.getId(), first.getId());
		assertEquals(report.getId(), first.getLessonReportId());
		assertEquals("GRAMMAR", first.getCategory());
		assertEquals("過去形", first.getItem());
		assertEquals("過去形を使うことができた。", first.getMemo());
		assertNotNull(first.getCreatedAt());
		assertNotNull(first.getUpdatedAt());
		
		LearningObservation second = found.get(1);
		
		assertEquals(pronunciation.getId(), second.getId());
		assertEquals(report.getId(), second.getLessonReportId());
		assertEquals("PRONUNCIATION", second.getCategory());
		assertEquals("発音", second.getItem());
		assertEquals("ゆっくり話すと正しく発音できた。", second.getMemo());
		assertNotNull(second.getCreatedAt());
		assertNotNull(second.getUpdatedAt());
		
		// 검사 3 : 다른 강사는 같은 일보의 관찰 기록을 조회할 수 없는지
		List<LearningObservation> inaccessible = learningObservationMapper.findObservationsByLessonReportId(report.getId(), otherInstructor.getId());
		
		assertTrue(inaccessible.isEmpty());
	}
	
	// 담당 강사만 일보를 수정할 수 있는지 Test
	@Test
	@Transactional
	void updateLessonReportOnlyForOwner() {
		
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
		
		// 학생과 수업 준비
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
		
		lessonMapper.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"ATTENDED",
				null
			);

		// 수정 전 일보 등록
		LessonReport report = new LessonReport(
				lesson.getId(),
				"過去形を練習しました。",
				"次回は過去形を復習する。"
			);
		lessonReportMapper.insertLessonReport(report);
		
		// DB 에서 최초 작성, 수정 시각 확보
		LessonReport original = lessonReportMapper.findLessonReportByLessonId(lesson.getId(), instructor.getId());
		assertNotNull(original);
		
		// Java 객체에 수정할 내용 설정
		report.setContent("会話を練習しました。");
		report.setNextCheck(null);
		
		// 검사 1 : 다른 강사의 수정 요청은 거절되는지
		int rejectedRows = lessonReportMapper.updateLessonReport(report, otherInstructor.getId());
		assertEquals(0, rejectedRows);
		
		LessonReport unchanged = lessonReportMapper.findLessonReportByLessonId(lesson.getId(), instructor.getId());
		
		assertNotNull(unchanged);
		assertEquals("過去形を練習しました。", unchanged.getContent());
		assertEquals("次回は過去形を復習する。", unchanged.getNextCheck());
		assertEquals(original.getUpdatedAt(), unchanged.getUpdatedAt());
		
		// 검사 2 : 담당 강사의 수정 요청은 성공하는지
		int updatedRows = lessonReportMapper.updateLessonReport(report, instructor.getId());
		assertEquals(1, updatedRows);
		
		LessonReport updated = lessonReportMapper.findLessonReportByLessonId(lesson.getId(), instructor.getId());
		
		assertNotNull(updated);
		assertEquals("会話を練習しました。", updated.getContent());
		assertNull(updated.getNextCheck());
		assertNotNull(updated.getUpdatedAt());
		
		// 수정 대상이 아닌 값은 그대로인지 확인
		assertEquals(original.getId(), updated.getId());
		assertEquals(original.getLessonId(), updated.getLessonId());
		assertEquals(original.getCreatedAt(), updated.getCreatedAt());
	}
	
	// 관찰 기록 수정 시 소유강사와 소속 일보 확인
	@Test
	@Transactional
	void updateOvservationOnlyForOwnerAndReport() {
		
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
		
		// 같은 학생의 출석 상태의 수업 두 건 준비
		Lesson firstLesson = new Lesson(
				student.getId(),
				LocalDateTime.of(2026, 11, 1, 15, 30)
			);
		lessonMapper.insertLesson(firstLesson);
		
		lessonMapper.updateLessonStatus(
				firstLesson.getId(),
				instructor.getId(),
				"ATTENDED",
				null
			);

		Lesson secondLesson = new Lesson(
				student.getId(),
				LocalDateTime.of(2026, 11, 2, 15, 30)
			);
		lessonMapper.insertLesson(secondLesson);
		
		lessonMapper.updateLessonStatus(
				secondLesson.getId(),
				instructor.getId(),
				"ATTENDED",
				null
			);
		
		// 수업별 일보 준비
		LessonReport firstReport = new LessonReport(
				firstLesson.getId(),
				"過去形を練習しました。",
				null
			);
		lessonReportMapper.insertLessonReport(firstReport);

		LessonReport secondReport = new LessonReport(
				secondLesson.getId(),
				"会話を練習しました。",
				null
				);
		lessonReportMapper.insertLessonReport(secondReport);
		
		// 첫 번째 일보에 관찰 기록 등록
		LearningObservation observation = new LearningObservation(
				firstReport.getId(),
				"GRAMMAR",
				"過去形",
				"過去形が難しかった。"
			);
		learningObservationMapper.insertLearningObservation(observation);
		
		// 변경 전 DB 정보 확보
		List<LearningObservation> originals = learningObservationMapper.findObservationsByLessonReportId(firstReport.getId(), instructor.getId());
		assertEquals(1, originals.size());
		
		LearningObservation original = originals.get(0);
		
		// Java 객체에 수정할 내용 설정
		observation.setCategory("SPEAKING");
		observation.setItem("会話");
		observation.setMemo("自分で話すことができた。");
		
		// 검사 1 : 다른 강사의 수정 요청은 거절
		int rejectedByOwner = learningObservationMapper.updateLearningObservation(
				observation,
				firstReport.getId(),
				otherInstructor.getId()
			);
		assertEquals(0, rejectedByOwner);
		
		List<LearningObservation> afterRejectedOwner = learningObservationMapper.findObservationsByLessonReportId(firstReport.getId(), instructor.getId());
		assertEquals(1, afterRejectedOwner.size());
		
		LearningObservation unchangedByOwner = afterRejectedOwner.get(0);
		
		assertEquals("GRAMMAR", unchangedByOwner.getCategory());
	    assertEquals("過去形", unchangedByOwner.getItem());
	    assertEquals("過去形が難しかった。", unchangedByOwner.getMemo());
	    assertEquals(original.getUpdatedAt(), unchangedByOwner.getUpdatedAt());
		
		// 검사 2 : 담당 강사라도 다른 일보 id 를 넣으면 거절
		int rejectedByReport = learningObservationMapper.updateLearningObservation(
				observation,
				secondReport.getId(),
				instructor.getId()
			);
		assertEquals(0, rejectedByReport);
		
		List<LearningObservation> afterRejectedReport = learningObservationMapper.findObservationsByLessonReportId(firstReport.getId(), instructor.getId());
		assertEquals(1, afterRejectedReport.size());
		
		LearningObservation unchangedByReport = afterRejectedReport.get(0);
		
		assertEquals("GRAMMAR", unchangedByReport.getCategory());
	    assertEquals("過去形", unchangedByReport.getItem());
	    assertEquals("過去形が難しかった。", unchangedByReport.getMemo());
	    assertEquals(original.getUpdatedAt(), unchangedByReport.getUpdatedAt());
		
		// 검사 3 : 올바른 강사와 일보 id 면 수정 성공
		int updatedRows = learningObservationMapper.updateLearningObservation(observation, firstReport.getId(), instructor.getId());
		assertEquals(1, updatedRows);
		
		List<LearningObservation> updatedList = learningObservationMapper.findObservationsByLessonReportId(firstReport.getId(), instructor.getId());
		assertEquals(1, updatedList.size());
		
		LearningObservation updated = updatedList.get(0);
		
		assertEquals("SPEAKING", updated.getCategory());
	    assertEquals("会話", updated.getItem());
	    assertEquals("自分で話すことができた。", updated.getMemo());
	    assertNotNull(updated.getUpdatedAt());
		
		// 수정 대상이 아닌 값은 유지
	    assertEquals(original.getId(), updated.getId());
	    assertEquals(original.getLessonReportId(), updated.getLessonReportId());
	    assertEquals(original.getCreatedAt(), updated.getCreatedAt());
	}
	
	// 다른 강사, 다른 일보의 삭제 요청은 거절되고, 올바른 삭제 시 선택한 관찰 기록만 삭제되는지 Test
	@Test
	@Transactional
	void deleteObservationOnlyForOwnerAndReport() {
		
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
		
		// 학생과 수업 두 건 준비
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				null,
				null,
				null
			);
		studentMapper.insertStudent(student);
		
		Lesson firstLesson = new Lesson(
				student.getId(),
				LocalDateTime.of(2026, 11, 1, 15, 30)
			);
		lessonMapper.insertLesson(firstLesson);
		
		lessonMapper.updateLessonStatus(
				firstLesson.getId(),
				instructor.getId(),
				"ATTENDED",
				null
			);
		
		Lesson secondLesson = new Lesson(
				student.getId(),
				LocalDateTime.of(2026, 11, 2, 15, 30)
			);
		lessonMapper.insertLesson(secondLesson);
		
		lessonMapper.updateLessonStatus(
				secondLesson.getId(),
				instructor.getId(),
				"ATTENDED",
				null
			);
		
		// 같은 강사의 일보 두 건 준비
		LessonReport firstReport = new LessonReport(
				firstLesson.getId(),
				"過去形を練習しました。",
				null
			);
		lessonReportMapper.insertLessonReport(firstReport);
		
		LessonReport secondReport = new LessonReport(
				secondLesson.getId(),
				"会話を練習しました。",
				null
			);
		lessonReportMapper.insertLessonReport(secondReport);
		
		// 첫 번째 일보에 삭제할 관찰 기록과 보존할 기록 준비
		LearningObservation target = new LearningObservation(
				firstReport.getId(),
				"GRAMMAR",
				"過去形",
				"過去形が難しかった。"
			);
	    learningObservationMapper.insertLearningObservation(target);

	    LearningObservation remaining = new LearningObservation(
	    		firstReport.getId(),
	    		"PRONUNCIATION",
	    		"発音",
	    		"正しく発音できた。"
	    	);
	    learningObservationMapper.insertLearningObservation(remaining);
		
		// 검사 1 : 다른 강사의 삭제 요청은 거절되는지
		int rejectedByOwner = learningObservationMapper.deleteLearningObservation(
				target.getId(),
				firstReport.getId(),
				otherInstructor.getId()
			);
		assertEquals(0, rejectedByOwner);
		
		List<LearningObservation> afterRejectedOwner  = learningObservationMapper.findObservationsByLessonReportId(firstReport.getId(), instructor.getId());
		
		assertEquals(2, afterRejectedOwner.size());
		assertEquals(target.getId(), afterRejectedOwner.get(0).getId());
		assertEquals(remaining.getId(), afterRejectedOwner.get(1).getId());
		
		// 검사 2 : 담당 강사라도 다른 일보 id 를 넣으면 거절되는지
		int rejectedByReport = learningObservationMapper.deleteLearningObservation(
				target.getId(),
				secondReport.getId(),
				instructor.getId()
			);
		assertEquals(0, rejectedByReport);

		List<LearningObservation> afterRejectedReport = learningObservationMapper.findObservationsByLessonReportId(firstReport.getId(), instructor.getId());

		assertEquals(2, afterRejectedReport.size());
		assertEquals(target.getId(), afterRejectedReport.get(0).getId());
		assertEquals(remaining.getId(), afterRejectedReport.get(1).getId());
		
		// 검사 3 : 올바른 강사와 일보 id 면 상제 성공하는지
		int deletedRows = learningObservationMapper.deleteLearningObservation(
				target.getId(),
				firstReport.getId(),
				instructor.getId()
			);
		assertEquals(1, deletedRows);

		// 선택한 기록만 삭제되고 다른 관찰 기록은 그대로 보존되는지
		List<LearningObservation> afterDelete = learningObservationMapper.findObservationsByLessonReportId(firstReport.getId(), instructor.getId());

		assertEquals(1, afterDelete.size());
		assertEquals(remaining.getId(), afterDelete.get(0).getId());
		assertEquals("PRONUNCIATION", afterDelete.get(0).getCategory());
		assertEquals("発音", afterDelete.get(0).getItem());
		assertEquals("正しく発音できた。", afterDelete.get(0).getMemo());
		
		// 부모 클래스의 객체인 수업일보와 수업이 유지되는지
		assertNotNull(lessonReportMapper.findLessonReportByLessonId(firstLesson.getId(), instructor.getId()));
		assertNotNull(lessonMapper.findLessonById(firstLesson.getId(), instructor.getId()));
	}
	
	// 일보와 관찰 기록의 통합 및 공백 정리 확인
	@Test
	@Transactional
	void registerLessonReportWithObservation() {
		
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
		
		// 수업 등록 및 출석 처리
		Lesson lesson = new Lesson(
				student.getId(),
				LocalDateTime.of(2026, 11, 1, 15, 30)
			);
		lessonMapper.insertLesson(lesson);
		
		lessonMapper.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"ATTENDED",
				null
			);
		
		// 화면에서 받을 관찰 기록 입력값 두 건 준비
		List<ObservationInput> inputs = new ArrayList<>();
		
		inputs.add(new ObservationInput(
				"      GRAMMAR  ",
				"  過去形       ",
				"     過去形を使うことができた。     "
			));
		
		inputs.add(new ObservationInput(
				"    PRONUNCIATION    ",
				"       発音    ",
				"  正しく発音できた。      "
			));
		
		// LessonReportService 한 번 호출로 일보와 관찰 기록 저장
		Long reportId = lessonReportService.registerLessonReport(
				lesson.getId(),
				instructor.getId(),
				"             過去形を練習しました。    ",
				"  次回は過去形を復習する。         ",
				inputs
			);
		
		assertNotNull(reportId);
		
		// 검사 1 : 일보 저장 및 앞뒤 공백 정리됐는지
		LessonReport savedReport = lessonReportMapper.findLessonReportByLessonId(lesson.getId(), instructor.getId());
		
		assertNotNull(savedReport);
		assertEquals(lesson.getId(), savedReport.getLessonId());
		assertEquals("過去形を練習しました。", savedReport.getContent());
		assertEquals("次回は過去形を復習する。", savedReport.getNextCheck());
		
		// 검사 2 : 같은 일보에 관찰 기록 두 건 저장
		List<LearningObservation> savedObservations = learningObservationMapper.findObservationsByLessonReportId(reportId, instructor.getId());
		
		assertEquals(2, savedObservations.size());
		
		LearningObservation first = savedObservations.get(0);
		assertEquals(reportId, first.getLessonReportId());
		assertEquals("GRAMMAR", first.getCategory());
		assertEquals("過去形", first.getItem());
		assertEquals("過去形を使うことができた。", first.getMemo());
		
		LearningObservation second = savedObservations.get(1);
		assertEquals(reportId, second.getLessonReportId());
		assertEquals("PRONUNCIATION", second.getCategory());
		assertEquals("発音", second.getItem());
		assertEquals("正しく発音できた。", second.getMemo());
	}
	
	// 관찰 항목 없이 일보 등록 및 선택 입력의 빈 값 처리 확인
	@Test
	@Transactional
	void registerLessonReportWithoutObservations() {
		
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
		
		// 수업 등록 및 출석 처리
		Lesson lesson = new Lesson(
				student.getId(),
				LocalDateTime.of(2026, 11, 1, 15, 30)
			);
		lessonMapper.insertLesson(lesson);
		
		lessonMapper.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"ATTENDED",
				null
			);
		
		// 관찰 항목이 없는 빈 목록
		List<ObservationInput> inputs = new ArrayList<>();
		
		// 다음 확인 사항은 공백만 입력
		Long reportId = lessonReportService.registerLessonReport(
				lesson.getId(),
				instructor.getId(),
				"会話を練習しました。",
				"         ",
				inputs
			);
		
		assertNotNull(reportId);
		
		// 검사 1 : 일보는 저장되고 다음 확인 사항은 null 인지
		LessonReport saved = lessonReportMapper.findLessonReportByLessonId(lesson.getId(), instructor.getId());
		
		assertNotNull(saved);
		assertEquals(reportId, saved.getId());
		assertEquals("会話を練習しました。", saved.getContent());
		assertNull(saved.getNextCheck());
		
		// 검사 2 : 관찰 기록은 저장되지 않는지
		List<LearningObservation> observations = learningObservationMapper.findObservationsByLessonReportId(reportId, instructor.getId());
		
		assertTrue(observations.isEmpty());
	}
	
	// 다른 강사의 수업일보 등록 요청을 거절하고 LessonNotFoundException 이 나오는지 확인
	@Test
	@Transactional
	void rejectLessonReportRegistrationForOtherInstructor() {
		
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
		
		// 첫 번째 강사의 학생과 수업 준비
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
		
		lessonMapper.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"ATTENDED",
				null
			);
		
		List<ObservationInput> inputs = new ArrayList<>();
		
		// 다른 강사의 id 로 일보 등록 시도
		boolean rejected = false;
		
		try {
			lessonReportService.registerLessonReport(
					lesson.getId(),
					otherInstructor.getId(),
					"会話を練習しました。",
					null,
					inputs
				);
		} catch (LessonNotFoundException e) {
			rejected = true;
		}
		
		// 검사 1 : 접근 거절 예외 발생
		assertTrue(rejected);
		
		// 검사 2 : 실제 담당 강사로 조회시 일보가 없는지 확인
		LessonReport saved = lessonReportMapper.findLessonReportByLessonId(lesson.getId(), instructor.getId());
		
		assertNull(saved);
	}
	
	// 출석 이외의(예정, 취소, 결석) 수업 상태에서는 일보 등록 거절
	@Test
	@Transactional
	void rejectLessonReportRegistrationForNonAttendedLessons() {
		
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
		
		// 일보를 작성 할 수 없는 세 가지 상태
		String[] statuses = {
				"SCHEDULED",
				"CANCELLED",
				"ABSENT"
		};
		
		for (String status : statuses) {
			// 각 상태를 검사할 수업 준비
			Lesson lesson = new Lesson(
					student.getId(),
					LocalDateTime.of(2026, 11, 1, 15, 30)
				);
			lessonMapper.insertLesson(lesson);
			
			lessonMapper.updateLessonStatus(
					lesson.getId(),
					instructor.getId(),
					status,
					null
				);
			
			List<ObservationInput> inputs = new ArrayList<>();
			
			// 출석 이외의 수업에 일보 등록 시도
			boolean rejected = false;
			
			try {
				lessonReportService.registerLessonReport(
						lesson.getId(),
						instructor.getId(),
						"会話を練習しました。",
						null,
						inputs
					);
			
			} catch (IllegalArgumentException e) {
				rejected = true;
				
				assertEquals("出席・遅刻の授業のみ記録を作成できます。", e.getMessage());
			}
			
			// 검사 1 : 해당 상태에서 등록이 거절됐는지
			assertTrue(rejected);
			
			// 검사 2 : DB 에 일보가 저장되지 않았는지
			LessonReport saved = lessonReportMapper.findLessonReportByLessonId(lesson.getId(), instructor.getId());
			
			assertNull(saved);
		}
	}
	
	// 같은 수업의 중복 일보 등록 거절되는지 및 기존 내용 유지되는지 확인
	@Test
	@Transactional
	void rejectDuplicateLessonReportRegistration() {
		
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
		
		// 수업 등록 및 출석 처리
		Lesson lesson = new Lesson(
				student.getId(),
				LocalDateTime.of(2026, 11, 1, 15, 30)
			);
		lessonMapper.insertLesson(lesson);
		
		lessonMapper.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"ATTENDED",
				null
			);
			
			List<ObservationInput> inputs = new ArrayList<>();
		
		// 첫 번째 일보 등록
		Long originalId = lessonReportService.registerLessonReport(
				lesson.getId(),
				instructor.getId(),
				"過去形を練習しました。",
				"次回は過去形を復習する。",
				inputs
			);
		
		// 같은 수업에 두 번째 일보 등록 시도
		boolean rejected = false;
		
		try {
			lessonReportService.registerLessonReport(
					lesson.getId(), 
					instructor.getId(),
					"会話を練習しました。", 
					"次回は発音を練習する。", 
					inputs
				);
		
		} catch (IllegalArgumentException e) {
			rejected = true;
			
			assertEquals("この授業の記録は既に登録されています。", e.getMessage());
		}
		
		// 검사 1 : 중복 등록이 거절되는지
		assertTrue(rejected);
		
		// 검사 2 : 기존 일보의 id 와 내용이 그대로인지 확인
		LessonReport saved = lessonReportMapper.findLessonReportByLessonId(lesson.getId(), instructor.getId());
		
		assertNotNull(saved);
		assertEquals(originalId, saved.getId());
		assertEquals("過去形を練習しました。", saved.getContent());
		assertEquals("次回は過去形を復習する。", saved.getNextCheck());
	}
	
	// 필수 입력값 누락시 일보 등록 거절
	// (수업 내용, 관찰 분류, 관찰 항목, 관찰 내용)
	@Test
	@Transactional
	void rejectLessonReportRegistrationWithoutRequiredValues() {
		
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
		
		// 수업 등록 및 출석 처리
		Lesson lesson = new Lesson(
				student.getId(),
				LocalDateTime.of(2026, 11, 1, 15, 30)
			);
		lessonMapper.insertLesson(lesson);
		
		lessonMapper.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"ATTENDED",
				null
			);
		
		// null, 빈 문자열, 공백만 있는 문자열을 각각 검사
		String[] missingValues = {null, "", "      "};
		
		for (String missing : missingValues) {
			
			// 검사 1 : 수업 내용 누락
			boolean contentRejected = false;
			
			try {
				lessonReportService.registerLessonReport(
						lesson.getId(),
						instructor.getId(),
						missing,
						null,
						new ArrayList<>()
					);
				
			} catch (IllegalArgumentException e) {
				contentRejected = true;
				
				assertEquals("授業内容を入力してください。", e.getMessage());
			}
			
			assertTrue(contentRejected);
			
			// 검사 2 : 관찰 분류 누락
			List<ObservationInput> inputs = new ArrayList<>();
			
			inputs.add(new ObservationInput(
					missing,
					"過去形",
					"過去形を使うことができた。"
				));
			
			boolean categoryRejected = false;

			try {
				lessonReportService.registerLessonReport(
						lesson.getId(),
						instructor.getId(),
						"過去形を練習しました。",
						null,
						inputs
					);

			} catch (IllegalArgumentException e) {
				categoryRejected = true;
				assertEquals("観察の分類を選択してください。", e.getMessage());
			}
			
			assertTrue(categoryRejected);
			
			// 검사 3 : 관찰 항목 누락
			inputs = new ArrayList<>();

			inputs.add(new ObservationInput(
					"GRAMMAR",
					missing,
					"過去形を使うことができた。"
				));

			boolean itemRejected = false;

			try {
				lessonReportService.registerLessonReport(
						lesson.getId(),
						instructor.getId(),
						"過去形を練習しました。",
						null,
						inputs
					);

			} catch (IllegalArgumentException e) {
				itemRejected = true;
				assertEquals("観察項目を入力してください。", e.getMessage());
			}

			assertTrue(itemRejected);
			
			// 검사 4 : 관찰 내용 누락
			inputs = new ArrayList<>();

			inputs.add(new ObservationInput(
					"GRAMMAR",
					"過去形",
					missing
				));

			boolean memoRejected = false;

			try {
				lessonReportService.registerLessonReport(
						lesson.getId(),
						instructor.getId(),
						"過去形を練習しました。",
						null,
						inputs
					);

			} catch (IllegalArgumentException e) {
				memoRejected = true;
				assertEquals("観察内容を入力してください。", e.getMessage());
			}

			assertTrue(memoRejected);
			
			// 전부 일보가 저장되지 않았는지 확인
			LessonReport saved = lessonReportMapper.findLessonReportByLessonId(lesson.getId(), instructor.getId());

	        assertNull(saved);
		}
	}
	
	// 정해지지 않은 학습 관찰 분류로 일보 등록 시도 시 거절되는지 Test
	@Test
	@Transactional
	void rejectLessonReportRegistrationWithInvalidCategory() {
		
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
		
		// 수업 등록 및 출석 처리
		Lesson lesson = new Lesson(
				student.getId(),
				LocalDateTime.of(2026, 11, 1, 15, 30)
			);
		lessonMapper.insertLesson(lesson);
		
		lessonMapper.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"ATTENDED",
				null
			);
		
		// 정해지지 않은 분류를 가진 학습 관찰 항목 준비
		List<ObservationInput> inputs = new ArrayList<>();
		
		inputs.add(new ObservationInput(
				"UNKNOWN",
				"過去形",
				"過去形を使うことができた。"
			));
		
		boolean rejected = false;
		
		try {
			lessonReportService.registerLessonReport(
					lesson.getId(),
					instructor.getId(),
					"過去形を練習しました。",
					null,
					inputs
				);

		} catch (IllegalArgumentException e) {
			rejected = true;

			assertEquals(
					"観察の分類が正しくありません。",
					e.getMessage()
				);
		}
		
		// 검사 1 : 학습 관찰 분류가 거절됐는지 확인
		assertTrue(rejected);
		
		// 검사 2 : 일보도 저장되지 않았는지 확인
		LessonReport saved = lessonReportMapper.findLessonReportByLessonId(lesson.getId(), instructor.getId());
		
		assertNull(saved);
	}
	
	// 최대 길이를 넘은 내용으로 일보 등록 시 거절되는지 Test
	@Test
	@Transactional
	void rejectLessonReportRegistrationWithTooLongValues() {
		
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
		
		// 수업 등록 및 출석 처리
		Lesson lesson = new Lesson(
				student.getId(),
				LocalDateTime.of(2026, 11, 1, 15, 30)
			);
		lessonMapper.insertLesson(lesson);
		
		lessonMapper.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"ATTENDED",
				null
			);
		
		// 검사 1 : 수업 내용 2001자 등록 안되는지 확인
		boolean contentRejected = false;
		
		try {
			lessonReportService.registerLessonReport(
					lesson.getId(),
					instructor.getId(),
					"잉".repeat(2001),
					null,
					new ArrayList<>()
				);
			
		} catch ( IllegalArgumentException e) {
			contentRejected = true;
			
			assertEquals("授業内容は2000文字以内で入力してください。", e.getMessage());
		}
		
		assertTrue(contentRejected);
		
		// 검사 2 : 다음 확인 사항 1001자 등록 안되는지 확인
		boolean nextCheckRejected = false;
		
		try {
			lessonReportService.registerLessonReport(
					lesson.getId(),
					instructor.getId(),
					"過去形を練習しました。",
					"잉".repeat(1001),
					new ArrayList<>()
				);
			
		} catch (IllegalArgumentException e) {
			nextCheckRejected = true;
			
			assertEquals("次回の確認事項は1000文字以内で入力してください。", e.getMessage());
		}
		
		assertTrue(nextCheckRejected);
		
		// 검사 3 : 관찰 항목 101자 등록 안되는지 확인
		List<ObservationInput> inputs = new ArrayList<>();
		
		inputs.add(new ObservationInput(
				"GRAMMAR",
				"잉".repeat(101),
				"過去形を使うことができた。"
			));
		
		boolean itemRejected = false;
		
		try {
			lessonReportService.registerLessonReport(
					lesson.getId(),
					instructor.getId(),
					"過去形を練習しました。",
					null,
					inputs
				);
			
		} catch (IllegalArgumentException e) {
			itemRejected = true;
			
			assertEquals("観察項目は100文字以内で入力してください。",e.getMessage());
		}
		
		assertTrue(itemRejected);
		
		// 검사 4 : 관찰 내용 1001자 등록 안되는지 확인
		inputs = new ArrayList<>();
		
		inputs.add(new ObservationInput(
				"GRAMMAR",
				"過去形",
				"잉".repeat(1001)
			));
		
		boolean memoRejected = false;
		
		try {
			lessonReportService.registerLessonReport(
					lesson.getId(),
					instructor.getId(),
					"過去形を練習しました。",
					null,
					inputs
				);
			
		} catch (IllegalArgumentException e) {
			memoRejected = true;
			
			assertEquals("観察内容は1000文字以内で入力してください。" ,e.getMessage());
		}
		
		assertTrue(memoRejected);
		
		// 전부 등록 안되어 일보가 저장 안됐는지 확인
		LessonReport saved = lessonReportMapper.findLessonReportByLessonId(lesson.getId(), instructor.getId());
		
		assertNull(saved);
	}
	
	// Service 를 통한 일보, 관찰 기록 조회 및 다른 강사의 접근 거절 Test
	@Test
	@Transactional
	void findLessonReportAndObservationsThroughServiceOnlyForOwner() {
		
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
		
		// 수업 등록 및 출석 처리
		Lesson lesson = new Lesson(
				student.getId(),
				LocalDateTime.of(2026, 11, 1, 15, 30)
			);
		lessonMapper.insertLesson(lesson);
		
		lessonMapper.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"ATTENDED",
				null
			);
		
		// 조회할 일보와 관찰 기록 준비
		LessonReport report = new LessonReport(
				lesson.getId(),
				"会話を練習しました。",
				"次回も会話を練習する。"
			);
		lessonReportMapper.insertLessonReport(report);
		
		LearningObservation observation = new LearningObservation(
				report.getId(),
				"SPEAKING",
				"自己紹介",
				"自分の名前を伝えることができた。"
			);
		learningObservationMapper.insertLearningObservation(observation);
		
		// 검사 1 : 담당 강사는 해당 수업의 일보를 조회할 수 있는지 확인
		LessonReport found = lessonReportService.findLessonReportByLessonId(
				lesson.getId(), 
				instructor.getId()
			);
		
		assertNotNull(found);
		assertEquals(report.getId(), found.getId());
		assertEquals(lesson.getId(), found.getLessonId());
		assertEquals("会話を練習しました。", found.getContent());
		
		// 검사 2 : 담당 강사는 일보에 연결된 관찰 기록을 조회할 수 있는지 확인
		List<LearningObservation> observations = lessonReportService.findObservationsByLessonId(
				lesson.getId(),
				instructor.getId()
			);
		
		assertEquals(1, observations.size());
		assertEquals(observation.getId(), observations.get(0).getId());
		assertEquals(report.getId(), observations.get(0).getLessonReportId());
		
		// 검사 3 : 다른 강사의 일보 조회는 거절되는지 확인
		boolean reportRejected = false;
		
		try {
			lessonReportService.findLessonReportByLessonId(
					lesson.getId(),
					otherInstructor.getId()
				);
			
		} catch (LessonNotFoundException e) {
			reportRejected = true;
		}
		
		assertTrue(reportRejected);
		
		// 검사 4 : 다른 강사의 관찰 기록 조회도 거절되는지 확인
		boolean observationsRejected = false;
		
		try {
			lessonReportService.findObservationsByLessonId(
					lesson.getId(),
					otherInstructor.getId()
				);
			
		} catch (LessonNotFoundException e) {
			observationsRejected = true;
		}
		
		assertTrue(observationsRejected);
	}
	
	// 담당 수업이지만 아직 일보가 없는 경우 -> 일보 전용 예외 발생하는지 Test
	@Test
	@Transactional
	void rejectFindingLessonReportWhenReportDoesNotExist() {
		
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
		
		// 수업만 등록하고 일보는 작성하지 않음
		Lesson lesson = new Lesson(
				student.getId(),
				LocalDateTime.of(2026, 11, 1, 15, 30)
			);
		lessonMapper.insertLesson(lesson);
		
		// 검사 1 : 일보 조회 시 전용 예외 발생하는지 확인
		boolean reportRejected = false;
		
		try {
			lessonReportService.findLessonReportByLessonId(
					lesson.getId(),
					instructor.getId()
				);
			
		} catch (LessonReportNotFoundException e) {
			reportRejected = true;
		}
		
		assertTrue(reportRejected);
		
		// 검사 2 : 관찰 기록 조회도 일보가 없으면 전용 예외 발생하는지 확인
		boolean observationsRejected = false;
		
		try {
			lessonReportService.findLessonReportByLessonId(
					lesson.getId(),
					instructor.getId()
				);
			
		} catch (LessonReportNotFoundException e) {
			observationsRejected = true;
		}
		
		assertTrue(observationsRejected);
	}
	
	// 일보에 관찰 기록이 없으면 빈 목록 반환 하는지 Test, 예외 발생 x
	@Test
	@Transactional
	void findEmptyObservationsWhenReportHasNoObservations() {
		
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
		
		// 수업 등록 및 출석 처리
		Lesson lesson = new Lesson(
				student.getId(),
				LocalDateTime.of(2026, 11, 1, 15, 30)
			);
		lessonMapper.insertLesson(lesson);

		lessonMapper.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"ATTENDED",
				null
			);
		
		// 일보만 등록하고 관찰 기록은 등록하지 않음
		LessonReport report = new LessonReport(
				lesson.getId(),
				"会話を練習しました。",
				null
			);
		lessonReportMapper.insertLessonReport(report);
		
		// 관찰 기록 조회
		List<LearningObservation> observations = lessonReportService.findObservationsByLessonId(
				lesson.getId(),
				instructor.getId()
			);
		
		// null 이 아닌 빈 목록인지 확인
		assertNotNull(observations);
		assertTrue(observations.isEmpty());
	}
	
	// 일보가 있는 수업은 출석 이외의 상태로 변경 거절
	@Test
	@Transactional
	void rejectNonAttendedStatusWhenLessonReportExists() {
		
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
		
		// 수업 등록 및 출석 처리
		Lesson lesson = new Lesson(
				student.getId(),
				LocalDateTime.of(2026, 11, 1, 15, 30)
			);
		lessonMapper.insertLesson(lesson);

		lessonMapper.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"ATTENDED",
				"予定どおり出席"
			);
		
		// 해당 수업에 일보 등록
		LessonReport report = new LessonReport(
				lesson.getId(),
				"会話を練習しました。",
				null
			);
		lessonReportMapper.insertLessonReport(report);
		
		// 변경을 거절해야 하는 출석 이외의 세 가지 상태
		String[] statuses = {
				"SCHEDULED",
				"CANCELLED",
				"ABSENT"
		};
		
		for (String status : statuses) {
			
			// 출석 이외의 상태로 변경 시도
			boolean rejected = false;
			
			try {
				lessonService.updateLessonStatus(
						lesson.getId(),
						instructor.getId(),
						status,
						"変更されてはいけないメモ"
					);
				
			} catch (IllegalArgumentException e) {
				rejected = true;
				
				assertEquals("授業記録があるため、出席・遅刻以外の状態には変更できません。", e.getMessage());
			}

			// 검사 1 : 상태 변경이 거절됐는지 검사
			assertTrue(rejected);
			
			// 검사 2 : DB 의 상태와 메모가 그대로인지 검사
			Lesson unchanged = lessonMapper.findLessonById(
					lesson.getId(),
					instructor.getId()
				);
			
			assertNotNull(unchanged);
			assertEquals("ATTENDED", unchanged.getStatus());
			assertEquals("予定どおり出席", unchanged.getStatusMemo());
		}
	}
	
	// 일보가 있어도 출석 상태를 유지하면 상태 메모 수정 허용
	@Test
	@Transactional
	void updateStatusMemoWhenLessonReportExists() {
		
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
		
		// 수업 등록 및 출석 처리
		Lesson lesson = new Lesson(
				student.getId(),
				LocalDateTime.of(2026, 11, 1, 15, 30)
			);
		lessonMapper.insertLesson(lesson);

		lessonMapper.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"ATTENDED",
				"変更前のメモ"
			);
		
		// 해당 수업에 일보 등록
		LessonReport report = new LessonReport(
				lesson.getId(),
				"会話を練習しました。",
				"次回も会話を練習する。"
			);
		lessonReportMapper.insertLessonReport(report);
		
		// 출석 상태에서 상태 메모 수정
		lessonService.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"ATTENDED",
				"予定どおり出席"
			);
		
		// 검사 1 : 출석 상태는 그대로, 메모는 변경됐는지
		Lesson updated = lessonMapper.findLessonById(
				lesson.getId(),
				instructor.getId()
			);
		
		assertNotNull(updated);
		assertEquals("ATTENDED", updated.getStatus());
		assertEquals("予定どおり出席", updated.getStatusMemo());
		
		// 검사 2 : 연결된 일보는 그대로인지
		LessonReport unchanged = lessonReportMapper.findLessonReportByLessonId(
				lesson.getId(),
				instructor.getId()
			);
		
		assertNotNull(unchanged);
		assertEquals(report.getId(), unchanged.getId());
		assertEquals("会話を練習しました。", unchanged.getContent());
		assertEquals("次回も会話を練習する。", unchanged.getNextCheck());
	}
	
	// 일보 수정과 관찰 기록 수정, 삭제, 추가
	@Test
	@Transactional
	void updateLessonReportWithObservations() {
		
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
		
		// 출석한 수업 준비
		Lesson lesson = new Lesson(
				student.getId(),
				LocalDateTime.of(2026, 11, 1, 15, 30)
			);
		lessonMapper.insertLesson(lesson);

		lessonMapper.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"ATTENDED",
				null
			);
		
		// 일보 준비
		LessonReport report = new LessonReport(
				lesson.getId(),
				"過去形を練習しました。",
				"次回は過去形を復習する。"
			);
		lessonReportMapper.insertLessonReport(report);
		
		// 수정할 관찰 기록
		LearningObservation grammar = new LearningObservation(
				report.getId(),
				"GRAMMAR",
				"過去形",
				"過去形を練習した。"
			);
		learningObservationMapper.insertLearningObservation(grammar);
		
		// 삭제할 관찰 기록
		LearningObservation pronunciation = new LearningObservation(
				report.getId(),
				"PRONUNCIATION",
				"発音",
				"ゆっくり話した。"
			);
		learningObservationMapper.insertLearningObservation(pronunciation);
		
		// 수정 전 정보 확보
		LessonReport originalReport = lessonReportMapper.findLessonReportByLessonId(
				lesson.getId(),
				instructor.getId()
			);
		assertNotNull(originalReport);

		List<LearningObservation> originals = learningObservationMapper.findObservationsByLessonReportId(
				report.getId(),
				instructor.getId()
			);
		assertEquals(2, originals.size());

		LearningObservation originalGrammar = originals.get(0);
		
		// 기존 문법 기록 수정 & 새 회화 기록 추가
		// 발음 기록은 inputs 에 넣지 않아서 DB 에서 삭제되도록 함
		List<ObservationUpdateInput> inputs = new ArrayList<>();

		inputs.add(new ObservationUpdateInput(
				grammar.getId(),
				"GRAMMAR",
				"過去形の復習",
				"過去形を正しく使えた。"
			));

		inputs.add(new ObservationUpdateInput(
				null,
				"SPEAKING",
				"自己紹介",
				"自分の名前を伝えることができた。"
			));

		lessonReportService.updateLessonReport(
				lesson.getId(),
				instructor.getId(),
				"過去形と会話を練習しました。",
				null,
				inputs
			);
		
		// 검사 1 : 일보 내용 수정, 다음 확인 사항 삭제
		LessonReport updatedReport = lessonReportMapper.findLessonReportByLessonId(
				lesson.getId(),
				instructor.getId()
			);

		assertNotNull(updatedReport);
		assertEquals("過去形と会話を練習しました。", updatedReport.getContent());
		assertNull(updatedReport.getNextCheck());
		
		// 일보의 id, 연결된 수업, 최초 작성 시각 유지
		assertEquals(originalReport.getId(), updatedReport.getId());
	    assertEquals(originalReport.getLessonId(), updatedReport.getLessonId());
	    assertEquals(originalReport.getCreatedAt(), updatedReport.getCreatedAt());
		
		// 변경 후 관찰 기록 조회
	    List<LearningObservation> updated = learningObservationMapper.findObservationsByLessonReportId(
	    		report.getId(),
	    		instructor.getId()
	    	);

	    assertEquals(2, updated.size());
		
		// 검사 2 : 기존 문법 기록 수정 및 id 최초 작성 시각 남겨둠
	    LearningObservation updatedGrammar = updated.get(0);

	    assertEquals(grammar.getId(), updatedGrammar.getId());
	    assertEquals(report.getId(), updatedGrammar.getLessonReportId());
	    assertEquals("GRAMMAR", updatedGrammar.getCategory());
	    assertEquals("過去形の復習", updatedGrammar.getItem());
	    assertEquals("過去形を正しく使えた。", updatedGrammar.getMemo());
	    assertEquals(originalGrammar.getCreatedAt(),updatedGrammar.getCreatedAt());
		
		// 검사 3 : 새 회화 기록 등록
	    LearningObservation added = updated.get(1);

	    assertNotNull(added.getId());
	    assertNotEquals(grammar.getId(), added.getId());
	    assertNotEquals(pronunciation.getId(), added.getId());
	    assertEquals(report.getId(), added.getLessonReportId());
	    assertEquals("SPEAKING", added.getCategory());
	    assertEquals("自己紹介", added.getItem());
	    assertEquals("自分の名前を伝えることができた。", added.getMemo());
		
		// 검사 4 : DB 에서 삭제한 발음 기록이 삭제됐는지
	    for (LearningObservation observation : updated) {
	    	assertNotEquals(pronunciation.getId(), observation.getId());
	    }
	}
	
	// 다른 일보의 관찰 ID를 넣으면 수정 거절 및 기존 데이터 유지
	@Test
	@Transactional
	void rejectObservationFromAnotherReport() {

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

	    // 수업 두 건 준비
	    Lesson lessonA = new Lesson(
	            student.getId(),
	            LocalDateTime.of(2026, 11, 1, 15, 30)
	    	);
	    lessonMapper.insertLesson(lessonA);

	    Lesson lessonB = new Lesson(
	            student.getId(),
	            LocalDateTime.of(2026, 11, 2, 15, 30)
	    	);
	    lessonMapper.insertLesson(lessonB);

	    lessonMapper.updateLessonStatus(
	    		lessonA.getId(),
	    		instructor.getId(),
	    		"ATTENDED",
	    		null
	    	);
	    lessonMapper.updateLessonStatus(
	    		lessonB.getId(),
	    		instructor.getId(),
	    		"ATTENDED",
	    		null
	    	);

	    // 일보 두 건 준비
	    LessonReport reportA = new LessonReport(
	            lessonA.getId(),
	            "過去形を練習しました。",
	            "次回は復習する。"
	    	);
	    lessonReportMapper.insertLessonReport(reportA);

	    LessonReport reportB = new LessonReport(
	            lessonB.getId(),
	            "会話を練習しました。",
	            null
	    	);
	    lessonReportMapper.insertLessonReport(reportB);

	    // 각 일보에 관찰 기록 한 건씩 준비
	    LearningObservation observationA = new LearningObservation(
	            reportA.getId(),
	            "GRAMMAR",
	            "過去形",
	            "過去形を使えた。"
	    	);
	    learningObservationMapper.insertLearningObservation(observationA);

	    LearningObservation observationB = new LearningObservation(
	            reportB.getId(),
	            "SPEAKING",
	            "自己紹介",
	            "自分の名前を伝えた。"
	    	);
	    learningObservationMapper.insertLearningObservation(observationB);

	    // report A 를 수정하면서 report B 의 관찰 id 를 전달
	    List<ObservationUpdateInput> inputs = new ArrayList<>();

	    inputs.add(new ObservationUpdateInput(
	            observationB.getId(),
	            "OTHER",
	            "変更後の項目",
	            "変更されてはいけない内容"
	    	));

	    boolean rejected = false;

	    try {
	        lessonReportService.updateLessonReport(
	                lessonA.getId(),
	                instructor.getId(),
	                "変更されてはいけない授業内容",
	                null,
	                inputs
	        	);

	    } catch (IllegalArgumentException e) {
	        rejected = true;
	        assertEquals("観察項目が正しくありません。", e.getMessage());
	    }

	    // 검사 1 : 잘못된 관찰 id 를 포함한 요청 거절
	    assertTrue(rejected);

	    // 검사 2 : 수정 대상인 report A 의 내용 유지
	    LessonReport unchangedReport = lessonReportMapper.findLessonReportByLessonId(
	    		lessonA.getId(),
	    		instructor.getId()
	    	);

	    assertNotNull(unchangedReport);
	    assertEquals("過去形を練習しました。", unchangedReport.getContent());
	    assertEquals("次回は復習する。", unchangedReport.getNextCheck());

	    // 검사 3 : 입력 목록에서 빠진 report A 의 관찰 기록도 삭제되지 않음
	    List<LearningObservation> unchangedA = learningObservationMapper.findObservationsByLessonReportId(
	    		reportA.getId(),
	    		instructor.getId()
	    	);

	    assertEquals(1, unchangedA.size());
	    assertEquals(observationA.getId(), unchangedA.get(0).getId());
	    assertEquals("GRAMMAR", unchangedA.get(0).getCategory());
	    assertEquals("過去形", unchangedA.get(0).getItem());
	    assertEquals("過去形を使えた。", unchangedA.get(0).getMemo());

	    // 검사 4 : 잘못 전달한 report B 의 관찰 기록도 변경되지 않음
	    List<LearningObservation> unchangedB = learningObservationMapper.findObservationsByLessonReportId(
	    		reportB.getId(),
	    		instructor.getId()
	    	);

	    assertEquals(1, unchangedB.size());
	    assertEquals(observationB.getId(), unchangedB.get(0).getId());
	    assertEquals(reportB.getId(), unchangedB.get(0).getLessonReportId());
	    assertEquals("SPEAKING", unchangedB.get(0).getCategory());
	    assertEquals("自己紹介", unchangedB.get(0).getItem());
	    assertEquals("自分の名前を伝えた。", unchangedB.get(0).getMemo());
	}
	
	// 다른 강사의 일보 수정 요청 거절 및 기존 데이터 유지
	@Test
	@Transactional
	void rejectLessonReportUpdateByOtherInstructor() {

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

	    // 출석한 수업 준비
	    Lesson lesson = new Lesson(
	            student.getId(),
	            LocalDateTime.of(2026, 11, 1, 15, 30)
	    	);
	    lessonMapper.insertLesson(lesson);

	    lessonMapper.updateLessonStatus(
	            lesson.getId(),
	            instructor.getId(),
	            "ATTENDED",
	            null
	    	);

	    // 기존 일보와 관찰 기록 준비
	    LessonReport report = new LessonReport(
	            lesson.getId(),
	            "過去形を練習しました。",
	            "次回は復習する。"
	    	);
	    lessonReportMapper.insertLessonReport(report);

	    LearningObservation observation = new LearningObservation(
	            report.getId(),
	            "GRAMMAR",
	            "過去形",
	            "過去形を使えた。"
	    	);
	    learningObservationMapper.insertLearningObservation(observation);

	    // 빈 목록은 기존 관찰 기록을 전부 삭제하라는 요청
	    List<ObservationUpdateInput> inputs = new ArrayList<>();

	    // 다른 강사의 ID로 일보 수정 및 관찰 기록 삭제 시도
	    boolean rejected = false;

	    try {
	        lessonReportService.updateLessonReport(
	                lesson.getId(),
	                otherInstructor.getId(),
	                "変更されてはいけない授業内容",
	                null,
	                inputs
	        );

	    } catch (LessonNotFoundException e) {
	        rejected = true;
	    }

	    // 검사 1 : 다른 강사의 수정 요청 거절
	    assertTrue(rejected);

	    // 검사 2 : 소유 강사로 조회하여 일보 내용이 그대로인지 확인
	    LessonReport unchangedReport =
	            lessonReportMapper.findLessonReportByLessonId(
	                    lesson.getId(),
	                    instructor.getId()
	            );

	    assertNotNull(unchangedReport);
	    assertEquals(report.getId(), unchangedReport.getId());
	    assertEquals("過去形を練習しました。", unchangedReport.getContent());
	    assertEquals("次回は復習する。", unchangedReport.getNextCheck());

	    // 검사 3 : 기존 관찰 기록도 삭제되지 않고 유지되는지 확인
	    List<LearningObservation> unchangedObservations =
	            learningObservationMapper.findObservationsByLessonReportId(
	                    report.getId(),
	                    instructor.getId()
	            );

	    assertEquals(1, unchangedObservations.size());

	    LearningObservation unchanged = unchangedObservations.get(0);

	    assertEquals(observation.getId(), unchanged.getId());
	    assertEquals(report.getId(), unchanged.getLessonReportId());
	    assertEquals("GRAMMAR", unchanged.getCategory());
	    assertEquals("過去形", unchanged.getItem());
	    assertEquals("過去形を使えた。", unchanged.getMemo());
	}
	
	// home 일정의 날짜 선택, 전체 상태 표시 및 강사별 조회 제한
	@Test
	@Transactional
	void findHomeLessonsOnlyForOwner() {
		
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
		
		// 각 강사의 학생 준비
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				null,
				null,
				null
			);
		studentMapper.insertStudent(student);
		
		Student otherStudent = new Student(
				otherInstructor.getId(),
				"鈴木花子",
				null,
				null,
				null
			);
		studentMapper.insertStudent(otherStudent);
		
		// test 에서 사용할 고정 기준 날짜
		LocalDate today = LocalDate.of(2026, 11, 16);
		
		// 아직 수업이 없으면 표시할 날짜도 없음
		assertNull(lessonMapper.findHomeLessonDate(
				instructor.getId(), today.atStartOfDay()
			));
		
		// 기준일에는 취소된 수업만 준비
		Lesson todayCancelled = new Lesson(
				student.getId(), today.atTime(10, 0)
			);
		lessonMapper.insertLesson(todayCancelled);
		
		lessonMapper.updateLessonStatus(
				todayCancelled.getId(),
				instructor.getId(),
				"CANCELLED",
				null
			);
		
		// 모레의 예정 수업
		Lesson scheduled = new Lesson(
				student.getId(),
				today.plusDays(2).atTime(15,0)
			);
		lessonMapper.insertLesson(scheduled);
		
		// 모레의 취소 수업 : 예정 수업보다 이른 시간
		Lesson cancelled = new Lesson(
				student.getId(),
				today.plusDays(2).atTime(10,0)
			);
		lessonMapper.insertLesson(cancelled);
		
		lessonMapper.updateLessonStatus(
				cancelled.getId(),
				instructor.getId(),
				"CANCELLED",
				null
			);
		
		// 다른 강사의 오늘 예정 수업 : 날짜 선택에 영향을 주면 안 됨
		Lesson otherToday = new Lesson(
				otherStudent.getId(),
				today.atTime(9, 0)
			);
		lessonMapper.insertLesson(otherToday);
		
		// 다른 강사의 모레 수업 : 목록에 섞이면 안 됨
		Lesson otherSameDay = new Lesson(
				otherStudent.getId(),
				today.plusDays(2).atTime(11, 0)
			);
		lessonMapper.insertLesson(otherSameDay);
		
		// 선택 날짜 다음 날 00:00 수업 : 선택 날짜 목록에서 제외
		Lesson nextDay = new Lesson(
				student.getId(),
				today.plusDays(3).atStartOfDay()
			);
		lessonMapper.insertLesson(nextDay);
		
		// 검사 1 : 오늘 취소 수업만 있으므로 모레 선택
		LocalDate selectedDate = lessonMapper.findHomeLessonDate(
				instructor.getId(), 
				today.atStartOfDay()
			);
		
		assertEquals(today.plusDays(2), selectedDate);
		
		// 검사 2 : 모레의 내 수업만 시간순으로 조회
		List<HomeLesson> found = lessonMapper.findHomeLessonsByDate(
				instructor.getId(),
	            selectedDate.atStartOfDay(),
	            selectedDate.plusDays(1).atStartOfDay()
			);

		assertEquals(2, found.size());

		assertEquals(cancelled.getId(), found.get(0).getId());
		assertEquals("CANCELLED", found.get(0).getStatus());

		assertEquals(scheduled.getId(), found.get(1).getId());
		assertEquals("SCHEDULED", found.get(1).getStatus());
		assertEquals(student.getId(), found.get(1).getStudentId());
		assertEquals("山田太郎", found.get(1).getStudentName());
		assertEquals(scheduled.getLessonAt(), found.get(1).getLessonAt());
		assertNull(found.get(1).getLessonReportId());
		
		// 검사 3 : 오늘 수업을 예정으로 바꾸면 오늘 선택
		lessonMapper.updateLessonStatus(
				todayCancelled.getId(), 
				instructor.getId(),
				"SCHEDULED",
				null
			);
		
		assertEquals(today, lessonMapper.findHomeLessonDate(instructor.getId(), today.atStartOfDay()));
	}
	
	// 미래의 수업의 출석, 결석 처리를 거절하고 기존 상태와 메모 유지
	@Test
	@Transactional
	void rejectAttendanceBeforeLessonStarts() {
		
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
		
		// 실행 날짜가 바뀌어도 미래가 되도록 내일 수업 준비
		LocalDate tomorrow = LocalDate.now(ZoneId.of("Asia/Tokyo")).plusDays(1);
		
		Lesson lesson = new Lesson(student.getId(), tomorrow.atTime(15,0));
		lessonMapper.insertLesson(lesson);
		
		// 변경 전 상태와 메모 설정
		lessonService.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"SCHEDULED",
				"変更前のメモ"
			);
		
		String[] statuses = {
				"ATTENDED",
				"ABSENT"
		};
		
		for (String status : statuses) {
			// 검사 1 : 미래 수업의 출석, 결석 변경 시 예외 발생
			IllegalArgumentException exception = assertThrows(
					IllegalArgumentException.class,
					() -> lessonService.updateLessonStatus(
							lesson.getId(),
							instructor.getId(),
							status,
							"変更後のメモ"
						)
				);
			assertEquals("授業開始前は、出席・遅刻・欠席に変更できません。", exception.getMessage());
			
			// 검사 2 : 거절된 요청의 상태와 메모가 DB 에 반영 안 됐는지 확인
			Lesson unchanged = lessonMapper.findLessonById(
					lesson.getId(),
					instructor.getId()
				);
			
			assertNotNull(unchanged);
			assertEquals("SCHEDULED", unchanged.getStatus());
			assertEquals("変更前のメモ", unchanged.getStatusMemo());
		}
		
		// 검사 3 : 수업 시작 전에도 취소는 가능
		lessonService.updateLessonStatus(
				lesson.getId(),
				instructor.getId(),
				"CANCELLED",
				"都合によりキャンセル"
			);
		
		Lesson cancelled = lessonMapper.findLessonById(lesson.getId(), instructor.getId());
		
		assertNotNull(cancelled);
		assertEquals("CANCELLED", cancelled.getStatus());
		assertEquals("都合によりキャンセル", cancelled.getStatusMemo());
	}
	
	// 담당 강사의 출석 수업 중 미작성 일보만 조회하고 작성 후 제외
	@Test
	@Transactional
	void findLessonWithoutReportOnlyForOwner() {
		
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
		
		// 각 강사의 학생 준비
		Student student = new Student(
				instructor.getId(),
				"山田太郎",
				null,
				null,
				null
			);
		studentMapper.insertStudent(student);
		
		Student otherStudent = new Student(
				otherInstructor.getId(),
				"鈴木花子",
				null,
				null,
				null
			);
		studentMapper.insertStudent(otherStudent);
		
		// 출석 처리할 수 있도록 어제 날짜 사용
		LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Tokyo")).minusDays(1);
		
		// 내 학생의 출석 수업 : 아직 일보 없음
		Lesson attended = new Lesson(
				student.getId(),
				yesterday.atTime(10, 0)
			);
		lessonMapper.insertLesson(attended);
		
		lessonService.updateLessonStatus(
				attended.getId(),
				instructor.getId(),
				"ATTENDED",
				null
			);
		
		// 다른 강사의 출석 수업 : 내 목록에 나오면 안 됨
		Lesson otherAttended = new Lesson(
				otherStudent.getId(),
				yesterday.atTime(11, 0)
			);
		lessonMapper.insertLesson(otherAttended);
		
		lessonService.updateLessonStatus(
				otherAttended.getId(),
				otherInstructor.getId(),
				"ATTENDED",
				null
			);
		
		// 예정, 취소, 결석 수업은 일보가 없어도 알림 대상이 아님
		String[] statuses = {
			"SCHEDULED",
			"CANCELLED",
			"ABSENT"
		};
		
		for (String status : statuses) {
			Lesson lesson = new Lesson(
					student.getId(), yesterday.atTime(13, 0)
				);
			lessonMapper.insertLesson(lesson);

			lessonService.updateLessonStatus(
					lesson.getId(), instructor.getId(), status, null
				);
		}
		
		// 검사 1 : 내 출석 수업 중 일보가 없는 한 건만 조회
		List<HomeLesson> found = lessonService.findLessonsWithoutReport(instructor.getId());

		assertEquals(1, found.size());

		HomeLesson missing = found.get(0);

		assertEquals(attended.getId(), missing.getId());
		assertEquals(student.getId(), missing.getStudentId());
		assertEquals("山田太郎", missing.getStudentName());
		assertEquals(attended.getLessonAt(), missing.getLessonAt());
		assertEquals("ATTENDED", missing.getStatus());
		assertNull(missing.getLessonReportId());
		
		// 해당 수업의 일보 작성
		lessonReportService.registerLessonReport(
				attended.getId(),
				instructor.getId(),
				"会話を練習しました。",
				null,
				List.of()
			);
		
		// 검사 2 : 일보를 작성하면 미작성 목록에서 제외
		List<HomeLesson> afterSave = lessonService.findLessonsWithoutReport(instructor.getId());

		assertTrue(afterSave.isEmpty());
	}
	
	// 달력의 월 범위, 날짜 중복 제거, 출결 조건, 강사별 조회 제한
	@Test
	@Transactional
	void findCalendarLessonDatesOnlyForOwner() {
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

	    // 각 강사의 학생 준비
	    Student student = new Student(
	            instructor.getId(), "山田太郎", null, null, null
	    );
	    studentMapper.insertStudent(student);

	    Student otherStudent = new Student(
	            otherInstructor.getId(), "鈴木花子", null, null, null
	    );
	    studentMapper.insertStudent(otherStudent);

	    // 연도가 바뀌는 월 경계도 함께 확인
	    YearMonth month = YearMonth.of(2026, 12);

	    // 수업이 없으면 빈 목록
	    assertTrue(lessonService.findLessonDatesForMonth(
	            instructor.getId(), month
	    ).isEmpty());

	    // 첫날 00:00의 예정 수업은 포함
	    Lesson firstDay = new Lesson(
	            student.getId(), month.atDay(1).atStartOfDay()
	    );
	    lessonMapper.insertLesson(firstDay);

	    // 같은 날짜에 수업이 여러 개여도 날짜는 한 번만 반환
	    Lesson sameDay = new Lesson(
	            student.getId(), month.atDay(1).atTime(14, 0)
	    );
	    lessonMapper.insertLesson(sameDay);

	    // 출석 수업도 포함
	    Lesson attended = new Lesson(
	            student.getId(), month.atDay(10).atTime(10, 0)
	    );
	    lessonMapper.insertLesson(attended);
	    lessonMapper.updateLessonStatus(
	            attended.getId(), instructor.getId(), "ATTENDED", null
	    );

	    // 마지막 날의 지각 수업도 포함
	    Lesson late = new Lesson(
	            student.getId(), month.atEndOfMonth().atTime(23, 59)
	    );
	    lessonMapper.insertLesson(late);
	    lessonMapper.updateLessonStatus(
	            late.getId(), instructor.getId(), "LATE", null
	    );

	    // 결석만 있는 날짜는 제외
	    Lesson absent = new Lesson(
	            student.getId(), month.atDay(15).atTime(10, 0)
	    );
	    lessonMapper.insertLesson(absent);
	    lessonMapper.updateLessonStatus(
	            absent.getId(), instructor.getId(), "ABSENT", null
	    );

	    // 취소만 있는 날짜는 제외
	    Lesson cancelled = new Lesson(
	            student.getId(), month.atDay(20).atTime(10, 0)
	    );
	    lessonMapper.insertLesson(cancelled);
	    lessonMapper.updateLessonStatus(
	            cancelled.getId(), instructor.getId(), "CANCELLED", null
	    );

	    // 다른 강사의 수업 날짜는 제외
	    Lesson otherLesson = new Lesson(
	            otherStudent.getId(), month.atDay(25).atTime(10, 0)
	    );
	    lessonMapper.insertLesson(otherLesson);

	    // 지난달 마지막 날은 제외
	    Lesson previousMonth = new Lesson(
	            student.getId(),
	            month.atDay(1).minusDays(1).atTime(23, 59)
	    );
	    lessonMapper.insertLesson(previousMonth);

	    // 다음 해 1월 1일 00:00은 제외
	    Lesson nextMonth = new Lesson(
	            student.getId(),
	            month.plusMonths(1).atDay(1).atStartOfDay()
	    );
	    lessonMapper.insertLesson(nextMonth);

	    // 내 수업 중 예정·출석·지각 날짜만 중복 없이 날짜순으로 조회
	    List<LocalDate> found = lessonService.findLessonDatesForMonth(
	            instructor.getId(), month
	    );

	    assertEquals(
	            List.of(
	                    month.atDay(1),
	                    month.atDay(10),
	                    month.atEndOfMonth()
	            ),
	            found
	    );
	}
	
}