package com.classbridge.report;

/*
 * 수업일보가 없거나 접근할 수 없을 때 사용하는 예외
 * author : LEE SEUNGJE
 * date : 2026.10.31
 */

public class LessonReportNotFoundException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	public LessonReportNotFoundException() {
		super("授業記録が見つかりません。");
	}
	
}