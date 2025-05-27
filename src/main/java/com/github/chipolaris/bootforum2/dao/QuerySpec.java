package com.github.chipolaris.bootforum2.dao;

import java.util.ArrayList;
import java.util.List;

public class QuerySpec {

    private QuerySpec(Class targetEntity) {
        this.targetEntity = targetEntity;
        this.filters = new ArrayList<FilterSpec>();
        this.orders = new ArrayList<OrderSpec>();
    }

    // Target entity is result type (select)
    private Class targetEntity;
    // Root entity to query from
    private Class rootEntity;
    // Target path to query, it can be nested path like "address.city"
    private String targetPath;
    private Integer startIndex;
    private Integer maxResult;
    private List<OrderSpec> orders;
    private List<FilterSpec> filters;

    public Class getTargetEntity() {
        return targetEntity;
    }
    public void setTargetEntity(Class targetEntity) {
        this.targetEntity = targetEntity;
    }

    public Class getRootEntity() {
        return rootEntity;
    }
    public void setRootEntity(Class rootEntity) {
        this.rootEntity = rootEntity;
    }

    public String getTargetPath() {
        return targetPath;
    }
    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public Integer getStartIndex() {
        return startIndex;
    }
    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    public Integer getMaxResult() {
        return maxResult;
    }
    public void setMaxResult(Integer maxResult) {
        this.maxResult = maxResult;
    }

    public List<OrderSpec> getOrders() {
        return orders;
    }
    public void setOrder(List<OrderSpec> orders) {
        this.orders = orders;
    }

    public List<FilterSpec> getFilters() {
        return filters;
    }
    public void setFilters(List<FilterSpec> filters) {
        this.filters = filters;
    }


    //
    public static Builder builder(Class targetEntity) {
        return new Builder (targetEntity);
    }

    public static class Builder<T> {

        private QuerySpec querySpec;

        // make constructor private to prevent instantiation from outside
        private Builder(Class targetEntity) {
            this.querySpec = new QuerySpec(targetEntity);
        }

        public Builder<T> rootEntityClass(Class rootEntity) {
            querySpec.rootEntity = rootEntity;
            return this;
        }

        public Builder<T> targetEntity(Class targetEntity) {
            querySpec.targetEntity = targetEntity;
            return this;
        }

        public Builder<T> targetPath(String targetPath) {
            querySpec.targetPath = targetPath;
            return this;
        }

        public Builder<T> startIndex(Integer startIndex) {
            querySpec.startIndex = startIndex;
            return this;
        }

        public Builder<T> maxResult(Integer maxResult) {
            querySpec.maxResult = maxResult;
            return this;
        }

        public Builder<T> order(OrderSpec order) {
            querySpec.orders.add(order);
            return this;
        }

        public Builder<T> orders(List<OrderSpec> orders) {
            querySpec.orders.addAll(orders);
            return this;
        }

        public Builder<T> filter(FilterSpec filter) {
            querySpec.filters.add(filter);
            return this;
        }

        public Builder<T> filters(List<FilterSpec> filters) {
            querySpec.filters.addAll(filters);
            return this;
        }

        //
        public QuerySpec build() {
            // if rootEntity is not set, set it to targetEntity
            if(querySpec.rootEntity == null) {
                querySpec.rootEntity = querySpec.targetEntity;
            }
            return this.querySpec;
        }

    }
}
