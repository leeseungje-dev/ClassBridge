package com.classbridge.lesson;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;


/*
 * author : LEE SEUNGJE
 * date : 2026.10.25 ~ 2026.11.15
 */

@Mapper
public abstract interface LessonMapper {
	
	// method
	// 수업 등록
	public abstract int insertLesson(Lesson lesson);
	
	// 수업 id 로 조회하고, 담당 강사 확인
	public abstract Lesson findLessonById(
			@Param("id") Long id,
			@Param("instructorId") Long instructorId
		);
	
	// 해당 강사가 소유한 학생의 수업 목록 조회
	public abstract List<Lesson> findLessonsByStudentId(
			@Param("studentId") Long studentId,
			@Param("instructorId") Long instructorId
		);
	
	// 담당 강사의 수업 일시 변경 (변경할 수업 id / 권한 강사 id / 변경 날짜)
	public abstract int updateLessonDateTime(
			@Param("id") Long id,
			@Param("instructorId") Long instructorId,
			@Param("lessonAt") LocalDateTime lessonAt
		);
	
	// 수업 상태와 상태 메모 변경 (변경할 수업 id / 권한 강사 id / 변경 상태 / 사유(취소, 결석 등))
	public abstract int updateLessonStatus(
			@Param("id") Long id,
			@Param("instructorId") Long instructorId,
			@Param("status") String status,
			@Param("statusMemo") String statusMemo
		);
	
	// home 화면에 표시할 수업 조회
	// 오늘부터 예정 수업이 있는 가장 가까운 날짜 조회
	public abstract LocalDate findHomeLessonDate(
			@Param("instructorId") Long instructorId,
			@Param("todayStart") LocalDateTime todayStart
		);
	
	// 선택한 날짜의 모든 수업 조회 및 담당 강사 확인
	public abstract List<HomeLesson> findHomeLessonsByDate(
			@Param("instructorId") Long instructorId,
			// 선택한 날짜의 00:00
			@Param("dayStart") LocalDateTime dayStart,
			// 선택한 날짜 다음 날의 00:00
			@Param("nextDayStart") LocalDateTime nextDayStart
		);
	
	// 출석했지만 일보가 없는 수업 조회 및 담당 강사 확인
	public abstract List<HomeLesson> findLessonsWithoutReport(
			@Param("instructorId") Long instructorId
		);
	
	// 선택한 수업보다 앞선 같은 학생의 가장 최근 출석 수업 조회
	public abstract Lesson findPreviousAttendedLesson(
			@Param("lessonId") Long lessonId,
			@Param("instructorId") Long instructorId
		);
	
	// 달력에 표시할 해당 월의 수업 날짜 조회
	public abstract List<LocalDate> findLessonDatesForMonth(
			@Param("instructorId") Long instructorId,
			@Param("monthStart") LocalDateTime monthStart,
			@Param("nextMonthStart") LocalDateTime nextMonthStart
		);
	
}