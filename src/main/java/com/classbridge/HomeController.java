package com.classbridge;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * author : LEE SEUNGJE
 * date : 2026.10.13 ~
 */

// web 요청을 처리하고 화면을 연결하는 클래스로 등록
@Controller
public class HomeController {

	// 브라우저에서 "/" 주소로 GET 요청을 보내면 이 메서드(home) 실행
	@GetMapping("/")
	// Model : (Spring이 제공) 화면에 전달할 데이터를 담는 인터페이스
	public String home(Model model) {
		// 화면에서 ${message}라는 이름으로 사용할 값 등록
		model.addAttribute("message", "ClassBridgeへようこそ。");
		
		return "home";	// 사용할 화면 이름 (home.html)
	}
	
}