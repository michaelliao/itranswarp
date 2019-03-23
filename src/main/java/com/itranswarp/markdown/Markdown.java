package com.itranswarp.markdown;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Markdown {

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

		this.sysRenderer = HtmlRenderer.builder().extensions(extensionList)
				.nodeRendererFactory(context -> new CustomLinkNodeRenderer(context, patternLinkRenderers)).build();

		this.ugcRenderer = HtmlRenderer.builder().extensions(extensionList).escapeHtml(true)
				.nodeRendererFactory(context -> new NoFollowLinkNodeRenderer(context)).build();
	}

	public String toHtml(String md) {
		Node document = this.parser.parse(md);
		return this.sysRenderer.render(document);
	}

	public String ugcToHtml(String md) {
		Node document = this.parser.parse(md);
		return this.ugcRenderer.render(document);
	}

}

/**
 * Convert link with special patterns to other element. e.g. VideoPlayer.
 * 
 * @author liaoxuefeng
 */
class CustomLinkNodeRenderer implements NodeRenderer {

	private final List<PatternLinkRenderer> patternLinkRenderers;
	private final HtmlNodeRendererContext context;
	private final HtmlWriter html;

	CustomLinkNodeRenderer(HtmlNodeRendererContext context, List<PatternLinkRenderer> patternLinkRenderers) {
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
 * Render link as rel="nofollow".
 * 
 * @author liaoxuefeng
 */
class NoFollowLinkNodeRenderer implements NodeRenderer {

	private final HtmlNodeRendererContext context;
	private final HtmlWriter html;

	NoFollowLinkNodeRenderer(HtmlNodeRendererContext context) {
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
		Map<String, String> attrs = new LinkedHashMap<>();
		attrs.put("rel", "nofollow");
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
