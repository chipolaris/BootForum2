package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.ForumGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ForumGroupRepository extends JpaRepository<ForumGroup, Long> {

    Optional<ForumGroup> findFirstByParentIsNull();
}
