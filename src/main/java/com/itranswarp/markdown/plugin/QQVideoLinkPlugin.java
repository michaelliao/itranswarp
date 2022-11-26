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
 * A link renderer that render QQ video link to video player.
 * 
 * Configuration in application.yaml:
 * 
 * <code>
 * spring:
 *   markdown:
 *     plugins:
 *       qq-video:
 *         enabled: true # default is none
 *         width:   100% # default is "100%"
 *         height: 480px # default is "480px"
 * </code>
 * 
 * @author liaoxuefeng
 */
@Component
@ConditionalOnProperty(name = "spring.markdown.plugins.qq-video.enabled", havingValue = "true")
public class QQVideoLinkPlugin implements PatternLinkRenderer {

    static final String URL_PREFIX = "https://v.qq.com/x/cover/";
    static final Pattern ID_PATTERN = Pattern.compile("^(\\w+)\\/(\\w+)\\.html$");

    static final String DEFAULT_WIDTH = "100%";
    static final String DEFAULT_HEIGHT = "480px";

    @Value("${spring.markdown.plugins.qq-video.width:" + DEFAULT_WIDTH + "}")
    String width = "100%";

    @Value("${spring.markdown.plugins.qq-video.height:" + DEFAULT_HEIGHT + "}")
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
        Matcher matcher = ID_PATTERN.matcher(url.substring(URL_PREFIX.length()));
        if (!matcher.matches()) {
            return false;
        }
        String id = matcher.group(2);
        // render as iframe:
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("src", "https://v.qq.com/txp/iframe/player.html?vid=" + id);
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
