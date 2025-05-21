package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.dynamic.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.dynamic.FilterSpec;
import com.github.chipolaris.bootforum2.dao.dynamic.QuerySpec;
import com.github.chipolaris.bootforum2.domain.ForumGroup;
import com.github.chipolaris.bootforum2.dto.ForumGroupCreateDTO;
import com.github.chipolaris.bootforum2.dto.ForumGroupDTO;
import com.github.chipolaris.bootforum2.dto.ForumGroupUpdateDTO;
import com.github.chipolaris.bootforum2.mapper.ForumGroupMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ForumGroupService {

    @Autowired
    private ForumGroupMapper forumGroupMapper;

    @Autowired
    private DynamicDAO dynamicDAO;

    @PersistenceContext
    protected EntityManager entityManager;

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
            ForumGroup parentForumGroup = entityManager.find(ForumGroup.class, parentId);

            if(parentForumGroup == null) {
                response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                        .addMessage(String.format("Parent forum group with id %d is not found", parentId));
            }
            else {
                ForumGroup forum = forumGroupMapper.toEntity(forumCreateDTO);
                forum.setParent(parentForumGroup);
                entityManager.persist(forum);

                response.setAckCode(ServiceResponse.AckCodeType.SUCCESS).setDataObject(forumGroupMapper.toForumGroupDTO(forum))
                        .addMessage("Forum group created successfully");
            }
        }

        return response;
    }

    @Transactional(readOnly = false)
    public ServiceResponse<ForumGroupDTO> updateForumGroup(ForumGroupUpdateDTO forumGroupUpdateDTO) {

        ServiceResponse<ForumGroupDTO> response = new ServiceResponse<>();

        ForumGroup forumGroup = entityManager.find(ForumGroup.class, forumGroupUpdateDTO.id());

        if(forumGroup == null) {
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage(String.format("Forum group with id %d is not found", forumGroupUpdateDTO.id()));
        }
        else {
            forumGroupMapper.mergeDTOToEntity(forumGroupUpdateDTO, forumGroup);

            forumGroup = entityManager.merge(forumGroup);

            response.setAckCode(ServiceResponse.AckCodeType.SUCCESS).setDataObject(forumGroupMapper.toForumGroupDTO(forumGroup))
                    .addMessage("Forum updated successfully");
        }
        return response;
    }

    @Transactional(readOnly = true)
    public ServiceResponse<ForumGroupDTO> getForumGroup(Long id) {

        ServiceResponse<ForumGroupDTO> response = new ServiceResponse<>();

        ForumGroup forumGroup = entityManager.find(ForumGroup.class, id);

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
}
