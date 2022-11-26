package com.itranswarp.markdown;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;
import javax.swing.text.html.parser.ParserDelegator;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;

@Component
public class Markdown {

    @Value("${spring.markdown.loading.url:/static/img/loading.svg}")
    String loadingUrl;

    @Autowired(required = false)
    List<PatternLinkRenderer> patternLinkRenderers = List.of();

    @Autowired(required = false)
    List<Extension> extensions = List.of();

    Parser parser;
    HtmlRenderer sysRenderer;
    HtmlRenderer ugcRenderer;

    @PostConstruct
    public void init() {
        List<Extension> extensionList = new ArrayList<>();
        extensionList.addAll(List.of(StrikethroughExtension.create(), TablesExtension.create()));
        extensionList.addAll(this.extensions);
        this.parser = Parser.builder().extensions(extensionList).build();

        this.sysRenderer = HtmlRenderer.builder().extensions(extensionList).nodeRendererFactory(context -> new CustomImageHtmlNodeRenderer(context, loadingUrl))
                .nodeRendererFactory(context -> new CustomLinkHtmlNodeRenderer(context, patternLinkRenderers)).build();

        this.ugcRenderer = HtmlRenderer.builder().extensions(extensionList).escapeHtml(true).nodeRendererFactory(context -> new SafeLinkNodeRenderer(context))
                .build();
    }

    public String toText(String md) {
        Node document = this.parser.parse(md);
        String html = this.sysRenderer.render(document);
        ParserDelegator parser = new ParserDelegator();
        StringBuilder sb = new StringBuilder(md.length());
        try (Reader reader = new StringReader(html)) {
            parser.parse(reader, new ParserCallback() {
                @Override
                public void handleText(char[] data, int pos) {
                    sb.append(data);
                }

                @Override
                public void handleEndTag(Tag t, int pos) {
                    if (t.breaksFlow()) {
                        sb.append('\n');
                    }
                }
            }, true);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sb.toString().strip();
    }

    public String toHtml(String md) {
        Node document = this.parser.parse(md);
        return this.sysRenderer.render(document);
    }

    public String ugcToHtml(String md) {
        Node document = this.parser.parse(md);
        return this.ugcRenderer.render(document);
    }

    public String ugcToHtml(String md, int maxLength) {
        String html = ugcToHtml(md);
        if (html.length() > maxLength) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "content", "Content is too long.");
        }
        return html;
    }

}

class CustomImageHtmlNodeRenderer implements NodeRenderer {

    private final HtmlNodeRendererContext context;
    private final HtmlWriter html;
    private final String loadingUrl;

    CustomImageHtmlNodeRenderer(HtmlNodeRendererContext context, String loadingUrl) {
        this.context = context;
        this.html = context.getWriter();
        this.loadingUrl = loadingUrl;
    }

    @Override
    public Set<Class<? extends Node>> getNodeTypes() {
        return Set.of(Image.class);
    }

    @Override
    public void render(Node node) {
        Image image = (Image) node;
        String url = context.encodeUrl(image.getDestination());
        AltTextVisitor altTextVisitor = new AltTextVisitor();
        image.accept(altTextVisitor);
        String altText = altTextVisitor.getAltText();
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("src", this.loadingUrl);
        attrs.put("data-src", url);
        attrs.put("alt", altText);
        if (image.getTitle() != null) {
            attrs.put("title", image.getTitle());
        }
        html.tag("img", context.extendAttributes(image, "img", attrs), true);
    }
}

/**
 * Convert link with special patterns to other element. e.g. VideoPlayer.
 * 
 * @author liaoxuefeng
 */
class CustomLinkHtmlNodeRenderer implements NodeRenderer {

    private final List<PatternLinkRenderer> patternLinkRenderers;
    private final HtmlNodeRendererContext context;
    private final HtmlWriter html;

    CustomLinkHtmlNodeRenderer(HtmlNodeRendererContext context, List<PatternLinkRenderer> patternLinkRenderers) {
        this.patternLinkRenderers = patternLinkRenderers;
        this.context = context;
        this.html = context.getWriter();
    }

    @Override
    public Set<Class<? extends Node>> getNodeTypes() {
        return Set.of(Link.class);
    }

    @Override
    public void render(Node node) {
        Link link = (Link) node;
        String url = context.encodeUrl(link.getDestination());
        String title = link.getTitle();
        for (PatternLinkRenderer renderer : this.patternLinkRenderers) {
            if (renderer.render(html, url, title)) {
                return;
            }
        }
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("href", url);
        if (title != null) {
            attrs.put("title", title);
        }
        html.tag("a", context.extendAttributes(node, "a", attrs));
        visitChildren(link);
        html.tag("/a");
    }

    protected void visitChildren(Node parent) {
        Node node = parent.getFirstChild();
        while (node != null) {
            Node next = node.getNext();
            context.render(node);
            node = next;
        }
    }
}

/**
 * Render link as rel="nofollow" href="no-script".
 * 
 * @author liaoxuefeng
 */
class SafeLinkNodeRenderer implements NodeRenderer {

    private final HtmlNodeRendererContext context;
    private final HtmlWriter html;

    SafeLinkNodeRenderer(HtmlNodeRendererContext context) {
        this.context = context;
        this.html = context.getWriter();
    }

    @Override
    public Set<Class<? extends Node>> getNodeTypes() {
        return Set.of(Link.class);
    }

    @Override
    public void render(Node node) {
        Link link = (Link) node;
        String url = context.encodeUrl(link.getDestination());
        if (url.toLowerCase().startsWith("javascript:")) {
            url = "javascript:void(0)";
        }
        String title = link.getTitle();
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("rel", "nofollow");
        attrs.put("href", url);
        attrs.put("target", "_blank");
        if (title != null) {
            attrs.put("title", title);
        }
        html.tag("a", context.extendAttributes(node, "a", attrs));
        visitChildren(link);
        html.tag("/a");
    }

    protected void visitChildren(Node parent) {
        Node node = parent.getFirstChild();
        while (node != null) {
            Node next = node.getNext();
            context.render(node);
            node = next;
        }
    }
}

class AltTextVisitor extends AbstractVisitor {

    private final StringBuilder sb = new StringBuilder();

    String getAltText() {
        return sb.toString();
    }

    @Override
    public void visit(Text text) {
        sb.append(text.getLiteral());
    }

    @Override
    public void visit(SoftLineBreak softLineBreak) {
        sb.append('\n');
    }

    @Override
    public void visit(HardLineBreak hardLineBreak) {
        sb.append('\n');
    }
}
