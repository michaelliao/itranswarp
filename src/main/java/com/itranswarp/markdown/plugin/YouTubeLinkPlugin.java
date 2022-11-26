package com.itranswarp.markdown.plugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;

import org.commonmark.renderer.html.HtmlWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.itranswarp.markdown.PatternLinkRenderer;

/**
 * A link renderer that render YouTube link to video player.
 * 
 * Configuration in application.yaml:
 * 
 * <code>
 * spring:
 *   markdown:
 *     plugins:
 *       youtube:
 *         enabled: true # default is none
 *         width:   100% # default is "100%"
 *         height: 480px # default is "480px"
 * </code>
 * 
 * @author liaoxuefeng
 */
@Component
@ConditionalOnProperty(name = "spring.markdown.plugins.youtube.enabled", havingValue = "true")
public class YouTubeLinkPlugin implements PatternLinkRenderer {

    static final String URL_PREFIX = "https://www.youtube.com/watch?v=";
    static final Pattern ID_PATTERN = Pattern.compile("^\\w+$");

    static final String DEFAULT_WIDTH = "100%";
    static final String DEFAULT_HEIGHT = "480px";

    @Value("${spring.markdown.plugins.youtube.width:" + DEFAULT_WIDTH + "}")
    String width = "100%";

    @Value("${spring.markdown.plugins.youtube.height:" + DEFAULT_HEIGHT + "}")
    String height = "480px";

    String style = "width:" + DEFAULT_WIDTH + ";height:" + DEFAULT_HEIGHT;

    @PostConstruct
    public void init() {
        this.style = "width:" + width + ";height:" + height;
    }

    @Override
    public boolean render(HtmlWriter html, String url, String title) {
        if (!url.startsWith(URL_PREFIX)) {
            return false;
        }
        String id = url.substring(URL_PREFIX.length());
        Matcher matcher = ID_PATTERN.matcher(id);
        if (!matcher.matches()) {
            return false;
        }
        // render as iframe:
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("src", "https://www.youtube.com/embed/" + id);
        attrs.put("style", style);
        attrs.put("scrolling", "no");
        attrs.put("border", "0");
        attrs.put("frameborder", "no");
        attrs.put("framespacing", "0");
        html.tag("iframe", attrs);
        html.tag("/iframe");
        return true;
    }

}
