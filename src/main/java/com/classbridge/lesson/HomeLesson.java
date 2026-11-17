package com.classbridge.lesson;

import java.time.LocalDateTime;

import lombok.Getter;
// 매개 변수가 없는 생성자를 만들어주는 lombok 기능 (mybatis 가 빈 객체를 만든 뒤 db 조회 결과를 각 필드에 채울 때 사용)
import lombok.NoArgsConstructor;

/*
 * 홈 화면에 표시할 수업 정보 (DTO : 정보 전달용 클래스, Data Transfer Object)
 * 수업 정보와 학생 이름, 일보의 존재 여부를 함께 담음
 * author : LEE SEUNGJE
 * date : 2026.11.15 ~ 2026.11.15
 */

@Getter
@NoArgsConstructor
public class HomeLesson {

	private Long id;
	private Long studentId;
	private String studentName;
	private LocalDateTime lessonAt;
	private String status;
	// 일보가 없으면 null, 있으면 해당 일보 id
	private Long lessonReportId;
	
}