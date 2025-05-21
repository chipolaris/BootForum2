package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.dynamic.DynamicDAO;
import com.github.chipolaris.bootforum2.domain.Forum;
import com.github.chipolaris.bootforum2.domain.ForumGroup;
import com.github.chipolaris.bootforum2.dto.ForumCreateDTO;
import com.github.chipolaris.bootforum2.dto.ForumDTO;
import com.github.chipolaris.bootforum2.dto.ForumUpdateDTO;
import com.github.chipolaris.bootforum2.mapper.ForumMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ForumService {

    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    private ForumMapper forumMapper;

    @Autowired
    private DynamicDAO dynamicDAO;

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
            ForumGroup forumGroup = entityManager.find(ForumGroup.class, forumGroupId);
            if(forumGroup == null) {
                response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                        .addMessage(String.format("Forum group with id %d is not found", forumGroupId));
            }
            else {
                Forum forum = forumMapper.toEntity(forumCreateDTO);
                forum.setForumGroup(forumGroup);
                entityManager.persist(forum);

                response.setAckCode(ServiceResponse.AckCodeType.SUCCESS).setDataObject(forumMapper.toForumDTO(forum))
                        .addMessage("Forum created successfully");
            }
        }

        return response;
    }

    @Transactional(readOnly = false)
    public ServiceResponse<ForumDTO> updateForum(ForumUpdateDTO forumUpdateDTO) {

        ServiceResponse<ForumDTO> response = new ServiceResponse<>();

        Forum forum = entityManager.find(Forum.class, forumUpdateDTO.id());

        if(forum == null) {
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage(String.format("Forum with id %d is not found",forumUpdateDTO.id()));
        }
        else {
            forumMapper.mergeIntoEntity(forumUpdateDTO, forum);

            forum = entityManager.merge(forum);

            response.setAckCode(ServiceResponse.AckCodeType.SUCCESS).setDataObject(forumMapper.toForumDTO(forum))
                    .addMessage("Forum updated successfully");
        }
        return response;
    }

    @Transactional(readOnly = true)
    public ServiceResponse<ForumDTO> getForum(Long id) {

        ServiceResponse<ForumDTO> response = new ServiceResponse<>();

        Forum forum = entityManager.find(Forum.class, id);

        if(forum == null) {
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE).
                    addMessage(String.format("Forum with id %d is not found", id));
        }
        else {
            response.setAckCode(ServiceResponse.AckCodeType.SUCCESS).setDataObject(forumMapper.toForumDTO(forum)).
                    addMessage("Forum fetched successfully");
        }

        return response;
    }

    @Transactional(readOnly = true)
    public ServiceResponse<List<ForumDTO>> getAllForums() {

        ServiceResponse<List<ForumDTO>> response = new ServiceResponse<>();

        List<Forum> forums = dynamicDAO.all(Forum.class);
        List<ForumDTO> forumDTOs = forums.stream().map(forumMapper::toForumDTO).toList();

        response.setAckCode(ServiceResponse.AckCodeType.SUCCESS).
                setDataObject(forumDTOs).addMessage("Forums fetched successfully");

        return response;
    }
}
