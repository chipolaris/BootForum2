package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.domain.User;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

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

	@Resource
	private GenericDAO genericDAO;
	
	@Override
	@Transactional(readOnly=true)
	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException {
		
		User user = genericDAO.findOne(User.class, Collections.singletonMap("username", username));
		
		if(user == null) {
			throw new UsernameNotFoundException("Can't find username: " + username);
		}
		
		return new AppUserDetails(user);
	}
}