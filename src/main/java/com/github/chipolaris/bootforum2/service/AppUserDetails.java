package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.domain.User.AccountStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

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
		
		Collection<GrantedAuthority> grantedAuthorities =
			new ArrayList<GrantedAuthority>();
		
		grantedAuthorities.add(new GrantedAuthority() {
			
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public String getAuthority() {
				return AppUserDetails.ROLE_PREFIX + 
					AppUserDetails.this.appUser.getRole().toString();
			}
		});
		
		return grantedAuthorities;
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
