package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.service.ServiceResponse.AckCodeType;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service @Transactional
public class UserService {
	
	//@Resource
	private PasswordEncoder passwordEncoder;
	
	@Resource
	private GenericDAO genericDAO;

	@Transactional(readOnly = true)
	public ServiceResponse<User> getUser(String username) {
		
		ServiceResponse<User> response = new ServiceResponse<>();
		
		User user = 
			genericDAO.findOne(User.class, Collections.singletonMap("username", username));
		
		response.setDataObject(user);
		
		return response;
	}
	
	@Transactional(readOnly = false)
	public ServiceResponse<User> addUser(User user) {
	
		ServiceResponse<User> response = new ServiceResponse<>();
		
		try {
			user.setPassword(passwordEncoder.encode(user.getPassword()));
			genericDAO.persist(user);
			response.setDataObject(user);
		}
		catch(Exception e) {
			response.setAckCode(AckCodeType.FAILURE);
			response.addMessage("Exception " + e.toString());
		}
		
		return response;
	}
}
