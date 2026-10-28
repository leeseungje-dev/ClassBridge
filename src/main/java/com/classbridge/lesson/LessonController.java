package com.classbridge.lesson;

import java.security.Principal;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.classbridge.student.Student;
import com.classbridge.student.StudentNotFoundException;
import com.classbridge.student.StudentService;
import com.classbridge.user.User;
import com.classbridge.user.UserService;

import jakarta.servlet.http.HttpServletResponse;

/*
 * 학생별 수업 화면의 요청 처리
 * author : LEE SEUNGJE
 * date : 2026.10.28 ~ 2026.10.28
 */

@Controller
public class LessonController {
	
	// field
	private final LessonService lessonService;
	private final StudentService studentService;
	private final UserService userService;
	
	// constructor
	public LessonController(LessonService lessonService, StudentService studentService, UserService userService) {
		this.lessonService = lessonService;
		this.studentService = studentService;
		this.userService = userService;
	}
	
	// method
	// 학생별 수업 목록 여는 기능
	@GetMapping("/students/{studentId}/lessons")
	public String lessonList(
			@PathVariable("studentId") Long studentId,
			Principal principal,
			Model model,
			HttpServletResponse response
	) {
		// 로그인한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
		
		try {
			// 화면에 표시할 학생 정보 조회 및 액세스 권한 확인
			Student student = studentService.findStudentById(studentId, instructor.getId());
			
			// 담당 학생의 수업 목록 조회
			List<Lesson> lessons = lessonService.findLessonByStudentId(studentId, instructor.getId());
			
			// 화면에 학생 정보와 수업 목록 전달
			model.addAttribute("student", student);
			model.addAttribute("lessons", lessons);
			
			return "lessons/list";
			
		} catch (StudentNotFoundException e) {
			response.setStatus(404);
			return "students/not-found";
		}
	}
	
	// 수업 등록 화면을 여는 기능
	@GetMapping("/students/{studentId}/lessons/new")
	public String lessonForm(
			@PathVariable("studentId") Long studentId,
			Principal principal,
			Model model,
			HttpServletResponse response
	) {
		// 로그인한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
		
		try {
			// 수업을 등록할 학생 조회 및 접근 권한 확인
			Student student = studentService.findStudentById(studentId, instructor.getId());
			
			// 화면에 학생 정보 전달
			model.addAttribute("student", student);
			
			return "lessons/new";
			
		} catch (StudentNotFoundException e) {
			response.setStatus(404);
			
			return "students/not-found";
		}
	}
	
	// 수업 등록 요청 처리
	@PostMapping("/students/{studentId}/lessons")
	public String registerLesson(
			@PathVariable("studentId") Long studentId,
			@RequestParam(value = "lessonAt", required = false) String lessonAt,
			Principal principal,
			Model model,
			HttpServletResponse response
	) {
		// 로그인 한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
		
		// 오류 시 등록 화면에 다시 표시할 학생 정보 확보
		Student student;
		
		try {
			student = studentService.findStudentById(studentId, instructor.getId());
			
		} catch (StudentNotFoundException e) {
			response.setStatus(404);
			
			return "students/not-found";
		}
		
		try {
			// 입력값이 있으면 문자열을 날짜와 시간으로 변환
			LocalDateTime parsedLessonAt = null;
			
			if (lessonAt != null && !lessonAt.isBlank()) {
				parsedLessonAt = LocalDateTime.parse(lessonAt);
			}
			
			// LessonService 에서 필수값과 접근 권한을 확인한 뒤 저장
			lessonService.registerLesson(studentId, instructor.getId(), parsedLessonAt);
			
			// 등록 성공 시 해당 학생의 수업 목록으로 이동
			return "redirect:/students/" + studentId + "/lessons";
			
		} catch (StudentNotFoundException e) {
			response.setStatus(404);
			
			return "students/not-found";
			
		} catch (DateTimeParseException e) {
			model.addAttribute("errorMessage", "授業日時を正しく入力してください。");
			
		} catch (IllegalArgumentException e) {
			model.addAttribute("errorMessage", e.getMessage());
		}
		
		// 입력 오류가 있으면 학생 정보와 입력값을 다시 등록
		model.addAttribute("student", student);
		model.addAttribute("lessonAt", lessonAt);
		
		return "lessons/new";
	}
	
	// 수업 일시 수정 화면을 여는 기능
	@GetMapping("/lessons/{id}/edit")
	public String lessonEditForm(
			@PathVariable("id") Long id,
			Principal principal,
			Model model,
			HttpServletResponse response
	) {
		// 로그인한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
		
		try {
			// 수업 조회 및 담당 강사인지 확인
			Lesson lesson = lessonService.findLessonById(id, instructor.getId());
			
			// 화면에 표시할 학생 정보 조회
			Student student = studentService.findStudentById(lesson.getStudentId(), instructor.getId());
			
			// 수정 화면에 수업 정보와 학생 정보 전달
			model.addAttribute("lesson", lesson);
			model.addAttribute("student", student);
			
			return "lessons/edit";
			
		} catch (LessonNotFoundException e) {
			response.setStatus(404);
			
			return "lessons/not-found";
			
		} catch (StudentNotFoundException e) {
			response.setStatus(404);
			
			return "students/not-found";
		}
	}
	
	// 저장 버튼을 눌렀을 때 수업 일시 수정 요청 처리
	@PostMapping("/lessons/{id}/edit")
	public String updateLessonDateTime(
			@PathVariable("id") Long id,
			@RequestParam(value = "lessonAt", required = false) String lessonAt,
			Principal principal,
			Model model,
			HttpServletResponse response
	) {
		// 로그인한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
		
		// 입력 오류 시 화면에 다시 표시할 정보 보존
		Lesson lesson;
		Student student;
		
		try {
			// 수업 조회 및 접근 권한 확인
			lesson = lessonService.findLessonById(id, instructor.getId());
			
			// 해당 수업의 학생 조회
			student = studentService.findStudentById(lesson.getStudentId(), instructor.getId());
			
		} catch (LessonNotFoundException e) {
			response.setStatus(404);
			
			return "lessons/not-found";
			
		} catch (StudentNotFoundException e) {
			response.setStatus(404);
			
			return "students/not-found";
		}
		
		try {
			// 입력한 문자열을 날짜와 시간으로 변환
			LocalDateTime parsedLessonAt = null;
			
			if (lessonAt != null && !lessonAt.isBlank()) {
				parsedLessonAt = LocalDateTime.parse(lessonAt);
			}
			
			// LessonService 에서 검사하고 DB 수정
			lessonService.updateLessonDateTime(id, instructor.getId(), parsedLessonAt);
			
			// 수정 성공 시 해당 학생의 수업 목록으로 이동
			return "redirect:/students/" + lesson.getStudentId() + "/lessons";
			
		} catch (LessonNotFoundException e) {
			// 처음 조회한 뒤 수업이 삭제된 경우
			response.setStatus(404);
			
			return "lessons/not-found";
			
		} catch (DateTimeParseException e) {
			model.addAttribute("errorMessage", "授業日時を正しく入力してください。");
			
		} catch (IllegalArgumentException e) {
			model.addAttribute("errorMessage", e.getMessage());
		}
		
		// 입력 오류 시 입력값을 유지하고 수정 화면 표시
		model.addAttribute("lesson", lesson);
		model.addAttribute("student", student);
		model.addAttribute("lessonAt", lessonAt);
		
		return "lessons/edit";
	}
	
	// 수업 상태 변경 화면을 여는 기능
	@GetMapping("/lessons/{id}/status")
	public String lessonStatusForm(
			@PathVariable("id") Long id,
			Principal principal,
			Model model,
			HttpServletResponse response
	) {
		// 로그인한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
		
		try {
			// 수업 조회 및 접근 권한 확인
			Lesson lesson = lessonService.findLessonById(id, instructor.getId());
			
			// 해당 수업을 받는 학생 조회
			Student student = studentService.findStudentById(lesson.getStudentId(), instructor.getId());
			
			// 화면에 학생 정보와 현재 수업 정보 전달
			model.addAttribute("student", student);
			model.addAttribute("lesson", lesson);
			model.addAttribute("status", lesson.getStatus());
			model.addAttribute("statusMemo", lesson.getStatusMemo());
			
			return "lessons/status";
			
		} catch (LessonNotFoundException e) {
			response.setStatus(404);
			
			return "lessons/not-found";
			
		} catch (StudentNotFoundException e) {
			response.setStatus(404);
			
			return "students/not-found";
		}
	}
	
	// 수업 상태와 상태 메모 변경 요청 처리
	@PostMapping("/lessons/{id}/status")
	public String updateLessonStatus(
			@PathVariable("id") Long id,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "statusMemo", required = false) String statusMemo,
			Principal principal,
			Model model,
			HttpServletResponse response
	) {
		// 로그인한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
		
		// 입력 오류 시 화면에 다시 표시할 정보 확보
		Lesson lesson;
		Student student;
		
		try {
			// 수업 조회 및 접근 권한 확인
			lesson = lessonService.findLessonById(id, instructor.getId());
			
			// 해당 수업을 받은 학생 조회
			student = studentService.findStudentById(lesson.getStudentId(), instructor.getId());
			
		} catch (LessonNotFoundException e) {
			response.setStatus(404);
			
			return "lessons/not-found";
			
		} catch (StudentNotFoundException e) {
			response.setStatus(404);
			
			return "students/not-found";
		}
		
		try {
			// LessonService 에서 status 와 statusMemo 를 검사하고 DB 수정
			lessonService.updateLessonStatus(
					id, 
					instructor.getId(), 
					status, 
					statusMemo
				);
			
			// 변경 성공 시 해당 학생의 수업 목록으로 이동
			return "redirect:/students/" + lesson.getStudentId() + "/lessons";
			
		} catch (LessonNotFoundException e) {
			// 처음 조회한 뒤 수업이 삭제된 경우
			response.setStatus(404);
			
			return "lessons/not-found";
			
		} catch (IllegalArgumentException e) {
			model.addAttribute("errorMessage", e.getMessage());
		}
		
		// 입력 오류 시 사용자가 입력한 내용을 유지
		model.addAttribute("lesson", lesson);
		model.addAttribute("student", student);
		model.addAttribute("status", status);
		model.addAttribute("statusMemo", statusMemo);
		
		return "lessons/status";
	}
	
}