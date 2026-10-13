package com.classbridge.user;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * ID로 강사 한 명을 조회하는 기능 연결
 * author : LEE SEUNGJE
 * date : 2026.10.12 ~ 
 */

// @Mapper : MyBatis가 사용할 Mapper 인터페이스로 지정
// Mapper : DB의 저장, 조회, 수정, 삭제 통로
@Mapper
public abstract interface UserMapper {
	
	// method (조회, 저장)
	// @Param : 전달받은 값을 SQL에서 사용할 이름으로 지정 (SQL에서는 #{id}로 참조)
	public abstract User findUserById(@Param("id") Long id);
	
	public abstract int insertUser(User user);
	
}