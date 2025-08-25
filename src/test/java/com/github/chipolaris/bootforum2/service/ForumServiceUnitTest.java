package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.domain.Forum;
import com.github.chipolaris.bootforum2.domain.ForumGroup;
import com.github.chipolaris.bootforum2.dto.ForumCreateDTO;
import com.github.chipolaris.bootforum2.dto.ForumDTO;
import com.github.chipolaris.bootforum2.event.ForumCreatedEvent;
import com.github.chipolaris.bootforum2.mapper.ForumMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForumServiceUnitTest {

    @Mock
    private GenericDAO genericDAO;

    @Mock
    private DynamicDAO dynamicDAO; // Added mock for missing dependency

    @Mock
    private ForumMapper forumMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher; // Added mock for missing dependency

    @InjectMocks
    private ForumService forumService;

    @Test
    void createForum_whenForumGroupExists_shouldSucceed() {
        // Arrange
        Long parentGroupId = 1L;
        ForumCreateDTO createDTO = new ForumCreateDTO("Test Forum", "Description", "icon", "#fff", true, parentGroupId);

        ForumGroup mockForumGroup = new ForumGroup();
        mockForumGroup.setId(parentGroupId);

        // This is the DTO that the mapper will return after converting the persisted entity
        ForumDTO expectedForumDTO = new ForumDTO(null, "Test Forum", "Description", "icon", "#fff", true, parentGroupId, null);

        when(genericDAO.find(ForumGroup.class, parentGroupId)).thenReturn(mockForumGroup);
        // We don't need to mock the void 'mergeIntoEntity' method. We will verify its invocation.
        when(forumMapper.toForumDTO(any(Forum.class))).thenReturn(expectedForumDTO);

        // Act
        ServiceResponse<ForumDTO> response = forumService.createForum(createDTO);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        assertNotNull(response.getDataObject());
        assertEquals(expectedForumDTO, response.getDataObject());
        assertTrue(response.getMessages().get(0).contains("Forum created successfully"));

        // Verify interactions
        verify(genericDAO).find(ForumGroup.class, parentGroupId);

        // Capture the Forum entity passed to persist to verify its state
        ArgumentCaptor<Forum> forumCaptor = ArgumentCaptor.forClass(Forum.class);
        verify(genericDAO).persist(forumCaptor.capture());
        Forum capturedForum = forumCaptor.getValue();

        // Verify that the mapper was called to merge data into the captured entity
        verify(forumMapper).mergeIntoEntity(eq(createDTO), same(capturedForum));

        // Verify the service logic correctly set the ForumGroup
        assertNotNull(capturedForum.getForumGroup(), "ForumGroup should be set on the persisted Forum entity");
        assertEquals(mockForumGroup, capturedForum.getForumGroup(), "Persisted Forum entity should have the correct ForumGroup");

        // Verify the final DTO conversion
        verify(forumMapper).toForumDTO(capturedForum);

        // Verify that the event was published
        verify(eventPublisher).publishEvent(any(ForumCreatedEvent.class));
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