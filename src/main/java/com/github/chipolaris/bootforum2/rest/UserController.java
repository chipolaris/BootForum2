package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {
	
	@Autowired
	private UserService userService;

	//@GetMapping("/profile")
	@RequestMapping(path = "/profile", method = {RequestMethod.GET, RequestMethod.OPTIONS})
	public ResponseEntity<User> getUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
		// @AuthenticationPrincipal injects the principal object (UserDetails by default)
		if (userDetails == null) {
			// Should not happen if endpoint is secured correctly, but good practice
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		// Return the UserDetails object or map it to a DTO
		// Avoid sending sensitive info like passwords!
		User user = userService.getUser(userDetails.getUsername()).getDataObject();
		user.setPassword(null);
		return ResponseEntity.ok(user);
	}
}
