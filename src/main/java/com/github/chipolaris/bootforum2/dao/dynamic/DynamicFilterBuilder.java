package com.github.chipolaris.bootforum2.dao.dynamic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DynamicFilterBuilder {

    private final List<DynamicCriteriaQueryBuilder.DynamicFilter> filters = new ArrayList<>();

    public static DynamicFilterBuilder create() {
        return new DynamicFilterBuilder();
    }

    public DynamicFilterBuilder eq(String field, Object value) {
        filters.add(new DynamicCriteriaQueryBuilder.DynamicFilter(field, DynamicCriteriaQueryBuilder.Operator.EQ, value, null));
        return this;
    }

    public DynamicFilterBuilder ne(String field, Object value) {
        filters.add(new DynamicCriteriaQueryBuilder.DynamicFilter(field, DynamicCriteriaQueryBuilder.Operator.NE, value, null));
        return this;
    }

    public DynamicFilterBuilder gt(String field, Object value) {
        filters.add(new DynamicCriteriaQueryBuilder.DynamicFilter(field, DynamicCriteriaQueryBuilder.Operator.GT, value, null));
        return this;
    }

    public DynamicFilterBuilder gte(String field, Object value) {
        filters.add(new DynamicCriteriaQueryBuilder.DynamicFilter(field, DynamicCriteriaQueryBuilder.Operator.GTE, value, null));
        return this;
    }

    public DynamicFilterBuilder lt(String field, Object value) {
        filters.add(new DynamicCriteriaQueryBuilder.DynamicFilter(field, DynamicCriteriaQueryBuilder.Operator.LT, value, null));
        return this;
    }

    public DynamicFilterBuilder lte(String field, Object value) {
        filters.add(new DynamicCriteriaQueryBuilder.DynamicFilter(field, DynamicCriteriaQueryBuilder.Operator.LTE, value, null));
        return this;
    }

    public DynamicFilterBuilder between(String field, Object from, Object to) {
        filters.add(new DynamicCriteriaQueryBuilder.DynamicFilter(field, DynamicCriteriaQueryBuilder.Operator.BETWEEN, from, to));
        return this;
    }

    public DynamicFilterBuilder like(String field, String value) {
        filters.add(new DynamicCriteriaQueryBuilder.DynamicFilter(field, DynamicCriteriaQueryBuilder.Operator.LIKE, value, null));
        return this;
    }

    public DynamicFilterBuilder in(String field, Collection<?> values) {
        filters.add(new DynamicCriteriaQueryBuilder.DynamicFilter(field, DynamicCriteriaQueryBuilder.Operator.IN, values, null));
        return this;
    }

    public DynamicFilterBuilder isNull(String field, boolean isNull) {
        filters.add(new DynamicCriteriaQueryBuilder.DynamicFilter(field, DynamicCriteriaQueryBuilder.Operator.ISNULL, isNull, null));
        return this;
    }

    public List<DynamicCriteriaQueryBuilder.DynamicFilter> build() {
        return filters;
    }
}
