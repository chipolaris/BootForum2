package com.github.chipolaris.bootforum2.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="PERSON_T",
		uniqueConstraints= {@UniqueConstraint(columnNames="EMAIL", name="UNIQ_PERSON_EMAIL")})
@TableGenerator(name="PersonIdGenerator", table="ENTITY_ID_T", pkColumnName="GEN_KEY",
		pkColumnValue="PERSON_ID", valueColumnName="GEN_VALUE", initialValue = 1000, allocationSize=10)
public class Person extends BaseEntity {

	@PrePersist
	public void prePersist() {
		lowerCaseEmail();
		LocalDateTime now = LocalDateTime.now();
		if(this.getCreateDate() == null) {
			this.setCreateDate(now);
		}
		if(this.getUpdateDate() == null) {
			this.setUpdateDate(now);
		}
	}

	@PreUpdate
	public void preUpdate() {
		lowerCaseEmail();
		this.setUpdateDate(LocalDateTime.now());
	}

	private void lowerCaseEmail() {
		if(this.email != null) {
			this.email = this.email.toLowerCase();
		}
	}

	@Id
	@GeneratedValue(strategy=GenerationType.TABLE, generator="PersonIdGenerator")
	private Long id;

	@Column(name="FIRST_NAME", length=100)
	private String firstName;

	@Column(name="LAST_NAME", length=100)
	private String lastName;

	@Column(name="EMAIL", length=100)
	private String email;

	@Override
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
}