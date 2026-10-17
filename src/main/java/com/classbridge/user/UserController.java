package com.classbridge.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/*
 * 회원가입 화면의 요청을 처리
 * author : LEE SEUNGJE
 * date : 2026.10.15 ~ 2026.10.15
 */

@Controller
public class UserController {
	// field
	private final UserService userService;
	
	// constructor
	public UserController(UserService userService) {
		this.userService = userService;
	}
	
	// method
	// 회원가입 화면 표시
	@GetMapping("/signup")
	public String signup() {
		return "signup";
	}
	
	// 회원가입 폼에서 보낸 데이터 처리
	@PostMapping("/signup")
	public String signup(@RequestParam("email") String email, @RequestParam("password") String password, Model model) {
		
		// 입력 오류나 중복 이메일 안내
		try {
			userService.registerUser(email, password);
		} catch(IllegalArgumentException e) {
			model.addAttribute("errorMessage", e.getMessage());
			model.addAttribute("email", email);
			
			return "signup";
		}
		
		// 가입 성공 시 홈으로 이동
		return "redirect:/";
	}
}