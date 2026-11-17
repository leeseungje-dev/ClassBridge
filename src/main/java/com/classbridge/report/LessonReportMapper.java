package com.classbridge.report;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 수업일보 DB 등록 및 조회
 * author : LEE SEUNGJE
 * date : 2026.10.29 ~ 2026.10.30
 */

@Mapper
public abstract interface LessonReportMapper {
	
	// 수업일보 등록
	public abstract int insertLessonReport(LessonReport lessonReport);
	
	// 수업 id 로 일보 조회 및 소유 강사 확인
	public abstract LessonReport findLessonReportByLessonId(
			@Param("lessonId") Long lessonId,
			@Param("instructorId") Long instructorId
	);
	
	// 수업일보 수정 및 소유 강사 확인
	public abstract int updateLessonReport(
			@Param("report") LessonReport report,
			@Param("instructorId") Long instructorId
		);
	
	// 수업 준비 상세 화면에서 사용할 선택 수업보다 앞선 같은 학생의 가장 최근 일보 조회
	public abstract LessonReport findPreviousLessonReport(
			@Param("lessonId") Long lessonId,
			@Param("instructorId") Long instructorId
		);
}