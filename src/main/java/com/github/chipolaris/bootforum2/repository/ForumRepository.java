package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.Forum;
import com.github.chipolaris.bootforum2.dto.RankedListItemDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ForumRepository extends JpaRepository<Forum, Long> {

    @Query("""
            SELECT new com.github.chipolaris.bootforum2.dto.RankedListItemDTO(f.id, f.title, f.forumGroup.title, SUM(d.stat.viewCount))
            FROM Forum f JOIN f.discussions d
            WHERE d.createDate >= :since
            GROUP BY f.id, f.title, f.forumGroup.title
            ORDER BY SUM(d.stat.viewCount) DESC
            """)
    List<RankedListItemDTO> findTopForumsByViews(@Param("since") LocalDateTime since, Pageable pageable);

    @Query("""
            SELECT new com.github.chipolaris.bootforum2.dto.RankedListItemDTO(f.id, f.title, f.forumGroup.title, f.stat.commentCount)
            FROM Forum f
            WHERE f.stat.lastComment.commentDate >= :since
            ORDER BY f.stat.commentCount DESC
            """)
    List<RankedListItemDTO> findTopForumsByComments(@Param("since") LocalDateTime since, Pageable pageable);
}