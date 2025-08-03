package com.github.chipolaris.bootforum2.dto;

/**
 * A Data Transfer Object to hold the result of an aggregation query
 * for counting comments by each commentator.
 *
 * @param commentor The username of the person who commented.
 * @param count The total number of comments by that user.
 */
public record CommentorCountDTO(String commentor, Long count) {
}