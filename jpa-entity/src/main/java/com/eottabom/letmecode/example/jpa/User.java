package com.eottabom.letmecode.example.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "users")
@EntityListeners(UserEntityListener.class)
public class User implements Persistable<String> {

	@Id
	private String id;

	private String name;

	private boolean isNew = true;

	protected User() {

	}

	public User(String id, String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public boolean isNew() {
		return this.isNew;
	}

	public void setIsNew(boolean isNew) {
		this.isNew = isNew;
	}

}
