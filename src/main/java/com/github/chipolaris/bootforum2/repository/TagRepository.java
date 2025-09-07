package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.github.chipolaris.bootforum2.dto.RankedListItemDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * Finds all tags and orders them by the sortOrder field.
     * @return A list of sorted tags.
     */
    List<Tag> findAllByOrderBySortOrderAsc();

    @Query("""
            SELECT new com.github.chipolaris.bootforum2.dto.RankedListItemDTO(t.id, t.label, '', SUM(d.stat.viewCount))
            FROM Tag t JOIN t.discussions d
            WHERE d.createDate >= :since
            GROUP BY t.id, t.label
            ORDER BY SUM(d.stat.viewCount) DESC
            """)
    List<RankedListItemDTO> findTopTagsByViews(@Param("since") LocalDateTime since, Pageable pageable);

    @Query("""
            SELECT new com.github.chipolaris.bootforum2.dto.RankedListItemDTO(t.id, t.label, '', SUM(d.stat.commentCount))
            FROM Tag t JOIN t.discussions d
            WHERE d.createDate >= :since
            GROUP BY t.id, t.label
            ORDER BY SUM(d.stat.commentCount) DESC
            """)
    List<RankedListItemDTO> findTopTagsByComments(@Param("since") LocalDateTime since, Pageable pageable);
}