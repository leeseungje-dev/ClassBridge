package com.classbridge.lesson;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.time.LocalDateTime;

/*
 * author : LEE SEUNGJE
 * date : 2026.10.25 ~ 2026.10.26
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
	
}