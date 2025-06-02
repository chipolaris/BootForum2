package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.service.ServiceResponse.AckCodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class UserService {

	private static final Logger logger = LoggerFactory.getLogger(UserService.class);

	private final PasswordEncoder passwordEncoder;
	private final GenericDAO genericDAO;
	private final DynamicDAO dynamicDAO;
	private final ApplicationEventPublisher eventPublisher;

	// Note: in Spring version >= 4.3, @AutoWired is implied for beans with single constructor
	public UserService(GenericDAO genericDAO, DynamicDAO dynamicDAO, PasswordEncoder passwordEncoder,
					   ApplicationEventPublisher eventPublisher) {
		this.genericDAO = genericDAO;
		this.dynamicDAO = dynamicDAO;
		this.passwordEncoder = passwordEncoder;
		this.eventPublisher = eventPublisher;
	}

	@Transactional(readOnly = true)
	public ServiceResponse<User> getUser(String username) {
		
		ServiceResponse<User> response = new ServiceResponse<>();

		QuerySpec userNameQuery = QuerySpec.builder(User.class).filter(FilterSpec.eq("username", username)).build();
		User user =	dynamicDAO.<User>findOptional(userNameQuery).orElse(null);

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
