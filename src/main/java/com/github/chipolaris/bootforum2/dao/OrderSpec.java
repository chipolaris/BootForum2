package com.github.chipolaris.bootforum2.dao;

public record OrderSpec(String field, Boolean ascending) {

    public static OrderSpec asc(String field) {
        return new OrderSpec(field, true);
    }

    public static OrderSpec desc(String field) {
        return new OrderSpec(field, false);
    }
}
