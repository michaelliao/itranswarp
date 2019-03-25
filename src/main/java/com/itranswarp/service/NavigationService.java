package com.itranswarp.service;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.itranswarp.bean.NavigationBean;
import com.itranswarp.model.Navigation;

@Component
public class NavigationService extends AbstractService<Navigation> {

	static final String KEY_NAVIGATIONS = "_navigations";

	public List<Navigation> getNavigationsFromCache() {
		List<Navigation> navs = this.redisService.get(KEY_NAVIGATIONS, TYPE_LIST_NAVIGATION);
		if (navs == null) {
			navs = getNavigations();
			this.redisService.set(KEY_NAVIGATIONS, navs);
		}
		return navs;
	}

	public void removeNavigationsFromCache() {
		this.redisService.del(KEY_NAVIGATIONS);
	}

	public List<Navigation> getNavigations() {
		return this.db.from(Navigation.class).orderBy("displayOrder").list();
	}

	@Transactional
	public Navigation createNavigation(NavigationBean bean) {
		long maxDisplayOrder = getNavigations().stream().mapToLong(c -> c.displayOrder).max().orElseGet(() -> 0);
		Navigation nav = new Navigation();
		nav.name = checkName(bean.name);
		nav.icon = checkIcon(bean.icon);
		nav.url = checkUrl(bean.url);
		nav.blank = bean.blank;
		nav.displayOrder = maxDisplayOrder + 1;
		this.db.insert(nav);
		return nav;
	}

	@Transactional
	public Navigation updateNavigation(Long id, NavigationBean bean) {
		Navigation nav = this.getById(id);
		if (bean.name != null) {
			nav.name = checkName(bean.name);
		}
		if (bean.icon != null) {
			nav.icon = checkIcon(bean.icon);
		}
		if (bean.url != null) {
			nav.url = checkUrl(bean.url);
		}
		if (bean.blank != null) {
			nav.blank = bean.blank;
		}
		this.db.update(nav);
		return nav;
	}

	@Transactional
	public void deleteNavigation(Long id) {
		Navigation nav = this.getById(id);
		this.db.remove(nav);
	}

	@Transactional
	public void sortNavigations(List<Long> ids) {
		List<Navigation> navs = getNavigations();
		sortEntities(navs, ids);
	}

	static final TypeReference<List<Navigation>> TYPE_LIST_NAVIGATION = new TypeReference<>() {
	};
}
