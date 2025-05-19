package com.github.chipolaris.bootforum2.dao.dynamic;

import jakarta.persistence.criteria.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DynamicCriteriaQueryBuilder<T> {

    private static final Logger log = LoggerFactory.getLogger(DynamicCriteriaQueryBuilder.class);


    private final CriteriaBuilder criteriaBuilder;

    private final List<DynamicFilter> filters;


    private final Root<T> root;

    public DynamicCriteriaQueryBuilder(CriteriaBuilder criteriaBuilder, Root<T> root, List<DynamicFilter> filters) {

        this.criteriaBuilder = criteriaBuilder;
        this.root = root;
        this.filters = filters != null ? filters : Collections.emptyList();
    }

    public List<Predicate> buildPredicates() {
        List<Predicate> predicates = new ArrayList<>();

        for (DynamicFilter filter : filters) {
            try {
                predicates.add(buildPredicate(filter, this.root));
            } catch (IllegalArgumentException e) {
                log.warn("Skipping invalid filter: {} due to {}", filter, e.getMessage());
            }
        }

        return predicates;
    }

    private Predicate buildPredicate(DynamicFilter filter, Root<T> root) {
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
                    return greaterThanComparable(path, comparable);
                }
                throw new IllegalArgumentException("GT operator requires a Comparable value");

            case GTE:
                if (value instanceof Comparable<?> comparable) {
                    return greaterThanOrEqualToComparable(path, comparable);
                }
                throw new IllegalArgumentException("GTE operator requires a Comparable value");

            case LT:
                if (value instanceof Comparable<?> comparable) {
                    return lessThanComparable(path, comparable);
                }
                throw new IllegalArgumentException("LT operator requires a Comparable value");

            case LTE:
                if (value instanceof Comparable<?> comparable) {
                    return lessThanOrEqualToComparable(path, comparable);
                }
                throw new IllegalArgumentException("LTE operator requires a Comparable value");

            case BETWEEN:
                if (value instanceof Comparable<?> val1 && valueTo instanceof Comparable<?> val2) {
                    return betweenComparable(path, val1, val2);
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
    private <T extends Comparable<? super T>> Predicate greaterThanComparable(Path<?> path, Comparable<?> value) {
        Expression<T> expression = (Expression<T>) path.as(value.getClass());
        return criteriaBuilder.greaterThan(expression, (T) value);
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<? super T>> Predicate greaterThanOrEqualToComparable(Path<?> path, Comparable<?> value) {
        Expression<T> expression = (Expression<T>) path.as(value.getClass());
        return criteriaBuilder.greaterThanOrEqualTo(expression, (T) value);
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<? super T>> Predicate lessThanComparable(Path<?> path, Comparable<?> value) {
        Expression<T> expression = (Expression<T>) path.as(value.getClass());
        return criteriaBuilder.lessThan(expression, (T) value);
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<? super T>> Predicate lessThanOrEqualToComparable(Path<?> path, Comparable<?> value) {
        Expression<T> expression = (Expression<T>) path.as(value.getClass());
        return criteriaBuilder.lessThanOrEqualTo(expression, (T) value);
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<? super T>> Predicate betweenComparable(Path<?> path, Comparable<?> from, Comparable<?> to) {
        Expression<T> expression = (Expression<T>) path.as(from.getClass());
        return criteriaBuilder.between(expression, (T) from, (T) to);
    }

    // Nested record to represent a filter
    public static record DynamicFilter(
            String field,
            Operator operator,
            Object value,
            Object valueTo
    ) {}

    public enum Operator {
        EQ, NE, GT, GTE, LT, LTE, BETWEEN, LIKE, IN, ISNULL
    }
}
