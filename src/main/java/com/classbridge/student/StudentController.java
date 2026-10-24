package com.classbridge.student;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import com.classbridge.user.User;
import com.classbridge.user.UserService;

import java.security.Principal;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import jakarta.servlet.http.HttpServletResponse;

/*
 * "/students" 요청을 받아 로그인한 강사의 학생 목록 화면에 전달
 * author : LEE SEUNGJE
 * date : 2026.10.21 ~ 2026.10.22
 */

@Controller
public class StudentController {
	
	// field
	private final StudentService studentService;
	private final UserService userService;
	
	// constructor
	public StudentController(StudentService studentService, UserService userService) {
		this.studentService = studentService;
		this.userService = userService;
	}
	
	// method
	// 학생 데이터 불러오기
	// Principal : 현재 로그인한 사용자를 특정
	// Model : HTML 화면에 데이터를 전달
	@GetMapping("/students")
	public String studentList(Principal principal, Model model) {
		
		// 로그인한 강사의 이메일로 강사 정보 조회
		User instructor = userService.findUserByEmail(principal.getName());
		
		// 해당 강사의 학생 목록 조회
		List<Student> students = studentService.findStudentsByInstructorId(instructor.getId());
		
		// 화면에서 사용할 이름을 붙여 학생 목록 전달
		model.addAttribute("students", students);
		
		return "students/list";
	}
	
	// 학생 등록 화면 표시
	@GetMapping("/students/new")
	public String studentForm() {
		return "students/new";
	}
	
	// 학생 등록 요청 처리
	// Model : Controller 가 HTML 화면에 전달할 값을 이름과 함께 담는 객체
	// required : '필수인'이라는 뜻이고 기본값은 true, false 로 설정해서 없어도 오류 x
	@PostMapping("/students")
	public String registerStudent (
			@RequestParam(value = "name") String name,
			@RequestParam(value = "learningStartDate", required = false) String learningStartDate,
			@RequestParam(value = "currentLevel", required = false) String currentLevel,
			@RequestParam(value = "instructorMemo", required = false) String instructorMemo,
			Principal principal,
			Model model
		) {
		
		// 로그인한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
		
		try {
			// 날짜를 입력하지 않았으면 null 유지
			LocalDate startDate = null;
			
			if (learningStartDate != null && !learningStartDate.isBlank()) {
				startDate = LocalDate.parse(learningStartDate);
			}
			
			// 기존 Service 를 통해 검사하고 저장
			studentService.registerStudent(
					instructor.getId(),
					name,
					startDate,
					currentLevel,
					instructorMemo
				);
		
			// null 이 아니어도 올바른 날짜 형식이 아닐 수 있기 때문에 
		} catch (DateTimeParseException e) {
			model.addAttribute("errorMessage", "学習開始日を正しく入力してください。");
			
		} catch (IllegalArgumentException e) {
			model.addAttribute("errorMessage", e.getMessage());
		}
		
		// 입력 오류가 있으면 입력값을 유지하고 등록 화면 표시
		if (model.containsAttribute("errorMessage")) {

			model.addAttribute("name", name);
			model.addAttribute("learningStartDate", learningStartDate);
			model.addAttribute("currentLevel", currentLevel);
			model.addAttribute("instructorMemo", instructorMemo);
			
			return "students/new";
		}
		
		// 등록 성공 시 학생 목록으로 이동
		return "redirect:/students";
	}
	
	// 학생 상세 화면 표시
	@GetMapping("/students/{id}")
	public String studentDetail (
			// 경로 + 변수 : 주소의 일부(주소 안의 학생 id)를 매개변수로 받음
			@PathVariable("id") Long id,
			Principal principal,
			Model model,
			HttpServletResponse response
		) {
		
		// 로그인한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
		
		try {
			// 해당 학생 조회 (학생 id 와 로그인한 강사 id 로)
			Student student = studentService.findStudentById(id, instructor.getId());

			// 상세 화면에 학생 정보 전달
			model.addAttribute("student", student);
			
			return "students/detail";
			
		} catch (StudentNotFoundException e) {
			response.setStatus(404);
			
			return "students/not-found";
		}
		
	}
	
	// 학생 수정 화면 표시
	@GetMapping("/students/{id}/edit")
	public String studentEditForm(
			@PathVariable("id") Long id,
			Principal principal,
			Model model,
			HttpServletResponse response
		) {
		
		// 로그인한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
			// 해당 강사 담당 학생인지 확인 후 조회
		try {
			Student student = studentService.findStudentById(id, instructor.getId());
		
			// 수정 화면에 기존 학생 정보 전달
			model.addAttribute("student", student);
		
			return "students/edit";
		
		} catch (StudentNotFoundException e) {
			response.setStatus(404);
			
			return "students/not-found";
		}
		
	}
	
	// 학생 수정 요청 처리
	@PostMapping("/students/{id}/edit")
	public String updateStudent(
			@PathVariable("id") Long id,
			@RequestParam("name") String name,
			@RequestParam(value = "learningStartDate", required = false) String learningStartDate,
			@RequestParam(value = "currentLevel", required = false) String currentLevel,
			@RequestParam(value = "instructorMemo", required = false) String instructorMemo,
			Principal principal,
			Model model,
			HttpServletResponse response
		) {
		
		// 로그인한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
		
		// 해당 강사의 학생인지 확인하고 조회
		Student student;
		
		try {
			student = studentService.findStudentById(id, instructor.getId());
		
		} catch (StudentNotFoundException e) {
			response.setStatus(404);
			
			return "students/not-found";
		}
		
		
		// 입력 오류로 수정 화면에 돌아갈 경우, 입력한 내용 유지
		student.setName(name);
		student.setCurrentLevel(currentLevel);
		student.setInstructorMemo(instructorMemo);
		
		try {
			// 선택 날짜 변환
			LocalDate startDate = null;
			
			if (learningStartDate != null && !learningStartDate.isBlank()) {
				startDate = LocalDate.parse(learningStartDate);
			}
			
			// Service 에서 입력값을 검사하고 DB 수정
			studentService.updateStudent(
					id,
					instructor.getId(),
					name,
					startDate,
					currentLevel,
					instructorMemo
				);
			
		} catch (StudentNotFoundException e) {
			// 처음 조회한 뒤 학생이 삭제된 경우 처리
			response.setStatus(404);
			return "students/not-found";
			
		} catch (DateTimeParseException e) {
			model.addAttribute("errorMessage", "学習開始日を正しく入力してください。");
		
		} catch (IllegalArgumentException e) {
			model.addAttribute("errorMessage", e.getMessage());
			
		}
		
		// 입력 오류가 있으면 입력 내용을 담아 수정 화면 표시
		if (model.containsAttribute("errorMessage")) {
			
			model.addAttribute("student", student);
			model.addAttribute("learningStartDate", learningStartDate);
			
			return "students/edit";
		}
		
		// 수정 성공 시 해당 학생 상세 화면으로 이동
		return"redirect:/students/" + id;
	}
	
	// 학생 삭제 요청 처리
	@PostMapping("/students/{id}/delete")
	public String deleteStudent(
			@PathVariable("id") Long id,
			Principal principal,
			HttpServletResponse response
		) {
		
		// 로그인한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
		
		try {
			// 해당 강사의 학생만 삭제 (학생 id 와 로그인한 강사 id 가 일치하는 경우에만)
			studentService.deleteStudent(id, instructor.getId());
		
			// 삭제 후 학생 목록으로 이동
			return "redirect:/students";
	
		} catch (StudentNotFoundException e) {
			response.setStatus(404);
			
			return "students/not-found";
		}
	}
	
}