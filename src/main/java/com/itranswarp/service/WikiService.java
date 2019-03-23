package com.itranswarp.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itranswarp.bean.AttachmentBean;
import com.itranswarp.bean.WikiBean;
import com.itranswarp.bean.WikiPageBean;
import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.model.User;
import com.itranswarp.model.Wiki;
import com.itranswarp.model.WikiPage;
import com.itranswarp.util.IdUtil;

@Component
public class WikiService extends AbstractService<Wiki> {

	@Autowired
	TextService textService;

	@Autowired
	AttachmentService attachmentService;

	static final String KEY_WIKIS = "_wikis_";
	static final long EXPIRES = 3600;

	public void removeWikiFromCache(String id) {
		this.redisService.del(KEY_WIKIS + id);
	}

	public Wiki getWikiTreeFromCache(String id) {
		Wiki wiki = this.redisService.get(KEY_WIKIS + id, Wiki.class);
		if (wiki == null) {
			wiki = getWikiTree(id, true);
			this.redisService.set(KEY_WIKIS + id, wiki, EXPIRES);
		}
		return wiki;
	}

	public Wiki getWikiTree(String id, boolean publishedOnly) {
		Wiki wiki = getById(id);
		long ts = System.currentTimeMillis();
		if (publishedOnly && wiki.publishAt > ts) {
			return null;
		}
		List<WikiPage> children = getWikiPages(id, publishedOnly);
		Map<String, WikiPage> nodes = new LinkedHashMap<>();
		children.forEach(wp -> {
			nodes.put(wp.id, wp);
		});
		treeIterate(wiki, nodes);
		if (!nodes.isEmpty() && !publishedOnly) {
			// there is error for tree structure, append to root for fix:
			nodes.forEach((nodeId, node) -> {
				wiki.addChild(node);
			});
		}
		return wiki;
	}

	private List<WikiPage> getWikiPages(String wikiId, boolean publishedOnly) {
		if (publishedOnly) {
			return this.db.from(WikiPage.class)
					.where("wikiId = ? AND publishAt < ?", wikiId, System.currentTimeMillis()).orderBy("parentId")
					.orderBy("displayOrder").orderBy("id").list();
		}
		return this.db.from(WikiPage.class).where("wikiId = ?", wikiId).orderBy("parentId").orderBy("displayOrder")
				.orderBy("id").list();
	}

	void treeIterate(Wiki root, Map<String, WikiPage> nodes) {
		List<String> toBeRemovedIds = new ArrayList<>();
		for (String id : nodes.keySet()) {
			WikiPage node = nodes.get(id);
			if (node.parentId.equals(root.id)) {
				root.addChild(node);
				toBeRemovedIds.add(id);
			}
		}
		toBeRemovedIds.forEach(id -> {
			nodes.remove(id);
		});
		root.getChildren().forEach(child -> {
			treeIterate(child, nodes);
		});
	}

	void treeIterate(WikiPage root, Map<String, WikiPage> nodes) {
		List<String> toBeRemovedIds = new ArrayList<>();
		for (String id : nodes.keySet()) {
			WikiPage node = nodes.get(id);
			if (node.parentId.equals(root.id)) {
				root.addChild(node);
				toBeRemovedIds.add(id);
			}
		}
		toBeRemovedIds.forEach(id -> {
			nodes.remove(id);
		});
		root.getChildren().forEach(child -> {
			treeIterate(child, nodes);
		});
	}

	@Transactional
	public Wiki createWiki(User user, WikiBean bean) {
		Wiki wiki = new Wiki();
		wiki.id = IdUtil.nextId();
		wiki.description = checkDescription(bean.description);
		wiki.name = checkName(bean.name);
		wiki.publishAt = checkPublishAt(bean.publishAt);
		wiki.tag = checkTag(bean.tag);
		wiki.textId = this.textService.createText(bean.content).id;

		AttachmentBean atta = new AttachmentBean();
		atta.name = wiki.name;
		atta.data = bean.image;
		wiki.imageId = this.attachmentService.createAttachment(user, atta).id;
		return wiki;
	}

	@Transactional
	public Wiki updateWiki(String id, WikiBean bean) {
		Wiki wiki = this.getById(id);
		if (bean.name != null) {
			wiki.name = checkName(bean.name);
		}
		if (bean.description != null) {
			wiki.description = checkDescription(bean.description);
		}
		if (bean.tag != null) {
			wiki.tag = checkTag(bean.tag);
		}
		if (bean.publishAt != null) {
			wiki.publishAt = checkPublishAt(bean.publishAt);
		}
		this.db.update(wiki);
		return wiki;
	}

	@Transactional
	public WikiPage updateWikiPage(User user, String id, WikiPageBean bean) {
		WikiPage wikipage = getWikiPageById(id);
		Wiki wiki = getById(wikipage.wikiId);
		super.checkPermission(user, wiki.userId);
		if (bean.name != null) {
			wikipage.name = checkName(bean.name);
		}
		if (bean.content != null) {
			wikipage.textId = this.textService.createText(bean.content).id;
		}
		if (bean.publishAt != null) {
			wikipage.publishAt = checkPublishAt(bean.publishAt);
		}
		this.db.update(wikipage);
		return wikipage;
	}

	@Transactional
	public WikiPage createWikiPage(User user, Wiki wiki, String parentId, WikiPageBean bean) {
		super.checkPermission(user, wiki.userId);
		WikiPage parent = null;
		if (wiki.id.equals(parentId)) {
			// append as top level:
		} else {
			parent = getWikiPageById(parentId);
			if (!parent.wikiId.equals(wiki.id)) {
				throw new IllegalArgumentException("Inconsist wikiId for node: " + parent);
			}
		}
		WikiPage lastChild = this.db.from(WikiPage.class).where("wikiId = ? AND parentId = ?", wiki.id, parentId)
				.orderBy("displayOrder").desc().first();
		WikiPage newChild = new WikiPage();
		newChild.wikiId = wiki.id;
		newChild.parentId = parentId;
		newChild.name = checkName(bean.name);
		newChild.publishAt = checkPublishAt(bean.publishAt);
		newChild.textId = textService.createText(bean.content).id;
		newChild.displayOrder = lastChild == null ? 0 : lastChild.displayOrder + 1;
		this.db.insert(newChild);
		return newChild;
	}

	@Transactional
	public WikiPage moveWikiPage(User user, String wikiPageId, String toParentId, int displayIndex) {
		WikiPage wikiPage = getWikiPageById(wikiPageId);
		Wiki wiki = getById(wikiPage.wikiId);
		super.checkPermission(user, wiki.userId);
		if (!wikiPage.parentId.equals(toParentId)) {
			// check to prevent recursive:
			List<String> leafToRootIdList = getLeafToRootIds(wiki, toParentId);
			if (leafToRootIdList.contains(wikiPage.id)) {
				throw new ApiException(ApiError.OPERATION_FAILED, null, "Will cause recursive.");
			}
		}
		// update parentId:
		wikiPage.parentId = toParentId;
		this.db.updateProperties(wikiPage, "parentId");
		// sort:
		List<WikiPage> pages = this.db.from(WikiPage.class).where("parentId = ?", toParentId).orderBy("displayOrder")
				.list();
		List<String> pageIds = new ArrayList<>(pages.stream().map(p -> p.id).collect(Collectors.toList()));
		pageIds.remove(wikiPageId);
		if (displayIndex < 0 || displayIndex > pageIds.size()) {
			pageIds.add(wikiPageId);
		} else {
			pageIds.add(displayIndex, wikiPageId);
		}
		sortEntities(pages, pageIds);
		return wikiPage;
	}

	@Transactional
	public void deleteWiki(User user, String id) {
		Wiki wiki = getById(id);
		super.checkPermission(user, wiki.userId);
		WikiPage child = this.db.from(WikiPage.class).where("parentId = ?", id).first();
		if (child != null) {
			throw new ApiException(ApiError.OPERATION_FAILED, null, "Could not remove non-empty wiki.");
		}
		this.db.remove(wiki);
		this.attachmentService.deleteAttachment(user, wiki.imageId);
	}

	@Transactional
	public void deleteWikiPage(User user, String id) {
		WikiPage wikiPage = getWikiPageById(id);
		Wiki wiki = getById(wikiPage.wikiId);
		super.checkPermission(user, wiki.userId);
		WikiPage child = this.db.from(WikiPage.class).where("parentId = ?", id).first();
		if (child != null) {
			throw new ApiException(ApiError.OPERATION_FAILED, null, "Could not remove non-empty wiki page.");
		}
		this.db.remove(wikiPage);
	}

	public WikiPage getWikiPageById(String id) {
		WikiPage wikipage = this.db.fetch(WikiPage.class, id);
		if (wikipage == null) {
			throw new ApiException(ApiError.PARAMETER_INVALID, null, "not found");
		}
		return wikipage;
	}

	List<String> getLeafToRootIds(Wiki wiki, String leafId) {
		if (leafId.equals(wiki.id)) {
			return List.of(leafId);
		}
		List<String> nodeIds = new ArrayList<>();
		String currentId = leafId;
		for (int i = 0; i < 100; i++) {
			nodeIds.add(currentId);
			WikiPage node = getWikiPageById(currentId);
			if (node.parentId.equals(wiki.id)) {
				nodeIds.add(wiki.id);
				break;
			}
			currentId = node.parentId;
		}
		return nodeIds;
	}
}
