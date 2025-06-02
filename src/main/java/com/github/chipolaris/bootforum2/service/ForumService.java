package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.domain.Forum;
import com.github.chipolaris.bootforum2.domain.ForumGroup;
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

        ServiceResponse<ForumDTO> response = new ServiceResponse<>();
        Long forumGroupId = forumCreateDTO.parentGroupId();

        // make sure forum group is specified
        if(forumGroupId == null) {
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage("Forum group is not specified");
        }
        else {
            // make sure the specified forum group exists
            ForumGroup forumGroup = genericDAO.find(ForumGroup.class, forumGroupId);
            if(forumGroup == null) {
                response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                        .addMessage(String.format("Forum group with id %d is not found", forumGroupId));
            }
            else {
                Forum forum = forumMapper.toEntity(forumCreateDTO);
                forum.setForumGroup(forumGroup);
                genericDAO.persist(forum);

                response.setAckCode(ServiceResponse.AckCodeType.SUCCESS).setDataObject(forumMapper.toForumDTO(forum))
                        .addMessage("Forum created successfully");

                eventPublisher.publishEvent(new ForumCreatedEvent(this, forum));

                logger.info("Forum {} created successfully", forum.getTitle());
            }
        }

        return response;
    }

    @Transactional(readOnly = false)
    public ServiceResponse<ForumDTO> updateForum(ForumUpdateDTO forumUpdateDTO) {

        ServiceResponse<ForumDTO> response = new ServiceResponse<>();

        Forum forum = genericDAO.find(Forum.class, forumUpdateDTO.id());

        if(forum == null) {
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage(String.format("Forum with id %d is not found",forumUpdateDTO.id()));
        }
        else {
            forumMapper.mergeIntoEntity(forumUpdateDTO, forum);

            forum = genericDAO.merge(forum);

            response.setAckCode(ServiceResponse.AckCodeType.SUCCESS).setDataObject(forumMapper.toForumDTO(forum))
                    .addMessage("Forum updated successfully");

            logger.info("Forum {} updated successfully", forum.getTitle());
        }
        return response;
    }

    @Transactional(readOnly = true)
    public ServiceResponse<ForumDTO> getForum(Long id) {

        ServiceResponse<ForumDTO> response = new ServiceResponse<>();

        Forum forum = genericDAO.find(Forum.class, id);

        if(forum == null) {
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage(String.format("Forum with id %d is not found", id));
        }
        else {
            response.setAckCode(ServiceResponse.AckCodeType.SUCCESS).setDataObject(forumMapper.toForumDTO(forum))
                    .addMessage("Forum fetched successfully");
        }

        return response;
    }

    @Transactional(readOnly = true)
    public ServiceResponse<List<ForumDTO>> getAllForums() {

        ServiceResponse<List<ForumDTO>> response = new ServiceResponse<>();

        List<Forum> forums = genericDAO.all(Forum.class);
        List<ForumDTO> forumDTOs = forums.stream().map(forumMapper::toForumDTO).toList();

        response.setAckCode(ServiceResponse.AckCodeType.SUCCESS).
                setDataObject(forumDTOs).addMessage("Forums fetched successfully");

        return response;
    }

    @Transactional(readOnly = true)
    public ServiceResponse<ForumViewDTO> getForumView(long id) {
        ServiceResponse<ForumViewDTO> response = new ServiceResponse<>();

        Forum forum = genericDAO.find(Forum.class, id);

        if(forum == null) {
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage(String.format("Forum with id %d is not found", id));
        }
        else {
            response.setAckCode(ServiceResponse.AckCodeType.SUCCESS)
                    .setDataObject(forumMapper.toForumViewDTO(forum)).
                    addMessage("Forum fetched successfully");
        }

        return response;
    }
}
