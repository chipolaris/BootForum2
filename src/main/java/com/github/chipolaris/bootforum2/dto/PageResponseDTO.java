package com.github.chipolaris.bootforum2.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponseDTO<T>(List<T> content,
                                 int number,
                                 int size,
                                 long totalElements,
                                 int totalPages,
                                 boolean last,
                                 boolean first,
                                 int numberOfElements,
                                 boolean empty) {
    /*
     * This method provides an alternative to a dedicate StructMapper object mapper
     */
    public static <T> PageResponseDTO<T> from(Page<T> page) {
        return new PageResponseDTO<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst(),
                page.getNumberOfElements(),
                page.isEmpty()
        );
    }
}
