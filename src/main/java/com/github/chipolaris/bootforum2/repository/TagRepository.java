package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * Finds all tags and orders them by the sortOrder field.
     * @return A list of sorted tags.
     */
    List<Tag> findAllByOrderBySortOrderAsc();
}