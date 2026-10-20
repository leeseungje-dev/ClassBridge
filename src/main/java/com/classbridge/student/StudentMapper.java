package com.classbridge.student;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 목록 화면에 학생 여러 명을 보여주는 메서드를 위해
import java.util.List;

/*
 * author : LEE SEUNGJE
 * date : 2026.10.18 ~ 2026.10.20
 */

@Mapper
public abstract interface StudentMapper {

	// method
	// id 로 학생을 식별, instructorId 로 그 학생을 조회할 권한이 있는지(지금 로그인한 강사의 학생인가?)도 확인
	public abstract Student findStudentById(
		@Param("id") Long id,
		@Param("instructorId") Long instructorId
	);
	
	public abstract int insertStudent(Student student);
	
	public abstract int updateStudent(
			@Param("student") Student student,
			@Param("instructorId") Long instructorId
		);
	
	public abstract int deleteStudent(
			@Param("id") Long id,
			@Param("instructorId") Long instructorId
		);
	
	// ~<> : (Generic) 어떤 타입을 담을 것인가
	public abstract List<Student> findStudentsByInstructorId(
			@Param("instructorId") Long instructorId
		);
	
}