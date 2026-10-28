package com.classbridge.lesson;

import lombok.Getter;

import java.time.LocalDateTime;

/*
 * author : LEE SEUNGJE
 * date : 2026.10.25 ~ 2026.10.25
 */

@Getter
public class Lesson {
	
	// field
	private Long id;
	private Long studentId;
	private LocalDateTime lessonAt;
	private String status;
	private String statusMemo;
	private LocalDateTime createdAt;
	
	// constructor
	// 조회 결과를 객체로 만들 때 사용
	protected Lesson() {
	}
	
	// 새 수업을 등록할 때 사용
	public Lesson(Long studentId, LocalDateTime lessonAt) {
		this.studentId = studentId;
		this.lessonAt = lessonAt;
	}
	
}