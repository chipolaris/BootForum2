package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.ForumGroup;
import com.github.chipolaris.bootforum2.dto.ForumGroupCreateDTO;
import com.github.chipolaris.bootforum2.dto.ForumGroupDTO;
import com.github.chipolaris.bootforum2.dto.ForumGroupUpdateDTO;
import com.github.chipolaris.bootforum2.mapper.ForumGroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForumGroupServiceUnitTest {

    @Mock
    private ForumGroupMapper forumGroupMapper;

    @Mock
    private DynamicDAO dynamicDAO;

    @Mock
    private GenericDAO genericDAO;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ForumGroupService forumGroupService;

    // region createForumGroup Tests
    @Test
    void createForumGroup_whenParentGroupIdIsNull_shouldReturnFailure() {
        // Arrange
        ForumGroupCreateDTO createDTO = new ForumGroupCreateDTO("New Group", "icon", "#fff",  null);

        // Act
        ServiceResponse<ForumGroupDTO> response = forumGroupService.createForumGroup(createDTO);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains("Parent forum group is not specified"));
        verify(genericDAO, never()).find(any(), any());
        verify(genericDAO, never()).persist(any());
    }

    @Test
    void createForumGroup_whenParentGroupNotFound_shouldReturnFailure() {
        // Arrange
        Long parentId = 1L;
        ForumGroupCreateDTO createDTO = new ForumGroupCreateDTO("New Group", "icon", "#fff",  parentId);
        when(genericDAO.find(ForumGroup.class, parentId)).thenReturn(null);

        // Act
        ServiceResponse<ForumGroupDTO> response = forumGroupService.createForumGroup(createDTO);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains(String.format("Parent forum group with id %d is not found", parentId)));
        verify(genericDAO).find(ForumGroup.class, parentId);
        verify(genericDAO, never()).persist(any());
    }

    @Test
    void createForumGroup_whenParentGroupExists_shouldSucceed() {
        // Arrange
        Long parentId = 1L;
        ForumGroupCreateDTO createDTO = new ForumGroupCreateDTO("New Group", "icon.png", "#123456", parentId);

        ForumGroup parentForumGroup = new ForumGroup();
        parentForumGroup.setId(parentId);
        parentForumGroup.setTitle("Parent Group");

        ForumGroup newForumGroupEntity = new ForumGroup(); // Entity returned by mapper
        newForumGroupEntity.setTitle(createDTO.title());

        ForumGroupDTO expectedDto = new ForumGroupDTO(null, "New Group", "icon.png", "#123456",  parentId, null, null);

        when(genericDAO.find(ForumGroup.class, parentId)).thenReturn(parentForumGroup);
        when(forumGroupMapper.toEntity(createDTO)).thenReturn(newForumGroupEntity);
        when(forumGroupMapper.toForumGroupDTO(any(ForumGroup.class))).thenReturn(expectedDto);
        // doNothing().when(genericDAO).persist(any(ForumGroup.class)); // Not strictly needed if verifying persist

        // Act
        ServiceResponse<ForumGroupDTO> response = forumGroupService.createForumGroup(createDTO);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        assertNotNull(response.getDataObject());
        assertEquals(expectedDto, response.getDataObject());
        assertTrue(response.getMessages().contains("Forum group with title 'New Group' created successfully"));

        verify(genericDAO).find(ForumGroup.class, parentId);
        verify(forumGroupMapper).toEntity(createDTO);

        ArgumentCaptor<ForumGroup> forumGroupCaptor = ArgumentCaptor.forClass(ForumGroup.class);
        verify(genericDAO).persist(forumGroupCaptor.capture());
        ForumGroup persistedForumGroup = forumGroupCaptor.getValue();

        assertEquals(newForumGroupEntity.getTitle(), persistedForumGroup.getTitle());
        assertEquals(parentForumGroup, persistedForumGroup.getParent()); // Verify parent was set

        verify(forumGroupMapper).toForumGroupDTO(persistedForumGroup);
    }
    // endregion

    // region updateForumGroup Tests
    @Test
    void updateForumGroup_whenGroupNotFound_shouldReturnFailure() {
        // Arrange
        Long groupId = 1L;
        ForumGroupUpdateDTO updateDTO = new ForumGroupUpdateDTO(groupId, "Updated Title", "icon", "#fff");
        when(genericDAO.find(ForumGroup.class, groupId)).thenReturn(null);

        // Act
        ServiceResponse<ForumGroupDTO> response = forumGroupService.updateForumGroup(updateDTO);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains(String.format("Forum group with id %d is not found", groupId)));
        verify(genericDAO).find(ForumGroup.class, groupId);
        verify(genericDAO, never()).merge(any());
    }

    @Test
    void updateForumGroup_whenGroupExists_shouldSucceed() {
        // Arrange
        Long groupId = 1L;
        ForumGroupUpdateDTO updateDTO = new ForumGroupUpdateDTO(groupId, "Updated Title", "updated_icon.png", "#654321");

        ForumGroup existingForumGroup = new ForumGroup();
        existingForumGroup.setId(groupId);
        existingForumGroup.setTitle("Old Title");

        ForumGroup mergedForumGroup = new ForumGroup(); // Simulate the merged entity
        mergedForumGroup.setId(groupId);
        mergedForumGroup.setTitle(updateDTO.title());
        mergedForumGroup.setIcon(updateDTO.icon());
        mergedForumGroup.setIconColor(updateDTO.iconColor());

        ForumGroupDTO expectedDto = new ForumGroupDTO(groupId, "Updated Title", "updated_icon.png", "#654321", null, null, null);

        when(genericDAO.find(ForumGroup.class, groupId)).thenReturn(existingForumGroup);
        doNothing().when(forumGroupMapper).mergeDTOToEntity(updateDTO, existingForumGroup);
        when(genericDAO.merge(existingForumGroup)).thenReturn(mergedForumGroup); // Return the "merged" entity
        when(forumGroupMapper.toForumGroupDTO(mergedForumGroup)).thenReturn(expectedDto);

        // Act
        ServiceResponse<ForumGroupDTO> response = forumGroupService.updateForumGroup(updateDTO);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        assertNotNull(response.getDataObject());
        assertEquals(expectedDto, response.getDataObject());
        assertTrue(response.getMessages().contains("Forum group with id %d updated successfully".formatted(groupId)));

        verify(genericDAO).find(ForumGroup.class, groupId);
        verify(forumGroupMapper).mergeDTOToEntity(updateDTO, existingForumGroup);
        verify(genericDAO).merge(existingForumGroup);
        verify(forumGroupMapper).toForumGroupDTO(mergedForumGroup);
    }
    // endregion

    // region getForumGroup Tests
    @Test
    void getForumGroup_whenGroupNotFound_shouldReturnFailure() {
        // Arrange
        Long groupId = 1L;
        when(genericDAO.find(ForumGroup.class, groupId)).thenReturn(null);

        // Act
        ServiceResponse<ForumGroupDTO> response = forumGroupService.getForumGroup(groupId);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains(String.format("Forum group with id %d is not found", groupId)));
        verify(genericDAO).find(ForumGroup.class, groupId);
    }

    @Test
    void getForumGroup_whenGroupExists_shouldSucceed() {
        // Arrange
        Long groupId = 1L;
        ForumGroup existingForumGroup = new ForumGroup();
        existingForumGroup.setId(groupId);
        existingForumGroup.setTitle("Existing Group");

        ForumGroupDTO expectedDto = new ForumGroupDTO(groupId, "Existing Group", "icon", "#fff", null, null, null);

        when(genericDAO.find(ForumGroup.class, groupId)).thenReturn(existingForumGroup);
        when(forumGroupMapper.toForumGroupDTO(existingForumGroup)).thenReturn(expectedDto);

        // Act
        ServiceResponse<ForumGroupDTO> response = forumGroupService.getForumGroup(groupId);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        assertNotNull(response.getDataObject());
        assertEquals(expectedDto, response.getDataObject());
        assertTrue(response.getMessages().contains("Forum group fetched successfully"));

        verify(genericDAO).find(ForumGroup.class, groupId);
        verify(forumGroupMapper).toForumGroupDTO(existingForumGroup);
    }
    // endregion

    // region getRootForumGroup Tests
    @Test
    void getRootForumGroup_whenRootGroupNotFound_shouldReturnFailure() {
        // Arrange
        when(dynamicDAO.findOptional(any(QuerySpec.class))).thenReturn(Optional.empty());

        // Act
        ServiceResponse<ForumGroupDTO> response = forumGroupService.getRootForumGroup();

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains("No root forum group found"));

        ArgumentCaptor<QuerySpec> querySpecCaptor = ArgumentCaptor.forClass(QuerySpec.class);
        verify(dynamicDAO).findOptional(querySpecCaptor.capture());
        QuerySpec capturedQuerySpec = querySpecCaptor.getValue();
        assertEquals(ForumGroup.class, capturedQuerySpec.getRootEntity());
        assertNotNull(capturedQuerySpec.getFilters());
        assertEquals(1, capturedQuerySpec.getFilters().size());
        assertEquals("parent", capturedQuerySpec.getFilters().get(0).field());
        assertEquals(FilterSpec.Operator.IS_NULL, capturedQuerySpec.getFilters().get(0).operator());
    }

    @Test
    void getRootForumGroup_whenRootGroupExists_shouldSucceed() {
        // Arrange
        ForumGroup rootForumGroup = new ForumGroup();
        rootForumGroup.setId(1L);
        rootForumGroup.setTitle("Root Group");
        rootForumGroup.setParent(null); // Explicitly for clarity

        ForumGroupDTO expectedDto = new ForumGroupDTO(1L, "Root Group", "root_icon", "#000", null, null, null);

        when(dynamicDAO.findOptional(any(QuerySpec.class))).thenReturn(Optional.of(rootForumGroup));
        when(forumGroupMapper.toForumGroupDTO(rootForumGroup)).thenReturn(expectedDto);

        // Act
        ServiceResponse<ForumGroupDTO> response = forumGroupService.getRootForumGroup();

        // Assert
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        assertNotNull(response.getDataObject());
        assertEquals(expectedDto, response.getDataObject());
        assertTrue(response.getMessages().contains("Successfully retrieved root forum group"));

        verify(dynamicDAO).findOptional(any(QuerySpec.class)); // QuerySpec verified in the "not found" test
        verify(forumGroupMapper).toForumGroupDTO(rootForumGroup);
    }
    // endregion
}