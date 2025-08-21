package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.ForumSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForumSettingRepository extends JpaRepository<ForumSetting, Long> {
    Optional<ForumSetting> findByCategoryAndKeyName(String category, String keyName);
}
