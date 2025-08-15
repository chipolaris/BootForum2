package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.Comment;
import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.domain.Forum;
import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.dto.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    /**
     * Finds the most recent comment for a given discussion.
     *
     * @param discussion The discussion in which to find the latest comment.
     * @return an Optional containing the latest Comment in the discussion, or empty if the discussion has no comments.
     */
    Optional<Comment> findTopByDiscussionOrderByCreateDateDesc(Discussion discussion);

    /**
     * Finds the most recent comment for a given forum by traversing through the discussion.
     *
     * @param forum The forum in which to find the latest comment.
     * @return an Optional containing the latest Comment in the forum, or empty if the forum has no comments.
     */
    @Query("SELECT c FROM Comment c WHERE c.discussion.forum = :forum ORDER BY c.createDate DESC LIMIT 1")
    Optional<Comment> findLatestByForum(@Param("forum") Forum forum);

    /**
     * Efficiently counts the number of comments associated with a given discussion.
     *
     * @param discussion The discussion entity to count comments for.
     * @return A long representing the total number of comments.
     */
    long countByDiscussion(Discussion discussion);

    /**
     * Efficiently counts the number of comments associated with a given forum
     * by traversing through the discussion.
     *
     * @param forum The forum entity to count comments for.
     * @return A long representing the total number of comments in that forum.
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.discussion.forum = :forum")
    long countByForum(@Param("forum") Forum forum);

    /**
     * Counts the total number of attachments across all comments in a given discussion.
     *
     * @param discussion The discussion to query against.
     * @return The total count of attachments.
     */
    @Query("SELECT COUNT(a) FROM Comment c JOIN c.attachments a WHERE c.discussion = :discussion")
    long countAttachmentsByDiscussion(@Param("discussion") Discussion discussion);

    /**
     * Counts the total number of images across all comments in a given discussion.
     *
     * @param discussion The discussion to query against.
     * @return The total count of images.
     */
    @Query("SELECT COUNT(i) FROM Comment c JOIN c.images i WHERE c.discussion = :discussion")
    long countImagesByDiscussion(@Param("discussion") Discussion discussion);

    /**
     * Private query method to fetch comment counts per user for a specific discussion.
     * This uses a DTO projection for type-safe and efficient data retrieval.
     *
     * @param discussion The discussion to query against.
     * @return A list of DTOs, each containing a user and their comment count.
     */
    @Query("""
            SELECT new com.github.chipolaris.bootforum2.dto.CommentorCountDTO(c.createBy, COUNT(c))
            FROM Comment c
            WHERE c.discussion = :discussion
            GROUP BY c.createBy
            """)
    List<CommentorCountDTO> findCommentorCounts(@Param("discussion") Discussion discussion);

    /**
     * Default method to provide a map of commentators and their comment counts for a given discussion.
     * This encapsulates the logic of fetching and transforming the data.
     *
     * @param discussion The discussion for which to retrieve the commentor map.
     * @return A Map where the key is the commentator's username and the value is their comment count.
     */
    default Map<String, Integer> getCommentorMap(Discussion discussion) {
        return findCommentorCounts(discussion).stream()
                .collect(Collectors.toMap(
                        CommentorCountDTO::commentor,
                        dto -> dto.count().intValue()
                ));
    }

    /**
     *
     * @param username
     * @param pageable
     * @return
     */
    @Query("""
            SELECT new com.github.chipolaris.bootforum2.dto.MyRecentCommentDTO(
                c.id, c.title, c.createDate, c.discussion.id, c.discussion.title)
            FROM Comment c WHERE c.createBy = :username
            """)
    List<MyRecentCommentDTO> findRecentCommentsForUser(@Param("username") String username, Pageable pageable);

    /**
     *
     * @param username
     * @param pageable
     * @return
     */
    @Query("""
            SELECT new com.github.chipolaris.bootforum2.dto.ReplyToMyCommentDTO(
                r.id, r.title, r.createDate, r.createBy, p.id, p.title, r.discussion.id, r.discussion.title)
            FROM Comment r JOIN r.replyTo p WHERE p.createBy = :username
            """)
    List<ReplyToMyCommentDTO> findRepliesToUserComments(@Param("username") String username, Pageable pageable);

    /**
     *
     * @param username
     * @param pageable
     * @return
     */
    @Query("""
            SELECT new com.github.chipolaris.bootforum2.dto.MyLikedCommentDTO(
                c.id, c.title, c.createBy, v.createDate, c.discussion.id, c.discussion.title)
            FROM Comment c JOIN c.commentVote.votes v WHERE v.voterName = :username AND v.voteValue > 0
            """)
    List<MyLikedCommentDTO> findLikedCommentsByUser(@Param("username") String username, Pageable pageable);

    /**
     *
     * @param username
     * @return
     */
    long countByCreateBy(String username);

    /**
     *
     * @param username
     * @return
     */
    @Query("SELECT SUM(c.commentVote.voteUpCount) FROM Comment c WHERE c.createBy = :username")
    Long sumVoteUpCountByCreateBy(@Param("username") String username);

    /**
     *
     * @param username
     * @return
     */
    @Query("SELECT SUM(c.commentVote.voteDownCount) FROM Comment c WHERE c.createBy = :username")
    Long sumVoteDownCountByCreateBy(@Param("username") String username);

    @Query("SELECT new com.github.chipolaris.bootforum2.dto.RankedCommentDTO(c.id, c.title, CAST(c.commentVote.voteUpCount AS long), c.discussion.id, c.discussion.title) FROM Comment c WHERE c.createBy = :username")
    List<RankedCommentDTO> findMostLikedCommentsForUser(@Param("username") String username, Pageable pageable);

    @Query("SELECT new com.github.chipolaris.bootforum2.dto.RankedCommentDTO(c.id, c.title, CAST(c.commentVote.voteDownCount AS long), c.discussion.id, c.discussion.title) FROM Comment c WHERE c.createBy = :username")
    List<RankedCommentDTO> findMostDislikedCommentsForUser(@Param("username") String username, Pageable pageable);
}