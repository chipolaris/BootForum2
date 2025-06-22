package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // New method to find latest comments by a user
    List<Comment> findTop10ByCreateByOrderByCreateDateDesc(String createBy);
}