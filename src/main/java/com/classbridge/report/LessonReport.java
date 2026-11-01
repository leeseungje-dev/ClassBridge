package com.classbridge.report;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/*
 * 수업일보 데이터를 담는 클래스
 * author : LEE SEUNGJE
 * date : 2026.10.29 ~ 2026.10.31
 */

@Getter
public class LessonReport {
	
	// field
	private Long id;
	private Long lessonId;
	
	@Setter
	private String content;
	
	@Setter
	private String nextCheck;
	
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	
	// constructor
	// MyBatis 가 DB 조회 결과를 객체에 담을 때 사용
	protected LessonReport() {
	}
	
	// 새 수업일보를 등록할 때 사용
	public LessonReport(Long lessonId, String content, String nextCheck) {
		this.lessonId = lessonId;
		this.content = content;
		this.nextCheck = nextCheck;
	}
	
}