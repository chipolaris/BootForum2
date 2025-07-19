package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Find latest 10 comments made by username
     * @param username
     * @return
     */
    List<Comment> findTop10ByCreateByOrderByCreateDateDesc(String username);

    /**
     * Finds the most recent comment entity based on its creation date, eagerly fetching
     * the associated discussion to prevent LazyInitializationException.
     *
     * @return an Optional containing the latest Comment entity, or empty if no comments exist.
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.discussion ORDER BY c.createDate DESC LIMIT 1")
    Optional<Comment> findLatestCommentWithDiscussion();

    /**
     * Finds the most recent comment entity based on its creation date.
     * This is the preferred way to get the latest comment to avoid DB-specific LOB function issues.
     *
     * @return an Optional containing the latest Comment entity, or empty if no comments exist.
     */
    Optional<Comment> findTopByOrderByCreateDateDesc();
}