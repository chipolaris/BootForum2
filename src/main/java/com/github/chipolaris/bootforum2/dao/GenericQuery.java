package com.github.chipolaris.bootforum2.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Repository
public class GenericQuery {

	@PersistenceContext
	protected EntityManager entityManager;
	
	/**
	 * 
	 * @param <E>
	 * @param queryMeta
	 * @return
	 */
	public <E> List<E> findEntities(QueryMeta<E> queryMeta) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<E> query = builder.createQuery(queryMeta.getTargetEntityClass());

		Root<?> root = query.from(queryMeta.getRootEntityClass());
		
		// if queryMeta.targetPath is null, the selection/projection/result will default to root 
		if(queryMeta.getTargetPath() != null) {
			query.select(getPathGeneric(root, queryMeta.getTargetPath()));
		}
		
		List<Predicate> predicateList = null;
		
		if(queryMeta.getFilters() != null) {
			predicateList = buildPredicates(builder, root, queryMeta.getFilters());
		}
		
		if(predicateList != null && !predicateList.isEmpty()) {
			Predicate[] predicates = new Predicate[predicateList.size()];
			predicateList.toArray(predicates);
			query.where(predicates);
		}
		
		if(queryMeta.getSortField() != null && !queryMeta.getSortField().isBlank()) {
			query.orderBy(Boolean.TRUE.equals(queryMeta.getSortDesc()) ?
				builder.desc(getPathGeneric(root, queryMeta.getSortField())) : 
				builder.asc(getPathGeneric(root, queryMeta.getSortField()))
				);
		}
		
		TypedQuery<E> typedQuery = entityManager.createQuery(query);
    	
		if(queryMeta.getStartIndex() != null) {
			typedQuery.setFirstResult(queryMeta.getStartIndex());
		}
		if(queryMeta.getMaxResult() != null) {
			typedQuery.setMaxResults(queryMeta.getMaxResult());
		}
		
		return typedQuery.getResultList();
	}
	
	/**
	 * 
	 * @param <E>
	 * @param queryMeta
	 * @return
	 */
	public <E> long countEntities(QueryMeta<E> queryMeta) {
		
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> query = builder.createQuery(Long.class);

		Root<?> root = query.from(queryMeta.getRootEntityClass());
		
		if(queryMeta.getTargetPath() != null) {
			query.select(builder.count(getPathGeneric(root, queryMeta.getTargetPath())));
		}
		else {
			query.select(builder.count(root));
		}
		
		List<Predicate> predicateList = null;
		
		if(queryMeta.getFilters() != null) {
			predicateList = buildPredicates(builder, root, queryMeta.getFilters());
		}
		
		if(predicateList != null && !predicateList.isEmpty()) {
			Predicate[] predicates = new Predicate[predicateList.size()];
			predicateList.toArray(predicates);
			query.where(predicates);
		}
	
		return entityManager.createQuery(query).getSingleResult();
	}
	
	private <E> List<Predicate> buildPredicates(CriteriaBuilder builder, Root<?> root, Map<String, List<FilterMeta>> filters) {
		
		List<Predicate> predicateList = new ArrayList<>();
		
		for (String key : filters.keySet()) {
			
			List<FilterMeta> filterMetas = filters.get(key);
			
			for(FilterMeta meta : filterMetas) {
				
				Predicate predicate = null;
				
				switch (meta.getMatchMode()) {

					case STARTS_WITH:
						predicate = builder.like(getPathGeneric(root, key), meta.getValue() + "%");
						break;
						
					case ENDS_WITH:
						predicate = builder.like(getPathGeneric(root, key), "%" + meta.getValue());
						break;
					
					case CONTAINS:
						predicate = builder.like(getPathGeneric(root, key), "%" + meta.getValue() + "%");
						break;
					
					case NOT_CONTAINS:
						predicate = builder.not(builder.like(getPathGeneric(root, key), "%" + meta.getValue() + "%"));
						break;	
						
					case EQUALS:
						predicate = builder.equal(getPathGeneric(root, key), meta.getValue());
						break;
						
					case NOT_EQUALS:
						predicate = builder.notEqual(getPathGeneric(root, key), meta.getValue());
						break;
					
					case LESS_THAN:
						predicate = builder.lessThan(GenericQuery.<Comparable>getPathGeneric(root, key), (Comparable)meta.getValue());
						break;
					
					case LESS_THAN_OR_EQUAL_TO:
						predicate = builder.lessThanOrEqualTo(GenericQuery.<Comparable>getPathGeneric(root, key), (Comparable)meta.getValue());
						break;
					
					case GREATER_THAN:
						if(meta.getValue() instanceof Comparable) {
							predicate = builder.greaterThan(GenericQuery.<Comparable>getPathGeneric(root, key), (Comparable)meta.getValue());
						}
						break;
						
					case GREATER_THAN_OR_EQUAL_TO:
						if(meta.getValue() instanceof Comparable) {
							predicate = builder.greaterThanOrEqualTo(GenericQuery.<Comparable>getPathGeneric(root, key), (Comparable)meta.getValue());
						}
						break;
						
					case DATE_BEFORE:
						if(meta.getValue().getClass() == Date.class) {
							predicate = builder.lessThan(GenericQuery.<Date>getPathGeneric(root, key), (Date)meta.getValue());
						}
						break;
						
					case DATE_AFTER:
						if(meta.getValue().getClass() == Date.class) {
							predicate = builder.greaterThan(GenericQuery.<Date>getPathGeneric(root, key), (Date)meta.getValue());
						}
						break;
						
					case DATE_IS:
						if(meta.getValue().getClass() == Date.class) {
							predicate = builder.equal(GenericQuery.<Date>getPathGeneric(root, key), (Date)meta.getValue());
						}
						break;
					
					case DATE_IS_NOT:
						if(meta.getValue().getClass() == Date.class) {
							predicate = builder.notEqual(GenericQuery.<Date>getPathGeneric(root, key), (Date)meta.getValue());
						}
						break;
					
					default:
						break;
				}
				
				if(predicate != null) {
					predicateList.add(predicate);
				}
			}
			
		}
		
		return predicateList;
	}
	
	/**
	 * Helper method to build Path expression, given the root object and 
	 * 	expression string
	 * @param <E>
	 * @param root
	 * @param pathExpression
	 * @return
	 */
	static private <E> Path<E> getPathGeneric(Root<?> root, String pathExpression) {
		
		String[] paths = pathExpression.split("\\."); 
		if(paths.length > 1) {
			String lastPathEntry = paths[paths.length - 1]; // last entry
			Join<?,?> join = null;
			for(int i = 0; i < paths.length - 1; i++) {
				if(join == null) {
					join = root.join(paths[i]);
				}
				else {
					join = join.join(paths[i]);
				}
			}
			
			return join.<E>get(lastPathEntry);
		}
		
		return root.<E>get(pathExpression);
	}
}
