package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.service.ServiceResponse.AckCodeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class UserService {
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private DynamicDAO dynamicDAO;

	@Transactional(readOnly = true)
	public ServiceResponse<User> getUser(String username) {
		
		ServiceResponse<User> response = new ServiceResponse<>();

		QuerySpec userNameQuery = QuerySpec.builder(User.class).filter(FilterSpec.eq("username", username)).build();
		User user =	dynamicDAO.<User>findOptional(userNameQuery).orElse(null);
			//genericDAO.findOne(User.class, Collections.singletonMap("username", username));

		if(user == null) {
			response.setAckCode(AckCodeType.FAILURE).addMessage("User not found");
		}
		else {
			response.setDataObject(user).addMessage(String.format("User found", username));;
		}

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
