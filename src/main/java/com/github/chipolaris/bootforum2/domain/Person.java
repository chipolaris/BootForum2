package com.github.chipolaris.bootforum2.domain;


import jakarta.persistence.*;

@Entity
@Table(name = "PERSON_T")
@TableGenerator(name = "PersonIdGenerator", table = "ENTITY_ID_T", pkColumnName = "GEN_KEY", pkColumnValue = "PERSON_ID", valueColumnName = "GEN_VALUE", initialValue = 1000, allocationSize = 10)
public class Person extends BaseEntity {

	@Column(name = "FIRST_NAME", length = 100, nullable = false)
	private String firstName;

	@Column(name = "LAST_NAME", length = 100, nullable = false)
	private String lastName;

	@Column(name = "EMAIL", length = 100, unique = true, nullable = false)
	private String email;

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

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "PersonIdGenerator")
	private Long id;

	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
