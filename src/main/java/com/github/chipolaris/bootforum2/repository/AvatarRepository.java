package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.Avatar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AvatarRepository extends JpaRepository<Avatar, Long> {

    /**
     * Finds an Avatar entity by the associated username.
     * @param userName The username to search for.
     * @return an Optional containing the Avatar if found, otherwise empty.
     */
    Optional<Avatar> findByUserName(String userName);
}