package com.github.chipolaris.bootforum2.dao.dynamic;

/**
 * @param field: the field name to compare
 * @param operator: the operator EQ, NE, GT, GTE, LT, LTE, BETWEEN, LIKE, IN, ISNULL
 * @param value: the value to compare with
 * @param valueTo: the value to compare with if the operator is BETWEEN
 */
public record DynamicFilter(
        String field,
        Operator operator,
        Object value,
        Object valueTo
) {
    // factory method to construct a DynamicFilter object without the valueTo parameter
    public static DynamicFilter of(String field, Operator operator, Object value) {
        return new DynamicFilter(field, operator, value, null);
    }

    public enum Operator {
        EQ, NE, GT, GTE, LT, LTE, BETWEEN, LIKE, IN, ISNULL
    }
}

