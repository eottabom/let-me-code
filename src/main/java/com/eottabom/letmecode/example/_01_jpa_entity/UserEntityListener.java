package com.eottabom.letmecode.example._01_jpa_entity;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;

public class UserEntityListener {

	// @formatter:off
	/*
		1) @PostPersist
		- Entity 가 Database 에 처음 저장 (INSERT) 된 직후에 호출된다.
		동작 과정
		-> userRepository.save(user) 호출
		-> JPA 가 SQL INSERT 문 실행
		-> INSERT 쿼리 실행 완료 직후 @PostPersist 어노테이션이 붙은 메서드 호출

		2) @PostLoad
		- Entity 가 Database 에서 조회(load) 될 때마다 호출
		- 기존에 저장된 Entity 를 다시 불러올 때 isNew = false 로 초기화

		동작 과정
		-> userRepository.findById("id") 호출
		-> Database 에서 SELECT 후 영속성 컨텍스트에 주입
		-> 주입된 직후 @PostLoad 메서드 호출

		전체적인 흐름은
		1) 새 User 객체 생성시 isNew = true (생성자에서 설정)
		2) 저장 시 @PostPersist 발생 isNew = false 로 변경
		3) 로드 시 @PostLoad 발생 isNew = false 로 변경

		이런 방식으로 Entity 의 저장 상태를 자동으로 관리되어진다.

		왜 중요한가?!
		 Spring Data JPA 가 'Persistable#isNew()' 의 반환 값으로,
		 Entity 가 persist(신규) 와 merge(기존) 을 결정하므로,
		 "isNew" 플래그를 저장/조회 시점에 false 로 처리가 반드시 필요하다.
	 */
	// @formatter:on
	@PostPersist
	@PostLoad
	public void setNotNew(User user) {
		System.out.println("@PostPersist/@PostLoad called");
		user.setIsNew(false);
	}

}
