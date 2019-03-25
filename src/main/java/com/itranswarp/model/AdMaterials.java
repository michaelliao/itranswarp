package com.itranswarp.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "ad_materials")
public class AdMaterials extends AbstractEntity {

	@Column(nullable = false, updatable = false)
	public long userId;

	@Column(nullable = false, updatable = false)
	public long adPeriodId;

	@Column(nullable = false, updatable = false)
	public long imageId;

	@Column(nullable = false)
	public long weight;

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

	@Column(nullable = false, length = VAR_ENUM)
	public String geo;

	@Column(nullable = false, length = VAR_CHAR_TAGS)
	public String tags;

	@Column(nullable = false, length = VAR_CHAR_URL)
	public String url;
}
