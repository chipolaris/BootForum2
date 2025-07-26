package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.domain.Comment;
import com.github.chipolaris.bootforum2.domain.Discussion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service dedicated to managing Hibernate Search indexes.
 */
@Service
public class IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Triggers a full re-index of specified entities in a background thread.
     * This method will first purge the existing index for the target entities.
     *
     * @param target a string representing the entity to re-index ("all", "Discussion", "Comment", etc.).
     */
    @Async // Ensures this long-running task doesn't block the caller's thread.
    @Transactional
    public void reindex(String target) {
        logger.info("Received request to re-index target: '{}'", target);

        try {
            SearchSession searchSession = Search.session(entityManager);
            MassIndexer massIndexer;

            // FIX: Configure the MassIndexer with the target type from the start.
            if ("all".equalsIgnoreCase(target)) {
                logger.info("Configuring re-indexing for ALL indexed entities...");
                // massIndexer() with no arguments targets all indexed entities.
                massIndexer = searchSession.massIndexer();
            } else {
                Class<?> targetClass = resolveTargetClass(target);
                logger.info("Configuring re-indexing for entity: {}", targetClass.getSimpleName());
                // massIndexer(Class) targets a specific entity.
                massIndexer = searchSession.massIndexer(targetClass);
            }
            
            // Configure and run the MassIndexer
            massIndexer.purgeAllOnStart(true)      // Deletes existing index data before starting
                    .batchSizeToLoadObjects(25) // Tune based on your entity size and memory
                    .threadsToLoadObjects(4);   // Tune based on your server's core count

            logger.info("Starting re-indexing process for target '{}'...", target);

            // startAndWait() blocks this async thread until completion, not the web thread.
            // It takes no arguments.
            massIndexer.startAndWait();

            logger.info("Successfully completed re-indexing for target: '{}'", target);

        } catch (ClassNotFoundException e) {
            logger.error("Invalid target class for re-indexing: '{}'. Please provide a valid @Indexed entity name.", target);
        } catch (InterruptedException e) {
            logger.error("Re-indexing process was interrupted for target: '{}'", target, e);
            // It's good practice to re-interrupt the thread when catching InterruptedException
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("An unexpected error occurred during re-indexing for target: '{}'", target, e);
        }
    }

    /**
     * Resolves a string name to a class type.
     *
     * @param className The simple name of the class (e.g., "Discussion").
     * @return The resolved Class object.
     * @throws ClassNotFoundException if the name does not match a known indexed entity.
     */
    private Class<?> resolveTargetClass(String className) throws ClassNotFoundException {
        // This can be expanded with a more dynamic mechanism if you have many entities,
        // but a switch is clear and effective for a known set.
        return switch (className.toLowerCase()) {
            case "discussion" -> Discussion.class;
            case "comment" -> Comment.class;
            // Add other @Indexed entities here as needed
            default -> throw new ClassNotFoundException("No indexed entity found for target: " + className);
        };
    }
}