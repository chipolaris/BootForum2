package com.github.chipolaris.bootforum2.dao;

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
import java.util.Optional;

@Repository
public class DynamicDAO {

    private static final Logger logger = LoggerFactory.getLogger(DynamicDAO.class);

    @PersistenceContext
    protected EntityManager entityManager;

    /**
     * Find entities matching specs from querySpec
     *
     * An example to find all with sorting/paging
     *
     *      dynamicDAO.find(QuerySpec.builder(User.class).build().startIndex(0).maxResult(10).sortField("username").sortDesc(true).build())
     *
     * @param querySpec
     * @return
     * @param <E>
     */
    public <E> List<E> find(QuerySpec querySpec) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery(querySpec.getTargetEntity());
        Root<?> root = criteriaQuery.from(querySpec.getRootEntity());

        // if query.targetPath is null, the selection/projection/result will default to root
        if(querySpec.getTargetPath() != null) {
            criteriaQuery.select((Selection<? extends E>) resolvePath(root, querySpec.getTargetPath()));
        }

        // if filters list is specified, apply filters to the query
        List<FilterSpec> filters = querySpec.getFilters();
        if(filters != null && !filters.isEmpty()) {
            List<Predicate> predicates = buildPredicates(criteriaBuilder, root, filters);

            if (!predicates.isEmpty()) {
                criteriaQuery.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));
            }
        }

        // if orderSpecs are specified, apply sorting to the query
        List<OrderSpec> orderSpecs = querySpec.getOrders();
        if(orderSpecs != null && !orderSpecs.isEmpty()) {
            List<Order> orders = orderSpecs.stream()
                    .map(spec -> spec.ascending()
                            ? criteriaBuilder.asc(resolvePath(root, spec.field()))
                            : criteriaBuilder.desc(resolvePath(root, spec.field())))
                    .toList();
            criteriaQuery.orderBy(orders);
        }

        TypedQuery<E> typedQuery = entityManager.createQuery(criteriaQuery);

        // if startIndex and maxResult are specified, apply pagination to the query
        if(querySpec.getStartIndex() != null) {
            typedQuery.setFirstResult(querySpec.getStartIndex());
        }
        if(querySpec.getMaxResult() != null) {
            typedQuery.setMaxResults(querySpec.getMaxResult());
        }

        return typedQuery.getResultList();
    }

    /**
     * Finds a single entity matching the given QuerySpec, optimized for scenarios
     * where at most one result is expected.
     *
     * @param querySpec The QuerySpec defining the criteria.
     * @param <E>       The type of the entity.
     * @return An Optional containing the found entity, or an empty Optional if no entity matches.
     */
    public <E> Optional<E> findOptional(QuerySpec querySpec) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery(querySpec.getTargetEntity());
        Root<?> root = criteriaQuery.from(querySpec.getRootEntity());

        // if query.targetPath is null, the selection/projection/result will default to root
        if (querySpec.getTargetPath() != null) {
            criteriaQuery.select((Selection<? extends E>) resolvePath(root, querySpec.getTargetPath()));
        }
        // If targetPath is null, JPA defaults to selecting the root entity.

        // if filters list is specified, apply filters to the query
        List<FilterSpec> filters = querySpec.getFilters();
        if (filters != null && !filters.isEmpty()) {
            List<Predicate> predicates = buildPredicates(criteriaBuilder, root, filters);
            if (!predicates.isEmpty()) {
                criteriaQuery.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));
            }
        }

        // if sortField is specified, apply sorting to the query
        // Sorting is still relevant as you might want the "first" by a certain order.
        List<OrderSpec> orderSpecs = querySpec.getOrders();
        if(orderSpecs != null && !orderSpecs.isEmpty()) {
            List<Order> orders = orderSpecs.stream()
                    .map(spec -> spec.ascending()
                            ? criteriaBuilder.asc(resolvePath(root, spec.field()))
                            : criteriaBuilder.desc(resolvePath(root, spec.field())))
                    .toList();
            criteriaQuery.orderBy(orders);
        }

        TypedQuery<E> typedQuery = entityManager.createQuery(criteriaQuery);

        // Optimization: Only fetch the first result.
        // If startIndex is provided in QuerySpec, it will be respected.
        if (querySpec.getStartIndex() != null) {
            typedQuery.setFirstResult(querySpec.getStartIndex());
        }
        typedQuery.setMaxResults(1);


        List<E> results = typedQuery.getResultList();
        if (results.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(results.get(0));
        }
    }

    public <E> long count(QuerySpec querySpec) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);

        Root<?> root = criteriaQuery.from(querySpec.getRootEntity());

        if(querySpec.getTargetPath() != null && !querySpec.getTargetPath().isBlank()) {
            criteriaQuery.select(criteriaBuilder.count(resolvePath(root, querySpec.getTargetPath())));
        }
        else {
            criteriaQuery.select(criteriaBuilder.count(root));
        }

        // Ensure filters list is not null before checking if it's empty
        List<FilterSpec> filters = querySpec.getFilters();
        if(filters != null && !filters.isEmpty()) {
            List<Predicate> predicateList = buildPredicates(criteriaBuilder, root, filters);
            // Only add WHERE clause if there are actual predicates
            if (!predicateList.isEmpty()) {
                criteriaQuery.where(criteriaBuilder.and(predicateList.toArray(new Predicate[0])));
            }
        }

        return entityManager.createQuery(criteriaQuery).getSingleResult();
    }

    /**
     * Check if any entity exists that matches the given DynamicQuery.
     * This method attempts to fetch at most one record, avoid using count like the @{@link #count(QuerySpec)}
     *
     * @param querySpec The DynamicQuery defining the criteria.
     * @param <E>       The type of the entity, used for context if targetPath is not specified.
     * @return true if at least one entity exists, false otherwise.
     */
    public <E> boolean exists(QuerySpec querySpec) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> criteriaQuery = criteriaBuilder.createQuery(Object.class);
        Root<?> root = criteriaQuery.from(querySpec.getRootEntity());

        // Select a minimal piece of data. If targetPath is specified, use it. Otherwise, select the ID or a literal.
        if (querySpec.getTargetPath() != null && !querySpec.getTargetPath().isBlank()) {
            criteriaQuery.select(resolvePath(root, querySpec.getTargetPath()));
        } else {
            // For maximum optimization for existence, a literal is best.
            criteriaQuery.select(criteriaBuilder.literal(1)); // Select a constant value
        }

        // Apply filters if they exist
        List<FilterSpec> filters = querySpec.getFilters();
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
    private List<Predicate> buildPredicates(CriteriaBuilder criteriaBuilder, Root<?> root, List<FilterSpec> filters) {
        List<Predicate> predicates = new ArrayList<>();

        for (FilterSpec filter : filters) {
            try {
                predicates.add(buildPredicate(criteriaBuilder, root, filter));
            } catch (IllegalArgumentException e) {
                logger.warn("Skipping invalid filter: {} due to {}", filter, e.getMessage());
            }
        }

        return predicates;
    }

    private Predicate buildPredicate(CriteriaBuilder criteriaBuilder, Root<?> root, FilterSpec filter) {
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

            case NOT_BETWEEN:
                if (value instanceof Comparable<?> val1 && valueTo instanceof Comparable<?> val2) {
                    return criteriaBuilder.not(betweenComparable(criteriaBuilder, path, val1, val2));
                }
                throw new IllegalArgumentException("NOT_BETWEEN operator requires two Comparable values");

            case LIKE:
                return criteriaBuilder.like(path.as(String.class), "%" + value + "%");

            case NOT_LIKE:
                return criteriaBuilder.not(criteriaBuilder.like(path.as(String.class), "%" + value + "%"));

            case IN:
                if (value instanceof Collection<?> collection) {
                    return path.in(collection);
                }
                // Handle array case for IN operator if FilterSpec.in can pass varargs as array
                if (value != null && value.getClass().isArray()) {
                    return path.in((Object[]) value);
                }
                throw new IllegalArgumentException("IN operator requires a Collection or an array value");

            case NOT_IN:
                if (value instanceof Collection<?> collection) {
                    return path.in(collection).not();
                }
                // Handle array case for NOT_IN operator
                if (value != null && value.getClass().isArray()) {
                    return path.in((Object[]) value).not();
                }
                throw new IllegalArgumentException("NOT_IN operator requires a Collection or an array value");

            case IS_NULL:
                return criteriaBuilder.isNull(path);

            case IS_NOT_NULL:
                return criteriaBuilder.isNotNull(path);

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
