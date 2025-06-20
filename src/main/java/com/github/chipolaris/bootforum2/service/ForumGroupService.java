package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.GenericDAO;
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

        Long parentId = forumCreateDTO.parentGroupId();

        // make sure parent forum group is specified
        if(parentId == null) {
            return ServiceResponse.failure("Parent forum group is not specified");
        }
        else {
            // make sure the specified parent forum group exists
            ForumGroup parentForumGroup = genericDAO.find(ForumGroup.class, parentId);

            if(parentForumGroup == null) {
                return ServiceResponse.failure("Parent forum group with id %d is not found".formatted(parentId));
            }
            else {
                ForumGroup forumGroup = null;
                try {
                    forumGroup = forumGroupMapper.toEntity(forumCreateDTO);
                    forumGroup.setParent(parentForumGroup);
                    genericDAO.persist(forumGroup);

                    eventPublisher.publishEvent(new ForumGroupCreatedEvent(this, forumGroup));

                    logger.info("Forum group {} created successfully", forumGroup.getTitle());
                    return ServiceResponse.success("Forum group with title '%s' created successfully".formatted(forumGroup.getTitle()),
                            forumGroupMapper.toForumGroupDTO(forumGroup));
                } catch (Exception e) {
                    logger.error("Exception creating forum group: %s".formatted(e.getMessage()));
                    return ServiceResponse.failure("Exception creating forum group: %s".formatted(e.getMessage()));
                }
            }
        }
    }

    @Transactional(readOnly = false)
    public ServiceResponse<ForumGroupDTO> updateForumGroup(ForumGroupUpdateDTO forumGroupUpdateDTO) {

        ForumGroup forumGroup = genericDAO.find(ForumGroup.class, forumGroupUpdateDTO.id());

        if(forumGroup == null) {
            logger.warn("Forum group with id {} is not found", forumGroupUpdateDTO.id());

            return ServiceResponse.failure("Forum group with id %d is not found".formatted(forumGroupUpdateDTO.id()));
        }
        else {
            try {
                forumGroupMapper.mergeDTOToEntity(forumGroupUpdateDTO, forumGroup);

                forumGroup = genericDAO.merge(forumGroup);

                logger.info("Forum group {} updated successfully", forumGroup.getTitle());

                return ServiceResponse.success("Forum group with id %d updated successfully".formatted(forumGroup.getId()),
                        forumGroupMapper.toForumGroupDTO(forumGroup));
            } catch (Exception e) {
                logger.error("Exception updating forum group with id %d: %s".formatted(e.getMessage()));
                return ServiceResponse.failure("Exception updating forum group with id %d: %s".formatted(e.getMessage()));
            }
        }
    }

    @Transactional(readOnly = true)
    public ServiceResponse<ForumGroupDTO> getForumGroup(Long id) {

        ForumGroup forumGroup = genericDAO.find(ForumGroup.class, id);

        if(forumGroup == null) {
            return ServiceResponse.failure("Forum group with id %d is not found".formatted(id));
        }
        else {

            try {
                return ServiceResponse.success("Forum group fetched successfully", forumGroupMapper.toForumGroupDTO(forumGroup));
            } catch (Exception e) {
                logger.error("Exception fetching forum group with id %d: %s".formatted(id, e.getMessage()));
                return ServiceResponse.failure("Exception fetching forum group with id %d: %s".formatted(id, e.getMessage()));
            }
        }
    }

    @Transactional(readOnly = true)
    public ServiceResponse<ForumGroupDTO> getRootForumGroup() {

        QuerySpec rooForumGroupQuery = QuerySpec.builder(ForumGroup.class).filter(FilterSpec.isNull("parent")).build();
        ForumGroup rootForumGroup = dynamicDAO.<ForumGroup>findOptional(rooForumGroupQuery).orElse(null);

        if(rootForumGroup == null) {
            return ServiceResponse.failure("No root forum group found");
        }
        else {
            try {
                return ServiceResponse.success("Successfully retrieved root forum group", forumGroupMapper.toForumGroupDTO(rootForumGroup));
            } catch (Exception e) {
                logger.error("Exception retrieving root forum group: %s".formatted(e.getMessage()));
                return ServiceResponse.failure("Exception retrieving root forum group: %s".formatted(e.getMessage()));
            }
        }

    }

    @Transactional(readOnly = true)
    public ServiceResponse<ForumTreeTableDTO> getForumTreeTable() {

        QuerySpec rooForumGroupQuery = QuerySpec.builder(ForumGroup.class).filter(FilterSpec.isNull("parent")).build();
        ForumGroup rootForumGroup = dynamicDAO.<ForumGroup>findOptional(rooForumGroupQuery).orElse(null);

        if(rootForumGroup == null) {
            return ServiceResponse.failure("No root forum group found");
        }
        else {
            try {
                return ServiceResponse.success("Successfully retrieved forum tree table",
                        forumGroupMapper.toForumTreeTableDTO(rootForumGroup));
            } catch (Exception e) {
                logger.error("Exception retrieving forum tree table: %s".formatted(e.getMessage()));
                return ServiceResponse.failure("Exception retrieving forum tree table: %s".formatted(e.getMessage()));
            }
        }
    }
}
