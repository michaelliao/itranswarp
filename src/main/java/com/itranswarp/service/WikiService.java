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
public class WikiService extends AbstractDbService<Wiki> {

    @Autowired
    TextService textService;

    @Autowired
    AttachmentService attachmentService;

    static final String KEY_WIKIS = "__wikis__";

    public void removeWikiFromCache(long id) {
        this.redisService.del(KEY_WIKIS + id);
    }

    public Wiki getWikiTreeFromCache(long id) {
        Wiki wiki = this.redisService.get(KEY_WIKIS + id, Wiki.class);
        if (wiki == null) {
            wiki = getWikiTree(id);
            this.redisService.set(KEY_WIKIS + id, wiki);
        }
        return wiki;
    }

    public Wiki getWikiTree(long id) {
        Wiki wiki = getById(id);
        List<WikiPage> children = getWikiPages(id);
        Map<Long, WikiPage> nodes = new LinkedHashMap<>();
        children.forEach(wp -> {
            nodes.put(wp.id, wp);
        });
        treeIterate(wiki, nodes);
        if (!nodes.isEmpty()) {
            // there is error for tree structure, append to root for fix:
            nodes.forEach((nodeId, node) -> {
                wiki.addChild(node);
            });
        }
        return wiki;
    }

    public List<WikiPage> getWikiPages(long wikiId) {
        return this.db.from(WikiPage.class).where("wikiId = ?", wikiId).orderBy("parentId").orderBy("displayOrder").orderBy("id").list();
    }

    void treeIterate(Wiki root, Map<Long, WikiPage> nodes) {
        List<Long> toBeRemovedIds = new ArrayList<>();
        for (Long id : nodes.keySet()) {
            WikiPage node = nodes.get(id);
            if (node.parentId == root.id) {
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

    void treeIterate(WikiPage root, Map<Long, WikiPage> nodes) {
        List<Long> toBeRemovedIds = new ArrayList<>();
        for (Long id : nodes.keySet()) {
            WikiPage node = nodes.get(id);
            if (node.parentId == root.id) {
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

    public List<Wiki> getWikis() {
        return db.from(Wiki.class).orderBy("name").list();
    }

    @Transactional
    public Wiki createWiki(User user, WikiBean bean) {
        bean.validate(true);
        Wiki wiki = new Wiki();
        wiki.id = IdUtil.nextId();
        wiki.name = bean.name;
        wiki.tag = bean.tag;
        wiki.description = bean.description;
        wiki.publishAt = bean.publishAt;
        wiki.textId = this.textService.createText(bean.content).id;

        AttachmentBean atta = new AttachmentBean();
        atta.name = wiki.name;
        atta.data = bean.image;
        wiki.imageId = this.attachmentService.createAttachment(user, atta).id;
        this.db.insert(wiki);
        return wiki;
    }

    @Transactional
    public Wiki updateWiki(User user, long id, WikiBean bean) {
        bean.validate(false);
        Wiki wiki = this.getById(id);
        wiki.name = bean.name;
        wiki.tag = bean.tag;
        wiki.description = bean.description;
        wiki.publishAt = bean.publishAt;
        if (bean.content != null) {
            wiki.textId = this.textService.createText(bean.content).id;
        }
        if (bean.image != null) {
            AttachmentBean atta = new AttachmentBean();
            atta.name = wiki.name;
            atta.data = bean.image;
            wiki.imageId = this.attachmentService.createAttachment(user, atta).id;
        }
        this.db.update(wiki);
        return wiki;
    }

    @Transactional
    public WikiPage updateWikiPage(User user, long id, WikiPageBean bean) {
        bean.validate(false);
        WikiPage wikipage = getWikiPageById(id);
        Wiki wiki = getById(wikipage.wikiId);
        super.checkPermission(user, wiki.userId);
        wikipage.name = bean.name;
        wikipage.publishAt = bean.publishAt;
        if (bean.content != null) {
            wikipage.textId = this.textService.createText(bean.content).id;
        }
        this.db.update(wikipage);
        return wikipage;
    }

    @Transactional
    public WikiPage createWikiPage(User user, Wiki wiki, WikiPageBean bean) {
        bean.validate(true);
        super.checkPermission(user, wiki.userId);
        WikiPage parent = null;
        long parentId = bean.parentId;
        if (wiki.id == parentId) {
            // append as top level:
        } else {
            parent = getWikiPageById(parentId);
            if (parent.wikiId != wiki.id) {
                throw new IllegalArgumentException("Inconsist wikiId for node: " + parent);
            }
        }
        WikiPage lastChild = this.db.from(WikiPage.class).where("wikiId = ? AND parentId = ?", wiki.id, parentId).orderBy("displayOrder").desc().first();
        WikiPage newChild = new WikiPage();
        newChild.wikiId = wiki.id;
        newChild.parentId = parentId;
        newChild.name = bean.name;
        newChild.publishAt = bean.publishAt;
        newChild.textId = textService.createText(bean.content).id;
        newChild.displayOrder = lastChild == null ? 0 : lastChild.displayOrder + 1;
        this.db.insert(newChild);
        return newChild;
    }

    @Transactional
    public WikiPage moveWikiPage(User user, long wikiPageId, long toParentId, int displayIndex) {
        WikiPage wikiPage = getWikiPageById(wikiPageId);
        Wiki wiki = getById(wikiPage.wikiId);
        super.checkPermission(user, wiki.userId);
        if (wikiPage.parentId != toParentId) {
            // check to prevent recursive:
            List<Long> leafToRootIdList = getLeafToRootIds(wiki, toParentId);
            if (leafToRootIdList.contains(wikiPage.id)) {
                throw new ApiException(ApiError.OPERATION_FAILED, null, "Will cause recursive.");
            }
        }
        // update parentId:
        wikiPage.parentId = toParentId;
        this.db.updateProperties(wikiPage, "parentId");
        // sort:
        List<WikiPage> pages = this.db.from(WikiPage.class).where("parentId = ?", toParentId).orderBy("displayOrder").list();
        List<Long> pageIds = new ArrayList<>(pages.stream().map(p -> p.id).collect(Collectors.toList()));
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
    public void deleteWiki(User user, long id) {
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
    public WikiPage deleteWikiPage(User user, long id) {
        WikiPage wikiPage = getWikiPageById(id);
        Wiki wiki = getById(wikiPage.wikiId);
        super.checkPermission(user, wiki.userId);
        WikiPage child = this.db.from(WikiPage.class).where("parentId = ?", id).first();
        if (child != null) {
            throw new ApiException(ApiError.OPERATION_FAILED, null, "Could not remove non-empty wiki page.");
        }
        this.db.remove(wikiPage);
        return wikiPage;
    }

    public WikiPage getWikiPageById(long id) {
        WikiPage wikipage = this.db.fetch(WikiPage.class, id);
        if (wikipage == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, null, "not found");
        }
        return wikipage;
    }

    List<Long> getLeafToRootIds(Wiki wiki, long leafId) {
        if (leafId == wiki.id) {
            return List.of(leafId);
        }
        List<Long> nodeIds = new ArrayList<>();
        long currentId = leafId;
        for (int i = 0; i < 100; i++) {
            nodeIds.add(currentId);
            WikiPage node = getWikiPageById(currentId);
            if (node.parentId == wiki.id) {
                nodeIds.add(wiki.id);
                break;
            }
            currentId = node.parentId;
        }
        return nodeIds;
    }
}
