package com.classbridge.student;

/*
 * author : LEE SEUNGJE
 * date : 2026.10.24 ~ 2026.10.24
 */

public class StudentNotFoundException extends RuntimeException {
	// field
	private static final long serialVersionUID = 1L;
	
	// method
	public StudentNotFoundException() {
		super("学生が見つかりません。");
	}
}