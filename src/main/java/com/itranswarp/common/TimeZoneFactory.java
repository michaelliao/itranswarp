package com.itranswarp.common;

import java.time.ZoneId;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TimeZoneFactory implements FactoryBean<ZoneId> {

	@Value("${spring.application.timezone:}")
	public String timezone;

	@Override
	public ZoneId getObject() throws Exception {
		return timezone.isEmpty() ? ZoneId.systemDefault() : ZoneId.of(timezone);
	}

	@Override
	public Class<?> getObjectType() {
		return ZoneId.class;
	}

}
