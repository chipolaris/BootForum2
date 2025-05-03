package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    boolean existsByEmail(String email);

    boolean existsByFirstNameAndLastName(String admin, String user);
}
