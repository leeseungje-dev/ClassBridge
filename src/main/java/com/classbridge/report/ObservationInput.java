package com.classbridge.report;

import lombok.Getter;

/*
 * 관찰 기록 항목의 입력 내용 묶음 (카테고리, 항목, 내용)
 * (일보 & 관찰 기록 통합 저장 시 전달 목적)
 * 새 일보를 저장하기 전에는 아직 일보 id 가 없기 때문에, 입력 항목은 사전에 저장
 * author : LEE SEUNGJE
 * date : 2026.10.30 ~ 2026.10.30
 */

@Getter
public class ObservationInput {
	
	// field
	private final String category;
	private final String item;
	private final String memo;
	
	// constructor
	public ObservationInput(
			String category,
			String item,
			String memo
	) {
		this.category = category;
		this.item = item;
		this.memo = memo;
	}
	
}