package com.classbridge.report;

import java.security.Principal;
import java.util.List;
import java.util.ArrayList;

import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.classbridge.lesson.Lesson;
import com.classbridge.lesson.LessonNotFoundException;
import com.classbridge.lesson.LessonService;
import com.classbridge.observation.LearningObservation;
import com.classbridge.student.Student;
import com.classbridge.student.StudentNotFoundException;
import com.classbridge.student.StudentService;
import com.classbridge.user.User;
import com.classbridge.user.UserService;

import jakarta.servlet.http.HttpServletResponse;

/*
 * 수업일보 화면의 요청 처리 Controller
 * author : LEE SEUNGJE
 * date : 2026.10.31 ~ 2026.10.31
 */

@Controller
public class LessonReportController {
	
	// field
	private final LessonReportService lessonReportService;
	private final LessonService lessonService;
	private final StudentService studentService;
	private final UserService userService;
	
	// constructor
	public LessonReportController(
			LessonReportService lessonReportService,
			LessonService lessonService,
			StudentService studentService,
			UserService userService
	) {
		this.lessonReportService = lessonReportService;
		this.lessonService = lessonService;
		this.studentService = studentService;
		this.userService = userService;
	}
	
	// method
	// 수업일보 상세 화면 표시
	@GetMapping("/lessons/{lessonId}/report")
	public String reportDetail(
			@PathVariable("lessonId") Long lessonId,
			Principal principal,
			Model model,
			HttpServletResponse response
	) {
		// 로그인한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
		
		try {
			// 일보 조회 및 접근 권한 확인
			LessonReport report = lessonReportService.findLessonReportByLessonId(
					lessonId, 
					instructor.getId()
				);
			
			// 화면에 표시할 수업과 학생 정보 조회
			Lesson lesson = lessonService.findLessonById(
					lessonId,
					instructor.getId()
				);
			
			Student student = studentService.findStudentById(
					lesson.getStudentId(),
					instructor.getId()
				);
			
			// 일보에 연결된 관찰 기록 조회
			List<LearningObservation> observations = lessonReportService.findObservationsByLessonId(
					lessonId,
					instructor.getId()
				);
			
			// 화면에 데이터 전달
			model.addAttribute("student", student);
			model.addAttribute("lesson", lesson);
			model.addAttribute("report", report);
			model.addAttribute("observations", observations);
			
			return "reports/detail";
			
		} catch (LessonReportNotFoundException e ) {
			response.setStatus(404);
			return "reports/not-found";
			
		} catch (LessonNotFoundException e ) {
			response.setStatus(404);
			return "lessons/not-found";
			
		} catch (StudentNotFoundException e ) {
			response.setStatus(404);
			return "students/not-found";
		}
	}
	
	// 수업일보 작성 화면 표시
	@GetMapping("/lessons/{lessonId}/report/new")
	public String reportForm(
			@PathVariable("lessonId") Long lessonId,
			Principal principal,
			Model model,
			HttpServletResponse response
	) {
		// 로그인한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
		
		try {
			// 수업 조회 및 접근 권한 확인
			Lesson lesson = lessonService.findLessonById(
					lessonId, 
					instructor.getId()
				);
			
			// 이미 일보가 있으면 원래 일보 상세로 이동
			try {
				lessonReportService.findLessonReportByLessonId(
						lessonId, 
						instructor.getId()
					);
				
				return "redirect:/lessons/" + lessonId + "/report";
				
			} catch (LessonReportNotFoundException e) {
				// 아직 일보가 없으면 아래의 작성 조건 확인으로 진행
			}
			
			// 화면에 표시할 학생 정보 조회
			Student student = studentService.findStudentById(
					lesson.getStudentId(), 
					instructor.getId()
				);
			
			model.addAttribute("student", student);
			model.addAttribute("lesson", lesson);
			
			// 출석하지 않은 수업에는 작성 조건 안내
			if (!"ATTENDED".equals(lesson.getStatus())) {
				model.addAttribute("errorMessage", "出席した授業のみ記録を作成できます。");
				
				return "reports/not-available";
			}
			
			return "reports/new";
			
		} catch (LessonNotFoundException e) {
			response.setStatus(404);
			return "lessons/not-found";
			
		} catch (StudentNotFoundException e) {
			response.setStatus(404);
			return "students/not-found";
		}
	}
	
	// 수업일보와 관찰 기록 등록 요청 처리 기능
	@PostMapping("/lessons/{lessonId}/report")
	public String registerLessonReport(
			@PathVariable("lessonId") Long lessonId,
			@RequestParam MultiValueMap<String, String> parameters,
			Principal principal,
			Model model,
			HttpServletResponse response
	) {
		// 로그인한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
		
		// 입력 오류 시 다시 표시할 수업과 학생 정보 확보
		Lesson lesson;
		Student student;
		
		try {
			lesson = lessonService.findLessonById(
					lessonId, 
					instructor.getId()
				);
			
			student = studentService.findStudentById(
					lesson.getStudentId(), 
					instructor.getId()
				);
			
			// 이미 일보가 있으면 원래 일보 상세로 이동
			try {
				lessonReportService.findLessonReportByLessonId(
						lessonId, 
						instructor.getId()
					);
				
				return "redirect:/lessons/" + lessonId + "/report";
			
			} catch (LessonReportNotFoundException e) {
				// 일보가 없으면 등록 처리로 진행
			}
			
		} catch (LessonNotFoundException e) {
			response.setStatus(404);
			
			return "lessons/not-found";
			
		} catch (StudentNotFoundException e) {
			response.setStatus(404);
			
			return "students/not-found";
		}
		
		model.addAttribute("student", student);
		model.addAttribute("lesson", lesson);
		
		// 작성 화면을 연 뒤 수업 상태가 바뀌었을 수도 있으므로 다시 확인
		if (!"ATTENDED".equals(lesson.getStatus())) {
			model.addAttribute(
					"errorMessage",
					"出席した授業のみ記録を作成できます。"
				);
			
			return "reports/not-available";
		}
		
		// 일보 입력값 받기
		String content = parameters.getFirst("content");
		String nextCheck = parameters.getFirst("nextCheck");
		
		// 관찰 항목이 없으면 빈 목록으로 받기
		List<String> categories = parameters.getOrDefault("category", List.of());
		
		List<String> items = parameters.getOrDefault("item", List.of());
		
		List<String> memos = parameters.getOrDefault("memo", List.of());
		
		// Service 에 전달할 관찰 입력 목록
		List<ObservationInput> observations = new ArrayList<>();
		
		try {
			// 분류, 항목, 내용의 개수가 일치하는지 확인
			if (categories.size() != items.size()
					|| categories.size() != memos.size()) {

				throw new IllegalArgumentException("観察項目を正しく入力してください。");
			}
			
			// 같은 순서의 분류, 항목, 내용을 한 객체로 묶기
			for (int i = 0; i < categories.size(); i++) {
				ObservationInput input = new ObservationInput(
						categories.get(i),
						items.get(i),
						memos.get(i)
					);
				observations.add(input);
			}
			
			// Service 에서 입력값을 검사하고 함께 저장
			lessonReportService.registerLessonReport(
					lessonId, 
					instructor.getId(), 
					content, 
					nextCheck, 
					observations
				);
			
			// 저장 성공 시 해당 수업의 일보 상세로 이동
			return "redirect:/lessons/" + lessonId + "/report";
			
		} catch (LessonNotFoundException e) {
			response.setStatus(404);
			
			return "lessons/not-found";
			
		} catch (IllegalArgumentException e) {
			model.addAttribute("errorMessage", e.getMessage());
		}
		
		// 입력 오류가 있으면 입력 내용을 유지하고 작성 화면 표시
		model.addAttribute("content", content);
		model.addAttribute("nextCheck", nextCheck);
		model.addAttribute("observations", observations);
		
		return "reports/new";
	}
	
	// 수업일보 수정 화면 표시
	@GetMapping("/lessons/{lessonId}/report/edit")
	public String reportEditForm(
			@PathVariable("lessonId") Long lessonId,
			Principal principal,
			Model model,
			HttpServletResponse response
	) {
		// 로그인한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
		
		try {
			// 기존 일보 조회 및 접근 권한 확인
			LessonReport report = lessonReportService.findLessonReportByLessonId(lessonId, instructor.getId());
			
			// 화면에 표시할 수업과 학생 정보 조회
			Lesson lesson = lessonService.findLessonById(
					lessonId,
					instructor.getId()
				);
			
			Student student = studentService.findStudentById(
					lesson.getStudentId(),
					instructor.getId()
				);
			
			// 기존 관찰 기록 조회
			List<LearningObservation> observations = lessonReportService.findObservationsByLessonId(
					lessonId,
					instructor.getId()
				);
			
			// 기존 관찰 기록을 수정 화면용 입력 객체로 변환
			List<ObservationUpdateInput> inputs = new ArrayList<>();
			
			for (LearningObservation observation : observations) {
				inputs.add(new ObservationUpdateInput(
						observation.getId(),
						observation.getCategory(),
						observation.getItem(),
						observation.getMemo())
					);
			}
			
			// 화면에 표시할 정보 전달
			model.addAttribute("student", student);
			model.addAttribute("lesson", lesson);
			
			// 입력란에 미리 채울 기존 내용 전달
			model.addAttribute("content", report.getContent());
			model.addAttribute("nextCheck", report.getNextCheck());
			model.addAttribute("observations", inputs);
			
			return "reports/edit";

		} catch (LessonReportNotFoundException e) {
			response.setStatus(404);
			
			return "reports/not-found";
			
		} catch (LessonNotFoundException e) {
			response.setStatus(404);
			
			return "lessons/not-found";
			
		} catch (StudentNotFoundException e) {
			response.setStatus(404);
			
			return "students/not-found";
		}
	}
	
	// 입력받은 수업일보와 관찰 기록 수정 요청 처리 기능
	@PostMapping("/lessons/{lessonId}/report/edit")
	public String updateLessonReport(
			@PathVariable("lessonId") Long lessonId,
			@RequestParam MultiValueMap<String, String> parameters,
			Principal principal,
			Model model,
			HttpServletResponse response
	) {
		// 로그인 한 강사 확인
		User instructor = userService.findUserByEmail(principal.getName());
		
		Lesson lesson;
		Student student;
		
		try {
			// 기존 일보 존재 여부 및 접근 권한 확인
			lessonReportService.findLessonReportByLessonId(
					lessonId,
					instructor.getId()
				);
			
			// 화면에 표시할 수업과 학생 정보 확보
			lesson = lessonService.findLessonById(
					lessonId,
					instructor.getId()
				);
			
			student = studentService.findStudentById(
					lesson.getStudentId(),
					instructor.getId()
				);
			
		} catch (LessonReportNotFoundException e) {
			response.setStatus(404);
			
			return "reports/not-found";
			
		} catch (LessonNotFoundException e) {
			response.setStatus(404);
			
			return "lessons/not-found";
			
		} catch (StudentNotFoundException e) {
			response.setStatus(404);
			
			return "students/not-found";
		}
		
		model.addAttribute("student", student);
		model.addAttribute("lesson", lesson);
		
		// 일보 입력값 받기
		String content = parameters.getFirst("content");
		String nextCheck = parameters.getFirst("nextCheck");
		
		// 관찰 항목 입력값 받기
		List<String> observationIds = parameters.getOrDefault("observationId", List.of());
		List<String> categories = parameters.getOrDefault("category", List.of());
		List<String> items = parameters.getOrDefault("item", List.of());
		List<String> memos = parameters.getOrDefault("memo", List.of());
		
		// 항목별 입력값의 개수가 다르면 잘못된 요청으로 처리
		if (observationIds.size() != categories.size()
				|| observationIds.size() != items.size()
				|| observationIds.size() != memos.size()
		) {
			
			response.setStatus(400);
			model.addAttribute("errorMessage", "入力情報が正しくありません。授業一覧から編集画面を開き直してください。");
			
			return "reports/not-available";
		}
		
		List<ObservationUpdateInput> observations = new ArrayList<>();
		
		try {
			// 같은 순서의 id, 분류, 항목, 내용을 한 객체로 묶기
			for (int i = 0; i < observationIds.size(); i++) {
				String rawId = observationIds.get(i);
				
				// 빈 id 는 새로 추가한 관찰 항목
				Long observationId = null;
				
				if (!rawId.isBlank()) {
					observationId = Long.valueOf(rawId);
				}
				
				observations.add(new ObservationUpdateInput(
						observationId,
						categories.get(i),
						items.get(i),
						memos.get(i)
					));
			}
		} catch (NumberFormatException e) {
			// id 가 숫자로 변환되지 않으면 저장하지 않음
			response.setStatus(400);
			model.addAttribute("errorMessage", "入力情報が正しくありません。授業一覧から編集画面を開き直してください。");
			
			return "reports/not-available";
		}
		
		try {
			// Service 에서 검사 후 일보와 관찰 기록을 함께 수정
			lessonReportService.updateLessonReport(
					lessonId,
					instructor.getId(),
					content,
					nextCheck,
					observations
				);
			
			// 수정 성공 시 일보 상세로 이동
			return "redirect:/lessons/" + lessonId + "/report";
			
		} catch (LessonReportNotFoundException e) {
			response.setStatus(404);
			
			return "reports/not-found";
			
		} catch (LessonNotFoundException e) {
			response.setStatus(404);
			
			return "lessons/not-found";
			
		} catch (IllegalArgumentException e) {
			model.addAttribute("errorMessage", e.getMessage());
		}
		
		// 입력 오류 시 수정 중이던 내용을 다시 표시
		model.addAttribute("content", content);
		model.addAttribute("nextCheck", nextCheck);
		model.addAttribute("observations", observations);
		
		return "reports/edit";
	}
	
}