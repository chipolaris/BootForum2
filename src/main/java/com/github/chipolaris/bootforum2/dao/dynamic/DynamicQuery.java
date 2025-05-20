package com.github.chipolaris.bootforum2.dao.dynamic;


import com.github.chipolaris.bootforum2.dao.QueryMeta;

import java.util.ArrayList;
import java.util.List;

public class DynamicQuery {

    private DynamicQuery(Class targetEntity) {
        this.targetEntity = targetEntity;
        this.filters = new ArrayList<DynamicFilter>();
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
    private List<DynamicFilter> filters;

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

    public List<DynamicFilter> getFilters() {
        return filters;
    }
    public void setFilters(List<DynamicFilter> filters) {
        this.filters = filters;
    }


    //
    public static Builder builder(Class targetEntity) {
        return new Builder (targetEntity);
    }

    public static class Builder<T> {

        private DynamicQuery dynamicQuery;

        // make constructor private to prevent instantiation from outside
        private Builder(Class targetEntity) {
            this.dynamicQuery = new DynamicQuery(targetEntity);
        }

        public Builder<T> rootEntityClass(Class rootEntity) {
            dynamicQuery.rootEntity = rootEntity;
            return this;
        }

        public Builder<T> targetEntity(Class targetEntity) {
            dynamicQuery.targetEntity = targetEntity;
            return this;
        }

        public Builder<T> targetPath(String targetPath) {
            dynamicQuery.targetPath = targetPath;
            return this;
        }

        public Builder<T> startIndex(Integer startIndex) {
            dynamicQuery.startIndex = startIndex;
            return this;
        }

        public Builder<T> maxResult(Integer maxResult) {
            dynamicQuery.maxResult = maxResult;
            return this;
        }

        public Builder<T> sortField(String sortField) {
            dynamicQuery.sortField = sortField;
            return this;
        }

        public Builder<T> sortDesc(Boolean sortDesc) {
            dynamicQuery.sortDesc = sortDesc;
            return this;
        }

        public Builder<T> filter(DynamicFilter filter) {
            dynamicQuery.filters.add(filter);
            return this;
        }

        //
        public DynamicQuery build() {
            // if rootEntity is not set, set it to targetEntity
            if(dynamicQuery.rootEntity == null) {
                dynamicQuery.rootEntity = dynamicQuery.targetEntity;
            }
            return this.dynamicQuery;
        }

    }
}
