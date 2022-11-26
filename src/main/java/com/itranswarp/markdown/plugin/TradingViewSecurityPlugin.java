package com.itranswarp.markdown.plugin;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.PostConstruct;

import org.commonmark.node.CustomNode;
import org.commonmark.node.Delimited;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser.Builder;
import org.commonmark.parser.Parser.ParserExtension;
import org.commonmark.parser.delimiter.DelimiterProcessor;
import org.commonmark.parser.delimiter.DelimiterRun;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlRenderer.HtmlRendererExtension;
import org.commonmark.renderer.html.HtmlWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.itranswarp.util.IdUtil;
import com.itranswarp.util.JsonUtil;

/**
 * A securities renderer that render charts of stocks, futures, etc.
 * 
 * Usage: Apple $$NASDAQ:AAPL$$ officially announced its March event yesterday.
 * 
 * Configuration in application.yaml:
 * 
 * <code>
 * spring:
 *   markdown:
 *     plugins:
 *       trading-view:
 *         enabled: true            # default is none
 *         width: 100%              # default is "100%"
 *         height: 480px            # default is "480px"
 *         interval: D              # default is "D", options are "1", "5", "60", "D", "W".
 *         toolbar-bg: "#f1f3f6"    # default is "#f1f3f6", color of toolbar bg. NOTE using quote ("") because # is comment.
 *         timezone: Asia/Shanghai  # default to empty, which is system default.
 *         locale: zh_CN            # default to empty, which is system default.
 *         theme: Light             # "Light" or "Dark"
 *         style: 1                 # style of 0 ~ 9. Default to 1.
 *         enable-publishing: false # default to false.
 *         save-image: false        # default to false.
 *         hide-top-toolbar: false  # default to false.
 * </code>
 * 
 * Check https://www.tradingview.com/widget/advanced-chart/ for more
 * information.
 * 
 * @author liaoxuefeng
 */
@Component
@ConditionalOnProperty(name = "spring.markdown.plugins.trading-view.enabled", havingValue = "true")
public class TradingViewSecurityPlugin implements ParserExtension, HtmlRendererExtension {

    static final String DEFAULT_WIDTH = "100%";
    static final String DEFAULT_HEIGHT = "480px";

    static final String DEFAULT_TOOLBAR_BG = "#f1f3f6";

    @Value("${spring.markdown.plugins.trading-view.width:" + DEFAULT_WIDTH + "}")
    String width = "100%";

    @Value("${spring.markdown.plugins.trading-view.height:" + DEFAULT_HEIGHT + "}")
    String height = "480px";

    @Value("${spring.markdown.plugins.trading-view.interval:D}")
    String interval = "D";

    @Value("${spring.markdown.plugins.trading-view.timezone:}")
    String timezone = "";

    @Value("${spring.markdown.plugins.trading-view.locale:}")
    String locale = "";

    @Value("${spring.markdown.plugins.trading-view.theme:Light}")
    String theme = "Light";

    @Value("${spring.markdown.plugins.trading-view.style:1}")
    String style = "1";

    @Value("${spring.markdown.plugins.trading-view.toolbar-bg:" + DEFAULT_TOOLBAR_BG + "}")
    String toolbarBg = DEFAULT_TOOLBAR_BG;

    @Value("${spring.markdown.plugins.trading-view.enable-publishing:false}")
    boolean enablePublishing = false;

    @Value("${spring.markdown.plugins.trading-view.save-image:false}")
    boolean saveImage = false;

    @Value("${spring.markdown.plugins.trading-view.hide-top-toolbar:false}")
    boolean hideTopToolbar = false;

    String cssStyle = "width:" + DEFAULT_WIDTH + ",height:" + DEFAULT_HEIGHT;

    String options;

    @PostConstruct
    public void init() {
        this.cssStyle = "width:" + this.width + ";height:" + this.height;
        if (this.timezone.isEmpty()) {
            this.timezone = ZoneId.systemDefault().getId();
        }
        if (this.locale.isEmpty()) {
            this.locale = Locale.getDefault().toString();
        }
        Map<String, Object> opts = Map.of( // options
                "interval", this.interval, //
                "timezone", this.timezone, //
                "theme", this.theme, //
                "style", this.style, //
                "locale", this.locale, //
                "toolbar_bg", this.toolbarBg, //
                "hide_top_toolbar", this.hideTopToolbar, //
                "enable_publishing", this.enablePublishing, //
                "save_image", this.saveImage //
        );
        this.options = JsonUtil.writeJson(opts);
    }

    @Override
    public void extend(HtmlRenderer.Builder rendererBuilder) {
        rendererBuilder.nodeRendererFactory(context -> new TradingViewNodeRenderer(context, this.cssStyle, this.options));
    }

    @Override
    public void extend(Builder parserBuilder) {
        parserBuilder.customDelimiterProcessor(new SecurityDelimiterProcessor());
    }

}

/**
 * Node renderer.
 * 
 * @author liaoxuefeng
 */
class TradingViewNodeRenderer implements NodeRenderer {

    static final Map<String, String> SCRIPT_ATTRS = Map.of("src", "https://s3.tradingview.com/tv.js");
    static final Map<String, String> DIV_WIDGET_ATTRS = Map.of("class", "tradingview-widget-container");

    final HtmlNodeRendererContext context;
    final String cssStyle;
    final String options;

    TradingViewNodeRenderer(HtmlNodeRendererContext context, String cssStyle, String options) {
        this.context = context;
        this.cssStyle = cssStyle;
        this.options = options;
    }

    @Override
    public Set<Class<? extends Node>> getNodeTypes() {
        return Set.of(Security.class);
    }

    @Override
    public void render(Node node) {
        HtmlWriter html = context.getWriter();
        Node child = node.getFirstChild();
        if ((child == null) || (child != node.getLastChild()) || !(child instanceof Text)) {
            html.raw("$$");
            while (child != null) {
                context.render(child);
                child = child.getNext();
            }
            html.raw("$$");
            return;
        }
        final String symbol = ((Text) child).getLiteral();
        final String containerId = "tv" + IdUtil.nextId();
        final Map<String, String> containerAttrs = Map.of("id", containerId, "style", this.cssStyle);
        html.tag("script", SCRIPT_ATTRS);
        html.tag("/script");
        html.tag("div", DIV_WIDGET_ATTRS);
        html.tag("div", containerAttrs);
        html.tag("/div");
        html.tag("/div");
        html.tag("script");
        html.raw("(function () { var opts = " + this.options + "; opts.container_id = \"" + containerId + "\"; opts.symbol = " + JsonUtil.writeJson(symbol)
                + "; opts.autosize = true; opts.allow_symbol_change = false; new TradingView.widget(opts); })();");
        html.tag("/script");
    }

}

/**
 * Node processor.
 * 
 * @author liaoxuefeng
 */
class SecurityDelimiterProcessor implements DelimiterProcessor {

    @Override
    public char getOpeningCharacter() {
        return '$';
    }

    @Override
    public char getClosingCharacter() {
        return '$';
    }

    @Override
    public int getMinLength() {
        return 2;
    }

    @Override
    public int getDelimiterUse(DelimiterRun opener, DelimiterRun closer) {
        if (opener.length() >= 2 && closer.length() >= 2) {
            // Use exactly two delimiters even if we have more, and don't care about
            // internal openers/closers.
            return 2;
        } else {
            return 0;
        }
    }

    @Override
    public void process(Text opener, Text closer, int delimiterUse) {
        Node node = new Security();
        Node tmp = opener.getNext();
        while (tmp != null && tmp != closer) {
            Node next = tmp.getNext();
            node.appendChild(tmp);
            tmp = next;
        }
        opener.insertAfter(node);
    }
}

/**
 * Node type: Security.
 * 
 * @author liaoxuefeng
 */
class Security extends CustomNode implements Delimited {

    private static final String DELIMITER = "$$";

    @Override
    public String getOpeningDelimiter() {
        return DELIMITER;
    }

    @Override
    public String getClosingDelimiter() {
        return DELIMITER;
    }

}
