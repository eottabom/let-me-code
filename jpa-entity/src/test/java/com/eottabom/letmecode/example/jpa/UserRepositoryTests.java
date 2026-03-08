package com.eottabom.letmecode.example.jpa;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTests {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void directIdAssignmentAndIsNew() {
		// given, id 가 이미 존재하므로 JPA 는 보통 merge 대상으로 인식한다.
		User user = new User("user123", "아이유");

		// then, 저장 전
		assertThat(user.isNew()).isTrue();

		// when, Persistable 구현 때문에 isNew() 는 true 가 되고, persist() 실행된다.
		this.userRepository.save(user);
		// 변경사항을 db에 저장한다. (@PostPersist)
		this.userRepository.flush();
		// jpa 영속성 컨텍스트를 초기화 하여 1차 캐시를 비운다. (@PostLoad)
		this.entityManager.clear();

		// then, 저장 후 새로 로드한 Entity 는 @PostLoad 로 인해 isNew() == false 가 된다.
		User loaded = this.userRepository.findById("user123").orElseThrow();
		assertThat(loaded.isNew()).isFalse();
	}

	@Test
	void entityWithNullIdIsNew() {
		User user = new User(null, "방탄소년단");
		// id 가 null 이므로 새로운 entity 로 간주된다.
		assertThat(user.isNew()).isTrue();
	}

}