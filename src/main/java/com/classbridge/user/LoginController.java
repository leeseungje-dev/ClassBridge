package com.classbridge.user;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * 로그인 화면 표시
 * author : LEE SEUNGJE
 * date : 2026.11.02 ~ 2026.11.02
 */

@Controller
public class LoginController {
	
	@GetMapping("/login")
	public String loginForm() {
		return "login";
	}
	
}