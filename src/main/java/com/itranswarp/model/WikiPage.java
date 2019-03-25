package com.itranswarp.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "wiki_pages", indexes = @Index(name = "IDX_WIKIID", columnList = "wikiId"))
public class WikiPage extends AbstractSortableEntity {

	@Column(nullable = false)
	public long parentId;

	@Column(nullable = false)
	public long textId;

	@Column(nullable = false)
	public long wikiId;

	@Column(nullable = false, length = VAR_CHAR_NAME)
	public String name;

	@Column(nullable = false)
	public long views;

	@Column(nullable = false)
	public long publishAt;

	@Transient
	private List<WikiPage> children = null;

	public void addChild(WikiPage wikiPage) {
		if (this.children == null) {
			this.children = new ArrayList<>();
		}
		this.children.add(wikiPage);
	}

	@Transient
	public List<WikiPage> getChildren() {
		return this.children == null ? List.of() : this.children;
	}
}
