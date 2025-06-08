package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.domain.BaseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

@Service @Transactional
public class GenericService {

	private static final Logger logger = LoggerFactory.getLogger(GenericService.class);

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private DynamicDAO dynamicDAO;
	
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
	 * @param <E>
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

	@Transactional(readOnly=true)
	public <E, D> ServiceResponse<D> findEntityDTO(Class<E> entityClass, Object entityId, Function<E, D> mapper) {

		ServiceResponse<D> response = new ServiceResponse<>();

		if (entityId == null) {
			logger.warn("Attempted to fetch entity with null ID.");
			return response.setAckCode(ServiceResponse.AckCodeType.FAILURE).addMessage("Entity ID cannot be null.");
		}

		E entity = genericDAO.find(entityClass, entityId);

		if(entity == null) {
			String error = String.format("Entity %s with id %d not found.", entityClass.getSimpleName(), entityId);
			return response.setAckCode(ServiceResponse.AckCodeType.FAILURE).addMessage(error);
		}

		try {
			return response.setDataObject(mapper.apply(entity));
		}
		catch (Exception e) {
			return response.setAckCode(ServiceResponse.AckCodeType.FAILURE).addMessage("Error mapping entity to DTO.");
		}
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

		response.setDataObject((List<E>) genericDAO.all(entityClass));
		
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

		response.setDataObject(genericDAO.count(entityClass));
		
		return response;
	}
}
