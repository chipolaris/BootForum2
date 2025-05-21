package com.github.chipolaris.bootforum2.dao;

/**
 * @param field: the field name to compare
 * @param operator: EQ, NE, GT, GTE, LT, LTE, BETWEEN, NOT_BETWEEN, LIKE, NOT_LIKE, IN, NOT_IN, IS_NULL, IS_NOT_NULL
 * @param value: the value to compare with
 * @param valueTo: the value to compare with if the operator is BETWEEN
 */
public record FilterSpec(
        String field,
        Operator operator,
        Object value,
        Object valueTo
) {
    // factory methods to construct a DynamicFilter object with different parameter configurations
    public static FilterSpec of(String field, Operator operator, Object value) {
        return new FilterSpec(field, operator, value, null);
    }

    public static FilterSpec eq(String field, Object value) {
        return new FilterSpec(field, Operator.EQ, value, null);
    }

    public static FilterSpec ne(String field, Object value) {
        return new FilterSpec(field, Operator.NE, value, null);
    }

    public static FilterSpec between(String field, Object valueFrom, Object valueTo) {
        return new FilterSpec(field, Operator.BETWEEN, valueFrom, valueTo);
    }

    public static FilterSpec notBetween(String field, Object valueFrom, Object valueTo) {
        return new FilterSpec(field, Operator.NOT_BETWEEN, valueFrom, valueTo);
    }

    public static FilterSpec like(String field, Object value) {
        return new FilterSpec(field, Operator.LIKE, value, null);
    }

    public static FilterSpec notLike(String field, Object value) {
        return new FilterSpec(field, Operator.NOT_LIKE, value, null);
    }

    public static FilterSpec isNull(String field) {
        return new FilterSpec(field, Operator.IS_NULL, null, null);
    }

    public static FilterSpec isNotNull(String field) {
        return new FilterSpec(field, Operator.IS_NOT_NULL, null, null);
    }

    /**
     *
     * @param field
     * @param value is expected to be an array of objects or a Collection of objects
     * @return
     */
    public static FilterSpec in(String field, Object value) {
        return new FilterSpec(field, Operator.IN, value, null);
    }

    /**
     *
     * @param field
     * @param value is expected to be an array of objects or a Collection of objects
     * @return
     */
    public static FilterSpec notIn(String field, Object value) {
        return new FilterSpec(field, Operator.NOT_IN, value, null);
    }

    public enum Operator {
        EQ, NE, GT, GTE, LT, LTE, BETWEEN, NOT_BETWEEN, LIKE, NOT_LIKE, IN, NOT_IN, IS_NULL, IS_NOT_NULL
    }
}

