package com.classbridge.report;

import lombok.Getter;

/*
 * 일보 수정 시 전달할 관찰 항목의 입력값
 * author : LEE SEUNGJE
 * date : 2026.11.01
 */

@Getter
public class ObservationUpdateInput {
	
	// field
	private final Long id;
	private final String category;
	private final String item;
	private final String memo;
	
	// constructor
	public ObservationUpdateInput(
			Long id,
			String category,
			String item,
			String memo
	) {
		this.id = id;
		this.category = category;
		this.item = item;
		this.memo = memo;
	}
	
}