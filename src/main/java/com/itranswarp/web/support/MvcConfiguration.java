package com.itranswarp.web.support;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import com.itranswarp.util.JsonUtil;
import com.itranswarp.web.view.AbstractFilter;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Extension;
import com.mitchellbosecke.pebble.extension.Filter;
import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import com.mitchellbosecke.pebble.spring.servlet.PebbleViewResolver;

/**
 * MVC configuration.
 */
public class MvcConfiguration {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${spring.profiles.active:native}")
    String activeProfile;

    /**
     * Locale resolver by cookie.
     *
     * @return LocaleResolver object.
     */
    @Bean(name = "localeResolver")
    public LocaleResolver createLocaleResolver() {
        var resolver = new CookieLocaleResolver();
        resolver.setCookieName("__locale__");
        resolver.setCookieHttpOnly(true);
        resolver.setCookieMaxAge(Integer.MAX_VALUE);
        resolver.setCookiePath("/");
        return resolver;
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            /**
             * Keep "/static/" prefix
             */
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
            }

            @Override
            public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
                final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
                converter.setObjectMapper(JsonUtil.OBJECT_MAPPER);
                converters.add(converter);
            }
        };
    }

    /**
     * Init view resolver.
     *
     * @return PebbleViewResolver
     */
    @Bean
    public ViewResolver pebbleViewResolver(@Autowired Extension extension) {
        // disable cache for native profile:
        boolean cache = !"native".equals(activeProfile);
        logger.info("set cache as {} for active profile is {}.", cache, activeProfile);
        PebbleEngine engine = new PebbleEngine.Builder().autoEscaping(true).cacheActive(cache).extension(extension).loader(new ClasspathLoader()).build();
        PebbleViewResolver viewResolver = new PebbleViewResolver();
        viewResolver.setPrefix("templates/");
        viewResolver.setSuffix("");
        viewResolver.setPebbleEngine(engine);
        return viewResolver;
    }

    @Bean
    Extension pebbleExtension(@Autowired(required = false) AbstractFilter[] filters, @Autowired(required = false) AbstractFunction[] functions) {
        return new AbstractExtension() {
            @Override
            public Map<String, Filter> getFilters() {
                Map<String, Filter> map = new HashMap<>();
                if (filters != null) {
                    for (AbstractFilter filter : filters) {
                        map.put(filter.getName(), filter);
                    }
                }
                return map;
            }

            @Override
            public Map<String, Function> getFunctions() {
                Map<String, Function> map = new HashMap<>();
                if (functions != null) {
                    for (AbstractFunction function : functions) {
                        map.put(function.getName(), function);
                    }
                }
                return map;
            }
        };
    }
}
