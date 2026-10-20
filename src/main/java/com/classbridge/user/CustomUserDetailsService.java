package com.classbridge.user;

import org.springframework.stereotype.Service;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/*
 * <login Service>
 * 로그인 email 로 DB 조회 후 Spring Security 에 전달 
 * (비밀번호가 맞는지 검사에 필요한 정보를 준비하는 역할)
 * author : LEE SEUNGJE
 * date : 2026.10.18 ~ 2026.10.18
 */

@Service
// UserDetailsService : SpringSecurity 가 정해둔 규칙(사용자이름 -> 사용자 정보 반환)
public class CustomUserDetailsService implements UserDetailsService {
	
	// field
	private final UserMapper userMapper;
	
	// constructor
	public CustomUserDetailsService(UserMapper userMapper) {
		this.userMapper = userMapper;
	}
	
	// method
	@Override
	// UserDetails : SpringSecurity가 사용할 사용자 정보와 타입
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		
		User user = userMapper.findUserByEmail(email.strip());
		if(user == null) {
			throw new UsernameNotFoundException("メールアドレスまたはパスワードが正しくありません。");
		}
		
		// ~.User : SpringSecurity가 제공하는 클래스(로그인 확인에 사용할 사용자 정보를 담음)
		// .withUsername(user.getEmail()) : 로그인 식별자로 사용할 email 지정
		// .password(user.getPassword()) : DB에서 조회한 비밀번호 해시를 지정
		// .build() : 위 정보를 담은 객체 완성 (UserDetails타입의 User 객체를 반환)
		return org.springframework.security.core.userdetails
				.User.withUsername(user.getEmail())
				.password(user.getPassword())
				.roles(user.getRole())
				.build();
	}
	
}