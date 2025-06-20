package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.domain.*;
import com.github.chipolaris.bootforum2.dto.ForumCreateDTO;
import com.github.chipolaris.bootforum2.dto.ForumDTO;
import com.github.chipolaris.bootforum2.dto.ForumUpdateDTO;
import com.github.chipolaris.bootforum2.dto.ForumViewDTO;
import com.github.chipolaris.bootforum2.event.ForumCreatedEvent;
import com.github.chipolaris.bootforum2.mapper.ForumMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ForumService {

    private static final Logger logger = LoggerFactory.getLogger(ForumService.class);

    private final ForumMapper forumMapper;
    private final DynamicDAO dynamicDAO;
    private final GenericDAO genericDAO;
    private final ApplicationEventPublisher eventPublisher;

    // Note: in Spring version >= 4.3, @AutoWired is implied for beans with single constructor
    public ForumService(GenericDAO genericDAO, ForumMapper forumMapper, DynamicDAO dynamicDAO,
                        ApplicationEventPublisher eventPublisher) {
        this.genericDAO = genericDAO;
        this.forumMapper = forumMapper;
        this.dynamicDAO = dynamicDAO;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly=false)
    public ServiceResponse<ForumDTO> createForum(ForumCreateDTO forumCreateDTO) {

        Long forumGroupId = forumCreateDTO.parentGroupId();

        // make sure forum group is specified
        if(forumGroupId == null) {
            return ServiceResponse.failure("Forum group is not specified");
        }
        else {
            // make sure the specified forum group exists
            ForumGroup forumGroup = genericDAO.find(ForumGroup.class, forumGroupId);
            if(forumGroup == null) {
                return ServiceResponse.failure("Forum group with id %d is not found".formatted(forumGroupId));
            }
            else {
                try {
                    // create forum
                    Forum forum = Forum.newForum();

                    forumMapper.mergeIntoEntity(forumCreateDTO, forum);

                    forum.setForumGroup(forumGroup);

                    genericDAO.persist(forum);

                    eventPublisher.publishEvent(new ForumCreatedEvent(this, forum));

                    logger.info("Forum {} created successfully", forum.getTitle());
                    return ServiceResponse.success("Forum created successfully", forumMapper.toForumDTO(forum));
                } catch (Exception e) {
                    logger.error("Exception creating forum: %s".formatted(e.getMessage()));
                    return ServiceResponse.failure("Exception creating forum: %s".formatted(e.getMessage()));
                }
            }
        }
    }

    @Transactional(readOnly = false)
    public ServiceResponse<ForumDTO> updateForum(ForumUpdateDTO forumUpdateDTO) {

        ServiceResponse<ForumDTO> response = new ServiceResponse<>();

        Forum forum = genericDAO.find(Forum.class, forumUpdateDTO.id());

        if(forum == null) {
            return ServiceResponse.failure("Forum with id %d is not found".formatted(forumUpdateDTO.id()));
        }
        else {
            forumMapper.mergeIntoEntity(forumUpdateDTO, forum);

            forum = genericDAO.merge(forum);

            logger.info("Forum {} updated successfully", forum.getTitle());

            return ServiceResponse.success("Forum updated successfully", forumMapper.toForumDTO(forum));
        }
    }

    @Transactional(readOnly = true)
    public ServiceResponse<ForumDTO> getForum(Long id) {

        ServiceResponse<ForumDTO> response = new ServiceResponse<>();

        Forum forum = genericDAO.find(Forum.class, id);

        if(forum == null) {
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage("Forum with id %d is not found".formatted(id));
        }
        else {
            response.setAckCode(ServiceResponse.AckCodeType.SUCCESS).setDataObject(forumMapper.toForumDTO(forum))
                    .addMessage("Forum fetched successfully");
        }

        return response;
    }

    @Transactional(readOnly = true)
    public ServiceResponse<List<ForumDTO>> getAllForums() {

        List<Forum> forums = genericDAO.all(Forum.class);
        List<ForumDTO> forumDTOs = forums.stream().map(forumMapper::toForumDTO).toList();

        return ServiceResponse.success("Forums fetched successfully", forumDTOs);
    }

    @Transactional(readOnly = true)
    public ServiceResponse<ForumViewDTO> getForumView(long id) {

        Forum forum = genericDAO.find(Forum.class, id);

        if(forum == null) {
            return ServiceResponse.failure("Forum with id %d is not found".formatted(id));
        }
        else {
            return ServiceResponse.success("Forum fetched successfully", forumMapper.toForumViewDTO(forum));
        }
    }
}
