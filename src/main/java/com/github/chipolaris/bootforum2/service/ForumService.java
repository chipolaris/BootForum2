package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.domain.Forum;
import com.github.chipolaris.bootforum2.domain.ForumGroup;
import com.github.chipolaris.bootforum2.dto.ForumCreateDTO;
import com.github.chipolaris.bootforum2.dto.ForumDTO;
import com.github.chipolaris.bootforum2.dto.ForumUpdateDTO;
import com.github.chipolaris.bootforum2.mapper.ForumMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ForumService {

    @Resource
    private ForumMapper forumMapper;

    @Resource
    private GenericDAO genericDAO;

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
            }
        }

        return response;
    }

    @Transactional(readOnly = false)
    public ServiceResponse<ForumDTO> updateForum(ForumUpdateDTO forumUpdateDTO) {

        ServiceResponse<ForumDTO> response = new ServiceResponse<>();

        Forum forum = forumMapper.toEntity(forumUpdateDTO);

        forum = genericDAO.merge(forum);

        response.setAckCode(ServiceResponse.AckCodeType.SUCCESS).setDataObject(forumMapper.toForumDTO(forum))
                .addMessage("Forum updated successfully");

        return response;
    }

    @Transactional(readOnly = true)
    public ServiceResponse<ForumDTO> getForum(Long id) {

        ServiceResponse<ForumDTO> response = new ServiceResponse<>();

        Forum forum = genericDAO.find(Forum.class, id);

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

        List<Forum> forums = genericDAO.findAll(Forum.class);
        List<ForumDTO> forumDTOs = forums.stream().map(forumMapper::toForumDTO).toList();

        response.setAckCode(ServiceResponse.AckCodeType.SUCCESS).
                setDataObject(forumDTOs).addMessage("Forums fetched successfully");

        return response;
    }
}
