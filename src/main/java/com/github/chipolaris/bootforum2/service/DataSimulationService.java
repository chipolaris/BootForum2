package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.*;
import com.github.chipolaris.bootforum2.dto.FileCreatedDTO;
import com.github.chipolaris.bootforum2.event.*;
import com.github.chipolaris.bootforum2.mapper.FileInfoMapper;
import com.github.chipolaris.bootforum2.repository.UserRepository;
import com.github.javafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class DataSimulationService {

    private static final Logger logger = LoggerFactory.getLogger(DataSimulationService.class);

    private final GenericDAO genericDAO;
    private final DynamicDAO dynamicDAO;
    private final FileService fileService;
    private final StatService statService;
    private final SystemStatistic systemStatistic;
    private final FileInfoMapper fileInfoMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final Faker faker = new Faker();
    private final Random random = new Random();

    public DataSimulationService(GenericDAO genericDAO, DynamicDAO dynamicDAO, FileService fileService,
                                 StatService statService, SystemStatistic systemStatistic,
                                 FileInfoMapper fileInfoMapper, ApplicationEventPublisher eventPublisher,
                                 PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.genericDAO = genericDAO;
        this.dynamicDAO = dynamicDAO;
        this.fileService = fileService;
        this.statService = statService;
        this.systemStatistic = systemStatistic;
        this.fileInfoMapper = fileInfoMapper;
        this.eventPublisher = eventPublisher;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    private static final List<String> ICON_CHOICES = List.of(
            "heroUserSolid", "heroHomeSolid", "heroCog6ToothSolid", "heroBellSolid",
            "heroAcademicCapSolid", "heroArchiveBoxArrowDownSolid", "heroFaceSmileSolid",
            "heroPhotoSolid", "heroLinkSolid", "heroLockClosedSolid", "heroMapPinSolid",
            "heroStarSolid", "heroGiftSolid", "heroGlobeAltSolid", "heroHomeModernSolid",
            "heroFilmSolid", "heroCakeSolid", "heroMusicalNoteSolid", "heroShoppingCartSolid",
            "heroScissorsSolid", "heroWifiSolid", "heroTagSolid", "heroRocketLaunchSolid",
            "heroWrenchScrewdriverSolid"
    );

    private static final List<String> COLOR_CHOICES = List.of(
            "#ef4444" /* text-red-500 */, "#f97316" /* text-orange-500*/, "#f59e0b" /*text-amber-500*/,
            "#eab308" /*"text-yellow-500*/, "#84cc16" /*text-lime-500*/, "#22c55e" /*text-green-500*/,
            "#10b981" /*text-emerald-500*/, "#14b8a6" /*text-teal-500*/, "#06b6d4" /*text-cyan-500*/,
            "#0ea5e9" /*text-sky-500*/, "#3b82f6" /*text-blue-500*/, "#6366f1" /*text-indigo-500*/,
            "#8b5cf6" /*text-violet-500*/, "#a855f7" /*text-purple-500*/, "#d946ef" /*text-fuchsia-500*/,
            "#ec4899" /*text-pink-500*/, "#f43f5e" /*text-rose-500*/
    );

    /**
     * NEW: Generates a specified number of simulated users.
     * @param count The number of users to create.
     */
    @Async
    @Transactional
    public void generateSimulatedUsers(int count) {
        logger.info("Starting simulated user generation for {} users...", count);
        final String fakePassword = "fake123";
        final String encodedPassword = passwordEncoder.encode(fakePassword);

        for (int i = 0; i < count; i++) {
            User user = User.newUser(); // Factory method sets defaults (roles, status, etc.)
            String baseUsername = faker.name().username();
            String finalUsername = baseUsername + "_fake";

            // Ensure username is unique to avoid database constraint violations
            int attempt = 0;
            while (userRepository.existsByUsername(finalUsername)) {
                attempt++;
                finalUsername = baseUsername + attempt + "_fake";
            }

            user.setUsername(finalUsername);
            user.setPassword(encodedPassword);

            // Populate Person details
            Person person = user.getPerson();
            person.setFirstName(faker.name().firstName());
            person.setLastName(faker.name().lastName());
            person.setEmail(faker.internet().safeEmailAddress(finalUsername));

            genericDAO.persist(user);
            eventPublisher.publishEvent(new UserCreatedEvent(this, user));

            if ((i + 1) % 100 == 0) {
                logger.info("Generated {}/{} users...", i + 1, count);
            }
        }

        logger.info("Successfully completed simulated user generation for {} users.", count);
    }

    @Async
    @Transactional
    public void generateSimulatedDiscussions() {
        logger.info("Starting generation of simulated discussion...");

        // Step 1: Fetch all fake users to use as authors for discussions and comments
        logger.info("Fetching fake users for content creation...");
        QuerySpec userQuery = QuerySpec.builder(User.class).filter(FilterSpec.like("username", "%_fake")).build();
        List<User> fakeUsers = dynamicDAO.find(userQuery);
        List<String> fakeUsernames = fakeUsers.stream().map(User::getUsername).collect(Collectors.toList());

        // Step 2: Fetch root forum group
        logger.info("Fetching root forum group...");
        QuerySpec rooForumGroupQuery = QuerySpec.builder(ForumGroup.class).filter(FilterSpec.isNull("parent")).build();
        ForumGroup rootForumGroup = dynamicDAO.<ForumGroup>findOptional(rooForumGroupQuery).orElse(null);

        if (fakeUsernames.isEmpty()) {
            logger.warn("No fake users found in the database. Falling back to generating random author names.");
        } else {
            logger.info("Found {} fake users to use as authors.", fakeUsernames.size());
        }

        for (int i = 0; i < 3; i++) { // Create 3 Forum Groups
            ForumGroup forumGroup = createForumGroup();
            // set root forum group as parent
            forumGroup.setParent(rootForumGroup);

            // persist forum group
            genericDAO.persist(forumGroup);
            eventPublisher.publishEvent(new ForumGroupCreatedEvent(this, forumGroup));

            for (int j = 0; j < 3; j++) { // Create 3 Forums in each group
                Forum forum = createForum(forumGroup);
                genericDAO.persist(forum);
                eventPublisher.publishEvent(new ForumCreatedEvent(this, forum));

                for (int k = 0; k < 5; k++) { // Create 5 Discussions in each forum
                    Discussion discussion = createDiscussion(forum, fakeUsernames); // Pass usernames
                    genericDAO.persist(discussion);

                    List<Comment> commentsInDiscussion = new ArrayList<>();
                    int commentCount = 10 + random.nextInt(11); // 10 to 20 comments
                    for (int l = 0; l < commentCount; l++) {
                        Comment comment = createComment(discussion, commentsInDiscussion, fakeUsernames); // Pass usernames
                        genericDAO.persist(comment);

                        commentsInDiscussion.add(comment);
                    }
                    // sync discussion stat
                    statService.syncDiscussionStat(discussion);
                }
                // sync forum stat
                statService.syncForumStat(forum);
            }
        }

        this.systemStatistic.initializeStatistics(); // re-initialize SystemStatistic

        for(User user : fakeUsers) {
            statService.syncUserStat(user);
        }

        logger.info("Successfully completed simulated data generation.");
    }

    /**
     * NEW: Generates simulated up/down votes for all existing discussions and comments.
     * This method is designed to be run after other data simulation methods.
     */
    @Async
    @Transactional
    public void generateSimulatedVotes() {
        logger.info("Starting simulated vote generation...");

        List<User> allUsers = genericDAO.all(User.class);
        List<Discussion> allDiscussions = genericDAO.all(Discussion.class);
        List<Comment> allComments = genericDAO.all(Comment.class);

        if (allUsers.isEmpty() || (allDiscussions.isEmpty() && allComments.isEmpty())) {
            logger.warn("Not enough data to generate votes. Need at least one user and one discussion/comment.");
            return;
        }

        logger.info("Generating votes for {} discussions...", allDiscussions.size());
        for (Discussion discussion : allDiscussions) {
            // Each user has a chance to vote on this discussion
            for (User user : allUsers) {
                // Skip if the user is the author of the discussion
                if (user.getUsername().equals(discussion.getCreateBy())) {
                    continue;
                }

                // 50% chance for a user to vote on any given discussion
                if (random.nextBoolean()) {
                    addVoteOnDiscussion(discussion, user.getUsername());
                }
            }
        }

        logger.info("Generating votes for {} comments...", allComments.size());
        for (Comment comment : allComments) {
            // Each user has a chance to vote on this comment
            for (User user : allUsers) {
                // Skip if the user is the author of the comment
                if (user.getUsername().equals(comment.getCreateBy())) {
                    continue;
                }

                // 30% chance for a user to vote on any given comment
                if (random.nextInt(100) < 30) {
                    addVoteOnComment(comment, user.getUsername());
                }
            }
        }

        logger.info("Successfully completed simulated vote generation.");
    }

    private void addVoteOnDiscussion(Discussion discussion, String username) {
        DiscussionStat discussionStat = discussion.getStat();
        if (discussionStat.getVotes() == null) {
            discussionStat.setVotes(new HashSet<>());
        }

        // Check if user has already voted (precaution for this simulation context)
        boolean alreadyVoted = discussionStat.getVotes().stream()
                .anyMatch(v -> v.getVoterName().equals(username));
        if (alreadyVoted) {
            return;
        }

        Vote vote = createRandomVote(username);
        discussionStat.getVotes().add(vote);

        if (vote.getVoteValue() > 0) {
            discussionStat.addVoteUpCount();
        } else {
            discussionStat.addVoteDownCount();
        }
    }

    private void addVoteOnComment(Comment comment, String username) {
        CommentVote commentVote = comment.getCommentVote();
        if (commentVote == null) {
            commentVote = new CommentVote();
            comment.setCommentVote(commentVote);
        }
        if (commentVote.getVotes() == null) {
            commentVote.setVotes(new HashSet<>());
        }

        // Check if user has already voted
        boolean alreadyVoted = commentVote.getVotes().stream()
                .anyMatch(v -> v.getVoterName().equals(username));
        if (alreadyVoted) {
            return;
        }

        Vote vote = createRandomVote(username);
        commentVote.getVotes().add(vote);

        if (vote.getVoteValue() > 0) {
            commentVote.addVoteUpCount();
        } else {
            commentVote.addVoteDownCount();
        }
    }

    private Vote createRandomVote(String username) {
        Vote vote = new Vote();
        vote.setVoterName(username);
        // 75% chance for an upvote (1), 25% for a downvote (-1)
        short voteValue = (random.nextInt(4) < 3) ? (short) 1 : (short) -1;
        vote.setVoteValue(voteValue);
        return vote;
    }

    private ForumGroup createForumGroup() {
        ForumGroup forumGroup = new ForumGroup();
        forumGroup.setTitle(faker.book().genre() + " Discussions");
        forumGroup.setIcon(getRandom(ICON_CHOICES));
        forumGroup.setIconColor(getRandom(COLOR_CHOICES));
        forumGroup.setSortOrder(faker.number().numberBetween(1, 100));
        return forumGroup;
    }

    private Forum createForum(ForumGroup forumGroup) {
        Forum forum = Forum.newForum(); // Use factory method
        forum.setForumGroup(forumGroup);
        forum.setTitle(faker.book().title());
        forum.setDescription(faker.lorem().sentence(10));
        forum.setIcon(getRandom(ICON_CHOICES));
        forum.setIconColor(getRandom(COLOR_CHOICES));
        forum.setSortOrder(faker.number().numberBetween(1, 100));
        return forum;
    }

    private Discussion createDiscussion(Forum forum, List<String> fakeUsernames) {
        Discussion discussion = Discussion.newDiscussion(); // Use factory method
        discussion.setForum(forum);
        discussion.setTitle(faker.lorem().sentence(5, 3));
        discussion.setContent(faker.lorem().paragraph(15));

        // Use a pre-existing fake user if available, otherwise fallback to a random name
        if (!fakeUsernames.isEmpty()) {
            discussion.setCreateBy(getRandom(fakeUsernames));
        } else {
            discussion.setCreateBy(faker.name().username()); // Fallback
        }

        // Add 3 random text file attachments
        List<FileInfo> attachments = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            createAndAddAttachment(attachments);
        }
        discussion.setAttachments(attachments);

        return discussion;
    }

    private Comment createComment(Discussion discussion, List<Comment> existingComments, List<String> fakeUsernames) {
        Comment comment = new Comment();
        comment.setDiscussion(discussion);
        comment.setTitle("Re: " + discussion.getTitle());
        comment.setContent(faker.lorem().paragraph(5));

        // Use a pre-existing fake user if available, otherwise fallback to a random name
        if (!fakeUsernames.isEmpty()) {
            comment.setCreateBy(getRandom(fakeUsernames));
        } else {
            comment.setCreateBy(faker.name().username()); // Fallback
        }

        // 25% chance to be a reply to another comment
        if (!existingComments.isEmpty() && random.nextInt(4) == 0) {
            comment.setReplyTo(getRandom(existingComments));
        }

        // 15% chance to have attachments
        if (random.nextInt(100) < 15) {
            List<FileInfo> attachments = new ArrayList<>();
            int fileCount = 1 + random.nextInt(2); // 1 or 2 files
            for (int i = 0; i < fileCount; i++) {
                createAndAddAttachment(attachments);
            }
            comment.setAttachments(attachments);
        }

        return comment;
    }

    private void createAndAddAttachment(List<FileInfo> attachments) {
        String filename = faker.file().fileName(null, null, "txt", null);
        String content = faker.lorem().paragraph(20);
        MultipartFile multipartFile = new InMemoryMultipartFile(filename, content);

        ServiceResponse<FileCreatedDTO> response = fileService.storeFile(multipartFile);
        if (response.isSuccess() && response.getDataObject() != null) {
            FileInfo fileInfo = fileInfoMapper.toEntity(response.getDataObject());
            genericDAO.persist(fileInfo);
            attachments.add(fileInfo);
        }
    }

    private <T> T getRandom(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }

    /**
     * A simple in-memory implementation of MultipartFile for data generation.
     */
    private static class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public InMemoryMultipartFile(String originalFilename, String content) {
            this.name = originalFilename;
            this.originalFilename = originalFilename;
            this.contentType = "text/plain";
            this.content = content.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String getName() { return name; }

        @Override
        public String getOriginalFilename() { return originalFilename; }

        @Override
        public String getContentType() { return contentType; }

        @Override
        public boolean isEmpty() { return content.length == 0; }

        @Override
        public long getSize() { return content.length; }

        @Override
        public byte[] getBytes() throws IOException { return content; }

        @Override
        public InputStream getInputStream() throws IOException { return new ByteArrayInputStream(content); }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            throw new UnsupportedOperationException("This is an in-memory file.");
        }
    }
}