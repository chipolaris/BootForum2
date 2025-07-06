package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.Person;
import com.github.chipolaris.bootforum2.dto.PersonDTO;
import com.github.chipolaris.bootforum2.dto.PersonUpdateDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PersonMapper {

    /**
     * Maps a Person entity to a PersonDTO.
     *
     * @param person The source Person entity.
     * @return The mapped PersonDTO.
     */
    PersonDTO toDTO(Person person);

    /**
     * Updates an existing Person entity from a PersonDTO.
     *
     * @param personDTO The source PersonDTO with new data.
     * @param person    The target Person entity to be updated.
     */
    void updatePersonFromDTO(PersonUpdateDTO personDTO, @MappingTarget Person person);
}