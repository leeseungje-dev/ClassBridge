package com.classbridge.lesson;

import java.security.Principal;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.util.MultiValueMap;

import com.classbridge.student.Student;
import com.classbridge.student.StudentNotFoundException;
import com.classbridge.student.StudentService;
import com.classbridge.user.User;
import com.classbridge.user.UserService;
import com.classbridge.report.LessonReport;
import com.classbridge.report.LessonReportService;
import com.classbridge.report.LessonReportNotFoundException;
import com.classbridge.report.ObservationUpdateInput;
import com.classbridge.observation.ObservationHistory;
import com.classbridge.observation.LearningObservation;

import jakarta.servlet.http.HttpServletResponse;

/*
 * 학생별 수업 화면의 요청 처리
 * author : LEE SEUNGJE
 * date : 2026.10.28 ~ 2026.11.16
 */

@Controller
public class LessonController {
	
	// field
	private final LessonService lessonService;
	private final StudentService studentService;
	private final UserService userService;
	private final LessonReportService lessonReportService;
	private final LessonAttendanceService lessonAttendanceService;
	
	// constructor
	public LessonController(
			LessonService lessonService,
			StudentService studentService,
			UserService userService,
			LessonReportService lessonReportService,
			LessonAttendanceService lessonAttendanceService
	) {
		this.lessonService = lessonService;
		this.studentService = studentService;
		this.userService = userService;
		this.lessonReportService = lessonReportService;
		this.lessonAttendanceService = lessonAttendanceService;
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
			
			// 일보가 없을 때 사용할 초기 입력값
			LessonReport report = null;
			String content = "";
			String nextCheck = "";
			List<ObservationUpdateInput> inputs = new ArrayList<>();
			
			// 기존 일보가 있는지 확인
			try {
				report = lessonReportService.findLessonReportByLessonId(
						id,
						instructor.getId()
				);
				
			} catch (LessonReportNotFoundException e) {
				// 일보가 없으면 빈 입력값으로 작성 화면 표시
			}
			
			if (report != null) {
				content = report.getContent();
				nextCheck = report.getNextCheck();
				
				// 기존 관찰 기록 조회
				List<LearningObservation> observations = lessonReportService.findObservationsByLessonId(
						id,
						instructor.getId()
				);
				
				// 수정 시 기존 기록 id 를 유지하도록 입력 객체로 변환
				for (LearningObservation observation : observations) {
					inputs.add(new ObservationUpdateInput(
							observation.getId(),
							observation.getCategory(),
							observation.getItem(),
							observation.getMemo()
					));
				}
			}
			
			// 기존 수업 정보와 출결 입력값
			model.addAttribute("student", student);
			model.addAttribute("lesson", lesson);
			model.addAttribute("status", lesson.getStatus());
			model.addAttribute("statusMemo", lesson.getStatusMemo());

			// 일보와 관찰 입력값
			model.addAttribute("content", content);
			model.addAttribute("nextCheck", nextCheck);
			model.addAttribute("observations", inputs);
			model.addAttribute("hasReport", report != null);
			
			return "lessons/status";
			
		} catch (LessonNotFoundException e) {
			response.setStatus(404);
			
			return "lessons/not-found";
			
		} catch (StudentNotFoundException e) {
			response.setStatus(404);
			
			return "students/not-found";
		}
	}
	
	// 출결 상태와 일보, 관찰기록의 통합 저장 요청
	@PostMapping("/lessons/{id}/status")
	public String updateLessonStatus(
			@PathVariable("id") Long id,
			@RequestParam MultiValueMap<String, String> parameters,
			Principal principal,
			Model model,
			HttpServletResponse response
	) {
		// 로그인한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
		
		// 입력 오류 시 화면에 다시 표시할 정보 확보
		Lesson lesson;
		Student student;
		boolean hasReport = false;
		
		try {
			// 수업과 학생 조회 및 접근 권한 확인
			lesson = lessonService.findLessonById(id, instructor.getId());
			
			// 해당 수업을 받은 학생 조회
			student = studentService.findStudentById(lesson.getStudentId(), instructor.getId());
			
			// 오류 화면에 사용할 기존 일보 존재 여부 확인
			try {
				lessonReportService.findLessonReportByLessonId(id, instructor.getId());
				
				hasReport = true;
				
			} catch (LessonReportNotFoundException e) {
				// 일보가 없으면 false 유지
			}
			
		} catch (LessonNotFoundException e) {
			response.setStatus(404);
			
			return "lessons/not-found";
			
		} catch (StudentNotFoundException e) {
			response.setStatus(404);
			
			return "students/not-found";
		}
		
		// 출결과 일보 입력값
		String status = parameters.getFirst("status");
		String statusMemo = parameters.getFirst("statusMemo");
		String content = parameters.getFirst("content");
		String nextCheck = parameters.getFirst("nextCheck");
		
		// 관찰 항목별 입력값
		List<String> ids = parameters.getOrDefault("observationId", List.of());
		List<String> categories = parameters.getOrDefault("category", List.of());
		List<String> items = parameters.getOrDefault("item", List.of());
		List<String> memos = parameters.getOrDefault("memo", List.of());
		
		model.addAttribute("student", student);
		model.addAttribute("lesson", lesson);
		model.addAttribute("hasReport", hasReport);
		
		// 항목별 개수가 다르면 잘못된 요청으로 처리
		if (
				ids.size() != categories.size()
				|| ids.size() != items.size()
				|| ids.size() != memos.size()
		) {
			response.setStatus(400);
			model.addAttribute(
					"errorMessage",
					"入力情報が正しくありません。画面を開き直してください。"
			);
			return "reports/not-available";
		}

		List<ObservationUpdateInput> observations = new ArrayList<>();
		
		try {
			// 같은 순서의 관찰 입력을 한 객체로 묶기
			for (int i = 0; i < ids.size(); i++) {
				Long observationId = null;

				// 새 항목의 ID는 빈 문자열
				if (!ids.get(i).isBlank()) {
					observationId = Long.valueOf(ids.get(i));
				}

				observations.add(new ObservationUpdateInput(
						observationId,
						categories.get(i),
						items.get(i),
						memos.get(i)
				));
			}

		} catch (NumberFormatException e) {
			response.setStatus(400);
			model.addAttribute(
					"errorMessage",
					"入力情報が正しくありません。画面を開き直してください。"
			);
			return "reports/not-available";
		}
		
		try {
			// 최종 상태에 맞춰 출결과 일보를 함께 저장
			lessonAttendanceService.saveAttendance(
					id,
					instructor.getId(),
					status,
					statusMemo,
					content,
					nextCheck,
					observations
			);

			// 저장 성공 시 해당 수업 상세로 이동
			return "redirect:/lessons/" + id;

		} catch (LessonNotFoundException e) {
			response.setStatus(404);
			return "lessons/not-found";

		} catch (LessonReportNotFoundException e) {
			response.setStatus(404);
			return "reports/not-found";

		} catch (IllegalArgumentException e) {
			model.addAttribute("errorMessage", e.getMessage());
		}
		
		// 입력 오류가 있으면 숨겨진 영역의 입력도 포함하여 유지
		model.addAttribute("status", status);
		model.addAttribute("statusMemo", statusMemo);
		model.addAttribute("content", content);
		model.addAttribute("nextCheck", nextCheck);
		model.addAttribute("observations", observations);

		return "lessons/status";
	}
	
	// 수업 준비 상세 화면 표시
	@GetMapping("/lessons/{id}")
	public String lessonDetail(
			@PathVariable("id") Long id,
			Principal principal,
			Model model,
			HttpServletResponse response
	) {
		// 로그인한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
		
		try {
			// 선택한 수업 조회 및 접근 권한 확인
			Lesson lesson = lessonService.findLessonById(id, instructor.getId());
			
			// 해당 수업의 학생 조회
			Student student = studentService.findStudentById(
					lesson.getStudentId(),
					instructor.getId()
				);
			
			// 일보 유무와 관계없이 직전 출석 수업 조회
			Lesson previousLesson = lessonService.findPreviousAttendedLesson(
					id,
					instructor.getId()
				);
			
			// 직전 출석 수업의 일보 조회
			LessonReport previousReport = null;
			
			if (previousLesson != null) {
				try {
					previousReport = lessonReportService.findLessonReportByLessonId(
							previousLesson.getId(),
							instructor.getId()
						);
					
				} catch (LessonReportNotFoundException e) {
					// 직전 출석 수업의 일보가 없으면 null 유지
					// 화면에서 미작성 안내와 작성 링크 표시
				}
			}
			
			// 이전 수업들의 누적 관찰 기록을 분류별로 조회
			Map<String, List<ObservationHistory>> observationGroups = lessonReportService.findObservationHistoryByCategory(
					id,
					instructor.getId()
				);
			
			// 화면에 전달
			model.addAttribute("lesson", lesson);
			model.addAttribute("student", student);
			model.addAttribute("previousLesson", previousLesson);
			model.addAttribute("previousReport", previousReport);
			model.addAttribute("observationGroups", observationGroups);
			
			return "lessons/detail";
			
		} catch (LessonNotFoundException e) {
			response.setStatus(404);
			return "lessons/not-found";
			
		} catch (StudentNotFoundException e) {
			response.setStatus(404);
			return "students/not-found";
		}
	}
	
}