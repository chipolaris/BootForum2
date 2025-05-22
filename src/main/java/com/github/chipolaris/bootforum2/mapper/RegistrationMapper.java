package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.Registration;
import com.github.chipolaris.bootforum2.dto.RegistrationDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RegistrationMapper {

    @Mapping(target = "id", ignore = true) // ID is auto-generated
    @Mapping(target = "registrationKey", ignore = true) // Will be set manually in the service
    @Mapping(target = "password", ignore = true) // Will be set manually after encoding in the service
    @Mapping(target = "email", expression = "java(dto.email().toLowerCase())") // Convert email to lowercase
    @Mapping(target = "createDate", ignore = true) // Handled by @PrePersist
    @Mapping(target = "updateDate", ignore = true) // Handled by @PrePersist
    Registration toEntity(RegistrationDTO dto);
}