package com.github.chipolaris.bootforum2.dao;

import jakarta.persistence.*;
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
	 * Check if an object with the given entity type and primary key
	 * @param entityClass
	 * @param id
	 * @return
	 * @param <E>
	 */
	public <E> boolean existsById(Class<E> entityClass, Object id) {
		/*
		 * This is a recommended way to check existence if id is the primary key
		 * This is backed by first level cache.
		 * And it avoids even a query if the entity is already managed.
		 */
		return entityManager.find(entityClass, id) != null;
	}

	/**
     * Find all entities of the given entity type
     * @param entityClass
     * @return
     */
	public <E> List<E> all(Class<E> entityClass) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery(entityClass);
		criteriaQuery.select(criteriaQuery.from(entityClass));
		return entityManager.createQuery(criteriaQuery).getResultList();
    }
    
    /**
     * Count all entities of the given entity type
     * @param entityClass
     * @return
     */
	public <E> long count(Class<E> entityClass) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
		criteriaQuery.select(criteriaBuilder.count(criteriaQuery.from(entityClass)));
		return entityManager.createQuery(criteriaQuery).getSingleResult();
    }

	/**
	 * Generic method to return the entity with the maximum value of a specified field.
	 *
	 * @param entityClass   The entity class
	 * @param fieldName     The field to compute max on
	 * @param <T>           The entity type
	 * @param <V>           The field type (must be Comparable)
	 * @return              The entity instance with the max field value
	 */
	public <T, V extends Comparable<? super V>> T greatest(
			Class<T> entityClass,
			String fieldName
	) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<T> cq = cb.createQuery(entityClass);
		Root<T> root = cq.from(entityClass);

		// Determine the class of the field V
		Path<V> fieldPathForType = getPathGeneric(root, fieldName);
		@SuppressWarnings("unchecked") // Safe cast due to V extends Comparable and path resolution
		Class<V> fieldClass = (Class<V>) fieldPathForType.getJavaType();

		// Subquery to find the max value of the field
		Subquery<V> subquery = cq.subquery(fieldClass);
		Root<T> subRoot = subquery.from(entityClass);
		subquery.select(cb.greatest(this.<V>getPathGeneric(subRoot, fieldName)));

		// Main query where field = max(field)
		cq.select(root).where(cb.equal(fieldPathForType, subquery));

		try {
			return entityManager.createQuery(cq)
					.setMaxResults(1) // In case of multiple entities with the same max value
					.getSingleResult();
		} catch (NoResultException e) {
			return null; // Return null if no entity is found (e.g., empty table)
		}
	}

	/**
	 * Generic method to return the entity with the minimum value of a specified field.
	 *
	 * @param entityClass   The entity class
	 * @param fieldName     The field to compute max on
	 * @param <T>           The entity type
	 * @param <V>           The field type (must be Comparable)
	 * @return              The entity instance with the max field value
	 */
	public <T, V extends Comparable<? super V>> T least(
			Class<T> entityClass,
			String fieldName
	) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<T> cq = cb.createQuery(entityClass);
		Root<T> root = cq.from(entityClass);

		// Determine the class of the field V
		Path<V> fieldPathForType = getPathGeneric(root, fieldName);
		@SuppressWarnings("unchecked") // Safe cast due to V extends Comparable and path resolution
		Class<V> fieldClass = (Class<V>) fieldPathForType.getJavaType();

		// Subquery to find the min value of the field
		Subquery<V> subquery = cq.subquery(fieldClass);
		Root<T> subRoot = subquery.from(entityClass);
		subquery.select(cb.least(this.<V>getPathGeneric(subRoot, fieldName)));

		// Main query where field = min(field)
		cq.select(root).where(cb.equal(fieldPathForType, subquery));

		try {
			return entityManager.createQuery(cq)
					.setMaxResults(1) // In case of multiple entities with the same min value
					.getSingleResult();
		} catch (NoResultException e) {
			return null; // Return null if no entity is found
		}
	}

	/*
	 * TODO: methods below are to be refactored to another class and/or to be removed
	 */
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
}