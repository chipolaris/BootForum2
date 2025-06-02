package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.ForumGroup;
import com.github.chipolaris.bootforum2.dto.ForumGroupCreateDTO;
import com.github.chipolaris.bootforum2.dto.ForumGroupDTO;
import com.github.chipolaris.bootforum2.dto.ForumGroupUpdateDTO;
import com.github.chipolaris.bootforum2.dto.ForumTreeTableDTO;
import com.github.chipolaris.bootforum2.event.ForumGroupCreatedEvent;
import com.github.chipolaris.bootforum2.mapper.ForumGroupMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ForumGroupService {

    private static final Logger logger = LoggerFactory.getLogger(ForumGroupService.class);

    private final ForumGroupMapper forumGroupMapper;
    private final DynamicDAO dynamicDAO;
    private final GenericDAO genericDAO;
    private final ApplicationEventPublisher eventPublisher;

    // Note: in Spring version >= 4.3, @AutoWired is implied for beans with single constructor
    public ForumGroupService(GenericDAO genericDAO,  DynamicDAO dynamicDAO, ForumGroupMapper forumGroupMapper,
                             ApplicationEventPublisher eventPublisher) {
        this.genericDAO = genericDAO;
        this.eventPublisher = eventPublisher;
        this.forumGroupMapper = forumGroupMapper;
        this.dynamicDAO = dynamicDAO;
    }

    @Transactional(readOnly=false)
    public ServiceResponse<ForumGroupDTO> createForumGroup(ForumGroupCreateDTO forumCreateDTO) {

        ServiceResponse<ForumGroupDTO> response = new ServiceResponse<>();
        Long parentId = forumCreateDTO.parentGroupId();

        // make sure parent forum group is specified
        if(parentId == null) {
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage("Parent forum group is not specified");
        }
        else {
            // make sure the specified parent forum group exists
            ForumGroup parentForumGroup = genericDAO.find(ForumGroup.class, parentId);

            if(parentForumGroup == null) {
                response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                        .addMessage(String.format("Parent forum group with id %d is not found", parentId));
            }
            else {
                ForumGroup forumGroup = forumGroupMapper.toEntity(forumCreateDTO);
                forumGroup.setParent(parentForumGroup);
                genericDAO.persist(forumGroup);

                response.setAckCode(ServiceResponse.AckCodeType.SUCCESS).setDataObject(forumGroupMapper.toForumGroupDTO(forumGroup))
                        .addMessage("Forum group created successfully");

                eventPublisher.publishEvent(new ForumGroupCreatedEvent(this, forumGroup));

                logger.info("Forum group {} created successfully", forumGroup.getTitle());
            }
        }

        return response;
    }

    @Transactional(readOnly = false)
    public ServiceResponse<ForumGroupDTO> updateForumGroup(ForumGroupUpdateDTO forumGroupUpdateDTO) {

        ServiceResponse<ForumGroupDTO> response = new ServiceResponse<>();

        ForumGroup forumGroup = genericDAO.find(ForumGroup.class, forumGroupUpdateDTO.id());

        if(forumGroup == null) {
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage(String.format("Forum group with id %d is not found", forumGroupUpdateDTO.id()));
        }
        else {
            forumGroupMapper.mergeDTOToEntity(forumGroupUpdateDTO, forumGroup);

            forumGroup = genericDAO.merge(forumGroup);

            response.setAckCode(ServiceResponse.AckCodeType.SUCCESS).setDataObject(forumGroupMapper.toForumGroupDTO(forumGroup))
                    .addMessage("Forum group updated successfully");

            logger.info("Forum group {} updated successfully", forumGroup.getTitle());
        }
        return response;
    }

    @Transactional(readOnly = true)
    public ServiceResponse<ForumGroupDTO> getForumGroup(Long id) {

        ServiceResponse<ForumGroupDTO> response = new ServiceResponse<>();

        ForumGroup forumGroup = genericDAO.find(ForumGroup.class, id);

        if(forumGroup == null) {
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE).
                    addMessage(String.format("Forum group with id %d is not found", id));
        }
        else {
            response.setAckCode(ServiceResponse.AckCodeType.SUCCESS).setDataObject(forumGroupMapper.toForumGroupDTO(forumGroup)).
                    addMessage("Forum group fetched successfully");
        }

        return response;
    }

    @Transactional(readOnly = true)
    public ServiceResponse<ForumGroupDTO> getRootForumGroup() {

        ServiceResponse<ForumGroupDTO> response = new ServiceResponse<>();

        QuerySpec rooForumGroupQuery = QuerySpec.builder(ForumGroup.class).filter(FilterSpec.isNull("parent")).build();
        ForumGroup rootForumGroup = dynamicDAO.<ForumGroup>findOptional(rooForumGroupQuery).orElse(null);

        if(rootForumGroup == null) {
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE).
                    addMessage("No root forum group found");
        }
        else {
            response.setAckCode(ServiceResponse.AckCodeType.SUCCESS)
                    .setDataObject(forumGroupMapper.toForumGroupDTO(rootForumGroup))
                    .addMessage("Successfully retrieved root forum group");
        }

        return response;
    }

    @Transactional(readOnly = true)
    public ServiceResponse<ForumTreeTableDTO> getForumTreeTable() {

        ServiceResponse<ForumTreeTableDTO> response = new ServiceResponse<>();

        QuerySpec rooForumGroupQuery = QuerySpec.builder(ForumGroup.class).filter(FilterSpec.isNull("parent")).build();
        ForumGroup rootForumGroup = dynamicDAO.<ForumGroup>findOptional(rooForumGroupQuery).orElse(null);

        if(rootForumGroup == null) {
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE).
                    addMessage("No root forum group found");
        }
        else {
            response.setAckCode(ServiceResponse.AckCodeType.SUCCESS)
                    .setDataObject(forumGroupMapper.toForumTreeTableDTO(rootForumGroup))
                    .addMessage("Successfully retrieved forum tree table");
        }

        return response;
    }
}
