package com.itranswarp.markdown;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.itranswarp.markdown.plugin.BilibiliLinkPlugin;
import com.itranswarp.markdown.plugin.TradingViewSecurityPlugin;
import com.itranswarp.util.IdUtil;

public class MarkdownTest {

    Markdown markdown;

    @BeforeAll
    static void initIdUtil() throws ReflectiveOperationException {
        Field f = IdUtil.class.getDeclaredField("shardingId");
        f.setAccessible(true);
        f.set(null, 1L);
    }

    @BeforeEach
    void setUp() throws Exception {
        markdown = new Markdown();
        markdown.loadingUrl = "/static/loading.gif";
        markdown.patternLinkRenderers = List.of(new BilibiliLinkPlugin());
        TradingViewSecurityPlugin tv = new TradingViewSecurityPlugin();
        tv.init();
        markdown.extensions = List.of(tv);
        markdown.init();
    }

    @Test
    void testToHtml() {
        // em:
        assertEquals("<p>This is <em>Sparta</em></p>\n", markdown.toHtml("This is *Sparta*"));
        // strong:
        assertEquals("<p>This is <strong>Sparta</strong></p>\n", markdown.toHtml("This is **Sparta**"));
        // code:
        assertEquals("<p>This is <code>Sparta</code></p>\n", markdown.toHtml("This is `Sparta`"));
        // code:
        assertEquals("<p>This <code>is`Sparta</code></p>\n", markdown.toHtml("This ``is`Sparta``"));
        // line:
        assertEquals("<p>This is</p>\n<hr />\n<p>Sparta</p>\n", markdown.toHtml("This is\n\n---\n\nSparta"));
        // quote:
        assertEquals("<blockquote>\n<p>This is <em>Sparta</em></p>\n</blockquote>\n", markdown.toHtml("> This is *Sparta*"));
        // h1:
        assertEquals("<h1>This is Sparta</h1>\n", markdown.toHtml("# This is Sparta"));
        // h2:
        assertEquals("<h2>This is Sparta</h2>\n", markdown.toHtml("## This is Sparta"));
        // h3:
        assertEquals("<h3>This is Sparta</h3>\n", markdown.toHtml("### This is Sparta"));
        // h4:
        assertEquals("<h4>This is Sparta</h4>\n", markdown.toHtml("#### This is Sparta"));
        // h5:
        assertEquals("<h5>This is Sparta</h5>\n", markdown.toHtml("##### This is Sparta"));
        // h6:
        assertEquals("<h6>This is Sparta</h6>\n", markdown.toHtml("###### This is Sparta"));
        // code:
        assertEquals("<pre><code>This is Sparta\n</code></pre>\n", markdown.toHtml("    This is Sparta"));
        // code:
        assertEquals("<pre><code class=\"language-java\">int a = x &lt; 1 || x &gt; 3 ? a &amp; b: 0;\n</code></pre>\n",
                markdown.toHtml("```java\nint a = x < 1 || x > 3 ? a & b: 0;\n```"));
        // a:
        assertEquals("<p>This is <a href=\"https://www.example.com/test?a=1&amp;b=2\">Sparta</a></p>\n",
                markdown.toHtml("This is [Sparta](https://www.example.com/test?a=1&b=2)"));
        // img:
        assertEquals("<p>This is <img src=\"/static/loading.gif\" data-src=\"https://www.example.com/test.jpg\" alt=\"Sparta\" /></p>\n",
                markdown.toHtml("This is ![Sparta](https://www.example.com/test.jpg)"));
        // del:
        assertEquals("<p>This is <del>Sparta</del></p>\n", markdown.toHtml("This is ~~Sparta~~"));
        // html:
        assertEquals("<p style=\"width:100px\">Hello</p>\n", markdown.toHtml("<p style=\"width:100px\">Hello</p>"));
        // table:
        String md = "| ID | city   |    zip |\n" //
                + "|----|:--------:|-------:|\n" //
                + "| 1  |  Beijing | 100101 |\n" //
                + "| 2  | Shanghai | 200258 |\n" //
                + "| 3  |  Tianjin | 300688 |\n";
        String html = "<table>\n" //
                + "<thead>\n" //
                + "<tr><th>ID</th><th align=\"center\">city</th><th align=\"right\">zip</th></tr>\n" //
                + "</thead>\n" //
                + "<tbody>\n" //
                + "<tr><td>1</td><td align=\"center\">Beijing</td><td align=\"right\">100101</td></tr>\n" //
                + "<tr><td>2</td><td align=\"center\">Shanghai</td><td align=\"right\">200258</td></tr>\n" //
                + "<tr><td>3</td><td align=\"center\">Tianjin</td><td align=\"right\">300688</td></tr>\n" //
                + "</tbody>\n" //
                + "</table>\n";
        assertEquals(html, markdown.toHtml(md));
        // script:
        assertEquals("<script>\nalert('hello');\n</script>\n", markdown.toHtml("<script>\nalert('hello');\n</script>"));
    }

    @Test
    void testToText() {
        // em:
        assertEquals("This is Sparta", markdown.toText("This is *Sparta*"));
        // strong:
        assertEquals("This is Sparta", markdown.toText("This is **Sparta**"));
        // code:
        assertEquals("This is Sparta", markdown.toText("This is `Sparta`"));
        // code:
        assertEquals("This is`Sparta", markdown.toText("This ``is`Sparta``"));
        // multiline:
        assertEquals("This is Sparta", markdown.toText("This is\nSparta"));
        // multiline:
        assertEquals("This is\nSparta", markdown.toText("This is\n\nSparta"));
        // line:
        assertEquals("This is\nSparta", markdown.toText("This is\n\n---\n\nSparta"));
        // quote:
        assertEquals("This is Sparta", markdown.toText("> This is *Sparta*"));
        // h1:
        assertEquals("This is Sparta", markdown.toText("# This is Sparta"));
        // h2:
        assertEquals("This is Sparta", markdown.toText("## This is Sparta"));
        // h3:
        assertEquals("This is Sparta", markdown.toText("### This is Sparta"));
        // h4:
        assertEquals("This is Sparta", markdown.toText("#### This is Sparta"));
        // h5:
        assertEquals("This is Sparta", markdown.toText("##### This is Sparta"));
        // h6:
        assertEquals("This is Sparta", markdown.toText("###### This is Sparta"));
        // code:
        assertEquals("This is Sparta", markdown.toText("    This is Sparta"));
        // code:
        assertEquals("int a = x < 1 || x > 3 ? a & b: 0;", markdown.toText("```java\nint a = x < 1 || x > 3 ? a & b: 0;\n```"));
        // a:
        assertEquals("This is Sparta", markdown.toText("This is [Sparta](https://www.example.com/test?a=1&b=2)"));
        // img:
        assertEquals("This is", markdown.toText("This is ![Sparta](https://www.example.com/test.jpg)"));
        // del:
        assertEquals("This is Sparta", markdown.toText("This is ~~Sparta~~"));
        // html:
        assertEquals("Hello", markdown.toText("<p style=\"width:100px\">Hello</p>"));
        // table:
        String md = "| ID | city   |    zip |\n" //
                + "|----|:--------:|-------:|\n" //
                + "| 1  |  Beijing | 100101 |\n" //
                + "| 2  | Shanghai | 200258 |\n" //
                + "| 3  |  Tianjin | 300688 |\n";
        assertEquals("ID\ncity\nzip\n1\nBeijing\n100101\n2\nShanghai\n200258\n3\nTianjin\n300688", markdown.toText(md));
        // script:
        assertEquals("", markdown.toText("<script>\nalert('hello');\n</script>"));
    }

    @Test
    void testRawHtml() {
        assertEquals("<p><a click=\"alert('hi')\">link</a></p>\n", markdown.toHtml("<a click=\"alert('hi')\">link</a>"));
        assertEquals("<p>&lt;a click=&quot;alert('hi')&quot;&gt;link&lt;/a&gt;</p>\n", markdown.ugcToHtml("<a click=\"alert('hi')\">link</a>"));
    }

    @Test
    void testPatternLink() {
        // valid bilibili:
        assertEquals(
                "<p>This is <iframe src=\"//player.bilibili.com/player.html?aid=5740892\" style=\"width:100%;height:480px\" scrolling=\"no\" border=\"0\" frameborder=\"no\" framespacing=\"0\"></iframe></p>\n",
                markdown.toHtml("This is [Lego](https://www.bilibili.com/video/av5740892)"));
        // invalid bilibili:
        assertEquals("<p>This is <a href=\"https://www.bilibili.com/account/history\">Lego</a></p>\n",
                markdown.toHtml("This is [Lego](https://www.bilibili.com/account/history)"));
        // bilibili in code:
        assertEquals("<pre><code>[Lego](https://www.bilibili.com/video/av5740892)\n</code></pre>\n",
                markdown.toHtml("```\n[Lego](https://www.bilibili.com/video/av5740892)\n```"));
    }

    @Test
    void testTradingView() {
        // valid symbol:
        String validTV = markdown.toHtml("This is $$DASDAQ:AAPL$$ stock.");
        System.out.println(validTV);
        assertTrue(validTV.contains("<script src=\"https://s3.tradingview.com/tv.js\">"));
        assertTrue(validTV.contains("new TradingView.widget(opts);"));
        assertTrue(validTV.contains("opts.symbol = \"DASDAQ:AAPL\";"));
        assertTrue(validTV.contains("<div class=\"tradingview-widget-container\">"));
        // invalid:
        assertEquals("<p>This is $$DASDAQ<em>italic</em>AAPL$$ stock.</p>\n", markdown.toHtml("This is $$DASDAQ*italic*AAPL$$ stock."));
        // code:
        assertEquals("<pre><code>This is $$DASDAQ:AAPL$$ stock.\n</code></pre>\n", markdown.toHtml("```\nThis is $$DASDAQ:AAPL$$ stock.\n```"));
    }

    @Test
    void testUgc() {
        assertEquals("<p>This is <a rel=\"nofollow\" href=\"https://www.example.com/test?a=1&amp;b=2\" target=\"_blank\">Sparta</a></p>\n",
                markdown.ugcToHtml("This is [Sparta](https://www.example.com/test?a=1&b=2)"));
        assertEquals("<p>&lt;script&gt;alert('hi');&lt;/script&gt;</p>\n", markdown.ugcToHtml("<script>alert('hi');</script>"));
        assertEquals("<p>This is <a rel=\"nofollow\" href=\"javascript:void(0)\" target=\"_blank\">Sparta</a></p>\n",
                markdown.ugcToHtml("This is [Sparta](javascript:alert('hi'))"));
        assertEquals("<p>&lt;script&gt;\nalert('hello');\n&lt;/script&gt;</p>\n", markdown.ugcToHtml("<script>\nalert('hello');\n</script>"));
    }
}
