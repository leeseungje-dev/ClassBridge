package com.classbridge.report;

import java.util.List;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.classbridge.lesson.Lesson;
import com.classbridge.lesson.LessonService;
import com.classbridge.observation.LearningObservation;
import com.classbridge.observation.LearningObservationMapper;

/*
 * 수업일보와 학습 관찰 기록의 통합 처리 Service (보존 작업을 하나로 묶기 위해)
 * author : LEE SEUNGJE
 * date : 2026.10.30 ~ 2026.10.30
 */

@Service
public class LessonReportService {
	
	// field
	private final LessonReportMapper lessonReportMapper;
	private final LearningObservationMapper learningObservationMapper;
	private final LessonService lessonService;
	
	// constructor
	public LessonReportService(
			LessonReportMapper lessonReportMapper,
			LearningObservationMapper learningObservationMapper,
			LessonService lessonService
	) {
		this.lessonReportMapper = lessonReportMapper;
		this.learningObservationMapper = learningObservationMapper;
		this.lessonService = lessonService;
	}
	
	// method
	// 일보 content : 필수 검사, 앞뒤 공백 제거, 길이 검사
	private String normalizeContent(String content) {
		
		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("授業内容を入力してください。");
		}
		
		String normalizedContent = content.strip();
		
		if (normalizedContent.codePointCount(
				0, normalizedContent.length()) > 2000
		) {
			throw new IllegalArgumentException("授業内容は2000文字以内で入力してください。");
		}
		
		return normalizedContent;
	}
	
	// 일보 nextCheck : 앞뒤 공백 제거, 내용이 없으면 null 처리, 길이 검사
	private String normalizeNextCheck(String nextCheck) {
		
		if (nextCheck == null) {
			return null;
		}
		
		String normalizedNextCheck = nextCheck.strip();
		
		if (normalizedNextCheck.isEmpty()) {
			return null;
		}
		
		if (normalizedNextCheck.codePointCount(
				0, normalizedNextCheck.length()) > 1000
		) {
			throw new IllegalArgumentException("次回の確認事項は1000文字以内で入力してください。");
		}
		
		return normalizedNextCheck;
	}
	
	// 관찰 기록 category : 필수 검사 및 여섯 개의 선택지 중 한 개 선택했는지 검사
	private String normalizeCategory(String category) {
		
		if (category == null || category.isBlank()) {
			throw new IllegalArgumentException("観察の分類を選択してください。");
		}
		
		String normalizedCategory = category.strip();
		
		if (
				!normalizedCategory.equals("GRAMMAR")
				&& !normalizedCategory.equals("EXPRESSION")
				&& !normalizedCategory.equals("PRONUNCIATION")
				&& !normalizedCategory.equals("SPEAKING")
				&& !normalizedCategory.equals("LISTENING")
				&& !normalizedCategory.equals("OTHER")
		) {
			throw new IllegalArgumentException("観察の分類が正しくありません。");
		}
		
		return normalizedCategory;
	}
	
	// 관찰 기록 item : 필수 검사, 앞뒤 공백 제거, 길이 검사
	private String normalizeItem(String item) {
		
		if (item == null || item.isBlank()) {
			throw new IllegalArgumentException("観察項目を入力してください。");
		}
		
		String normalizedItem = item.strip();
		
		if (normalizedItem.codePointCount(
				0, normalizedItem.length()) > 100
		) {
			throw new IllegalArgumentException("観察項目は100文字以内で入力してください。");
		}
		
		return normalizedItem;
	}
	
	// 관찰 기록 memo : 필수 검사, 앞뒤 공백 제거, 길이 검사
	private String normalizeObservationMemo(String memo) {
		
		if (memo == null || memo.isBlank()) {
			throw new IllegalArgumentException("観察内容を入力してください。");
		}
		
		String normalizedMemo = memo.strip();
		
		if (normalizedMemo.codePointCount(
				0, normalizedMemo.length()) > 1000
		) {
			throw new IllegalArgumentException("観察内容は1000文字以内で入力してください。");
		}
		
		return normalizedMemo;
	}
	
	// 일보와 관찰 기록을 함께 등록
	@Transactional
	public Long registerLessonReport(
			Long lessonId,
			Long instructorId,
			String content,
			String nextCheck,
			List<ObservationInput> observations
	) {
		// 수업 조회 및 담당 강사 확인
		Lesson lesson = lessonService.findLessonById(lessonId, instructorId);
		
		// 수업이 출석상태가 아니면 일보 작성 불가능
		if (!"ATTENDED".equals(lesson.getStatus())) {
			throw new IllegalArgumentException("出席した授業のみ記録を作成できます。");
		}
		
		// 이미 일보가 있으면 중복 등록 거절
		LessonReport existing = lessonReportMapper.findLessonReportByLessonId(lessonId, instructorId);
		
		if (existing != null) {
			throw new IllegalArgumentException("この授業の記録は既に登録されています。");
		}
		
		// 일보 입력값 검사 및 정리
		String normalizedContent = normalizeContent(content);
		String normalizedNextCheck = normalizeNextCheck(nextCheck);
		
		// 검사하고 정리한 관찰 입력 내용을 담을 목록
		List<ObservationInput> normalizedObservations = new ArrayList<>();
		
		// 관찰 항목이 없으면 이 부분은 스킵
		if (observations != null) {
			for (ObservationInput input : observations) {
				
				if (input == null) {
					throw new IllegalArgumentException("観察項目を正しく入力してください。");
				}
				
				String category = normalizeCategory(input.getCategory());
				String item = normalizeItem(input.getItem());
				String memo = normalizeObservationMemo(input.getMemo());
				
				normalizedObservations.add(new ObservationInput(category, item, memo));
			}
		}
		
		// 일보를 먼저 저장해서 id 발급
		LessonReport report = new LessonReport(
				lessonId,
				normalizedContent,
				normalizedNextCheck
			);
		
		int insertedRows = lessonReportMapper.insertLessonReport(report);
		
		if (insertedRows != 1 || report.getId() == null) {
			throw new IllegalStateException("授業記録を保存できませんでした。");
		}
		
		// 발급된 일보 id 로 관찰 기록들을 저장
		for (ObservationInput input : normalizedObservations) {
			
			LearningObservation observation = new LearningObservation(
					report.getId(),
					input.getCategory(),
					input.getItem(),
					input.getMemo()
				);
			
			int observationRows = learningObservationMapper.insertLearningObservation(observation);
			
			if (observationRows != 1) {
				throw new IllegalStateException("観察記録を保存できませんでした。");
			}
		}
		
		return report.getId();
	}
	
	// 수업 id 로 일보 조회 및 담당 강사 확인
	public LessonReport findLessonReportByLessonId(
			Long lessonId,
			Long instructorId
	) {
		// 수업이 존재하고, 해당 강사의 수업인지 확인
		lessonService.findLessonById(lessonId, instructorId);
		
		// 해당 수업의 일보 조회
		LessonReport report = lessonReportMapper.findLessonReportByLessonId(lessonId, instructorId);
		
		// 수업은 있지만 작성된 일보가 없으면 전용 예외
		if (report == null) {
			throw new LessonReportNotFoundException();
		}
		
		return report;
	}
	
	// 수업의 일보에 연결된 관찰 기록 리스트 조회
	public List<LearningObservation> findObservationsByLessonId(
			Long lessonId,
			Long instructorId
	) {
		// 담당 강사의 수업인지 확인하고 일보 조회
		LessonReport report = findLessonReportByLessonId(lessonId, instructorId);
		
		// 확인된 일보 id 로 관찰 기록 리스트 조회
		return learningObservationMapper.findObservationsByLessonReportId(report.getId(), instructorId);
	}
	
	// 일보와 관찰 기록을 함께 수정
	@Transactional
	public void updateLessonReport (
			Long lessonId,
			Long instructorId,
			String content,
			String nextCheck,
			List<ObservationUpdateInput> observations
	) {
		// 담당 강사의 수업인지 확인하고 기존 일보 조회
		LessonReport report = findLessonReportByLessonId(lessonId, instructorId);
		
		// 수정 화면은 관찰 항목이 없어도 빈 목록을 전달
		if(observations == null) {
			throw new IllegalArgumentException("観察項目を正しく入力してください。");
		}
		
		// 일보 입력값 검사 및 정리
		String normalizedContent = normalizeContent(content);
		String normalizedNextCheck = normalizeNextCheck(nextCheck);
		
		// 해당 일보에 저장된 기존 관찰 기록 조회
		List<LearningObservation> existingObservations = learningObservationMapper.findObservationsByLessonReportId(
				report.getId(),
				instructorId
			);
		
		// 기존 관찰 기록의 id 목록 보존
		List<Long> existingIds = new ArrayList<>();
		
		for (LearningObservation existing : existingObservations) {
			existingIds.add(existing.getId());
		}
		
		// 수정 요청에 포함된 기존 id 와 정리된 입력값을 담을 목록
		List<Long> submittedIds = new ArrayList<>();
		
		List<ObservationUpdateInput> normalizedObservations = new ArrayList<>();
		
		// DB 를 변경하기 전 모든 관찰 입력값 검사
		for (ObservationUpdateInput input : observations) {
			
			if (input == null) {
				throw new IllegalArgumentException("観察項目を正しく入力してください。");
			}
			
			Long observationId = input.getId();
			
			// 기존 기록을 수정하는 경우
			if (observationId != null) {
				
				// 이 일보에 속하지 않는 관찰 id 는 거절
				if (!existingIds.contains(observationId)) {
					throw new IllegalArgumentException("観察項目が正しくありません。");
				}
				
				// 같은 관찰 id 를 두 번 보내는 요청도 거절
				if (submittedIds.contains(observationId)) {
					throw new IllegalArgumentException("同じ観察項目が重複しています。");
				}
				
				submittedIds.add(observationId);
			}
			
			String category = normalizeCategory(input.getCategory());
			String item = normalizeItem(input.getItem());
			String memo = normalizeObservationMemo(input.getMemo());
			
			normalizedObservations.add(new ObservationUpdateInput(
					observationId,
					category,
					item,
					memo
				));
		}
		
		// 일보 내용 수정
		report.setContent(normalizedContent);
		report.setNextCheck(normalizedNextCheck);
		
		int updatedRows = lessonReportMapper.updateLessonReport(report, instructorId);
		
		if (updatedRows != 1) {
			throw new LessonReportNotFoundException();
		}
		
		// 기존 관찰 기록의 삭제 또는 수정
		for (LearningObservation existing : existingObservations) {
			
			// 최종 입력 목록에서 빠진 기존 기록은 삭제
			if (!submittedIds.contains(existing.getId())) {
				int deletedRows = learningObservationMapper.deleteLearningObservation(
						existing.getId(),
						report.getId(),
						instructorId
					);
				
				if (deletedRows != 1) {
					throw new IllegalStateException("観察記録を削除できませんでした。");
				}
				
				continue;
			}
			
			// 기존 기록과 id 가 일치하는 수정 입력값 찾기
			for (ObservationUpdateInput input : normalizedObservations) {
				
				if (existing.getId().equals(input.getId())) {
					
					// 내용이 같으면 불필요한 update 를 하지 않음
	                boolean changed = !existing.getCategory().equals(input.getCategory())
	                		|| !existing.getItem().equals(input.getItem())
	                		|| !existing.getMemo().equals(input.getMemo());

	                if (changed) {
	                	existing.setItem(input.getItem());
	                	existing.setMemo(input.getMemo());

	                	int observationRows = learningObservationMapper.updateLearningObservation(
	                			existing,
	                			report.getId(),
	                			instructorId
	                		);

	                	if (observationRows != 1) {
	                		throw new IllegalStateException("観察記録を更新できませんでした。");
	                    }
	                }
	                
	                break;
				}
			}
		}
		
		// id 가 없는 입력값은 새로운 기록으로 등록
		for (ObservationUpdateInput input : normalizedObservations) {
			
			if (input.getId() == null) {
				LearningObservation observation = new LearningObservation(
						report.getId(),
						input.getCategory(),
						input.getItem(),
						input.getMemo()
					);
				
				int insertedRows = learningObservationMapper.insertLearningObservation(observation);
				
				if (insertedRows != 1) {
					throw new IllegalStateException("観察記録を保存できませんでした。");
				}
			}
		}
	}
	
}