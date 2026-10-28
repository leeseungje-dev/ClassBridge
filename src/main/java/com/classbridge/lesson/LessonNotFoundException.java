package com.classbridge.lesson;

/*
 * 수업이 없거나 해당 강사가 접근할 수 없을 때 발생
 * author : LEE SEUNGJE
 * date : 2026.10.27 ~ 2026.10.27
 */

public class LessonNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public LessonNotFoundException() {
		super("授業が見つかりません。");
	}
	
}