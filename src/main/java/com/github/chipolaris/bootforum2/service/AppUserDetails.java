package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.enumeration.AccountStatus;
import com.github.chipolaris.bootforum2.enumeration.UserRole; // Import UserRole
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority; // Import SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set; // Import Set
import java.util.stream.Collectors; // Import Collectors

public class AppUserDetails implements UserDetails {

	private static final String ROLE_PREFIX = "ROLE_";
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	protected AppUserDetails(User appUser) {
		this.appUser = appUser;
	}

	private User appUser;

	public User getUser() {
		return appUser;
	}

	public void setUser(User appUser) {
		this.appUser = appUser;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {

		Set<UserRole> roles = this.appUser.getUserRoles();
		if (roles == null || roles.isEmpty()) {
			return new ArrayList<>(); // Return empty list if no roles
		}

		// Map each UserRole to a SimpleGrantedAuthority with the "ROLE_" prefix
		return roles.stream()
				.map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toString()))
				.collect(Collectors.toList());
	}

	@Override
	public String getPassword() {
		return this.appUser.getPassword();
	}

	@Override
	public String getUsername() {
		return this.appUser.getUsername();
	}

	@Override
	public boolean isAccountNonExpired() {

		return true;
	}

	@Override
	public boolean isAccountNonLocked() {

		return appUser.getAccountStatus() == AccountStatus.ACTIVE;
	}

	@Override
	public boolean isCredentialsNonExpired() {

		return true;
	}

	@Override
	public boolean isEnabled() {

		return true;
	}
}