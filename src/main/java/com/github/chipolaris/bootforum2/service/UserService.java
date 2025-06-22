package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.dto.UserDTO;
import com.github.chipolaris.bootforum2.mapper.UserMapper;
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
	private final UserMapper userMapper;
	private final ApplicationEventPublisher eventPublisher;

	// Note: in Spring version >= 4.3, @AutoWired is implied for beans with single constructor
	public UserService(GenericDAO genericDAO, DynamicDAO dynamicDAO, PasswordEncoder passwordEncoder,
					   UserMapper userMapper, ApplicationEventPublisher eventPublisher) {
		this.genericDAO = genericDAO;
		this.dynamicDAO = dynamicDAO;
		this.passwordEncoder = passwordEncoder;
		this.userMapper = userMapper;
		this.eventPublisher = eventPublisher;
	}

	@Transactional(readOnly = true)
	public ServiceResponse<UserDTO> getUser(String username) {

		QuerySpec userNameQuery = QuerySpec.builder(User.class).filter(FilterSpec.eq("username", username)).build();
		User user =	dynamicDAO.<User>findOptional(userNameQuery).orElse(null);

		if(user == null) {
			return ServiceResponse.failure("User not found");
		}
		else {
			return ServiceResponse.success("Found user %s".formatted(username), userMapper.toDTO(user));
		}
	}
}
