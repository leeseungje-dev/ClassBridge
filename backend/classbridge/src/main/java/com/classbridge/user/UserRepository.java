package com.classbridge.user;

/*
Writer : LEE SEUNGJE
Date : 2026.10.10 ~ 2026.10.10
 */

import org.springframework.data.jpa.repository.JpaRepository;

// 저장, 조회 기능을 사용할 인터페이스
// Spring Data JPA의 기본 저장, 조회 기능 상속 (<관리, 식별 번호> : Generics)(User : 관리할 entity type, Long : id의 java type)
public interface UserRepository extends JpaRepository<User, Long> {

}