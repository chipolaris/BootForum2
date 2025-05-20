package com.github.chipolaris.bootforum2.dao.dynamic;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Repository
public class DynamicDAO {

    private static final Logger log = LoggerFactory.getLogger(DynamicDAO.class);

    @PersistenceContext
    protected EntityManager entityManager;

    // public methods

    public <E> List<E> findEntities(DynamicQuery queryMeta) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery(queryMeta.getTargetEntity());
        Root<?> root = criteriaQuery.from(queryMeta.getRootEntity());

        // if query.targetPath is null, the selection/projection/result will default to root
        if(queryMeta.getTargetPath() != null) {
            criteriaQuery.select((Selection<? extends E>) resolvePath(root, queryMeta.getTargetPath()));
        }

        // if filters list is specified, apply filters to the query
        List<DynamicFilter> filters = queryMeta.getFilters();
        if(filters != null && !filters.isEmpty()) {
            List<Predicate> predicates = buildPredicates(criteriaBuilder, root, filters);

            if (!predicates.isEmpty()) {
                criteriaQuery.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));
            }
        }

        // if sortField is specified, apply sorting to the query
        String sortField = queryMeta.getSortField();
        if(sortField != null && !sortField.isBlank()) {
            criteriaQuery.orderBy(Boolean.TRUE.equals(queryMeta.getSortDesc()) ?
                    criteriaBuilder.desc(resolvePath(root, sortField)) :
                    criteriaBuilder.asc(resolvePath(root, sortField))
            );
        }

        TypedQuery<E> typedQuery = entityManager.createQuery(criteriaQuery);

        // if startIndex and maxResult are specified, apply pagination to the query
        if(queryMeta.getStartIndex() != null) {
            typedQuery.setFirstResult(queryMeta.getStartIndex());
        }
        if(queryMeta.getMaxResult() != null) {
            typedQuery.setMaxResults(queryMeta.getMaxResult());
        }

        return typedQuery.getResultList();
    }

    public <E> long countEntities(DynamicQuery queryMeta) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);

        Root<?> root = criteriaQuery.from(queryMeta.getRootEntity());

        if(queryMeta.getTargetPath() != null && !queryMeta.getTargetPath().isBlank()) {
            criteriaQuery.select(criteriaBuilder.count(resolvePath(root, queryMeta.getTargetPath())));
        }
        else {
            criteriaQuery.select(criteriaBuilder.count(root));
        }

        if(!queryMeta.getFilters().isEmpty()) {
            List<Predicate> predicateList = buildPredicates(criteriaBuilder, root, queryMeta.getFilters());
            criteriaQuery.where(criteriaBuilder.and(predicateList.toArray(new Predicate[0])));
        }

        return entityManager.createQuery(criteriaQuery).getSingleResult();
    }

    /**
     * Check if any entity exists that matches the given DynamicQuery.
     * This method attempts to fetch at most one record, avoid using count like the @{@link #countEntities(DynamicQuery)}
     *
     * @param queryMeta The DynamicQuery defining the criteria.
     * @param <E>       The type of the entity, used for context if targetPath is not specified.
     * @return true if at least one entity exists, false otherwise.
     */
    public <E> boolean isExist(DynamicQuery queryMeta) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> criteriaQuery = criteriaBuilder.createQuery(Object.class);
        Root<?> root = criteriaQuery.from(queryMeta.getRootEntity());

        // Select a minimal piece of data. If targetPath is specified, use it. Otherwise, select the ID or a literal.
        if (queryMeta.getTargetPath() != null && !queryMeta.getTargetPath().isBlank()) {
            criteriaQuery.select(resolvePath(root, queryMeta.getTargetPath()));
        } else {
            // For maximum optimization for existence, a literal is best.
            criteriaQuery.select(criteriaBuilder.literal(1)); // Select a constant value
        }

        // Apply filters if they exist
        List<DynamicFilter> filters = queryMeta.getFilters();
        if (filters != null && !filters.isEmpty()) {
            List<Predicate> predicateList = buildPredicates(criteriaBuilder, root, filters);
            if (!predicateList.isEmpty()) {
                criteriaQuery.where(criteriaBuilder.and(predicateList.toArray(new Predicate[0])));
            }
        }

        TypedQuery<?> typedQuery = entityManager.createQuery(criteriaQuery);
        typedQuery.setMaxResults(1); // Crucial optimization: only need to find one record

        return !typedQuery.getResultList().isEmpty();
    }

    // ------
    // internal methods
    private List<Predicate> buildPredicates(CriteriaBuilder criteriaBuilder, Root<?> root, List<DynamicFilter> filters) {
        List<Predicate> predicates = new ArrayList<>();

        for (DynamicFilter filter : filters) {
            try {
                predicates.add(buildPredicate(criteriaBuilder, root, filter));
            } catch (IllegalArgumentException e) {
                log.warn("Skipping invalid filter: {} due to {}", filter, e.getMessage());
            }
        }

        return predicates;
    }

    private Predicate buildPredicate(CriteriaBuilder criteriaBuilder, Root<?> root, DynamicFilter filter) {
        Path<?> path = resolvePath(root, filter.field());
        Object value = filter.value();
        Object valueTo = filter.valueTo();

        switch (filter.operator()) {
            case EQ:
                return criteriaBuilder.equal(path, value);

            case NE:
                return criteriaBuilder.notEqual(path, value);

            case GT:
                if (value instanceof Comparable<?> comparable) {
                    return greaterThanComparable(criteriaBuilder, path, comparable);
                }
                throw new IllegalArgumentException("GT operator requires a Comparable value");

            case GTE:
                if (value instanceof Comparable<?> comparable) {
                    return greaterThanOrEqualToComparable(criteriaBuilder,path, comparable);
                }
                throw new IllegalArgumentException("GTE operator requires a Comparable value");

            case LT:
                if (value instanceof Comparable<?> comparable) {
                    return lessThanComparable(criteriaBuilder, path, comparable);
                }
                throw new IllegalArgumentException("LT operator requires a Comparable value");

            case LTE:
                if (value instanceof Comparable<?> comparable) {
                    return lessThanOrEqualToComparable(criteriaBuilder, path, comparable);
                }
                throw new IllegalArgumentException("LTE operator requires a Comparable value");

            case BETWEEN:
                if (value instanceof Comparable<?> val1 && valueTo instanceof Comparable<?> val2) {
                    return betweenComparable(criteriaBuilder, path, val1, val2);
                }
                throw new IllegalArgumentException("BETWEEN operator requires two Comparable values");

            case LIKE:
                return criteriaBuilder.like(path.as(String.class), "%" + value + "%");

            case IN:
                if (value instanceof Collection<?> collection) {
                    return path.in(collection);
                }
                throw new IllegalArgumentException("IN operator requires a Collection value");

            case ISNULL:
                return Boolean.TRUE.equals(value)
                        ? criteriaBuilder.isNull(path)
                        : criteriaBuilder.isNotNull(path);

            default:
                throw new IllegalArgumentException("Unknown operator: " + filter.operator());
        }
    }

    private Path<?> resolvePath(From<?, ?> root, String field) {
        String[] parts = field.split("\\.");
        Path<?> path = root;
        for (String part : parts) {
            path = path.get(part);
        }
        return path;
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<? super T>> Predicate greaterThanComparable(CriteriaBuilder criteriaBuilder, Path<?> path, Comparable<?> value) {
        Expression<T> expression = (Expression<T>) path.as(value.getClass());
        return criteriaBuilder.greaterThan(expression, (T) value);
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<? super T>> Predicate greaterThanOrEqualToComparable(CriteriaBuilder criteriaBuilder, Path<?> path, Comparable<?> value) {
        Expression<T> expression = (Expression<T>) path.as(value.getClass());
        return criteriaBuilder.greaterThanOrEqualTo(expression, (T) value);
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<? super T>> Predicate lessThanComparable(CriteriaBuilder criteriaBuilder, Path<?> path, Comparable<?> value) {
        Expression<T> expression = (Expression<T>) path.as(value.getClass());
        return criteriaBuilder.lessThan(expression, (T) value);
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<? super T>> Predicate lessThanOrEqualToComparable(CriteriaBuilder criteriaBuilder, Path<?> path, Comparable<?> value) {
        Expression<T> expression = (Expression<T>) path.as(value.getClass());
        return criteriaBuilder.lessThanOrEqualTo(expression, (T) value);
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<? super T>> Predicate betweenComparable(CriteriaBuilder criteriaBuilder, Path<?> path, Comparable<?> from, Comparable<?> to) {
        Expression<T> expression = (Expression<T>) path.as(from.getClass());
        return criteriaBuilder.between(expression, (T) from, (T) to);
    }
}
