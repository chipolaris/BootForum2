package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.dto.UserDTO;
import com.github.chipolaris.bootforum2.dto.UserRegisteredDTO;
import com.github.chipolaris.bootforum2.dto.UserSummaryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDTO(User user);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "email", source = "person.email")
    UserRegisteredDTO toUserRegisteredDTO(User user);

    @Mapping(target = "firstName", source = "user.person.firstName")
    @Mapping(target = "lastName", source = "user.person.lastName")
    @Mapping(target = "roles", expression = "java(user.getUserRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()))")
    @Mapping(target = "accountStatus", expression = "java(user.getAccountStatus().name())")
    @Mapping(target = "lastLogin", source = "user.stat.lastLogin")
    UserSummaryDTO toUserSummaryDTO(User user);
}