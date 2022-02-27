package com.itranswarp.service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itranswarp.bean.setting.AbstractSettingBean;
import com.itranswarp.bean.setting.Follow;
import com.itranswarp.bean.setting.Security;
import com.itranswarp.bean.setting.Snippet;
import com.itranswarp.bean.setting.Website;
import com.itranswarp.model.Setting;

@Component
public class SettingService extends AbstractDbService<Setting> {

    private static final String KEY_SETTING = "__setting__";

    public void deleteWebsiteFromCache() {
        this.redisService.hdel(KEY_SETTING, Website.class.getSimpleName());
    }

    public void deleteSnippetFromCache() {
        this.redisService.hdel(KEY_SETTING, Snippet.class.getSimpleName());
    }

    public void deleteFollowFromCache() {
        this.redisService.hdel(KEY_SETTING, Follow.class.getSimpleName());
    }

    public Website getWebsiteFromCache() {
        String group = Website.class.getSimpleName();
        Website bean = this.redisService.hget(KEY_SETTING, group, Website.class);
        if (bean == null) {
            bean = getWebsite();
            this.redisService.hset(KEY_SETTING, group, bean);
        }
        return bean;
    }

    public Snippet getSnippetFromCache() {
        String group = Snippet.class.getSimpleName();
        Snippet bean = this.redisService.hget(KEY_SETTING, group, Snippet.class);
        if (bean == null) {
            bean = getSnippet();
            this.redisService.hset(KEY_SETTING, group, bean);
        }
        return bean;
    }

    public Follow getFollowFromCache() {
        String group = Follow.class.getSimpleName();
        Follow bean = this.redisService.hget(KEY_SETTING, group, Follow.class);
        if (bean == null) {
            bean = getFollow();
            this.redisService.hset(KEY_SETTING, group, bean);
        }
        return bean;
    }

    public Website getWebsite() {
        return getSettingBean(Website.class);
    }

    public Snippet getSnippet() {
        return getSettingBean(Snippet.class);
    }

    public Follow getFollow() {
        return getSettingBean(Follow.class);
    }

    public Security getSecurity() {
        return getSettingBean(Security.class);
    }

    @Transactional
    public void setWebsite(Website bean) {
        setSettingBean(bean);
    }

    @Transactional
    public void setSnippet(Snippet bean) {
        setSettingBean(bean);
    }

    @Transactional
    public void setFollow(Follow bean) {
        setSettingBean(bean);
    }

    @Transactional
    public void setSecurity(Security bean) {
        setSettingBean(bean);
    }

    private <T extends AbstractSettingBean> T getSettingBean(Class<T> clazz) {
        Map<String, String> settings = getSettingsAsMap(clazz.getSimpleName());
        try {
            T bean = clazz.getConstructor().newInstance();
            bean.setSettings(settings);
            return bean;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends AbstractSettingBean> void setSettingBean(T bean) {
        String group = bean.getClass().getSimpleName();
        Map<String, String> settings = bean.getSettings();
        List<Setting> list = new ArrayList<>();
        settings.forEach((key, value) -> {
            Setting s = new Setting();
            s.settingGroup = group;
            s.settingKey = key;
            s.settingValue = value;
            list.add(s);
        });
        this.db.updateSql("DELETE FROM " + db.getTable(Setting.class) + " WHERE settingGroup = ?", group);
        this.db.insert(list);
    }

    private Map<String, String> getSettingsAsMap(String settingGroup) {
        List<Setting> list = db.from(Setting.class).where("settingGroup = ?", settingGroup).list();
        return list.stream().collect(Collectors.toMap(s -> s.settingKey, s -> s.settingValue));
    }

}
