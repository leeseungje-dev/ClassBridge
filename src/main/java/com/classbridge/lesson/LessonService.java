package com.classbridge.lesson;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.classbridge.student.StudentService;

/*
 * 수업 관리 업무처리
 * 로그인한 강사의 학생인지 확인하고 Mapper 호출
 * author : LEE SEUNGJE
 * date : 2026.10.27 ~ 2026.10.28
 */

@Service
public class LessonService {
	
	// field
	private final LessonMapper lessonMapper;
	private final StudentService studentService;
	
	// constructor
	public LessonService(LessonMapper lessonMapper, StudentService studentService) {
		this.lessonMapper = lessonMapper;
		this.studentService = studentService;
	}
	
	// method
	// 수업 한 건을 조회 (수업이 없거나 다른 강사의 수업이면 전용 예외)
	public Lesson findLessonById(Long id, Long instructorId) {
		
		Lesson lesson = lessonMapper.findLessonById(id, instructorId);
		
		if (lesson == null) {
			throw new LessonNotFoundException();
		}
		
		return lesson;
	}
	
	// 담당 학생의 수업 목록 조회
	public List<Lesson> findLessonByStudentId(Long studentId, Long instructorId) {
		// 학생이 존재하고, 로그인한 강사의 학생인지 확인
		studentService.findStudentById(studentId, instructorId);
		
		// 확인이 끝나면 수업 목록 조회
		return lessonMapper.findLessonsByStudentId(studentId, instructorId);
	}
	
	// 수업 등록
	@Transactional
	public Long registerLesson(Long studentId, Long instructorId, LocalDateTime lessonAt) {
		// 학생이 존재하고, 해당 강사의 학생인지 확인
		studentService.findStudentById(studentId, instructorId);
		
		// 수업 일시는 필수로 입력
		if (lessonAt == null) {
			throw new IllegalArgumentException("授業日時を入力してください。");
		}
		
		// 수업 정보를 객체에 담기
		Lesson lesson = new Lesson(studentId, lessonAt);
		
		// DB 에 저장
		lessonMapper.insertLesson(lesson);
		
		// 발급된 수업 id 반환
		return lesson.getId();
	}
	
	// 수업 일시 수정
	@Transactional
	public void updateLessonDateTime(Long id, Long instructorId, LocalDateTime lessonAt) {
		// 수업이 존재하고, 담당 강사의 수업인지 확인
		findLessonById(id, instructorId);
		
		// 수업 일시는 필수
		if (lessonAt == null) {
			throw new IllegalArgumentException("授業日時を入力してください。");
		}
		
		// DB 의 수업 일시 수정
		int updatedRows = lessonMapper.updateLessonDateTime(id, instructorId, lessonAt);
		
		// 조회 이후 수업이 삭제되는 등의 상황 확인
		if (updatedRows != 1) {
			throw new LessonNotFoundException();
		}
	}
	
	// 수업 status 와 statusMemo 변경
	@Transactional
	public void updateLessonStatus (
			Long id, 
			Long instructorId,
			String status,
			String statusMemo
	) {
		// 수업이 존재하고, 담당 강사의 수업인지 확인
		findLessonById(id, instructorId);
		
		// 상태는 필수
		if (status == null || status.isBlank()) {
			throw new IllegalArgumentException("授業の状態を選択してください。");
		}
		
		// 정해진 네 가지 상태만 사용
		if (
				!status.equals("SCHEDULED")
				&& !status.equals("ATTENDED")
				&& !status.equals("CANCELLED")
				&& !status.equals("ABSENT")
		) {
			throw new IllegalArgumentException("授業の状態が正しくありません。");
		}
		
		// 선택 입력인 메모의 앞뒤 공백 확인
		String normalizedMemo = null;
		
		if (statusMemo != null) {
			normalizedMemo = statusMemo.strip();
			
			if (normalizedMemo.isEmpty()) {
				normalizedMemo = null;
			}
		}
		
		// DB 열의 최대 길이(1000자 이내) 확인
		if (
				normalizedMemo != null
				&& normalizedMemo.codePointCount(0, normalizedMemo.length()) > 1000
		) {
			throw new IllegalArgumentException("状態メモは1000文字以内で入力してください。");
		}
		
		// DB 의 상태와 메모 변경
		int updatedRows = lessonMapper.updateLessonStatus(
				id, 
				instructorId, 
				status, 
				normalizedMemo
			);
		
		// 실제로 한 건이 수정됐는지 확인
		if (updatedRows != 1) {
			throw new LessonNotFoundException();
		}
	}
	
}