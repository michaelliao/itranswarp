package com.itranswarp.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "ad_periods")
public class AdPeriods extends AbstractEntity {

	@Column(nullable = false, updatable = false, length = VAR_ID)
	public String userId;

	@Column(nullable = false, updatable = false, length = VAR_ID)
	public String adSlotId;

	@Column(nullable = false, updatable = false)
	public long displayOrder;

	/**
	 * ISO date format like "2019-01-01".
	 */
	@Column(nullable = false, length = 10)
	public String startAt;

	/**
	 * ISO date format like "2019-02-01".
	 */
	@Column(nullable = false, length = 10)
	public String endAt;

}
