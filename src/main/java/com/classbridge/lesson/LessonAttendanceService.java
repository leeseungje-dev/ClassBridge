package com.classbridge.lesson;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.classbridge.report.LessonReport;
import com.classbridge.report.LessonReportMapper;
import com.classbridge.report.LessonReportService;
import com.classbridge.report.ObservationInput;
import com.classbridge.report.ObservationUpdateInput;

/*
 * 출결 상태와 수업일보의 통합 저장 처리
 * transaction 으로 일보 저장이 실패하면 출결 변경도 취소
 * author : LEE SEUNGJE
 * date : 2026.11.16 ~ 2026.11.16
 */

@Service
public class LessonAttendanceService {
	
	// field
	private final LessonService lessonService;
	private final LessonReportService lessonReportService;
	private final LessonReportMapper lessonReportMapper;
	
	// constructor
	public LessonAttendanceService(
			LessonService lessonService,
			LessonReportService lessonReportService,
			LessonReportMapper lessonReportMapper
	) {
		this.lessonService = lessonService;
		this.lessonReportService = lessonReportService;
		this.lessonReportMapper = lessonReportMapper;
	}
	
	// method
	//출결 상태와 일보, 관찰 기록을 함께 저장
	@Transactional
	public void saveAttendance(
			Long lessonId,
			Long instructorId,
			String status,
			String statusMemo,
			String content,
			String nextCheck,
			List<ObservationUpdateInput> observations
	) {
		// 상태 메모는 지각, 결석, 취소에만 저장
		String memoToSave = null;
		
		if (
				"LATE".equals(status)
				|| "ABSENT".equals(status)
				|| "CANCELLED".equals(status)
		) {
			memoToSave = statusMemo;
		}
		
		// 접근 권한, 상태, 시각, 기존 일보 제한을 검사한 뒤 출결 변경
		lessonService.updateLessonStatus(
				lessonId,
				instructorId,
				status,
				memoToSave
		);
		
		// 예정, 결석, 취소는 일보 입력을 저장하지 않음
		if (!"ATTENDED".equals(status) && !"LATE".equals(status)) {
			return;
		}
		
		// 관찰 항목이 없어도 빈 목록을 전달 받아야 함
		if (observations == null) {
			throw new IllegalArgumentException("観察項目を正しく入力してください。");
		}
		
		// 기존 일보가 있는지 확인
		LessonReport existing = lessonReportMapper.findLessonReportByLessonId(lessonId, instructorId);
		
		if (existing == null) {
			// 새 일보에는 기존 관찰 기록 id 가 없어야 함
			List<ObservationInput> newObservations = new ArrayList<>();
			
			for (ObservationUpdateInput input : observations) {
				if (input == null || input.getId() != null) {
					throw new IllegalArgumentException("観察項目が正しくありません。");
				}
				
				// 등록용 입력 객체로 변환
				newObservations.add(new ObservationInput(
						input.getCategory(),
						input.getItem(),
						input.getMemo()
				));
			}
			
			// 새 일보와 관찰 기록 등록
			lessonReportService.registerLessonReport(
					lessonId,
					instructorId,
					content,
					nextCheck,
					newObservations
			);
			
		} else {
			// 기존 일보와 관찰 기록 수정
			lessonReportService.updateLessonReport(
					lessonId,
					instructorId,
					content,
					nextCheck,
					observations
			);
		}
	}
	
}