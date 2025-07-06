package com.github.chipolaris.bootforum2.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet; // Import HashSet
import java.util.List;
import java.util.Set; // Import Set

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable; // Import CollectionTable
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection; // Import ElementCollection
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn; // Import JoinColumn for @CollectionTable
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.UniqueConstraint;

import com.github.chipolaris.bootforum2.enumeration.AccountStatus;
import com.github.chipolaris.bootforum2.enumeration.UserRole;

@Entity
@Table(name="USER_T",
		uniqueConstraints= {@UniqueConstraint(columnNames="USER_NAME", name="UNIQ_USER_USER_NAME")})
@TableGenerator(name="UserIdGenerator", table="ENTITY_ID_T", pkColumnName="GEN_KEY",
		pkColumnValue="USER_ID", valueColumnName="GEN_VALUE", initialValue = 1000, allocationSize=10)
public class User extends BaseEntity {

	public static User newUser() {
		User user = new User();

		user.userRoles = new HashSet<>(); // Initialize as HashSet
		user.userRoles.add(UserRole.USER); // default role
		user.setAccountStatus(AccountStatus.ACTIVE); // default

		user.setPerson(new Person());
		user.setPreferences(new Preferences());
		user.setSecurityChallenges(new ArrayList<>());
		user.setStat(new UserStat());
		user.getStat().setLastComment(new CommentInfo());
		user.getStat().setLastDiscussion(new DiscussionInfo());

		return user;
	}

	@PrePersist
	public void prePersist() {
		this.setCreateDate(LocalDateTime.now());
		// updateDate will be set by BaseEntity's preUpdate if it exists, or manually here if needed
		if (this.getUpdateDate() == null) {
			this.setUpdateDate(LocalDateTime.now());
		}
	}

	@PreUpdate
	public void preUpdate() {
		this.setUpdateDate(LocalDateTime.now());
	}

	@Id
	@GeneratedValue(strategy=GenerationType.TABLE, generator="UserIdGenerator")
	private Long id;

	@Column(name="USER_NAME", length=50, nullable=false, unique=true)
	private String username;

	@Column(name="PASSWORD", length=200, nullable=false)
	private String password;

	// Changed from single UserRole to a Set of UserRoles
	@ElementCollection(targetClass = UserRole.class, fetch = FetchType.EAGER)
	@CollectionTable(name = "USER_ROLES_T", joinColumns = @JoinColumn(name = "USER_ID", foreignKey = @ForeignKey(name="FK_USER_ROLES_USER")))
	@Enumerated(EnumType.STRING)
	@Column(name = "USER_ROLE", length=50, nullable = false)
	private Set<UserRole> userRoles; // Renamed for clarity (plural)

	@OneToOne(fetch=FetchType.EAGER, cascade=CascadeType.ALL)
	@JoinColumn(name="PERSON_ID", foreignKey = @ForeignKey(name="FK_USER_PERS"))
	private Person person;

	@OneToOne(fetch=FetchType.EAGER, cascade=CascadeType.ALL)
	@JoinColumn(name="PREFERENCE_ID", foreignKey = @ForeignKey(name="FK_USER_PREF"))
	private Preferences preferences;

	@Enumerated(EnumType.STRING)
	@Column(name="ACCOUNT_STATUS", length=50)
	private AccountStatus accountStatus;

	@OneToMany(mappedBy="user", cascade=CascadeType.ALL)
	@OrderBy("id ASC")
	private List<SecurityChallenge> securityChallenges;

	@OneToOne(fetch=FetchType.EAGER, cascade=CascadeType.ALL)
	@JoinColumn(name="USER_STAT_ID", foreignKey = @ForeignKey(name="FK_USER_USER_STAT"))
	private UserStat stat;

	@Override
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

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

	// Getter and Setter for the collection of roles
	public Set<UserRole> getUserRoles() {
		return userRoles;
	}
	public void setUserRoles(Set<UserRole> userRoles) {
		this.userRoles = userRoles;
	}

	// Convenience method to add a single role
	public void addUserRole(UserRole role) {
		if (this.userRoles == null) {
			this.userRoles = new HashSet<>();
		}
		this.userRoles.add(role);
	}

	// Convenience method to remove a single role
	public void removeUserRole(UserRole role) {
		if (this.userRoles != null) {
			this.userRoles.remove(role);
		}
	}


	public Person getPerson() {
		return person;
	}
	public void setPerson(Person person) {
		this.person = person;
	}

	public Preferences getPreferences() {
		return preferences;
	}
	public void setPreferences(Preferences preferences) {
		this.preferences = preferences;
	}

	public AccountStatus getAccountStatus() {
		return accountStatus;
	}
	public void setAccountStatus(AccountStatus accountStatus) {
		this.accountStatus = accountStatus;
	}

	public List<SecurityChallenge> getSecurityChallenges() {
		return securityChallenges;
	}
	public void setSecurityChallenges(List<SecurityChallenge> securityChallenges) {
		this.securityChallenges = securityChallenges;
	}

	public UserStat getStat() {
		return stat;
	}
	public void setStat(UserStat stat) {
		this.stat = stat;
	}
}