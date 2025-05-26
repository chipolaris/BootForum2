package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.*;
import com.github.chipolaris.bootforum2.dto.DiscussionCreateDTO;
import com.github.chipolaris.bootforum2.dto.DiscussionDTO;
import com.github.chipolaris.bootforum2.dto.FileInfoDTO;
import com.github.chipolaris.bootforum2.mapper.DiscussionMapper;
import com.github.chipolaris.bootforum2.mapper.FileInfoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscussionServiceUnitTest {

    @Mock
    private GenericDAO genericDAO;

    @Mock
    private DynamicDAO dynamicDAO;

    @Mock
    private DiscussionMapper discussionMapper;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FileInfoMapper fileInfoMapper;

    @Mock
    private AuthenticationFacade authenticationFacade;

    @InjectMocks
    private DiscussionService discussionService;

    private DiscussionCreateDTO discussionCreateDTO;
    private Forum testForum;
    private Discussion testDiscussion;
    private DiscussionDTO testDiscussionDTO;
    private User testUser;
    private String testUsername = "testUser";

    @BeforeEach
    void setUp() {
        discussionCreateDTO = new DiscussionCreateDTO(1L, "Test Title", "Test Comment");

        testForum = new Forum();
        testForum.setId(1L);
        testForum.setTitle("Test Forum");
        ForumStat forumStat = new ForumStat();
        forumStat.setCommentCount(0L);
        forumStat.setDiscussionCount(0L);
        forumStat.setLastComment(new CommentInfo()); // Initialize to avoid NPE
        testForum.setStat(forumStat);


        testDiscussion = new Discussion();
        testDiscussion.setId(1L);
        testDiscussion.setTitle("Test Title");
        testDiscussion.setForum(testForum);
        testDiscussion.setComments(new ArrayList<>());
        DiscussionStat discussionStat = new DiscussionStat();
        discussionStat.setLastComment(new CommentInfo());
        testDiscussion.setStat(discussionStat);

        testDiscussionDTO = new DiscussionDTO(1L, LocalDateTime.now(), "system","Test Title", null, null);

        testUser = new User();
        testUser.setUsername(testUsername);
    }

    @Test
    void createDiscussion_success() {
        // Arrange
        MultipartFile[] images = {};
        MultipartFile[] attachments = {};

        when(authenticationFacade.getCurrentUsername()).thenReturn(Optional.of(testUsername));
        when(genericDAO.find(eq(Forum.class), eq(1L))).thenReturn(testForum);
        when(discussionMapper.toEntity(any(DiscussionCreateDTO.class))).thenReturn(testDiscussion);
        when(discussionMapper.toDiscussionDTO(any(Discussion.class))).thenReturn(testDiscussionDTO);
        // Mock file processing if needed, for simplicity assuming no files or successful processing
        // when(fileStorageService.storeFile(any())).thenReturn(new ServiceResponse<>(new FileInfoDTO(null, "test.jpg", "image/jpeg", "/path/to/test.jpg")));
        // when(fileInfoMapper.toEntity(any(FileInfoDTO.class))).thenReturn(new FileInfo());


        // Act
        ServiceResponse<DiscussionDTO> response = discussionService.createDiscussion(discussionCreateDTO, images, attachments);

        // Assert
        assertNotNull(response);
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        assertNotNull(response.getDataObject());
        assertEquals(testDiscussionDTO.id(), response.getDataObject().id());
        assertEquals(testDiscussionDTO.title(), response.getDataObject().title());
        assertTrue(response.getMessages().contains("Discussion created successfully."));

        verify(genericDAO).find(eq(Forum.class), eq(1L));
        verify(discussionMapper).toEntity(any(DiscussionCreateDTO.class));
        verify(genericDAO).persist(any(Discussion.class));
        verify(genericDAO).merge(eq(testForum)); // Verify forum stat update
        verify(discussionMapper).toDiscussionDTO(any(Discussion.class));

        // Capture the persisted Discussion to check its initial comment and stat
        ArgumentCaptor<Discussion> discussionCaptor = ArgumentCaptor.forClass(Discussion.class);
        verify(genericDAO).persist(discussionCaptor.capture());
        Discussion persistedDiscussion = discussionCaptor.getValue();

        assertNotNull(persistedDiscussion.getComments());
        assertEquals(1, persistedDiscussion.getComments().size());
        Comment initialComment = persistedDiscussion.getComments().get(0);
        assertEquals(discussionCreateDTO.comment(), initialComment.getContent());
        assertEquals(testUsername, initialComment.getCreateBy());

        assertNotNull(persistedDiscussion.getStat());
        assertEquals(1, persistedDiscussion.getStat().getCommentCount());
        assertEquals(testUsername, persistedDiscussion.getStat().getLastComment().getCommentor());
    }

    @Test
    void createDiscussion_forumNotFound() {
        // Arrange
        when(authenticationFacade.getCurrentUsername()).thenReturn(Optional.of(testUsername));
        when(genericDAO.find(eq(Forum.class), eq(1L))).thenReturn(null);

        // Act
        ServiceResponse<DiscussionDTO> response = discussionService.createDiscussion(discussionCreateDTO, null, null);

        // Assert
        assertNotNull(response);
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains("Forum not found. Cannot create discussion."));
        assertNull(response.getDataObject());

        verify(genericDAO).find(eq(Forum.class), eq(1L));
        verify(genericDAO, never()).persist(any(Discussion.class));
    }

    @Test
    void createDiscussion_fileStorageFailure() {
        // Arrange
        MultipartFile mockImage = mock(MultipartFile.class);
        when(mockImage.isEmpty()).thenReturn(false);
        when(mockImage.getOriginalFilename()).thenReturn("test.jpg");
        MultipartFile[] images = {mockImage};

        when(authenticationFacade.getCurrentUsername()).thenReturn(Optional.of(testUsername));
        when(genericDAO.find(eq(Forum.class), eq(1L))).thenReturn(testForum);
        when(discussionMapper.toEntity(any(DiscussionCreateDTO.class))).thenReturn(testDiscussion);

        ServiceResponse<FileInfoDTO> failedFileResponse = new ServiceResponse<>();
        failedFileResponse.setAckCode(ServiceResponse.AckCodeType.FAILURE).addMessage("Storage error");
        when(fileStorageService.storeFile(any(MultipartFile.class))).thenReturn(failedFileResponse);
        // No need to mock fileInfoMapper.toEntity if storeFile fails

        when(discussionMapper.toDiscussionDTO(any(Discussion.class))).thenReturn(testDiscussionDTO);

        // Act
        ServiceResponse<DiscussionDTO> response = discussionService.createDiscussion(discussionCreateDTO, images, null);

        // Assert
        assertNotNull(response);
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode()); // Discussion creation still succeeds
        assertNotNull(response.getDataObject());

        // Verify that the file processing was attempted and logged (implicitly by logger call in service)
        // Verify that the discussion stat reflects 0 thumbnailCount if file storage failed
        ArgumentCaptor<Discussion> discussionCaptor = ArgumentCaptor.forClass(Discussion.class);
        verify(genericDAO).persist(discussionCaptor.capture());
        Discussion persistedDiscussion = discussionCaptor.getValue();
        assertEquals(0, persistedDiscussion.getStat().getThumbnailCount()); // Assuming failure means no thumbnail
    }

    @Test
    void findPaginatedDiscussions_success() {
        // Arrange
        long forumId = 1L;
        Pageable pageable = PageRequest.of(0, 10); // Controller sends 0-indexed
        List<Discussion> discussionsList = Collections.singletonList(testDiscussion);
        long totalElements = 1L;

        // Mock count query
        when(dynamicDAO.count(any(QuerySpec.class))).thenReturn(totalElements);
        // Mock find query
        doReturn(discussionsList).when(dynamicDAO).find(any(QuerySpec.class));
        when(discussionMapper.toDiscussionDTO(eq(testDiscussion))).thenReturn(testDiscussionDTO);

        // Act
        ServiceResponse<Page<DiscussionDTO>> response = discussionService.findPaginatedDiscussions(forumId, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        assertNotNull(response.getDataObject());
        Page<DiscussionDTO> resultPage = response.getDataObject();
        assertEquals(1, resultPage.getContent().size());
        assertEquals(testDiscussionDTO, resultPage.getContent().get(0));
        assertEquals(totalElements, resultPage.getTotalElements());
        assertEquals(0, resultPage.getNumber()); // Page number should match request
        assertEquals(10, resultPage.getSize());  // Page size should match request

        verify(dynamicDAO).count(any(QuerySpec.class));
        verify(dynamicDAO).find(any(QuerySpec.class));
        verify(discussionMapper).toDiscussionDTO(eq(testDiscussion));
    }

    @Test
    void findPaginatedDiscussions_serviceHandlesPageConversionCorrectly() {
        // Arrange: Simulate Pageable with page number > 0
        long forumId = 1L;
        Pageable pageable = PageRequest.of(1, 5); // Requesting page 2 (0-indexed page 1), size 5
        List<Discussion> discussionsList = Collections.singletonList(testDiscussion);
        long totalElements = 10L; // Example total

        when(dynamicDAO.count(any(QuerySpec.class))).thenReturn(totalElements);
        // Mock find query using doReturn().when()
        doReturn(discussionsList).when(dynamicDAO).find(any(QuerySpec.class));
        when(discussionMapper.toDiscussionDTO(eq(testDiscussion))).thenReturn(testDiscussionDTO);

        // Act
        ServiceResponse<Page<DiscussionDTO>> response = discussionService.findPaginatedDiscussions(forumId, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        Page<DiscussionDTO> resultPage = response.getDataObject();
        assertNotNull(resultPage);
        assertEquals(1, resultPage.getContent().size()); // Assuming mock returns 1 for this page
        assertEquals(totalElements, resultPage.getTotalElements());
        assertEquals(1, resultPage.getNumber()); // Asserting the page number in the response
        assertEquals(5, resultPage.getSize());   // Asserting the page size in the response

        // Verify QuerySpec startIndex was calculated correctly for page 1 (0-indexed)
        ArgumentCaptor<QuerySpec> querySpecCaptor = ArgumentCaptor.forClass(QuerySpec.class);
        // dynamicDAO.find is called once for count (in the service, though we mock count separately here)
        // and once for the actual data.
        // The QuerySpec for count is built slightly differently in the service.
        // We are interested in the QuerySpec passed to find for data.
        verify(dynamicDAO, times(1)).find(querySpecCaptor.capture()); // Only capture the one for data

        QuerySpec dataQuerySpec = querySpecCaptor.getValue();
        // Based on current service logic: page=1, size=5 -> startIndex = (1-1)*5 = 0
        assertEquals(5, dataQuerySpec.getStartIndex());
        assertEquals(5, dataQuerySpec.getMaxResult());
    }


    @Test
    void findPaginatedDiscussions_daoException() {
        // Arrange
        long forumId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        when(dynamicDAO.count(any(QuerySpec.class))).thenThrow(new RuntimeException("DAO count error"));

        // Act
        ServiceResponse<Page<DiscussionDTO>> response = discussionService.findPaginatedDiscussions(forumId, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertNull(response.getDataObject());
        assertTrue(response.getMessages().contains("An unexpected error occurred while fetching discussions for forum: " + forumId));

        verify(dynamicDAO).count(any(QuerySpec.class));
        verify(dynamicDAO, never()).find(any(QuerySpec.class)); // find should not be called if count fails
    }
}