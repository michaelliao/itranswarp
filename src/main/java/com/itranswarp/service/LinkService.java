package com.itranswarp.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.itranswarp.bean.LinkBean;
import com.itranswarp.model.Link;

@Component
public class LinkService extends AbstractService<Link> {

	static final String KEY_LINKS = "__links__";

	public String getLinkUrlFromCache(long id) {
		Map<String, String> links = this.redisService.hgetAll(KEY_LINKS);
		return links.get(String.valueOf(id));
	}

	public void updateLinksCache() {
		Map<String, String> map = getLinks().stream().collect(Collectors.toMap(l -> String.valueOf(l.id), l -> l.url));
		this.redisService.hsetAll(KEY_LINKS, map);
	}

	public List<Link> getLinks() {
		return this.db.from(Link.class).orderBy("id").list();
	}

	@Transactional
	public Link createLink(LinkBean bean) {
		bean.validate(true);
		Link link = new Link();
		link.name = bean.name;
		link.url = bean.url;
		this.db.insert(link);
		return link;
	}

	@Transactional
	public Link updateLink(Long id, LinkBean bean) {
		bean.validate(false);
		Link link = this.getById(id);
		link.name = bean.name;
		link.url = bean.url;
		this.db.update(link);
		return link;
	}

	@Transactional
	public void deleteLink(Long id) {
		Link link = this.getById(id);
		this.db.remove(link);
	}

	static final TypeReference<List<Link>> TYPE_LIST_LINK = new TypeReference<>() {
	};
}
