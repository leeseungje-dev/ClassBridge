package com.classbridge;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.ArrayList;

import com.classbridge.lesson.HomeLesson;
import com.classbridge.lesson.LessonService;
import com.classbridge.user.User;
import com.classbridge.user.UserService;

/*
 * home 화면의 요청 처리
 * author : LEE SEUNGJE
 * date : 2026.10.13 ~ 2026.11.15
 */

// web 요청을 처리하고 화면을 연결하는 클래스로 등록
@Controller
public class HomeController {
	
	// field
	private final LessonService lessonService;
	private final UserService userService;
	
	// constructor
	public HomeController(
			LessonService lessonService,
			UserService userService
	) {
		this.lessonService = lessonService;
		this.userService = userService;
	}
	

	// 브라우저에서 "/" 주소로 GET 요청을 보내면 이 메서드(home) 실행
	@GetMapping("/")
	// Model : (Spring 이 제공) 화면에 전달할 데이터를 담는 인터페이스
	public String home(Model model, Principal principal) {
		// 화면에서 ${message}라는 이름으로 사용할 값 등록
		model.addAttribute("message", "ClassBridgeへようこそ。");
		
		// Principal : 현재 요청을 보낸 로그인 사용자 객체
		// 로그인한 사용자에게만 수업 일정 표시
		if (principal != null) {
			
			// 로그인 이메일로 강사 정보 조회
			User instructor = userService.findUserByEmail(principal.getName());
			
			// 예정 수업이 있는 가장 가까운 날짜 조회
			LocalDate lessonDate = lessonService.findHomeLessonDate(instructor.getId());
			
			// 표시할 수업일이 있으면 해당 월, 없으면 일본 기준 이번 달
			YearMonth calendarMonth;

			if (lessonDate != null) {
			    calendarMonth = YearMonth.from(lessonDate);
			} else {
			    calendarMonth = YearMonth.now(ZoneId.of("Asia/Tokyo"));
			}

			// 달력에 필요한 데이터를 화면에 전달
			addCalendarAttributes(model, instructor.getId(), calendarMonth);
			
			// 선택한 날짜의 모든 수업 조회
			List<HomeLesson> homeLessons = lessonService.findHomeLessonsByDate(instructor.getId(), lessonDate);
			
			// 출석했지만 일보가 없는 수업 조회
			List<HomeLesson> lessonsWithoutReport = lessonService.findLessonsWithoutReport(instructor.getId());
			
			// home 화면에 전달
			model.addAttribute("loginEmail", principal.getName());
			model.addAttribute("lessonDate", lessonDate);
			model.addAttribute("homeLessons", homeLessons);
			model.addAttribute("lessonsWithoutReport", lessonsWithoutReport);
		}
		
		return "home";	// 사용할 화면 이름 (home.html)
	}
	
	// 이전 달, 다음 달을 누르면 선택한 월의 달력 영역만 반환
	@GetMapping("/home/calendar")
	public String calendar(
			@RequestParam("month")
			@DateTimeFormat(pattern = "yyyy-MM")
			YearMonth month,
			Model model,
			Principal principal
	) {
		// 로그인 한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
		
		// 선택한 월의 달력 데이터 준비
		addCalendarAttributes(model, instructor.getId(), month);
		
		// home html 중 달력 영역만 반환
		return "home :: lessonCalendar";
	}
	
	// home 에서 선택한 날짜의 수업 목록만 반환
	@GetMapping("/home/lessons")
	public String homeLessons(
			@RequestParam("date")
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate date,
			Principal principal,
			Model model
	) {
		// 로그인한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
		
		// 선택한 날짜의 해당 강사 수업만 조회
		List<HomeLesson> lessons = lessonService.findHomeLessonsByDate(
				instructor.getId(),
				date
			);
		
		// 수업 목록 html 에 사용할 데이터 전달
		model.addAttribute("lessonDate", date);
		model.addAttribute("homeLessons", lessons);
		
		// home html 중 수업 목록 영역만 반환
		return "home :: lessonSchedule";
	}
	
	// 달력 화면에 필요한 데이터를 준비
	private void addCalendarAttributes(
			Model model,
			Long instructorId,
			YearMonth calendarMonth
	) {
		// 일본 기준 오늘 날짜
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));
		
		// 선택한 달의 첫날
		LocalDate firstDay = calendarMonth.atDay(1);
		
		// 일요일부터 시작하도록 앞쪽 빈칸 개수 계산
		int leadingEmptyDays = firstDay.getDayOfWeek().getValue() % 7;
		
		// 달력 칸 목록 : 빈칸은 null, 나머지는 실제 날짜
		List<LocalDate> calendarDays = new ArrayList<>();
		
		for (int i = 0; i < leadingEmptyDays; i++) {
			calendarDays.add(null);
		}
		
		for (int day = 1; day <= calendarMonth.lengthOfMonth(); day++) {
			calendarDays.add(calendarMonth.atDay(day));
		}
		
		// 마지막 주도 토요일까지 빈칸으로 채우기
		while (calendarDays.size() % 7 != 0) {
			calendarDays.add(null);
		}
		
		// 예정, 출석, 지각 수업이 있는 날짜 조회
		List<LocalDate> lessonDates = lessonService.findLessonDatesForMonth(
				instructorId,
				calendarMonth
			);
		
		model.addAttribute("calendarMonth", calendarMonth);
		model.addAttribute("calendarDays", calendarDays);
		model.addAttribute("lessonDates", lessonDates);
		model.addAttribute("today", today);
		model.addAttribute("previousMonth", calendarMonth.minusMonths(1));
		model.addAttribute("nextMonth", calendarMonth.plusMonths(1));
	}
	
}