package com.itranswarp.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itranswarp.bean.SinglePageBean;
import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.model.SinglePage;
import com.itranswarp.model.User;

@Component
public class SinglePageService extends AbstractService<SinglePage> {

	@Autowired
	TextService textService;

	static final String KEY_SINGLE_PAGES = "_singlepages";

	public List<SinglePage> getAll() {
		return this.db.from(SinglePage.class).orderBy("id").desc().list();
	}

	public SinglePage getPublishedById(String id) {
		SinglePage sp = getById(id);
		if (sp.publishAt > System.currentTimeMillis()) {
			throw new ApiException(ApiError.ENTITY_NOT_FOUND, "SinglePage", "SinglePage not found.");
		}
		return sp;
	}

	@Transactional
	public SinglePage createSinglePage(User user, SinglePageBean bean) {
		SinglePage sp = new SinglePage();
		sp.name = checkName(bean.name);
		sp.publishAt = checkPublishAt(bean.publishAt);
		sp.tags = checkTags(bean.tags);
		sp.textId = textService.createText(bean.content).id;
		this.db.insert(sp);
		return sp;
	}

	@Transactional
	public void deleteSinglePage(String id) {
		SinglePage sp = this.getById(id);
		this.db.remove(sp);
	}

	@Transactional
	public SinglePage updateSinglePage(String id, SinglePageBean bean) {
		SinglePage sp = this.getById(id);
		if (bean.name != null) {
			sp.name = checkName(bean.name);
		}
		if (bean.tags != null) {
			sp.tags = checkTags(bean.tags);
		}
		if (bean.publishAt != null) {
			sp.publishAt = checkPublishAt(bean.publishAt);
		}
		if (bean.content != null) {
			sp.textId = textService.createText(bean.content).id;
		}
		this.db.update(sp);
		return sp;
	}
}
