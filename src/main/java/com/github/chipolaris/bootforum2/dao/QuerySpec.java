package com.github.chipolaris.bootforum2.dao;

import java.util.ArrayList;
import java.util.List;

public class QuerySpec {

    private QuerySpec(Class targetEntity) {
        this.targetEntity = targetEntity;
        this.filters = new ArrayList<FilterSpec>();
    }

    // Target entity is result type (select)
    private Class targetEntity;
    // Root entity to query from
    private Class rootEntity;
    // Target path to query, it can be nested path like "address.city"
    private String targetPath;
    private Integer startIndex;
    private Integer maxResult;
    private String sortField;
    private Boolean sortDesc;
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

    public String getSortField() {
        return sortField;
    }
    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public Boolean getSortDesc() {
        return sortDesc;
    }
    public void setSortDesc(Boolean sortDesc) {
        this.sortDesc = sortDesc;
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

        public Builder<T> sortField(String sortField) {
            querySpec.sortField = sortField;
            return this;
        }

        public Builder<T> sortDesc(Boolean sortDesc) {
            querySpec.sortDesc = sortDesc;
            return this;
        }

        public Builder<T> filter(FilterSpec filter) {
            querySpec.filters.add(filter);
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
