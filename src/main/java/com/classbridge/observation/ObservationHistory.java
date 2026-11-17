package com.classbridge.observation;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 수업 준비 화면에 표시할 누적 관찰 기록 DTO
 * author : LEE SEUNGJE
 * date : 2026.11.16 ~ 2026.11.16
 */

@Getter
@NoArgsConstructor
public class ObservationHistory {
	
	// 관찰기록 정보
	private Long id;
	private Long lessonId;
	// 실제 수업 일시 (관찰 기록 입력한 날짜가 아님)
	private LocalDateTime lessonAt;
	private String category;
	private String item;
	private String memo;
	
}