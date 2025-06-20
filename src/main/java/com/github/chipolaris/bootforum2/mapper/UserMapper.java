package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.dto.UserDTO;
import com.github.chipolaris.bootforum2.dto.UserRegisteredDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDTO(User user);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "email", source = "person.email")
    UserRegisteredDTO toUserRegisteredDTO(User user);
}