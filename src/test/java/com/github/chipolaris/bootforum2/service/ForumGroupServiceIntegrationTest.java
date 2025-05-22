package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.domain.ForumGroup;
import com.github.chipolaris.bootforum2.dto.ForumGroupCreateDTO;
import com.github.chipolaris.bootforum2.dto.ForumGroupDTO;
import com.github.chipolaris.bootforum2.dto.ForumGroupUpdateDTO;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test") // Ensure you have a test profile configured (e.g., for H2 or Testcontainers)
@Transactional // Rolls back database changes after each test
class ForumGroupServiceIntegrationTest {

    @Autowired
    private ForumGroupService forumGroupService;

    @Autowired
    private EntityManager entityManager;

    private ForumGroup rootGroup;
    private ForumGroup childGroup1;

    @BeforeEach
    void setUp() {
        // Clean up existing data to ensure a fresh state for each test
        // entityManager.createQuery("DELETE FROM Forum").executeUpdate(); // If forums are involved and might interfere
        entityManager.createQuery("DELETE FROM ForumGroup").executeUpdate();
        entityManager.flush();

        // Create a root group for testing parent-child relationships
        rootGroup = new ForumGroup();
        rootGroup.setTitle("Root Test Group");
        rootGroup.setIcon("root-icon");
        rootGroup.setIconColor("#ROOT");
        rootGroup.setSortOrder(0);
        // rootGroup.setParent(null) is default
        entityManager.persist(rootGroup);

        childGroup1 = new ForumGroup();
        childGroup1.setTitle("Child Group 1");
        childGroup1.setIcon("child-icon");
        childGroup1.setIconColor("#CHILD1");
        childGroup1.setSortOrder(0);
        childGroup1.setParent(rootGroup);
        entityManager.persist(childGroup1);

        entityManager.flush(); // Ensure data is persisted before tests run
    }

    // region createForumGroup Tests
    @Test
    void createForumGroup_whenParentExists_shouldSucceedAndPersist() {
        // Arrange
        ForumGroupCreateDTO createDTO = new ForumGroupCreateDTO("New Child Group", "new-icon", "#NEW",  rootGroup.getId());

        // Act
        ServiceResponse<ForumGroupDTO> response = forumGroupService.createForumGroup(createDTO);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        assertNotNull(response.getDataObject());
        Long newGroupId = response.getDataObject().id();
        assertNotNull(newGroupId);
        assertEquals("New Child Group", response.getDataObject().title());
        assertEquals(rootGroup.getId(), response.getDataObject().parentId());

        // Verify in DB
        ForumGroup persistedGroup = entityManager.find(ForumGroup.class, newGroupId);
        assertNotNull(persistedGroup);
        assertEquals("New Child Group", persistedGroup.getTitle());
        assertEquals("new-icon", persistedGroup.getIcon());
        assertEquals("#NEW", persistedGroup.getIconColor());
        //assertEquals(1, persistedGroup.getSortOrder()); // dont test sortOrder for now
        assertNotNull(persistedGroup.getParent());
        assertEquals(rootGroup.getId(), persistedGroup.getParent().getId());
    }

    @Test
    void createForumGroup_whenParentDoesNotExist_shouldReturnFailure() {
        // Arrange
        Long nonExistentParentId = 9999L;
        ForumGroupCreateDTO createDTO = new ForumGroupCreateDTO("Orphan Group", "orphan-icon", "#ORPHAN", nonExistentParentId);

        // Act
        ServiceResponse<ForumGroupDTO> response = forumGroupService.createForumGroup(createDTO);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains(String.format("Parent forum group with id %d is not found", nonExistentParentId)));
    }

    @Test
    void createForumGroup_whenParentIdIsNull_shouldReturnFailure() {
        // Arrange
        ForumGroupCreateDTO createDTO = new ForumGroupCreateDTO("Attempt Root Group", "root-icon", "#ROOT", null);

        // Act
        ServiceResponse<ForumGroupDTO> response = forumGroupService.createForumGroup(createDTO);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains("Parent forum group is not specified"));
    }
    // endregion

    // region updateForumGroup Tests
    @Test
    void updateForumGroup_whenGroupExists_shouldSucceedAndUpdate() {
        // Arrange
        ForumGroupUpdateDTO updateDTO = new ForumGroupUpdateDTO(childGroup1.getId(), "Updated Child Group 1", "updated-icon", "#UPDATED");

        // Act
        ServiceResponse<ForumGroupDTO> response = forumGroupService.updateForumGroup(updateDTO);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        assertNotNull(response.getDataObject());
        assertEquals("Updated Child Group 1", response.getDataObject().title());
        assertEquals("updated-icon", response.getDataObject().icon());
        assertEquals("#UPDATED", response.getDataObject().iconColor());
        // Note: sortOrder is not part of ForumGroupUpdateDTO, so it won't be updated by this DTO.
        // If you need to update sortOrder, it should be added to ForumGroupUpdateDTO and the service logic.

        // Verify in DB
        entityManager.flush(); // Ensure changes are flushed to DB before finding
        entityManager.clear(); // Clear persistence context to force a fresh load
        ForumGroup updatedGroup = entityManager.find(ForumGroup.class, childGroup1.getId());
        assertNotNull(updatedGroup);
        assertEquals("Updated Child Group 1", updatedGroup.getTitle());
        assertEquals("updated-icon", updatedGroup.getIcon());
        assertEquals("#UPDATED", updatedGroup.getIconColor());
    }

    @Test
    void updateForumGroup_whenGroupDoesNotExist_shouldReturnFailure() {
        // Arrange
        Long nonExistentGroupId = 8888L;
        ForumGroupUpdateDTO updateDTO = new ForumGroupUpdateDTO(nonExistentGroupId, "Non Existent", "no-icon", "#NONE");

        // Act
        ServiceResponse<ForumGroupDTO> response = forumGroupService.updateForumGroup(updateDTO);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains(String.format("Forum group with id %d is not found", nonExistentGroupId)));
    }
    // endregion

    // region getForumGroup Tests
    @Test
    void getForumGroup_whenGroupExists_shouldReturnCorrectDTO() {
        // Act
        ServiceResponse<ForumGroupDTO> response = forumGroupService.getForumGroup(childGroup1.getId());

        // Assert
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        assertNotNull(response.getDataObject());
        assertEquals(childGroup1.getId(), response.getDataObject().id());
        assertEquals(childGroup1.getTitle(), response.getDataObject().title());
        assertEquals(childGroup1.getIcon(), response.getDataObject().icon());
        assertEquals(childGroup1.getIconColor(), response.getDataObject().iconColor());
        assertEquals(childGroup1.getParent().getId(), response.getDataObject().parentId());
        // forums and subGroups would be null or empty based on your mapper and if they are eagerly fetched/mapped
    }

    @Test
    void getForumGroup_whenGroupDoesNotExist_shouldReturnFailure() {
        // Arrange
        Long nonExistentGroupId = 7777L;

        // Act
        ServiceResponse<ForumGroupDTO> response = forumGroupService.getForumGroup(nonExistentGroupId);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains(String.format("Forum group with id %d is not found", nonExistentGroupId)));
    }
    // endregion

    // region getRootForumGroup Tests
    @Test
    void getRootForumGroup_whenRootGroupExists_shouldReturnRootDTO() {
        // Act
        ServiceResponse<ForumGroupDTO> response = forumGroupService.getRootForumGroup();

        // Assert
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        assertNotNull(response.getDataObject());
        assertEquals(rootGroup.getId(), response.getDataObject().id());
        assertEquals(rootGroup.getTitle(), response.getDataObject().title());
        assertNull(response.getDataObject().parentId()); // Root group's parentId should be null
    }

    @Test
    void getRootForumGroup_whenNoRootGroupExists_shouldReturnFailure() {
        // Arrange: Delete the existing root group to simulate this scenario
        entityManager.remove(rootGroup);
        entityManager.remove(childGroup1); // Remove child to avoid constraint issues if any
        entityManager.flush();

        // Act
        ServiceResponse<ForumGroupDTO> response = forumGroupService.getRootForumGroup();

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains("No root forum group found"));
    }
    // endregion
}