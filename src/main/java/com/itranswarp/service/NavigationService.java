package com.itranswarp.service;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.itranswarp.bean.NavigationBean;
import com.itranswarp.model.Navigation;

@Component
public class NavigationService extends AbstractDbService<Navigation> {

    static final String KEY_NAVIGATIONS = "__navigations__";

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
        bean.validate(true);
        long maxDisplayOrder = getNavigations().stream().mapToLong(c -> c.displayOrder).max().orElseGet(() -> 0);
        Navigation nav = new Navigation();
        nav.name = bean.name;
        nav.icon = bean.icon;
        nav.url = bean.url;
        nav.blank = bean.blank;
        nav.displayOrder = maxDisplayOrder + 1;
        this.db.insert(nav);
        return nav;
    }

    @Transactional
    public Navigation updateNavigation(long id, NavigationBean bean) {
        bean.validate(false);
        Navigation nav = this.getById(id);
        nav.name = bean.name;
        nav.icon = bean.icon;
        nav.url = bean.url;
        nav.blank = bean.blank;
        this.db.update(nav);
        return nav;
    }

    @Transactional
    public void deleteNavigation(long id) {
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
