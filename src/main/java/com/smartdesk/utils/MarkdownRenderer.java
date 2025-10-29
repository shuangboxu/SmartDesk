package com.smartdesk.utils;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;

/**
 * Utility for converting Markdown content into styled HTML snippets for display.
 */
public final class MarkdownRenderer {

    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().escapeHtml(true).build();

    private MarkdownRenderer() {
    }

    public static String toHtml(final String markdown) {
        String source = markdown == null ? "" : markdown;
        Node document = PARSER.parse(source);
        String body = RENDERER.render(document);
        return """
            <html>
              <head>
                <meta charset="utf-8" />
              </head>
              <body>%s</body>
            </html>
            """.formatted(body);
    }
}
