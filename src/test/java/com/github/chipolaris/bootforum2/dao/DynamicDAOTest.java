package com.github.chipolaris.bootforum2.dao;

import com.github.chipolaris.bootforum2.domain.UserEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional // This @Transactional annotation rolls back DB changes after each test
class DynamicDAOTest {

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setup() {
        entityManager.createQuery("DELETE FROM UserEntity").executeUpdate();
        entityManager.persist(new UserEntity("Alice", 30, "ACTIVE"));
        entityManager.persist(new UserEntity("Bob", 40, "INACTIVE"));
        entityManager.persist(new UserEntity("Charlie", 25, null));
    }

    @Autowired
    DynamicDAO dynamicDAO;

    @Test
    void testFind() {

        // test single filter
        var filter = FilterSpec.eq("name", "Alice");
        QuerySpec querySpec = QuerySpec.builder(UserEntity.class).filter(filter).build();

        var results = dynamicDAO.<UserEntity>find(querySpec);
        assertEquals(1, results.size());
        assertEquals("Alice", results.get(0).getName());
        assertEquals(30, results.get(0).getAge());

        // test multiple filters
        var filter2 = FilterSpec.eq("name","Alice");
        var filter3 = FilterSpec.gt("age",30);

        querySpec = QuerySpec.builder(UserEntity.class).filter(filter2).filter(filter3).build();
        results = dynamicDAO.<UserEntity>find(querySpec);
        assertEquals(0, results.size());

        // test GTE
        var filter4 = FilterSpec.of("age", FilterSpec.Operator.GTE, 30);
        querySpec = QuerySpec.builder(UserEntity.class).filter(filter4).build();
        results = dynamicDAO.<UserEntity>find(querySpec);
        assertEquals(2, results.size());
    }

    @Test
    void testCount() {

        // No filter, should return all entities
        QuerySpec querySpec = QuerySpec.builder(UserEntity.class).build();
        var results = dynamicDAO.<UserEntity>count(querySpec);
        assertEquals(3, results);

        // test single filter
        var filter = FilterSpec.lte("age", 30);
        querySpec = QuerySpec.builder(UserEntity.class).filter(filter).build();
        results = dynamicDAO.<UserEntity>count(querySpec);
        assertEquals(2, results);
    }

    @Test
    void testExists() {
        // Test case: Entity exists
        QuerySpec queryExists = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.eq("name", "Alice")).build();
        assertTrue(dynamicDAO.exists(queryExists), "Optimized: Alice should exist");

        // Test case: Entity does not exist
        QuerySpec queryNotExists = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.eq("name", "Zelda")).build();
        assertFalse(dynamicDAO.exists(queryNotExists), "Optimized: Zelda should not exist");

        // Test case: Multiple filters, entity exists
        QuerySpec queryMultiFilterExists = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.eq("name","Bob"))
                .filter(FilterSpec.eq("age", 40)).build();
        assertTrue(dynamicDAO.exists(queryMultiFilterExists), "Optimized: Bob with age 40 should exist");

        // Test case: Multiple filters, entity does not exist
        QuerySpec queryMultiFilterNotExists = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.eq("name",  "Bob"))
                .filter(FilterSpec.eq("age", 35)).build();
        assertFalse(dynamicDAO.exists(queryMultiFilterNotExists), "Optimized: Bob with age 35 should not exist");

        // Test with targetPath (optimized version will select the targetPath if present, but still limit to 1)
        QuerySpec queryTargetPathExists = QuerySpec.builder(UserEntity.class)
                .targetPath("status")
                .filter(FilterSpec.eq("name", "Alice")).build();
        assertTrue(dynamicDAO.exists(queryTargetPathExists), "Optimized: Status should exist for Alice");

        // Test with targetPath where path value is null
        QuerySpec queryTargetPathIsNull = QuerySpec.builder(UserEntity.class)
                .targetPath("status")
                .filter(FilterSpec.eq("name", "Charlie")).build();
        // isExistOptimized will find Charlie, and since status is a path, it will try to select it.
        // If the entity exists, getResultList() will not be empty, even if the selected value is null.
        assertTrue(dynamicDAO.exists(queryTargetPathIsNull),
                "Optimized: Entity Charlie exists, so isExistOptimized should be true even if selected status is null");

        // Test with IS_NULL filter
        QuerySpec queryIsNullStatus = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.isNull("status")).build();
        assertTrue(dynamicDAO.exists(queryIsNullStatus), "Optimized: User with null status (Charlie) should exist");

        QuerySpec queryIsNotNullStatus = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.isNull("status")).build();
        assertTrue(dynamicDAO.exists(queryIsNotNullStatus), "Optimized: Users with non-null status (Alice, Bob) should exist");
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
        results = dynamicDAO.<UserEntity>find(queryInCollection);
        assertEquals(1, results.size());

        // using an array of strings, one match, one doesn't
        queryInCollection = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.in("name", new String[]{"Bob", "John"}))
                .build();
        results = dynamicDAO.<UserEntity>find(queryInCollection);
        assertEquals(1, results.size());

        // using an array list of strings
        queryInCollection = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.in("name", Arrays.asList("Alice", "Bob")))
                .build();
        results = dynamicDAO.<UserEntity>find(queryInCollection);
        assertEquals(2, results.size());
    }

    @Test
    void testNotInOperations() {
        QuerySpec queryNotInCollection = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.notIn("name", List.of("Alice", "Bob")))
                .build();
        var results = dynamicDAO.<UserEntity>find(queryNotInCollection);
        assertEquals(1, results.size()); // only "Charlie" is not in ("Alice", "Bob") list

        queryNotInCollection = QuerySpec.builder(UserEntity.class)
                .filter(FilterSpec.notIn("name", List.of("Mary", "John")))
                .build();
        results = dynamicDAO.<UserEntity>find(queryNotInCollection);
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
        results = dynamicDAO.<UserEntity>find(querySorting);
        assertEquals(3, results.size());
        assertEquals(25, results.get(0).getAge());
        assertEquals(30, results.get(1).getAge());
        assertEquals(40, results.get(2).getAge());

        QuerySpec queryPagination = QuerySpec.builder(UserEntity.class).startIndex(0).maxResult(2).build();
        results = dynamicDAO.<UserEntity>find(queryPagination);
        assertEquals(2, results.size());

        QuerySpec querySortingAndPagination = QuerySpec.builder(UserEntity.class)
                .order(OrderSpec.desc("age")).startIndex(0).maxResult(2).build();
        results = dynamicDAO.<UserEntity>find(querySortingAndPagination);
        assertEquals(2, results.size());
        assertEquals(40, results.get(0).getAge());
        assertEquals(30, results.get(1).getAge());

        querySortingAndPagination = QuerySpec.builder(UserEntity.class)
                .order(OrderSpec.desc("age")).startIndex(2).maxResult(2).build();
        results = dynamicDAO.<UserEntity>find(querySortingAndPagination);
        assertEquals(1, results.size());
        assertEquals(25, results.get(0).getAge());
    }
}

