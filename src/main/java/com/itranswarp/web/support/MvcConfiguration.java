package com.itranswarp.web.support;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import com.itranswarp.util.JsonUtil;
import com.itranswarp.web.view.AbstractFilter;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Extension;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.spring.servlet.PebbleViewResolver;

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
        var resolver = new CookieLocaleResolver("__locale__");
        resolver.setCookieHttpOnly(true);
        resolver.setCookieMaxAge(Duration.ofSeconds(3600 * 24 * 356 * 100));
        resolver.setCookiePath("/");
        return resolver;
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer(@Value("${spring.application.domain}") String domain) {
        return new WebMvcConfigurer() {
            /**
             * Keep "/static/" prefix
             */
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                final String httpReferer = "http://" + domain;
                final String httpsReferer = "https://" + domain;
                // check Referer if domain is configured:
                final boolean checkReferer = !"localhost".equalsIgnoreCase(domain);
                registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/").resourceChain(true)
                        // Referer check:
                        .addResolver(new ResourceResolver() {
                            @Override
                            public Resource resolveResource(HttpServletRequest request, String requestPath, List<? extends Resource> locations,
                                    ResourceResolverChain chain) {
                                if (checkReferer) {
                                    String referer = request.getHeader("Referer");
                                    if (referer != null) {
                                        referer = referer.toLowerCase();
                                        if (!referer.startsWith(httpsReferer) && !referer.startsWith(httpReferer)) {
                                            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                                        }
                                    }
                                }
                                return chain.resolveResource(request, requestPath, locations);
                            }

                            @Override
                            public String resolveUrlPath(String resourcePath, List<? extends Resource> locations, ResourceResolverChain chain) {
                                return chain.resolveUrlPath(resourcePath, locations);
                            }
                        })
                        // Path-resource-resolver:
                        .addResolver(new PathResourceResolver());
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
        PebbleViewResolver viewResolver = new PebbleViewResolver(engine);
        viewResolver.setPrefix("templates/");
        viewResolver.setSuffix("");
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
