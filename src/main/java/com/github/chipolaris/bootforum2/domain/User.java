package com.github.chipolaris.bootforum2.domain;

import jakarta.persistence.*;

@Entity
@Table(name="USER_T")
@TableGenerator(name="UserIdGenerator", table="ENTITY_ID_T", pkColumnName="GEN_KEY", 
pkColumnValue="USER_ID", valueColumnName="GEN_VALUE", initialValue = 1000, allocationSize=10)
public class User extends BaseEntity {

	@Column(name="USER_NAME", unique=true, nullable=false)
	private String username;
	
	@Column(name="PASSWORD", nullable=false)
	private String password;
	
	/*
	 * Note since ROLE is a keyword in SQL Server, use USER_ROLE to make
	 * sure it work there
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "USER_ROLE", length=50, nullable = false)
	private Role role;
	
	@Enumerated(EnumType.STRING)
	@Column(name="ACCOUNT_STATUS", length=50)
	private AccountStatus accountStatus;
	
	@OneToOne(fetch=FetchType.EAGER, cascade=CascadeType.ALL)
	@JoinColumn(name="PERSON_ID")
	private Person person;

	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	public Role getRole() {
		return role;
	}
	public void setRole(Role role) {
		this.role = role;
	}

	public AccountStatus getAccountStatus() {
		return accountStatus;
	}
	public void setAccountStatus(AccountStatus accountStatus) {
		this.accountStatus = accountStatus;
	}
	
	public Person getPerson() {
		return person;
	}
	public void setPerson(Person person) {
		this.person = person;
	}
	
	@Id
	@GeneratedValue(strategy=GenerationType.TABLE, generator="UserIdGenerator")
	private Long id;
	
	@Override
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	/*
	 * enums
	 */
	public enum Role {
		
		ADMIN 			("Administrator"),
		USER 			("Public User");
		
		private String label;
		
		Role(String label) {
			this.label = label;
		}
		
		public String getLabel() {
	    	return label;
	    }
	}
	
	public enum AccountStatus {

		ACTIVE 			("Active"),
		LOCKED			("Locked"),
		INACTIVE		("Inactive");
		
		private String label;
		
		AccountStatus(String label) {
			this.label = label;
		}
		
	    public String getLabel() {
	    	return label;
	    }
	}
}
