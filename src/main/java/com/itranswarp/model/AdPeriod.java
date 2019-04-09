package com.itranswarp.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "ad_periods")
public class AdPeriod extends AbstractEntity {

	@Column(nullable = false, updatable = false)
	public long userId;

	@Column(nullable = false, updatable = false)
	public long adSlotId;

	@Column(nullable = false, updatable = false)
	public long displayOrder;

	/**
	 * ISO date format like "2019-01-01".
	 */
	@Column(nullable = false, length = VAR_CHAR_DATE)
	public String startAt;

	/**
	 * ISO date format like "2019-02-01".
	 */
	@Column(nullable = false, length = VAR_CHAR_DATE)
	public String endAt;

}
