package com.github.chipolaris.bootforum2.dao;

import com.github.chipolaris.bootforum2.config.SeedDataInitializer;
import com.github.chipolaris.bootforum2.security.JwtAuthenticationFilter;
import com.github.chipolaris.bootforum2.service.ForumSettingService;
import com.github.chipolaris.bootforum2.service.SystemStatistic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test class is refactored to use @DataJpaTest for efficient slice testing of the persistence layer.
 */
@DataJpaTest
@Import(DynamicDAO.class) // We need to explicitly import our custom DAO class
class DynamicDAOTest {

    // Add a mocked beans so that {@link SpringBootAngularApplication.java} can inject them
    // during test context loading for this slice test.
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private SeedDataInitializer seedDataInitializer;

    @MockitoBean
    private ForumSettingService forumSettingService;

    @MockitoBean
    private SystemStatistic systemStatistic;
    // end mocked beans

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DynamicDAO dynamicDAO;

    // A simple entity for testing purposes, defined inside the test class for isolation.
    @Entity
    static class UserEntity {
        @Id @GeneratedValue
        private Long id;
        private String name;
        private Integer age;
        private String status;

        public UserEntity() {}

        public UserEntity(String name, Integer age, String status) {
            this.name = name;
            this.age = age;
            this.status = status;
        }

        public String getName() { return name; }
        public Integer getAge() { return age; }
        public String getStatus() { return status; }
    }

    @BeforeEach
    void setup() {
        // TestEntityManager is preferred for setting up test data
        entityManager.getEntityManager().createQuery("DELETE FROM UserEntity").executeUpdate();
        entityManager.persist(new UserEntity("Alice", 30, "ACTIVE"));
        entityManager.persist(new UserEntity("Bob", 40, "INACTIVE"));
        entityManager.persist(new UserEntity("Charlie", 25, null));
        entityManager.flush();
    }

    @Test
    void testFind() {

        // test single filter
        QuerySpec querySpec = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.eq("name", "Alice")).build();

        List<UserEntity> results = dynamicDAO.find(querySpec);
        assertEquals(1, results.size());
        assertEquals("Alice", results.get(0).getName());
        assertEquals(30, results.get(0).getAge());

        // test multiple filters
        querySpec = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.eq("name","Alice"))
                .filter(FilterSpec.gt("age",30)).build();
        results = dynamicDAO.find(querySpec);
        assertEquals(0, results.size());

        // test GTE
        querySpec = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.gte("age", 30)).build();
        results = dynamicDAO.find(querySpec);
        assertEquals(2, results.size());
    }

    @Test
    void testCount() {

        // No filter, should return all entities
        QuerySpec querySpec = QuerySpec.builder(UserEntity.class).build();
        long result = dynamicDAO.count(querySpec);
        assertEquals(3, result);

        // test single filter
        querySpec = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.lte("age", 30)).build();
        result = dynamicDAO.count(querySpec);
        assertEquals(2, result);
    }

    @Test
    void testExists() {
        // Test case: Entity exists
        QuerySpec queryExists = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.eq("name", "Alice")).build();
        assertTrue(dynamicDAO.exists(queryExists), "Alice should exist");

        // Test case: Entity does not exist
        QuerySpec queryNotExists = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.eq("name", "Zelda")).build();
        assertFalse(dynamicDAO.exists(queryNotExists), "Zelda should not exist");

        // Test case: Multiple filters, entity exists
        QuerySpec queryMultiFilterExists = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.eq("name","Bob"))
                .filter(FilterSpec.eq("age", 40)).build();
        assertTrue(dynamicDAO.exists(queryMultiFilterExists), "Bob with age 40 should exist");

        // Test with IS_NULL filter
        QuerySpec queryIsNullStatus = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.isNull("status")).build();
        assertTrue(dynamicDAO.exists(queryIsNullStatus), "User with null status (Charlie) should exist");

        // Test with IS_NOT_NULL filter
        QuerySpec queryIsNotNullStatus = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.isNotNull("status")).build();
        assertTrue(dynamicDAO.exists(queryIsNotNullStatus), "Users with non-null status (Alice, Bob) should exist");
    }

    @Test
    void testInOperations() {

        QuerySpec queryInCollection = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.in("name", List.of("Alice", "Bob")))
                .build();
        var results = dynamicDAO.<UserEntity>find(queryInCollection);
        assertEquals(2, results.size());

        queryInCollection = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.in("name", Collections.singletonList("Bob")))
                .build();
        results = dynamicDAO.find(queryInCollection);
        assertEquals(1, results.size());

        // using an array of strings, one match, one doesn't
        queryInCollection = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.in("name", new String[]{"Bob", "John"}))
                .build();
        results = dynamicDAO.find(queryInCollection);
        assertEquals(1, results.size());
    }

    @Test
    void testNotInOperations() {
        QuerySpec queryNotInCollection = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.notIn("name", List.of("Alice", "Bob")))
                .build();
        var results = dynamicDAO.<UserEntity>find(queryNotInCollection);
        assertEquals(1, results.size()); // only "Charlie" is not in ("Alice", "Bob") list
        assertEquals("Charlie", results.get(0).getName());

        queryNotInCollection = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.notIn("name", List.of("Mary", "John")))
                .build();
        results = dynamicDAO.find(queryNotInCollection);
        assertEquals(3, results.size()); // all entities are not in ("Mary", "John") list
    }

    @Test
    void testSortingAndPagination() {
        QuerySpec querySorting = QuerySpec.builder(UserEntity.class).order(OrderSpec.desc("age")).build();
        var results = dynamicDAO.<UserEntity>find(querySorting);
        assertEquals(3, results.size());
        assertEquals(40, results.get(0).getAge());
        assertEquals(30, results.get(1).getAge());
        assertEquals(25, results.get(2).getAge());

        querySorting = QuerySpec.builder(UserEntity.class).order(OrderSpec.asc("age")).build();
        results = dynamicDAO.find(querySorting);
        assertEquals(3, results.size());
        assertEquals(25, results.get(0).getAge());
        assertEquals(30, results.get(1).getAge());
        assertEquals(40, results.get(2).getAge());

        QuerySpec queryPagination = QuerySpec.builder(UserEntity.class).startIndex(0).maxResult(2).build();
        results = dynamicDAO.find(queryPagination);
        assertEquals(2, results.size());

        QuerySpec querySortingAndPagination = QuerySpec.builder(UserEntity.class)
                .order(OrderSpec.desc("age")).startIndex(0).maxResult(2).build();
        results = dynamicDAO.find(querySortingAndPagination);
        assertEquals(2, results.size());
        assertEquals(40, results.get(0).getAge());
        assertEquals(30, results.get(1).getAge());

        querySortingAndPagination = QuerySpec.builder(UserEntity.class)
                .order(OrderSpec.desc("age")).startIndex(2).maxResult(2).build();
        results = dynamicDAO.find(querySortingAndPagination);
        assertEquals(1, results.size());
        assertEquals(25, results.get(0).getAge());
    }
}