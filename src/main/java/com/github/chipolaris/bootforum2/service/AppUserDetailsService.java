package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note, Spring Boot will detect the existence of this bean (UserDetailsService)
 * and automatically use it.
 * E.g., no need to explicitly do the following in the app or security config:
 
  	@Resource(name = "appUserDetailsService")
	private UserDetailsService userDetailService;

	@Resource
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {

		auth.userDetailsService(userDetailService).passwordEncoder(passwordEncoder);
	}
 */
@Service
@Transactional
public class AppUserDetailsService implements UserDetailsService {

	@Autowired
	private DynamicDAO dynamicDAO;
	
	@Override
	@Transactional(readOnly=true)
	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException {

		QuerySpec querySpec = QuerySpec.builder(User.class).filter(FilterSpec.eq("username",username)).build();

		User user = dynamicDAO.<User>findOptional(querySpec).orElse(null);
		
		if(user == null) {
			throw new UsernameNotFoundException("Can't find username: " + username);
		}
		
		return new AppUserDetails(user);
	}
}