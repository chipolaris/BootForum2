package com.github.chipolaris.bootforum2.dao.dynamic;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DynamicDAO {

    private static final Logger log = LoggerFactory.getLogger(DynamicDAO.class);

    @PersistenceContext
    protected EntityManager entityManager;

    public <E> List<E> findEntities(Class<E> entityClass, List<DynamicCriteriaQueryBuilder.DynamicFilter> filters) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery(entityClass);
        Root<E> root = criteriaQuery.from(entityClass);

        DynamicCriteriaQueryBuilder<E> queryBuilder =
                new DynamicCriteriaQueryBuilder<>(criteriaBuilder, root, filters);

        List<Predicate> predicates = queryBuilder.buildPredicates();

        criteriaQuery.select(root);
        criteriaQuery.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));

        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    public <E> boolean exists(Class<E> entityClass, List<DynamicCriteriaQueryBuilder.DynamicFilter> filters) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<E> root = countQuery.from(entityClass);

        DynamicCriteriaQueryBuilder<E> queryBuilder =
                new DynamicCriteriaQueryBuilder<>(criteriaBuilder, root, filters);

        List<Predicate> predicates = queryBuilder.buildPredicates();

        countQuery.select(criteriaBuilder.count(root));
        countQuery.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));

        Long countResult = entityManager.createQuery(countQuery).getSingleResult();
        return countResult != null && countResult > 0;
    }

    public <E> long count(Class<E> entityClass, List<DynamicCriteriaQueryBuilder.DynamicFilter> filters) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<E> root = countQuery.from(entityClass);

        DynamicCriteriaQueryBuilder<E> queryBuilder =
                new DynamicCriteriaQueryBuilder<>(criteriaBuilder, root, filters);

        List<Predicate> predicates = queryBuilder.buildPredicates();

        countQuery.select(criteriaBuilder.count(root));
        countQuery.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));

        Long countResult = entityManager.createQuery(countQuery).getSingleResult();
        return countResult;
    }


    /*public <E> Optional<E> findFirst(Class<E> entityClass, List<DynamicCriteriaQueryBuilder.DynamicFilter> filters) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery(entityClass);

        DynamicCriteriaQueryBuilder<E> queryBuilder =
                new DynamicCriteriaQueryBuilder<>(criteriaBuilder, entityClass, filters);

        List<Predicate> predicates = queryBuilder.buildPredicates();

        criteriaQuery.select(criteriaQuery.from(entityClass));
        criteriaQuery.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));

        TypedQuery<E> typedQuery = entityManager.createQuery(criteriaQuery);
        typedQuery.setMaxResults(1);

        List<E> results = typedQuery.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }*/

}
