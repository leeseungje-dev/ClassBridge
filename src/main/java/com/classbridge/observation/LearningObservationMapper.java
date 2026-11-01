package com.classbridge.observation;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 학습 관찰 기록 DB 등록 및 조회
 * author : LEE SEUNGJE
 * date : 2026.10.29 ~ 2026.10.30
 */

@Mapper
public abstract interface LearningObservationMapper {
	
	// 관찰 기록 한 건 DB 등록
	int insertLearningObservation(LearningObservation Observation);
	
	// 수업일보에 속한 관찰 기록 목록 조회 및 담당 강사 확인
	List<LearningObservation> findObservationsByLessonReportId(
			@Param("lessonReportId") Long lessonReportId,
			@Param("instructorId") Long instructorId
		);
	
	// 관찰 기록 수정 및 해당 일보, 담당 강사 확인
	public abstract int updateLearningObservation(
			@Param("observation") LearningObservation observation,
			@Param("lessonReportId") Long lessonReportId,
			@Param("instructorId") Long instructorId
		);
	
	// 관찰 기록 삭제 및 해당 일보, 담당 강사 확인
	public abstract int deleteLearningObservation(
			@Param("id") Long id,
			@Param("lessonReportId") Long lessonReportId,
			@Param("instructorId") Long instructorId
		);
}