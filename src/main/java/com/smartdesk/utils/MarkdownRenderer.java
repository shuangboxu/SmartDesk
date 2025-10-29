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

    public static String toHtml(final String markdown, final String textColor, final String linkColor) {
        String source = markdown == null ? "" : markdown;
        Node document = PARSER.parse(source);
        String body = RENDERER.render(document);
        String safeTextColor = textColor == null || textColor.isBlank() ? "#1f2a4a" : textColor;
        String safeLinkColor = linkColor == null || linkColor.isBlank() ? "#3f51b5" : linkColor;
        return """
            <html>
              <head>
                <meta charset="utf-8" />
                <style>
                  html { background-color: transparent; }
                  body { font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif; font-size: 14px; margin: 0; color: %s; background-color: transparent; }
                  p { margin: 0 0 8px 0; line-height: 1.6; }
                  ul, ol { padding-left: 20px; margin-top: 0; margin-bottom: 8px; }
                  li { margin-bottom: 4px; }
                  code { font-family: 'JetBrains Mono', monospace; background: rgba(79, 99, 191, 0.12); padding: 2px 6px; border-radius: 6px; }
                  pre { font-family: 'JetBrains Mono', monospace; background: rgba(79, 99, 191, 0.16); padding: 10px 12px; border-radius: 12px; overflow-x: auto; }
                  pre code { background: transparent; padding: 0; }
                  a { color: %s; text-decoration: none; }
                  a:hover { text-decoration: underline; }
                  table { border-collapse: collapse; margin-bottom: 8px; }
                  th, td { border: 1px solid rgba(63, 81, 181, 0.3); padding: 6px 10px; }
                  blockquote { border-left: 3px solid rgba(63, 81, 181, 0.35); margin: 0; padding: 6px 12px; color: inherit; background: rgba(63, 81, 181, 0.08); border-radius: 0 12px 12px 0; }
                  hr { border: 0; border-top: 1px solid rgba(63, 81, 181, 0.2); margin: 12px 0; }
                </style>
              </head>
              <body class="markdown-body">%s</body>
            </html>
            """.formatted(safeTextColor, safeLinkColor, body);
    }
}
