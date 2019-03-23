package com.itranswarp.service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itranswarp.bean.AbstractSettingBean;
import com.itranswarp.bean.Snippet;
import com.itranswarp.bean.Website;
import com.itranswarp.model.Setting;

@Component
public class SettingService extends AbstractService<Setting> {

	public void deleteWebsiteFromCache() {
		this.redisService.del(Website.class.getSimpleName());
	}

	public void deleteSnippetFromCache() {
		this.redisService.del(Snippet.class.getSimpleName());
	}

	public Website getWebsiteFromCache() {
		String group = Website.class.getSimpleName();
		Website bean = this.redisService.get(group, Website.class);
		if (bean == null) {
			bean = getWebsite();
			this.redisService.set(group, bean);
		}
		return bean;
	}

	public Snippet getSnippetFromCache() {
		String group = Snippet.class.getSimpleName();
		Snippet bean = this.redisService.get(group, Snippet.class);
		if (bean == null) {
			bean = getSnippet();
			this.redisService.set(group, bean);
		}
		return bean;
	}

	public Website getWebsite() {
		return getSettingBean(Website.class);
	}

	public Snippet getSnippet() {
		return getSettingBean(Snippet.class);
	}

	@Transactional
	public void setWebsite(Website bean) {
		setSettingBean(bean);
	}

	@Transactional
	public void setSnippet(Snippet bean) {
		setSettingBean(bean);
	}

	private <T extends AbstractSettingBean> T getSettingBean(Class<T> clazz) {
		Map<String, String> settings = getSettingsAsMap(clazz.getSimpleName());
		try {
			T bean = clazz.getConstructor().newInstance();
			bean.setSettings(settings);
			return bean;
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
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
