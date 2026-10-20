package com.classbridge.student;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/*
 * author : LEE SEUNGJE
 * date : 2026.10.18 ~ 2026.10.19
 */

@Getter
public class Student {
	
	// field
	private Long id;
	private Long instructorId;
	@Setter
	private String name;
	@Setter
	private LocalDate learningStartDate;
	@Setter
	private String currentLevel;
	@Setter
	private String instructorMemo;
	private LocalDateTime createdAt;
	
	// constructor
	protected Student() {
	}
	
	// 새 학생 등록용 생성자
	public Student(Long instructorId, String name, LocalDate learningStartDate, String currentLevel, String instructorMemo) {
		this.instructorId = instructorId;
		this.name = name;
		this.learningStartDate = learningStartDate;
		this.currentLevel = currentLevel;
		this.instructorMemo = instructorMemo;
	}
	
}