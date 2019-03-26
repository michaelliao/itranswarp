package com.itranswarp.service;

import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.enums.Role;
import com.itranswarp.model.AbstractEntity;
import com.itranswarp.model.AbstractSortableEntity;
import com.itranswarp.model.User;
import com.itranswarp.redis.RedisService;
import com.itranswarp.util.ClassUtil;
import com.itranswarp.warpdb.WarpDb;

@Transactional
public class AbstractService<T extends AbstractEntity> {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected final int ITEMS_PER_PAGE = 10;

	private Class<T> entityClass;

	@Autowired
	protected WarpDb db;

	@Autowired
	protected RedisService redisService;

	public AbstractService() {
		this.entityClass = ClassUtil.getParameterizedType(this.getClass());
	}

	public T getById(Long id) {
		T t = this.db.fetch(entityClass, id);
		if (t == null) {
			throw new ApiException(ApiError.ENTITY_NOT_FOUND, entityClass.getSimpleName(),
					entityClass.getSimpleName() + " not found");
		}
		return t;
	}

	public T fetchById(Long id) {
		return this.db.fetch(entityClass, id);
	}

	protected void checkPermission(User user, long entityUserId) {
		if (user.role != Role.ADMIN && user.id != entityUserId) {
			throw new ApiException(ApiError.PERMISSION_DENIED);
		}
	}

	protected void sortEntities(List<? extends AbstractSortableEntity> entities, List<Long> ids) {
		if (ids == null) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "ids", "Invalid ids.");
		}
		if (entities.size() != ids.size() || entities.size() != new HashSet<>(ids).size()) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "ids", "Invalid ids.");
		}
		entities.forEach(entity -> {
			int n = ids.indexOf(entity.id);
			if (n == (-1)) {
				throw new ApiException(ApiError.PARAMETER_INVALID, "ids", "Invalid category ids.");
			}
			entity.displayOrder = n;
		});
		entities.forEach(entity -> {
			this.db.updateProperties(entity, "displayOrder");
		});
	}

}
