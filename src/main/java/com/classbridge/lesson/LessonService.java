package com.classbridge.lesson;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.YearMonth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.classbridge.student.StudentService;
import com.classbridge.report.LessonReport;
import com.classbridge.report.LessonReportMapper;

/*
 * 수업 관리 업무처리
 * 로그인한 강사의 학생인지 확인하고 Mapper 호출
 * author : LEE SEUNGJE
 * date : 2026.10.27 ~ 2026.11.15
 */

@Service
public class LessonService {
	
	// field
	private final LessonMapper lessonMapper;
	private final StudentService studentService;
	private final LessonReportMapper lessonReportMapper;
	
	// constructor
	public LessonService(LessonMapper lessonMapper, StudentService studentService, LessonReportMapper lessonReportMapper) {
		this.lessonMapper = lessonMapper;
		this.studentService = studentService;
		this.lessonReportMapper = lessonReportMapper;
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
		// 수업 조회 및 담당 강사 확인
		Lesson lesson = findLessonById(id, instructorId);
		
		// 상태는 필수
		if (status == null || status.isBlank()) {
			throw new IllegalArgumentException("授業の状態を選択してください。");
		}
		
		// 정해진 다섯 가지 상태만 사용
		if (
				!status.equals("SCHEDULED")
				&& !status.equals("ATTENDED")
				&& !status.equals("LATE")
				&& !status.equals("CANCELLED")
				&& !status.equals("ABSENT")
		) {
			throw new IllegalArgumentException("授業の状態が正しくありません。");
		}
		
		// 일보가 있는 수업은 출석, 지각 상태만 허용
		if (!"ATTENDED".equals(status) && !"LATE".equals(status)) {
			LessonReport report = lessonReportMapper.findLessonReportByLessonId(id, instructorId);
			
			if(report != null) {
				throw new IllegalArgumentException("授業記録があるため、出席・遅刻以外の状態には変更できません。");
			}
		}
		
		// 출석, 지각, 결석 처리는 수업 시작 시각부터 허용
		if ("ATTENDED".equals(status) || "LATE".equals(status) || "ABSENT".equals(status)) {
			
			// 일본 기준 현재 날짜와 시각
			LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Tokyo"));
			
			// 현재 시각이 수업 시작 전이면 변경 거절
			if (now.isBefore(lesson.getLessonAt())) {
				throw new IllegalArgumentException("授業開始前は、出席・遅刻・欠席に変更できません。");
			}
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
	
	// home 에 표시할 가장 가까운 예정 수업일 조회
	public LocalDate findHomeLessonDate(Long instructorId) {
		
		// 배포 서버의 시간대와 관계없이 일본 기준 오늘 날짜 사용
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));
		
		// 오늘 00:00 부터 예정 수업일 검색
		LocalDateTime todayStart = today.atStartOfDay();
		
		return lessonMapper.findHomeLessonDate(instructorId, todayStart);
	}
	
	// 선택한 날짜의 모든 수업 조회
	public List<HomeLesson> findHomeLessonsByDate(
			Long instructorId,
			LocalDate lessonDate
	) {
		
		// 예정 수업일이 없으면 빈 목록 반환
		if (lessonDate == null) {
			return List.of();
		}
		
		// 선택한 날짜의 00:00
		LocalDateTime dayStart = lessonDate.atStartOfDay();
		
		// 선택한 날짜의 다음 날 00:00
		LocalDateTime nextDayStart = lessonDate.plusDays(1).atStartOfDay();
		
		return lessonMapper.findHomeLessonsByDate(instructorId, dayStart, nextDayStart);
	}
	
	// 출석했지만 일보가 없는 담당 강사의 수업 조회
	public List<HomeLesson> findLessonsWithoutReport(Long instructorId) {
		return lessonMapper.findLessonsWithoutReport(instructorId);
	}
	
	// 선택한 수업보다 앞선 같은 학생의 가장 최근 출석 수업 조회
	public Lesson findPreviousAttendedLesson(
			Long lessonId,
			Long instructorId
	) {
		// 선택한 수업이 존재하고, 담당 강사의 수업인지 확인
		findLessonById(lessonId, instructorId);
		
		// 이전 출석 수업이 없으면 null 반환
		return lessonMapper.findPreviousAttendedLesson(lessonId, instructorId);
	}
	
	// 달력에 점으로 표시할 해당 월의 수업 날짜 조회
	public List<LocalDate> findLessonDatesForMonth(
			Long instructorId,
			YearMonth month
	) {
		// 선택한 달의 1일 00:00
		LocalDateTime monthStart = month.atDay(1).atStartOfDay();
		
		// 다음 달의 1일 00:00
		LocalDateTime nextMonthStart = month.plusMonths(1).atDay(1).atStartOfDay();
		
		return lessonMapper.findLessonDatesForMonth(
				instructorId,
				monthStart,
				nextMonthStart
			);
	}
	
}