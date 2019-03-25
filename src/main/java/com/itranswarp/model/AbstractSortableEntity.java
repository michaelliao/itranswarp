package com.itranswarp.model;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractSortableEntity extends AbstractEntity {

	@Column(nullable = false)
	public long displayOrder;

}
