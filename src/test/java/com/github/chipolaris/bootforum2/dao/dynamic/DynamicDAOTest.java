package com.github.chipolaris.bootforum2.dao.dynamic;

import com.github.chipolaris.bootforum2.domain.UserEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

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

    @Autowired
    DynamicDAO dynamicDAO2;

    @Test
    void testFindEntities2() {

        // test single filter
        var filter = DynamicFilter.of("name", DynamicFilter.Operator.EQ, "Alice");
        DynamicQuery dynamicQuery = DynamicQuery.builder(UserEntity.class).filter(filter).build();

        var results = dynamicDAO2.<UserEntity>findEntities(dynamicQuery);
        assertEquals(1, results.size());
        assertEquals("Alice", results.get(0).getName());
        assertEquals(30, results.get(0).getAge());

        // test multiple filters
        var filter2 = DynamicFilter.of("name", DynamicFilter.Operator.EQ, "Alice");
        var filter3 = DynamicFilter.of("age", DynamicFilter.Operator.GT, 30);

        dynamicQuery = DynamicQuery.builder(UserEntity.class).filter(filter2).filter(filter3).build();
        results = dynamicDAO2.<UserEntity>findEntities(dynamicQuery);
        assertEquals(0, results.size());

        // test GTE
        var filter4 = DynamicFilter.of("age", DynamicFilter.Operator.GTE, 30);
        dynamicQuery = DynamicQuery.builder(UserEntity.class).filter(filter4).build();
        results = dynamicDAO2.<UserEntity>findEntities(dynamicQuery);
        assertEquals(2, results.size());
    }

    @Test
    void testCountEntities2() {

        // No filter, should return all entities
        DynamicQuery dynamicQuery = DynamicQuery.builder(UserEntity.class).build();
        var results = dynamicDAO2.<UserEntity>countEntities(dynamicQuery);
        assertEquals(3, results);

        // test single filter
        var filter = DynamicFilter.of("age", DynamicFilter.Operator.LTE, 30);
        dynamicQuery = DynamicQuery.builder(UserEntity.class).filter(filter).build();
        results = dynamicDAO2.<UserEntity>countEntities(dynamicQuery);
        assertEquals(2, results);
    }

    @Test
    void testIsExist() {
        // Test case: Entity exists
        DynamicQuery queryExists = DynamicQuery.builder(UserEntity.class)
                .filter(DynamicFilter.of("name", DynamicFilter.Operator.EQ, "Alice")).build();
        assertTrue(dynamicDAO.isExist(queryExists), "Optimized: Alice should exist");

        // Test case: Entity does not exist
        DynamicQuery queryNotExists = DynamicQuery.builder(UserEntity.class)
                .filter(DynamicFilter.of("name", DynamicFilter.Operator.EQ, "Zelda")).build();
        assertFalse(dynamicDAO.isExist(queryNotExists), "Optimized: Zelda should not exist");

        // Test case: Multiple filters, entity exists
        DynamicQuery queryMultiFilterExists = DynamicQuery.builder(UserEntity.class)
                .filter(DynamicFilter.of("name", DynamicFilter.Operator.EQ, "Bob"))
                .filter(DynamicFilter.of("age", DynamicFilter.Operator.EQ, 40)).build();
        assertTrue(dynamicDAO.isExist(queryMultiFilterExists), "Optimized: Bob with age 40 should exist");

        // Test case: Multiple filters, entity does not exist
        DynamicQuery queryMultiFilterNotExists = DynamicQuery.builder(UserEntity.class)
                .filter(DynamicFilter.of("name", DynamicFilter.Operator.EQ, "Bob"))
                .filter(DynamicFilter.of("age", DynamicFilter.Operator.EQ, 35)).build();
        assertFalse(dynamicDAO.isExist(queryMultiFilterNotExists), "Optimized: Bob with age 35 should not exist");

        // Test with targetPath (optimized version will select the targetPath if present, but still limit to 1)
        DynamicQuery queryTargetPathExists = DynamicQuery.builder(UserEntity.class)
                //.targetEntity(String.class) // Type of the targetPath
                .targetPath("status")
                .filter(DynamicFilter.of("name", DynamicFilter.Operator.EQ, "Alice")).build();
        assertTrue(dynamicDAO.isExist(queryTargetPathExists), "Optimized: Status should exist for Alice");

        // Test with targetPath where path value is null
        DynamicQuery queryTargetPathIsNull = DynamicQuery.builder(UserEntity.class)
                //.targetEntity(String.class) // Type of the targetPath
                .targetPath("status")
                .filter(DynamicFilter.of("name", DynamicFilter.Operator.EQ, "Charlie")).build();
        // isExistOptimized will find Charlie, and since status is a path, it will try to select it.
        // If the entity exists, getResultList() will not be empty, even if the selected value is null.
        assertTrue(dynamicDAO.isExist(queryTargetPathIsNull),
                "Optimized: Entity Charlie exists, so isExistOptimized should be true even if selected status is null");

        // Test with ISNULL filter
        DynamicQuery queryIsNullStatus = DynamicQuery.builder(UserEntity.class)
                .filter(DynamicFilter.of("status", DynamicFilter.Operator.ISNULL, true)).build();
        assertTrue(dynamicDAO.isExist(queryIsNullStatus), "Optimized: User with null status (Charlie) should exist");

        DynamicQuery queryIsNotNullStatus = DynamicQuery.builder(UserEntity.class)
                .filter(DynamicFilter.of("status", DynamicFilter.Operator.ISNULL, false)).build();
        assertTrue(dynamicDAO.isExist(queryIsNotNullStatus), "Optimized: Users with non-null status (Alice, Bob) should exist");
    }
}

