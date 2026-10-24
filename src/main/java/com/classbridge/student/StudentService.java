package com.classbridge.student;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;

/*
 * Service : 화면에서 Controller 로 받은 학생 관리 요청 처리
 * 1. 필수값인 학생 이름이 넣어져 있는지 확인
 * 2. 선택 입력값과 날짜 처리
 * 3. Mapper 를 호출해서 CRUD 기능 연결
 * 4. 학생이 없거나, 접근할 수 없을 때 처리
 * author : LEE SEUNGJE
 * date : 2026.10.20 ~ 2026.10.22
 */

@Service
public class StudentService {
	
	// field
	private final StudentMapper studentMapper;
	
	// constructor
	public StudentService(StudentMapper studentMapper) {
		this.studentMapper = studentMapper;
	}
	
	// method (Mapper 메소드와 Service 메소드의 연결)
	// 학생 한 명을 조회
	public Student findStudentById(Long id, Long instructorId) {
		Student student = studentMapper.findStudentById(id, instructorId);
		
		if (student == null) {
			throw new StudentNotFoundException();
		}
		
		return student;
	}
	
	// 해당 강사의 학생 목록 조회
	public List<Student> findStudentsByInstructorId(Long instructorId) {
		return studentMapper.findStudentsByInstructorId(instructorId);
	}
	
	// 검사 및 정리 method 1 : 필수 항목(이름 필수 검사, 공백 정리, 길이 검사)
	private String normalizeStudentName(String name) {
		
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("学生名を入力してください。");
		}
		
		String normalizedName = name.strip();
		
		if (normalizedName.codePointCount(0, normalizedName.length()) > 100) {
			throw new IllegalArgumentException("学生名は100文字以内で入力してください。");
		}
		
		return normalizedName;
	}
	
	// 검사 및 정리 method 2 : 선택 항목 - currentLevel (공백 정리, 빈 값 null 처리, 지정값 확인)
	private String normalizeCurrentLevel(String currentLevel) {
		
		if (currentLevel == null) {
			return null;
		}
		
		String normalizedLevel = currentLevel.strip();
		
		if (normalizedLevel.isEmpty()) {
			return null;
		}
		
		if (
				!normalizedLevel.equals("入門")
				&& !normalizedLevel.equals("初級")
				&& !normalizedLevel.equals("中級")
				&& !normalizedLevel.equals("上級")
			) {
			throw new IllegalArgumentException("学習レベルは入門・初級・中級・上級から選択してください。");
		}
		
		return normalizedLevel;
	}
	
	// 검사 및 정리 method 3 : 선택 항목 - instructorMemo (공백 정리, 빈 값 null 처리, 길이 검사)
	private String normalizeInstructorMemo(String instructorMemo) {
		
		if (instructorMemo == null) {
			return null;
		}
		
		String normalizedMemo = instructorMemo.strip();
		
		if (normalizedMemo.isBlank()) {
			return null;
		}
		
		if (normalizedMemo.codePointCount(0, normalizedMemo.length()) > 1000) {
			throw new IllegalArgumentException("メモは1000文字以内で入力してください。");
		}
		
		return normalizedMemo;
	}
	
	// 학생 등록 (등록한 학생의 id 반환값을 보고 방금 만든 학생이 누구인지 식별하기 위한 id 리턴)
	// ㄴ> 추후 조회나 학생 상세 화면으로 이동하기 위한 id 리턴
	@Transactional
	public Long registerStudent(Long instructorId, String name, LocalDate learningStartDate, String currentLevel, String instructorMemo) {
		
		// 필수 입력값 검사 (필수 입력, 공백 정리, 길이 검사)
		String normalizedName = normalizeStudentName(name);
		
		// 선택 입력값의 공백 제거
		String normalizedLevel = normalizeCurrentLevel(currentLevel);
		
		String normalizedMemo = normalizeInstructorMemo(instructorMemo);
		
		// 객체에 정보를 담아 DB 저장
		Student student = new Student(
				instructorId,
				normalizedName,
				learningStartDate,
				normalizedLevel,
				normalizedMemo
			);
		studentMapper.insertStudent(student);
		
		return student.getId();
	}
	
	// 학생 정보 수정
	@Transactional
	public void updateStudent(Long id, Long instructorId, String name, LocalDate learningStartDate, String cureentLevel, String instructorMemo) {
		
		// 해당 검사의 학생인지 확인
		// 없거나 다른 강사의 학생이면 이 메서드에서 예외 발생
		Student student = findStudentById(id, instructorId);
		
		// 등록과 같은 규칙으로 입력값 검사 및 정리
		String normalizedName = normalizeStudentName(name);
		String normalizedLevel = normalizeCurrentLevel(cureentLevel);
		String normalizedMemo = normalizeInstructorMemo(instructorMemo);
		
		// 조회한 객체의 값 변경
		student.setName(normalizedName);
		student.setLearningStartDate(learningStartDate);
		student.setCurrentLevel(normalizedLevel);
		student.setInstructorMemo(normalizedMemo);
		
		// DB 수정
		int updatedRows = studentMapper.updateStudent(student, instructorId);
		
		// 조회 이후 학생이 삭제되는 등 상황 확인
		if (updatedRows != 1) {
			throw new StudentNotFoundException();
		}
	}
	
	// 학생 삭제
	@Transactional
	public void deleteStudent(Long id, Long instructorId) {
		
		// 학생 id 와 요청 강사 id 가 모두 일치하는 학생만 삭제
		int deletedRows = studentMapper.deleteStudent(id, instructorId);
		
		// 학생이 없거나 다른 강사의 학생이라면 삭제되지 않음
		if (deletedRows != 1) {
			throw new StudentNotFoundException();
		}
	}
	
}