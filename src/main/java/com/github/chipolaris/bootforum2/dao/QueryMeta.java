package com.github.chipolaris.bootforum2.dao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class QueryMeta<E> {
	
	private QueryMeta(Class<E> targetEntityClass) {
		this.targetEntityClass = targetEntityClass;
	}
	
	private Class<E> targetEntityClass;
	private Class<?> rootEntityClass;
	private String targetPath;
	private Integer startIndex;
	private Integer maxResult;
	private String sortField;
	private Boolean sortDesc;
	private Map<String, List<FilterMeta>> filters;
	
	public Class<E> getTargetEntityClass() {
		return targetEntityClass;
	}
	public void setTargetEntityClass(Class<E> targetEntityClass) {
		this.targetEntityClass = targetEntityClass;
	}
	public Class<?> getRootEntityClass() {
		return rootEntityClass;
	}
	public void setRootEntityClass(Class<?> rootEntityClass) {
		this.rootEntityClass = rootEntityClass;
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
	public Map<String, List<FilterMeta>> getFilters() {
		return filters;
	}
	public void setFilters(Map<String, List<FilterMeta>> filters) {
		this.filters = filters;
	}
	
	public static <T> Builder<T> builder(Class<T> targetEntityClass) {
		return new Builder<T>(targetEntityClass);
	}
	
	public static final class Builder<T> {
		
		private QueryMeta<T> queryMeta;
		
		private List<List<String>> tempFilterMeta; // hold temporary filterMeta until build() is called
		
		// make constructor private to prevent instantiation from outside
		private Builder(Class<T> targetEntityClass) {
			
			this.queryMeta = new QueryMeta<T>(targetEntityClass);
			
			this.tempFilterMeta = new ArrayList<>();
		}
		
		public Builder<T> rootEntityClass(Class<?> rootEntityClass) {
			
			queryMeta.rootEntityClass = rootEntityClass;
			return this;
		}
		
		public Builder<T> targetEntityClass(Class<T> targetEntityClass) {
			
			queryMeta.targetEntityClass = targetEntityClass;
			return this;
		}
		
		public Builder<T> targetPath(String targetPath) {
			
			queryMeta.targetPath = targetPath;
			return this;
		}
		
		public Builder<T> startIndex(Integer startIndex) {
			
			queryMeta.startIndex = startIndex;
			return this;
		}
		
		public Builder<T> maxResult(Integer maxResult) {
			
			queryMeta.maxResult = maxResult;
			return this;
		}
		
		public Builder<T> sortField(String sortField) {
			
			queryMeta.sortField = sortField;
			return this;
		}
		
		
		
		public Builder<T> filterMeta(String field, String value, String matchMode, String operator) {
			
			if(field != null && value != null && matchMode != null && operator != null) {
				tempFilterMeta.add(Arrays.asList(field, value, matchMode, operator));
			}
			
			return this;
		}
		
		public Builder<T> sortDesc(Boolean sortDesc) {
			
			queryMeta.sortDesc = sortDesc;
			return this;
		}
		
		public QueryMeta<T> build() {
			
			if(queryMeta.rootEntityClass == null) {
				queryMeta.rootEntityClass = queryMeta.targetEntityClass;
			}
			
			buildFilterMeta();
			
			return queryMeta;
		}
		
		private void buildFilterMeta() {
			
			this.queryMeta.filters = new HashMap<>();
			
			for(List<String> filterMetaList : this.tempFilterMeta) {
				
				String field = filterMetaList.get(0);
				String value = filterMetaList.get(1);
				String matchMode = filterMetaList.get(2);
				String operator = filterMetaList.get(3);
				
				List<FilterMeta> filterMeta = queryMeta.filters.get(field);
				
				if(filterMeta == null) {
					filterMeta = new ArrayList<>();
					queryMeta.filters.put(field, filterMeta);
				}
				
				Object valueObject = null;
				
				try {
					valueObject = toObject(queryMeta.rootEntityClass.getMethod("get" + uppercase(field)).getReturnType(), value);
				} 
				catch (NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
				}
				
				if(valueObject != null) {
					filterMeta.add(new FilterMeta(valueObject, matchMode, operator));
				}
			}
		}

		private String uppercase(String str) {
			if(!str.isBlank()) {
				return str.substring(0, 1).toUpperCase() + str.substring(1);
			}
			return null;
		}

		// helper method
		// convert the param value to an actual object of 'className' 
		private static Object toObject(Class<?> theClass, String value) {
			
			if(theClass == String.class) {
				return value;
			}
			else if(theClass == Boolean.class) {
				return Boolean.valueOf(value);
			}
			else if(theClass == Short.class) {
				return Short.valueOf(value);
			}
			else if(theClass == Integer.class) {
				return Integer.valueOf(value);
			}
			else if(theClass == Long.class) {
				return Long.valueOf(value);
			}
			else if(theClass == Double.class) {
				return Double.valueOf(value);
			}
			else if(theClass == Date.class) {
				
				try {
					return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(value);
				} 
				catch (ParseException e) {
					return null;
				}
			}
			
			return null;
		}
	}
}
