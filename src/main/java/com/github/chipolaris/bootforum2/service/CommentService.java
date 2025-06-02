package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.OrderSpec;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.Comment;
import com.github.chipolaris.bootforum2.dto.CommentDTO;
import com.github.chipolaris.bootforum2.dto.PageResponseDTO;
import com.github.chipolaris.bootforum2.mapper.CommentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    private final DynamicDAO dynamicDAO;
    private final CommentMapper commentMapper;

    public CommentService(DynamicDAO dynamicDAO, CommentMapper commentMapper) {
        this.dynamicDAO = dynamicDAO;
        this.commentMapper = commentMapper;
    }

    @Transactional(readOnly = true)
    public ServiceResponse<PageResponseDTO<CommentDTO>> findPaginatedComments(
            Long discussionId, Pageable pageable) {

        ServiceResponse<PageResponseDTO<CommentDTO>> response = new ServiceResponse<>();

        if (discussionId == null) {
            logger.warn("Attempted to fetch comments with null discussionId.");
            return response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage("Discussion ID cannot be null.");
        }

        try {
            // Count query for total elements
            QuerySpec countQuerySpec = QuerySpec.builder(Comment.class)
                    .filter(FilterSpec.eq("discussion.id", discussionId))
                    .build();
            long totalElements = dynamicDAO.count(countQuerySpec);

            // Data query with pagination and sorting
            int page = pageable.getPageNumber();
            int size = pageable.getPageSize();

            List<OrderSpec> orderSpecs = pageable.getSort().stream()
                    .map(order -> order.getDirection().isAscending() ?
                            OrderSpec.asc(order.getProperty()) : OrderSpec.desc(order.getProperty()))
                    .collect(Collectors.toList());

            // If no sort is provided by pageable, default to createDate ASC (as per controller's @PageableDefault)
            // However, @PageableDefault in controller handles this, so orderSpecs will reflect it.
            // If orderSpecs is empty and a default is strictly needed here, it could be added.
            // For now, relying on Pageable to carry the sort info.

            QuerySpec dataQuerySpec = QuerySpec.builder(Comment.class)
                    .filter(FilterSpec.eq("discussion.id", discussionId))
                    .startIndex(page * size)
                    .maxResult(size)
                    .orders(orderSpecs)
                    .build();

            List<Comment> comments = dynamicDAO.find(dataQuerySpec);

            List<CommentDTO> commentDTOs = comments.stream()
                    .map(commentMapper::toDTO)
                    .collect(Collectors.toList());

            Page<CommentDTO> pageResult = new PageImpl<>(commentDTOs, pageable, totalElements);

            response.setDataObject(PageResponseDTO.from(pageResult))
                    .addMessage(String.format("Fetched comments for discussion ID: %d", discussionId));

        } catch (Exception e) {
            logger.error(String.format("Error fetching comments for discussion ID %d: ", discussionId), e);
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage("An unexpected error occurred while fetching comments.");
        }

        return response;
    }
}