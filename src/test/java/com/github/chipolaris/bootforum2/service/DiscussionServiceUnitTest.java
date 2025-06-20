package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.*;
import com.github.chipolaris.bootforum2.dto.*;
import com.github.chipolaris.bootforum2.event.DiscussionCreatedEvent;
import com.github.chipolaris.bootforum2.mapper.DiscussionMapper;
import com.github.chipolaris.bootforum2.mapper.FileInfoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private FileService fileService;

    @Mock
    private FileInfoMapper fileInfoMapper;

    @Mock
    private AuthenticationFacade authenticationFacade;

    @Mock
    private ApplicationEventPublisher eventPublisher; // Added as it's a dependency

    @InjectMocks
    private DiscussionService discussionService;

    private DiscussionCreateDTO discussionCreateDTO;
    private Forum testForum;
    private Discussion testDiscussion;
    private DiscussionDTO testDiscussionDTO;
    private User testUser;
    private String testUsername = "testUser";
    private CommentInfo testCommentInfo;

    @BeforeEach
    void setUp() {
        discussionCreateDTO = new DiscussionCreateDTO(1L, "New Test Title", "New Test Comment");

        testForum = new Forum();
        testForum.setId(1L);
        testForum.setTitle("New Test Forum");
        ForumStat forumStat = new ForumStat();
        // Initialize ForumStat to avoid NPEs if it's accessed
        forumStat.setCommentCount(0L);
        forumStat.setDiscussionCount(0L);
        forumStat.setLastComment(new CommentInfo());
        testForum.setStat(forumStat);

        testCommentInfo = new CommentInfo();
        testCommentInfo.setCommentor(testUsername);
        testCommentInfo.setCommentDate(LocalDateTime.now().minusDays(1));
        testCommentInfo.setTitle("Old last comment title");

        testDiscussion = new Discussion();
        testDiscussion.setId(1L);
        testDiscussion.setTitle("New Test Title");
        testDiscussion.setContent("New Test Comment");
        testDiscussion.setForum(testForum);
        testDiscussion.setComments(new ArrayList<>());
        DiscussionStat discussionStat = new DiscussionStat();
        discussionStat.setLastComment(testCommentInfo); // Initialize with a distinct CommentInfo
        testDiscussion.setStat(discussionStat);

        testDiscussionDTO = new DiscussionDTO(1L, LocalDateTime.now(), testUsername,"New Test Title",
                null, null, null, null, null);

        testUser = User.newUser();
        testUser.setUsername(testUsername);
    }

    @Test
    void createDiscussion_success_publishesEventAndReturnsSuccess() {
        // Arrange
        MultipartFile[] images = {};
        MultipartFile[] attachments = {};

        when(authenticationFacade.getCurrentUsername()).thenReturn(Optional.of(testUsername));
        when(genericDAO.find(eq(Forum.class), eq(1L))).thenReturn(testForum);
        when(discussionMapper.toDiscussionDTO(any(Discussion.class))).thenReturn(testDiscussionDTO);
        // Mock file processing to return success with no files for simplicity
        // If files were present, you'd mock fileStorageService.storeFile and fileInfoMapper.toEntity

        // Act
        ServiceResponse<DiscussionDTO> response = discussionService.createDiscussion(discussionCreateDTO, images, attachments);

        // Assert
        assertNotNull(response);
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        assertNotNull(response.getDataObject());
        assertEquals(testDiscussionDTO.id(), response.getDataObject().id());
        assertEquals(testDiscussionDTO.title(), response.getDataObject().title());
        assertTrue(response.getMessages().contains("Discussion created successfully."));

        // Verify interactions
        verify(genericDAO).find(eq(Forum.class), eq(1L));

        ArgumentCaptor<Discussion> discussionCaptor = ArgumentCaptor.forClass(Discussion.class);
        verify(genericDAO).persist(discussionCaptor.capture());
        Discussion persistedDiscussion = discussionCaptor.getValue();

        assertNull(persistedDiscussion.getComments());

        assertEquals(discussionCreateDTO.content(), persistedDiscussion.getContent());
        assertEquals(testUsername, persistedDiscussion.getCreateBy());

        assertNotNull(persistedDiscussion.getStat());
        assertEquals(0, persistedDiscussion.getStat().getCommentCount());
        assertNotNull(persistedDiscussion.getStat().getLastComment());

        verify(discussionMapper).toDiscussionDTO(any(Discussion.class));
        verify(eventPublisher).publishEvent(any(DiscussionCreatedEvent.class)); // Verify event publication
    }

    @Test
    void createDiscussion_forumNotFound_returnsFailure() {
        // Arrange
        when(authenticationFacade.getCurrentUsername()).thenReturn(Optional.of(testUsername));
        when(genericDAO.find(eq(Forum.class), eq(1L))).thenReturn(null); // Forum not found

        // Act
        ServiceResponse<DiscussionDTO> response = discussionService.createDiscussion(discussionCreateDTO, null, null);

        // Assert
        assertNotNull(response);
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains("Forum not found. Cannot create discussion."));
        assertNull(response.getDataObject());

        verify(genericDAO).find(eq(Forum.class), eq(1L));
        verify(genericDAO, never()).persist(any(Discussion.class)); // Persist should not be called
        verify(eventPublisher, never()).publishEvent(any()); // No event should be published
    }

    @Test
    void createDiscussion_fileStorageFailure_stillCreatesDiscussionAndLogsWarning() {
        // Arrange
        MultipartFile mockImage = mock(MultipartFile.class);
        when(mockImage.isEmpty()).thenReturn(false); // Simulate a non-empty file
        when(mockImage.getOriginalFilename()).thenReturn("test-image.jpg");
        MultipartFile[] images = {mockImage};
        MultipartFile[] attachments = {};

        when(authenticationFacade.getCurrentUsername()).thenReturn(Optional.of(testUsername));
        when(genericDAO.find(eq(Forum.class), eq(1L))).thenReturn(testForum);

        // Simulate file storage failure
        ServiceResponse<FileCreatedDTO> failedFileResponse = new ServiceResponse<>();
        failedFileResponse.setAckCode(ServiceResponse.AckCodeType.FAILURE).addMessage("Disk full");
        when(fileService.storeFile(any(MultipartFile.class))).thenReturn(failedFileResponse);
        // fileInfoMapper.toEntity should not be called if storeFile fails and returns null dataObject

        when(discussionMapper.toDiscussionDTO(any(Discussion.class))).thenReturn(testDiscussionDTO);

        // Act
        ServiceResponse<DiscussionDTO> response = discussionService.createDiscussion(discussionCreateDTO, images, attachments);

        // Assert
        assertNotNull(response);
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode(), "Discussion creation should succeed even if file storage fails.");
        assertNotNull(response.getDataObject());

        verify(fileService).storeFile(eq(mockImage)); // Verify attempt to store file

        ArgumentCaptor<Discussion> discussionCaptor = ArgumentCaptor.forClass(Discussion.class);
        verify(genericDAO).persist(discussionCaptor.capture());
        Discussion persistedDiscussion = discussionCaptor.getValue();

        // Assert that the discussion's initial comment has no thumbnails due to storage failure
        assertTrue(persistedDiscussion.getThumbnails().isEmpty(), "Thumbnails should be empty on storage failure.");
        assertEquals(0, persistedDiscussion.getStat().getThumbnailCount(), "Thumbnail count in stat should be 0.");

        verify(eventPublisher).publishEvent(any(DiscussionCreatedEvent.class)); // Event should still be published
    }


    @Test
    void findPaginatedDiscussions_success_returnsPagedData() {
        // Arrange
        long forumId = 1L;
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createDate").descending());
        List<Discussion> discussionsList = Collections.singletonList(testDiscussion);
        long totalElements = 1L;

        when(dynamicDAO.count(any(QuerySpec.class))).thenReturn(totalElements);
        //when(dynamicDAO.find(any(QuerySpec.class))).thenReturn(discussionsList);
        doReturn(discussionsList).when(dynamicDAO).find(any(QuerySpec.class));
        when(discussionMapper.toDiscussionDTO(eq(testDiscussion))).thenReturn(testDiscussionDTO);

        // Act
        ServiceResponse<PageResponseDTO<DiscussionDTO>> response = discussionService.findPaginatedDiscussions(forumId, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        PageResponseDTO<DiscussionDTO> resultPage = response.getDataObject();
        assertNotNull(resultPage);
        assertEquals(1, resultPage.content().size());
        assertEquals(testDiscussionDTO.id(), resultPage.content().get(0).id());
        assertEquals(totalElements, resultPage.totalElements());
        assertEquals(0, resultPage.number());
        assertEquals(10, resultPage.size());
        assertEquals(1, resultPage.totalPages()); // totalElements = 1, size = 10 -> 1 page

        // Verify QuerySpec details
        ArgumentCaptor<QuerySpec> querySpecCaptor = ArgumentCaptor.forClass(QuerySpec.class);
        verify(dynamicDAO, times(1)).find(querySpecCaptor.capture()); // Capture QuerySpec for find
        QuerySpec capturedQuerySpec = querySpecCaptor.getValue();

        assertEquals(0, capturedQuerySpec.getStartIndex());
        assertEquals(10, capturedQuerySpec.getMaxResult());
        assertNotNull(capturedQuerySpec.getOrders());
        assertEquals(1, capturedQuerySpec.getOrders().size());
        assertEquals("createDate", capturedQuerySpec.getOrders().get(0).field());
        assertFalse(capturedQuerySpec.getOrders().get(0).ascending()); // descending
    }

    @Test
    void findPaginatedDiscussions_noDiscussionsFound_returnsEmptyPage() {
        // Arrange
        long forumId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        long totalElements = 0L;

        when(dynamicDAO.count(any(QuerySpec.class))).thenReturn(totalElements);
        when(dynamicDAO.find(any(QuerySpec.class))).thenReturn(Collections.emptyList());
        // discussionMapper.toDiscussionDTO will not be called if list is empty

        // Act
        ServiceResponse<PageResponseDTO<DiscussionDTO>> response = discussionService.findPaginatedDiscussions(forumId, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        PageResponseDTO<DiscussionDTO> resultPage = response.getDataObject();
        assertNotNull(resultPage);
        assertTrue(resultPage.content().isEmpty());
        assertEquals(totalElements, resultPage.totalElements());
        assertEquals(0, resultPage.number());
        assertEquals(0, resultPage.totalPages()); // No elements -> 0 pages
    }

    @Test
    void findPaginatedDiscussions_dynamicDaoCountThrowsException_returnsFailure() {
        // Arrange
        long forumId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        String errorMessage = "Database connection error during count";
        when(dynamicDAO.count(any(QuerySpec.class))).thenThrow(new RuntimeException(errorMessage));

        // Act
        ServiceResponse<PageResponseDTO<DiscussionDTO>> response = discussionService.findPaginatedDiscussions(forumId, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertNull(response.getDataObject());
        assertTrue(response.getMessages().contains("An unexpected error occurred while fetching discussions for forum: " + forumId));

        verify(dynamicDAO).count(any(QuerySpec.class));
        verify(dynamicDAO, never()).find(any(QuerySpec.class)); // find should not be called
        verify(discussionMapper, never()).toDiscussionDTO(any());
    }

    @Test
    void findPaginatedDiscussions_dynamicDaoFindThrowsException_returnsFailure() {
        // Arrange
        long forumId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        long totalElements = 5L; // Assume count succeeds
        String errorMessage = "Database connection error during find";

        when(dynamicDAO.count(any(QuerySpec.class))).thenReturn(totalElements);
        when(dynamicDAO.find(any(QuerySpec.class))).thenThrow(new RuntimeException(errorMessage));

        // Act
        ServiceResponse<PageResponseDTO<DiscussionDTO>> response = discussionService.findPaginatedDiscussions(forumId, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertNull(response.getDataObject());
        assertTrue(response.getMessages().contains("An unexpected error occurred while fetching discussions for forum: " + forumId));

        verify(dynamicDAO).count(any(QuerySpec.class));
        verify(dynamicDAO).find(any(QuerySpec.class));
        verify(discussionMapper, never()).toDiscussionDTO(any());
    }
}