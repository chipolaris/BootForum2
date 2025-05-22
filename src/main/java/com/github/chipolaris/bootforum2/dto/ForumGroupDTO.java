package com.github.chipolaris.bootforum2.dto;

import com.github.chipolaris.bootforum2.domain.ForumGroup;

import java.util.ArrayList;
import java.util.List;

public record ForumGroupDTO(Long id, String title, String icon, String iconColor, Long parentId,
                             List<ForumDTO> forums, List<ForumGroupDTO> subGroups) {
}
