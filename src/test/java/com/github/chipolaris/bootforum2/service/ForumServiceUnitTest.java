package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.domain.Forum;
import com.github.chipolaris.bootforum2.domain.ForumGroup;
import com.github.chipolaris.bootforum2.dto.ForumCreateDTO;
import com.github.chipolaris.bootforum2.dto.ForumDTO;
import com.github.chipolaris.bootforum2.mapper.ForumMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor; // Import ArgumentCaptor
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForumServiceUnitTest {

    @Mock
    private GenericDAO genericDAO;

    @Mock
    private ForumMapper forumMapper;

    @InjectMocks
    private ForumService forumService;

    @Test
    void createForum_whenForumGroupExists_shouldSucceed() {
        // Arrange
        Long parentGroupId = 1L;
        ForumCreateDTO createDTO = new ForumCreateDTO("Test Forum", "Description", "icon", "#fff", true, parentGroupId);

        ForumGroup mockForumGroup = new ForumGroup();
        mockForumGroup.setId(parentGroupId);

        Forum forumEntityFromMapper = new Forum(); // This is the real object returned by the mapper
        // We don't need to set its ID here, as it's set by the DB upon persist.
        // Its ForumGroup will be set by the service.

        // This is the DTO that the mapper will return after converting the persisted entity
        ForumDTO expectedForumDTO = new ForumDTO(null, "Test Forum", "Description", "icon", "#fff", true, parentGroupId, null);

        when(genericDAO.find(ForumGroup.class, parentGroupId)).thenReturn(mockForumGroup);
        when(forumMapper.toEntity(createDTO)).thenReturn(forumEntityFromMapper); // Mapper returns our real Forum object
        when(forumMapper.toForumDTO(any(Forum.class))).thenReturn(expectedForumDTO); // Mapper converts any Forum to the expected DTO

        // Act
        ServiceResponse<ForumDTO> response = forumService.createForum(createDTO);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        assertNotNull(response.getDataObject());
        assertEquals(expectedForumDTO, response.getDataObject());
        assertTrue(response.getMessages().get(0).contains("Forum created successfully"));

        // Verify interactions
        verify(genericDAO).find(ForumGroup.class, parentGroupId);
        verify(forumMapper).toEntity(createDTO);

        // Capture the Forum entity passed to persist
        ArgumentCaptor<Forum> forumCaptor = ArgumentCaptor.forClass(Forum.class);
        verify(genericDAO).persist(forumCaptor.capture()); // Verify persist was called and capture the argument

        Forum capturedForum = forumCaptor.getValue();
        assertNotNull(capturedForum.getForumGroup(), "ForumGroup should be set on the persisted Forum entity");
        assertEquals(mockForumGroup, capturedForum.getForumGroup(), "Persisted Forum entity should have the correct ForumGroup");
        // You can add more assertions on capturedForum if needed, e.g.,
        // assertEquals(createDTO.title(), capturedForum.getTitle());
        // (though this is implicitly tested by forumMapper.toEntity mock)

        verify(forumMapper).toForumDTO(capturedForum); // Verify DTO conversion was called with the captured (and now persisted) entity
    }

    @Test
    void createForum_whenParentGroupIdIsNull_shouldFail() {
        // Arrange
        ForumCreateDTO createDTO = new ForumCreateDTO("Test Forum", "Description", "icon", "#fff", true, null);

        // Act
        ServiceResponse<ForumDTO> response = forumService.createForum(createDTO);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().get(0).contains("Forum group is not specified"));
        verify(genericDAO, never()).find(any(), any());
        verify(genericDAO, never()).persist(any());
    }

    @Test
    void createForum_whenForumGroupNotFound_shouldFail() {
        // Arrange
        Long parentGroupId = 1L;
        ForumCreateDTO createDTO = new ForumCreateDTO("Test Forum", "Description", "icon", "#fff", true, parentGroupId);

        when(genericDAO.find(ForumGroup.class, parentGroupId)).thenReturn(null);

        // Act
        ServiceResponse<ForumDTO> response = forumService.createForum(createDTO);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().get(0).contains("Forum group with id " + parentGroupId + " is not found"));
        verify(genericDAO).find(ForumGroup.class, parentGroupId);
        verify(genericDAO, never()).persist(any());
    }
}