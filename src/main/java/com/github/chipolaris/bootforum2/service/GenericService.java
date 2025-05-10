package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.GenericQuery;
import com.github.chipolaris.bootforum2.dao.QueryMeta;
import com.github.chipolaris.bootforum2.domain.BaseEntity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @Transactional
public class GenericService {

	@Resource
	private GenericDAO genericDAO;
	
	@Resource
	private GenericQuery genericQuery;
	
	/**
	 * Save new instance of the given entity
	 * @param <E>: entity type must be a subclass of BaseEntity
	 * @param entity: object to be saved
	 * @return
	 */
	@Transactional(readOnly=false)
	public <E> ServiceResponse<Void> saveEntity(E entity) {
		
		ServiceResponse<Void> response = new ServiceResponse<>();
		genericDAO.persist(entity);
		
		return response;
	}
	
	/**
	 * Find entity with the given type/class and entityId
	 * 	Note that this method is referred over the {@link #getEntity(Class, Long)} method.
	 *  However, make sure entityId is not null when invoking this method
	 * @param entityClass
	 * @param entityId (must not be null)
	 * @return
	 */
	@Transactional(readOnly=true)
	public <E> ServiceResponse<E> findEntity(Class<E> entityClass, Object entityId) {
		
		ServiceResponse<E> response = new ServiceResponse<E>();
		
		response.setDataObject(genericDAO.find(entityClass, entityId));
		
		return response;
	}
	
	/**
	 * Update the given entity
	 * @param <E>: entity type must be a subclass of BaseEntity
	 * @param entity: object to be updated
	 * @return
	 */
	@Transactional(readOnly=false)
	public <E> ServiceResponse<E> updateEntity(E entity) {
		
		ServiceResponse<E> response = new ServiceResponse<>();
		E mergedEntity = genericDAO.merge(entity);
		response.setDataObject(mergedEntity);
		
		return response;
	}
	
	/**
	 * 
	 * @param <E>
	 * @param entity
	 * @return
	 */
	@Transactional(readOnly=false)
	public <E>ServiceResponse<Void> deleteEntity(E entity) {
		
		ServiceResponse<Void> response = 
			new ServiceResponse<Void>();
		
		genericDAO.remove(entity);
		
		return response;
	}

	/**
	 * Delete an entity with the given class and id
	 * @param <E>
	 * @param entityClass
	 * @param id
	 * @return
	 */
	@Transactional(readOnly=false)
	public <E extends BaseEntity> boolean deleteEntity(Class<E> entityClass, Long id) {

		E entity = genericDAO.find(entityClass, id);

		if(entity != null) {
			genericDAO.remove(entity);
			return true;
		}
		return false;
	}


	/**
	 * Get all entities of the given entityClass
	 * Useful for retrieving all entities of a type to populate drop down list
	 * 
	 * @param <E>
	 * @param entityClass
	 * @return
	 */
	@Transactional(readOnly=true)
	public <E> ServiceResponse<List<E>> getAllEntities(Class<E> entityClass) {
		
		ServiceResponse<List<E>> response = new ServiceResponse<List<E>>();
		response.setDataObject((List<E>) genericDAO.findAll(entityClass));
		
		return response;
	}
	
	/**
	 * Count number of entities of a given type
	 * 
	 * @param <E>
	 * @param entityClass: entity type to count
	 * @return
	 */
	@Transactional(readOnly=true)
	public <E> ServiceResponse<Long> countEntities(Class<E> entityClass) {
		
		ServiceResponse<Long> response = new ServiceResponse<Long>();
		response.setDataObject(genericDAO.countEntities(entityClass));
		
		return response;
	}
	
	@Transactional(readOnly=true)
	public <E> ServiceResponse<Long> countEntities(QueryMeta<E> queryMeta) {
		
		ServiceResponse<Long> response = new ServiceResponse<Long>();
		response.setDataObject(genericQuery.countEntities(queryMeta));
		
		return response;
	}
	
	@Transactional(readOnly=true)
	public <E> ServiceResponse<List<E>> findEntities(QueryMeta<E> queryMeta) {
		
		ServiceResponse<List<E>> response = new ServiceResponse<List<E>>();
		
		response.setDataObject(genericQuery.findEntities(queryMeta));
		
		return response;
	}
	
}
