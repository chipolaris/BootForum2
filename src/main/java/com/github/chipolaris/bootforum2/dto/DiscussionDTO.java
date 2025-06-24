package com.github.chipolaris.bootforum2.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DiscussionDTO(Long id, LocalDateTime createDate, String createBy, String title, String content,
                            List<FileInfoDTO> attachments, List<FileInfoDTO> images,
                            List<TagDTO> tags, DiscussionStatDTO stat) {
}
