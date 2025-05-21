package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.domain.Forum;
import com.github.chipolaris.bootforum2.domain.ForumGroup;
import com.github.chipolaris.bootforum2.dto.ForumCreateDTO;
import com.github.chipolaris.bootforum2.dto.ForumDTO;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test") // Ensure your test profile uses H2 or Testcontainers
@Transactional // Rollback DB changes after each test
class ForumServiceIntegrationTest {

    @Autowired
    private ForumService forumService;

    @Autowired
    private EntityManager entityManager;

    private ForumGroup testForumGroup;

    @BeforeEach
    void setUp() {
        // Clean up existing forums to avoid interference if needed,
        // though @Transactional should handle rollback.
        // entityManager.createQuery("DELETE FROM Forum").executeUpdate();
        // entityManager.createQuery("DELETE FROM ForumGroup").executeUpdate();


        testForumGroup = new ForumGroup();
        testForumGroup.setTitle("Test Parent Group");
        // set other required fields for ForumGroup
        entityManager.persist(testForumGroup);
        entityManager.flush(); // Ensure parent group is persisted before creating forum
    }

    @Test
    void createAndGetForum_shouldSucceed() {
        // Arrange
        ForumCreateDTO createDTO = new ForumCreateDTO("Integration Test Forum", "A description", "test-icon", "#123456", true, testForumGroup.getId());

        // Act: Create Forum
        ServiceResponse<ForumDTO> createResponse = forumService.createForum(createDTO);

        // Assert: Creation
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, createResponse.getAckCode());
        assertNotNull(createResponse.getDataObject());
        Long createdForumId = createResponse.getDataObject().id();
        assertNotNull(createdForumId);
        assertEquals("Integration Test Forum", createResponse.getDataObject().title());

        // Act: Get Forum
        ServiceResponse<ForumDTO> getResponse = forumService.getForum(createdForumId);

        // Assert: Retrieval
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, getResponse.getAckCode());
        assertNotNull(getResponse.getDataObject());
        assertEquals(createdForumId, getResponse.getDataObject().id());
        assertEquals("Integration Test Forum", getResponse.getDataObject().title());
        assertEquals("A description", getResponse.getDataObject().description());
        assertEquals(testForumGroup.getId(), getResponse.getDataObject().forumGroupId());

        // Verify directly in DB (optional, but good for sanity check)
        Forum persistedForum = entityManager.find(Forum.class, createdForumId);
        assertNotNull(persistedForum);
        assertEquals("Integration Test Forum", persistedForum.getTitle());
        assertEquals(testForumGroup.getId(), persistedForum.getForumGroup().getId());
    }

    @Test
    void createForum_withNonExistentParentGroup_shouldFail() {
        // Arrange
        Long nonExistentParentGroupId = 999L;
        ForumCreateDTO createDTO = new ForumCreateDTO("Failed Forum", "Desc", "icon", "#fff", true, nonExistentParentGroupId);

        // Act
        ServiceResponse<ForumDTO> response = forumService.createForum(createDTO);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().get(0).contains("Forum group with id " + nonExistentParentGroupId + " is not found"));
    }
}
