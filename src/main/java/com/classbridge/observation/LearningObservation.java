package com.classbridge.observation;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/*
 * (수업일보에 속한) 학습 관찰 기록을 담는 클래스
 * author : LEE SEUNGJE
 * date : 2026.10.29 ~ 2026.10.29
 */

@Getter
public class LearningObservation {

	// field
	private Long id;
	private Long lessonReportId;
	
	@Setter
	private String category;

	@Setter
	private String item;

	@Setter
	private String memo;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	
	// constructor
	// MyBatis가 DB 조회 결과를 객체에 담을 때 사용
	protected LearningObservation() {
	}
	
	// 새 관찰 기록을 등록할 때 사용
	public LearningObservation(
			Long lessonReportId,
			String category,
			String item,
			String memo
	) {
		this.lessonReportId = lessonReportId;
		this.category = category;
		this.item = item;
		this.memo = memo;
	}
	
}