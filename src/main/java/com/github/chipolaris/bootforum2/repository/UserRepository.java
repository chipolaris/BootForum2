package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.dto.admin.CountPerMonthDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.github.chipolaris.bootforum2.dto.RankedListItemDTO;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

    @Query("""
            SELECT new com.github.chipolaris.bootforum2.dto.RankedListItemDTO(u.id, u.username, u.person.email, COUNT(d.id))
            FROM User u JOIN Discussion d ON u.username = d.createBy
            WHERE d.createDate >= :since
            GROUP BY u.id, u.username, u.person.email
            ORDER BY COUNT(d.id) DESC
            """)
    List<RankedListItemDTO> findTopUsersByDiscussionCount(@Param("since") LocalDateTime since, Pageable pageable);

    @Query("""
            SELECT new com.github.chipolaris.bootforum2.dto.RankedListItemDTO(u.id, u.username, u.person.email, COUNT(c.id))
            FROM User u JOIN Comment c ON u.username = c.createBy
            WHERE c.createDate >= :since
            GROUP BY u.id, u.username, u.person.email
            ORDER BY COUNT(c.id) DESC
            """)
    List<RankedListItemDTO> findTopUsersByCommentCount(@Param("since") LocalDateTime since, Pageable pageable);

    @Query("""
        SELECT new com.github.chipolaris.bootforum2.dto.admin.CountPerMonthDTO(
            YEAR(u.createDate), MONTH(u.createDate), COUNT(u.id))
        FROM User u
        WHERE u.createDate >= :since
        GROUP BY YEAR(u.createDate), MONTH(u.createDate)
        ORDER BY YEAR(u.createDate), MONTH(u.createDate)
        """)
    List<CountPerMonthDTO> countByMonth(@Param("since") LocalDateTime since);


    @Query("""
            SELECT new com.github.chipolaris.bootforum2.dto.RankedListItemDTO(u.id, u.username, u.person.email, u.stat.reputation)
            FROM User u
            ORDER BY u.stat.reputation DESC
            """)
    List<RankedListItemDTO> findTopUsersByReputation(Pageable pageable);
}