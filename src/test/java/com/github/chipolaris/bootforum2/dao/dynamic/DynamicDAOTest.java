package com.github.chipolaris.bootforum2.dao.dynamic;

import com.github.chipolaris.bootforum2.domain.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@SpringBootTest
@Transactional // This @Transactional annotation rolls back DB changes after each test
class DynamicDAOTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DynamicDAO dynamicDAO;

    @BeforeEach
    void setup() {
        entityManager.createQuery("DELETE FROM UserEntity").executeUpdate();
        entityManager.persist(new UserEntity("Alice", 30, "ACTIVE"));
        entityManager.persist(new UserEntity("Bob", 40, "INACTIVE"));
        entityManager.persist(new UserEntity("Charlie", 25, null));
    }

    @Test
    void testEquality() {
        var filters = DynamicFilterBuilder.create()
                .eq("name", "Alice")
                .build();

        var results = dynamicDAO.findEntities(UserEntity.class, filters);

        assertEquals(1, results.size());
        assertEquals("Alice", results.get(0).getName());
    }

    @Test
    void testGreaterThan() {
        var filters = DynamicFilterBuilder.create()
                .gt("age", 30)
                .build();

        var results = dynamicDAO.findEntities(UserEntity.class, filters);

        assertEquals(1, results.size());
        assertEquals("Bob", results.get(0).getName());
    }

    @Test
    void testIsNull() {
        var filters = DynamicFilterBuilder.create()
                .isNull("status", true)
                .build();

        var results = dynamicDAO.findEntities(UserEntity.class, filters);

        assertEquals(1, results.size());
        assertEquals("Charlie", results.get(0).getName());
    }

    @Test
    void testInClause() {
        var filters = DynamicFilterBuilder.create()
                .in("status", List.of("ACTIVE", "INACTIVE"))
                .build();

        var results = dynamicDAO.findEntities(UserEntity.class, filters);

        assertEquals(2, results.size());
    }

    @Test
    void testExists() {
        var filters = DynamicFilterBuilder.create()
                .in("status", List.of("ACTIVE", "INACTIVE"))
                .build();

        var results = dynamicDAO.exists(UserEntity.class, filters);

        assertEquals(true, results);

        filters = DynamicFilterBuilder.create()
                .in("status", List.of("NOT-ACTIVE"))
                .build();

        results = dynamicDAO.exists(UserEntity.class, filters);

        assertEquals(false, results);
    }

    @Test
    void testCount() {
        var filters = DynamicFilterBuilder.create()
                .in("status", List.of("ACTIVE", "INACTIVE"))
                .build();

        var results = dynamicDAO.count(UserEntity.class, filters);

        assertEquals(2, results);

        filters = DynamicFilterBuilder.create()
                .isNull("status", true)
                .build();

        results = dynamicDAO.count(UserEntity.class, filters);

        assertEquals(1, results);
    }
}

