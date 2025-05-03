package com.github.chipolaris.bootforum2.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A generic DAO class to use in basic CRUD operations
 * for any entity
 */
@Repository
public class GenericDAO {
	
	@PersistenceContext//(unitName = "DealPostPersistenceUnit")
	protected EntityManager entityManager;
	
	/**
	 * Save an entity
	 * @param entity
	 */
    public void persist(Object entity) { 
    	entityManager.persist(entity);
    } 

    /**
     * Delete an entity
     * @param entity
     */
    public void remove(Object entity) { 
    	Object toBeDeleted = entityManager.merge(entity);
    	entityManager.remove(toBeDeleted);
    } 
    
    /**
     * Update an entity
     * @param entity
     * @return
     */
    public <E> E merge(E entity) { 
        return entityManager.merge(entity); 
    } 
    
    /**
     * Refresh an entity, useful to make
     * sure the data is the latest
     * @param entity
     */
    public void refresh(Object entity) { 
    	entityManager.refresh(entity); 
    }
    
    /**
     * Synchronize the persistence context to the underlying database.
 	 * Seldom used. But useful when need to explicitly write cached 
 	 * data to database. Noted the write will not be committed until
 	 * the commit is issued (e.g., by the transaction manager)
     */
    public void flush() {
    	entityManager.flush();
    }
    
    /**
     * Find the entity given the entityType (class name, and ID)
     * @param entityClass
     * @param id
     * @return
     */
	public <E> E find(Class<E> entityClass, Object id) { 
        return entityManager.find(entityClass, id); 
    }
	
	/**
	 * 
	 * @param <E>
	 * @param entityClass
	 * @param filters
	 * @return
	 */
	public <E> E findOne(Class<E> entityClass, Map<String, Object> filters) {
		
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<E> query = builder.createQuery(entityClass);

		Root<E> root = query.from(entityClass);
		query.select(root);
		
		Predicate[] predicates = buildPredicates(builder, root, filters);
		query.where(predicates);
		
		TypedQuery<E> typedQuery = entityManager.createQuery(query);
		typedQuery.setMaxResults(1);
		
		List<E> resultList = typedQuery.getResultList();
		
		if(resultList.isEmpty()) {
			return null;
		}
		
		return resultList.get(0);
	}
    
    /**
     * Find all entities of the given entity type
     * @param entityClass
     * @return
     */
	public <E> List<E> findAll(Class<E> entityClass) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery(entityClass);
		Root<E> entity = criteriaQuery.from(entityClass);
		criteriaQuery.select(entity);
		
		return entityManager.createQuery(criteriaQuery).getResultList();
    }
	
	/**
	 * Find all entities of the given entity type
	 * Also eager fetch the specified properties
	 * @param <E>
	 * @param entityClass
	 * @param joinFetchProperties
	 * @return
	 */
    public <E> List<E> findAll(Class<E> entityClass, List<String> joinFetchProperties) {
		
    	CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    	CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery(entityClass);
    	
    	Root<E> entity = criteriaQuery.from(entityClass);
    	
    	// fetch specified properties
    	for(String property : joinFetchProperties) {
    		entity.fetch(property, JoinType.LEFT);
    	}
    	
    	TypedQuery<E> typedQuery = entityManager.createQuery(criteriaQuery);
    	
    	return typedQuery.getResultList();
    }
    
    /**
     * Get entities of the given entityClass and 
     * within the pagination defined by the startPosition and the maxResult
     * 
     * @param <E>
     * @param entityClass
     * @param startPosition
     * @param maxResult
     * @return
     * 
     * TODO: cleanup (the original method name findEntitiesInBatch)
     */
    public <E> List<E> findBatch(Class<E> entityClass, int startPosition, int maxResult) {
    	    	
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery(entityClass);
		Root<E> entity = criteriaQuery.from(entityClass);
		criteriaQuery.select(entity);
		
		return entityManager.createQuery(criteriaQuery).setFirstResult(startPosition)
				.setMaxResults(maxResult).getResultList();
    }
    
    /**
     * Count all entities of the given entity type
     * @param entityClass
     * @return
     */
	public <E> long countEntities(Class<E> entityClass) {		
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
		criteriaQuery.select(criteriaBuilder.count(criteriaQuery.from(entityClass)));
		return entityManager.createQuery(criteriaQuery).getSingleResult();
    }
		
	/**
	 * @param <E>
	 * @param entityClass
	 * @param text
	 * @param fieldPaths
	 * @return
	 */
	public <E> Long searchTextCount(Class<E> entityClass, String text, List<String> fieldPaths) {
		
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		
		CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
		
		Root<E> root = criteriaQuery.from(entityClass);
		
		Predicate[] predicates = buildPredicates4TextSearch(criteriaBuilder, root, fieldPaths, text);
		criteriaQuery.select(criteriaBuilder.countDistinct(root));
		criteriaQuery.where(criteriaBuilder.or(predicates));
		
		TypedQuery<Long> typedQuery = entityManager.createQuery(criteriaQuery);
		
		return typedQuery.getSingleResult();
	}
	
	/**
	 * Search for entities (from entityClass) that contains the text in its structure
	 * 	(specified by the fieldPaths list)
	 * @param <E>
	 * @param entityClass
	 * @param text
	 * @param fieldPaths
	 * @param firstIndex
	 * @param pageSize
	 * @return
	 */
	public <E> List<E> searchText(Class<E> entityClass, String text, List<String> fieldPaths, int firstIndex, int pageSize) {
		
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		
		CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery(entityClass);
		
		Root<E> root = criteriaQuery.from(entityClass);
		
		Predicate[] predicates = buildPredicates4TextSearch(criteriaBuilder, root, fieldPaths, text);
		criteriaQuery.where(criteriaBuilder.or(predicates));
		criteriaQuery.distinct(true);
		
		TypedQuery<E> typedQuery = entityManager.createQuery(criteriaQuery);
		
		typedQuery.setFirstResult(firstIndex);
		typedQuery.setMaxResults(pageSize);
		
		return typedQuery.getResultList();
	}
	
	@SuppressWarnings("rawtypes")
	public <E> Integer deleteEquals(Class<E> entityClass, Map<String, Comparable> equalAttributes) {
		
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		
		CriteriaDelete<E> delete = builder.createCriteriaDelete(entityClass);
		
		// from
		Root<E> root = delete.from(entityClass);
		
		List<Predicate> predicateList = new ArrayList<Predicate>();
		
		// where
		for (String paramName : equalAttributes.keySet()) {
			
			Comparable value = equalAttributes.get(paramName);
			
			Predicate predicate = builder.equal(getPathGeneric(root, paramName), value);
			
			predicateList.add(predicate);
		}
		
		Predicate[] predicates = new Predicate[predicateList.size()];
		predicateList.toArray(predicates);
		delete.where(predicates);
		
		Query query = entityManager.createQuery(delete);
		
		return query.executeUpdate();
	}
	
	@SuppressWarnings("rawtypes")
	public <E> Integer deleteLessThan(Class<E> entityClass, Map<String, Comparable> lessThanAttributes) {
		
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		
		CriteriaDelete<E> delete = builder.createCriteriaDelete(entityClass);
		
		// from
		Root<E> root = delete.from(entityClass);
		
		List<Predicate> predicateList = new ArrayList<Predicate>();
		
		// where
		for (String paramName : lessThanAttributes.keySet()) {
			
			Comparable value = lessThanAttributes.get(paramName);
			
			Predicate predicate = builder.lessThan(getPathGeneric(root, paramName), value);
			
			predicateList.add(predicate);
		}
		
		Predicate[] predicates = new Predicate[predicateList.size()];
		predicateList.toArray(predicates);
		delete.where(predicates);
		
		Query query = entityManager.createQuery(delete);
		
		return query.executeUpdate();
	}
	
	@SuppressWarnings("rawtypes")
	public <E> Integer deleteGreaterThan(Class<E> entityClass, Map<String, Comparable> greaterThanAttributes) {
		
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		
		CriteriaDelete<E> delete = builder.createCriteriaDelete(entityClass);
		
		// from
		Root<E> root = delete.from(entityClass);
		
		List<Predicate> predicateList = new ArrayList<Predicate>();
		
		// where
		for (String paramName : greaterThanAttributes.keySet()) {
			
			Comparable value = greaterThanAttributes.get(paramName);
			
			Predicate predicate = builder.greaterThan(getPathGeneric(root, paramName), value);
			
			predicateList.add(predicate);
		}
		
		Predicate[] predicates = new Predicate[predicateList.size()];
		predicateList.toArray(predicates);
		delete.where(predicates);
		
		Query query = entityManager.createQuery(delete);
		
		return query.executeUpdate();
	}

	/**
	 * Helper method
	 * @param <E>
	 * @param filters
	 * @param builder
	 * @param root
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <E> Predicate[] buildPredicates(CriteriaBuilder builder, Root<E> root, Map<String, Object> filters) {
		List<Predicate> predicateList = new ArrayList<>();
		
		for (String paramName : filters.keySet()) {
			Object value = filters.get(paramName);
			
			Predicate predicate = null;
			
			if(value instanceof Entry) {
				@SuppressWarnings("rawtypes")
				Entry<Comparable, Comparable> valuePair = (Entry<Comparable, Comparable>) value;
				@SuppressWarnings("rawtypes")
				Comparable value1 = valuePair.getKey();
				@SuppressWarnings("rawtypes")
				Comparable value2 = valuePair.getValue();

				predicate = builder.between(getPathGeneric(root, paramName), value1, value2);
			}
			else {
				predicate = builder.equal(getPathGeneric(root, paramName), value);
			}
			
			predicateList.add(predicate);
		}
		
		Predicate[] predicates = new Predicate[predicateList.size()]; 
		predicateList.toArray(predicates);
		
		return predicates;
	}
	
	@SuppressWarnings("unchecked")
	private <E> Predicate[] buildNotPredicates(CriteriaBuilder builder, Root<E> root, Map<String, Object> filters) {
		List<Predicate> predicateList = new ArrayList<>();
		
		for (String paramName : filters.keySet()) {
			Object value = filters.get(paramName);
			
			Predicate predicate = null;
			
			if(value instanceof Entry) {
				@SuppressWarnings("rawtypes")
				Entry<Comparable, Comparable> valuePair = (Entry<Comparable, Comparable>) value;
				@SuppressWarnings("rawtypes")
				Comparable value1 = valuePair.getKey();
				@SuppressWarnings("rawtypes")
				Comparable value2 = valuePair.getValue();

				predicate = builder.not(builder.between(getPathGeneric(root, paramName), value1, value2));
			}
			else {
				predicate = builder.notEqual(getPathGeneric(root, paramName), value);
			}
			
			predicateList.add(predicate);
		}
		
		Predicate[] predicates = new Predicate[predicateList.size()]; 
		predicateList.toArray(predicates);
		
		return predicates;
	}	
	
	/**
	 * Helper method to create array of Predicate (	Predicate[] )
	 * @param <E>
	 * @param criteriaBuilder
	 * @param root
	 * @param searchText
	 * @param fieldPaths
	 * @return
	 */
	private <E> Predicate[] buildPredicates4TextSearch(CriteriaBuilder criteriaBuilder, Root<E> root,
			List<String> fieldPaths, String searchText) {
		
		List<Predicate> predicateList = new ArrayList<>();
		Predicate predicate = null;
		
		for(String fieldPath : fieldPaths) {
			predicate = criteriaBuilder.like(criteriaBuilder.upper(getPathGeneric(root, fieldPath)), "%"+searchText.toUpperCase()+"%");
			predicateList.add(predicate);
		}
		
		Predicate[] predicates = new Predicate[predicateList.size()];
		predicateList.toArray(predicates);
		
		return predicates;
	}
	
	/**
	 * Helper method to build Path expression, given the root object and 
	 * 	expression string
	 * @param <E>
	 * @param root
	 * @param pathExpression
	 * @return
	 */
	private <E> Path<E> getPathGeneric(Root<?> root, String pathExpression) {
		
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

	/**
	 * New methods go here
	 */

	/**
	 * Determine if an instance of the given entity class exists with the given [attributeName=attributeValue]
	 * @param entityClass
	 * @param attributeName
	 * @param attributeValue
	 * @return
	 * @param <T>
	 */
	public <T> boolean entityExists(Class<T> entityClass, String attributeName, Object attributeValue) {
		return entityExists(entityClass, Map.of(attributeName, attributeValue));
	}

	/**
	 * Determine if an instance of the given entity class exists with the given filters
	 * Where filters looks like [attribute1=attributeValue1, attribute2=attributeValue2, ...]
	 * @param entityClass
	 * @param filters
	 * @return
	 * @param <T>
	 */
	public <T> boolean entityExists(Class<T> entityClass, Map<String, Object> filters) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Integer> criteriaQuery = builder.createQuery(Integer.class);
		Root<T> root = criteriaQuery.from(entityClass);

		Predicate[] predicates = buildPredicates(builder, root, filters);

		criteriaQuery.select(builder.literal(1)).where(predicates);

		TypedQuery<Integer> typedQuery = entityManager.createQuery(criteriaQuery);
		typedQuery.setMaxResults(1); // LIMIT 1 for performance reasons

		return !typedQuery.getResultList().isEmpty();
	}

	/**
	 *
	 */
}