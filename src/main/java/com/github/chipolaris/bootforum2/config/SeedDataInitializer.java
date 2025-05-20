package com.github.chipolaris.bootforum2.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.chipolaris.bootforum2.dao.dynamic.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.dynamic.DynamicFilter;
import com.github.chipolaris.bootforum2.dao.dynamic.DynamicQuery;
import com.github.chipolaris.bootforum2.domain.ForumGroup;
import com.github.chipolaris.bootforum2.domain.Person;
import com.github.chipolaris.bootforum2.domain.User;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;

@Component
@Profile({"dev", "prod"}) // Your existing profiles
@Order(1)
public class SeedDataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(SeedDataInitializer.class);

    private final ObjectMapper objectMapper;
    private final DynamicDAO dynamicDAO;
    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;

    public SeedDataInitializer(ObjectMapper objectMapper, DynamicDAO dynamicDAO,
                               EntityManager entityManager, PasswordEncoder passwordEncoder) {
        this.objectMapper = objectMapper;
        this.dynamicDAO = dynamicDAO;
        this.entityManager = entityManager;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional // Ensure operations are transactional
    public void run(ApplicationArguments args) throws Exception {
        logger.info("Checking for seed data...");

        Resource resource = new ClassPathResource("seed-data.json");
        if (!resource.exists()) {
            logger.warn("seed-data.json not found. Skipping seed data initialization.");
            return;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            // ObjectMapper will deserialize to the SeedData record
            SeedData seedData = objectMapper.readValue(inputStream, SeedData.class);

            if (seedData.users() != null) { // Use record accessor users()
                for (SeedUser seedUser : seedData.users()) { // Iterate over SeedUser records
                    DynamicQuery userExistsQuery = DynamicQuery.builder(User.class)
                            .filter(DynamicFilter.of("username", DynamicFilter.Operator.EQ, seedUser.username())) // Use record accessor
                            .build();

                    boolean userExists = dynamicDAO.isExist(userExistsQuery); // or isExistOptimized

                    if (!userExists) {
                        logger.info("User '{}' not found. Seeding user...", seedUser.username()); // Use record accessor

                        User newUser = new User(); // User constructor initializes Person, Preferences, UserStat
                        newUser.setUsername(seedUser.username()); // Use record accessor
                        newUser.setPassword(passwordEncoder.encode(seedUser.password())); // Use record accessor
                        newUser.setUserRole(seedUser.userRole()); // Use record accessor
                        newUser.setAccountStatus(seedUser.accountStatus()); // Use record accessor

                        // createDate will be set by @PrePersist in User entity
                        // updateDate will be set by @PrePersist in User entity

                        if (seedUser.person() != null) { // Use record accessor
                            // Get the Person object initialized by User's constructor
                            Person person = newUser.getPerson();
                            if (person == null) { // Should not happen due to User constructor
                                person = new Person();
                                newUser.setPerson(person);
                            }
                            // Access fields of SeedPerson record
                            person.setFirstName(seedUser.person().firstName());
                            person.setLastName(seedUser.person().lastName());
                            person.setEmail(seedUser.person().email());
                            // email will be lowercased by Person's @PrePersist/@PreUpdate
                        }

                        entityManager.persist(newUser); // Persists User and cascaded Person, Preferences, UserStat
                        logger.info("User '{}' seeded successfully.", seedUser.username()); // Use record accessor
                    } else {
                        logger.info("User '{}' already exists. Skipping.", seedUser.username()); // Use record accessor
                    }
                }
            }
            // Seed Forum Groups
            if (seedData.forumGroups() != null) {
                for (SeedForumGroup seedForumGroup : seedData.forumGroups()) {
                    DynamicQuery forumGroupExistsQuery = DynamicQuery.builder(ForumGroup.class)
                            .filter(DynamicFilter.of("title", DynamicFilter.Operator.EQ, seedForumGroup.title()))
                            // For root, parent is null. If seeding sub-groups, you might add a parent filter.
                            .filter(DynamicFilter.of("parent", DynamicFilter.Operator.ISNULL, true))
                            .build();

                    boolean forumGroupExists = dynamicDAO.isExist(forumGroupExistsQuery);

                    if (!forumGroupExists) {
                        logger.info("Forum Group '{}' not found. Seeding forum group...", seedForumGroup.title());
                        ForumGroup newForumGroup = new ForumGroup();
                        newForumGroup.setTitle(seedForumGroup.title());
                        newForumGroup.setIcon(seedForumGroup.icon());
                        newForumGroup.setIconColor(seedForumGroup.iconColor());
                        newForumGroup.setSortOrder(seedForumGroup.sortOrder() != null ? seedForumGroup.sortOrder() : 0); // Default sortOrder if null
                        // For the root group, parent is null, which is the default for a new ForumGroup.
                        // createDate and updateDate are handled by @PrePersist/@PreUpdate

                        entityManager.persist(newForumGroup);
                        logger.info("Forum Group '{}' seeded successfully.", newForumGroup.getTitle());
                    } else {
                        logger.info("Root Forum Group '{}' already exists. Skipping.", seedForumGroup.title());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error during seed data initialization: {}", e.getMessage(), e);
            // Consider re-throwing or handling more specifically if needed
        }
    }
}