package com.smartdesk.core.chat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Provides best-effort extraction of textual content from attachments for providers without file APIs.
 */
public final class AttachmentTextExtractor {

    private static final int DEFAULT_MAX_CHARACTERS = 4000;

    public String extract(final ChatAttachment attachment) throws IOException {
        return extract(attachment, DEFAULT_MAX_CHARACTERS);
    }

    public String extract(final ChatAttachment attachment, final int maxCharacters) throws IOException {
        if (attachment == null) {
            return "";
        }
        if (!Files.exists(attachment.getFilePath())) {
            return "(附件无法读取: 文件已被移动或删除)";
        }
        if (!isPlainText(attachment)) {
            return buildMetadataOnlySnippet(attachment);
        }
        Charset charset = detectCharset(attachment.getMimeType());
        String content = Files.readString(attachment.getFilePath(), charset);
        if (content.length() > maxCharacters) {
            return content.substring(0, maxCharacters) + "\n... (内容已截断)";
        }
        return content;
    }

    private boolean isPlainText(final ChatAttachment attachment) {
        String mime = attachment.getMimeType() == null ? "" : attachment.getMimeType().toLowerCase(Locale.ROOT);
        if (mime.startsWith("text/")) {
            return true;
        }
        String name = attachment.getFileName() == null ? "" : attachment.getFileName().toLowerCase(Locale.ROOT);
        return name.endsWith(".txt") || name.endsWith(".csv") || name.endsWith(".json")
            || name.endsWith(".md") || name.endsWith(".log") || name.endsWith(".yaml") || name.endsWith(".yml");
    }

    private Charset detectCharset(final String mimeType) {
        if (mimeType == null) {
            return StandardCharsets.UTF_8;
        }
        String lower = mimeType.toLowerCase(Locale.ROOT);
        if (lower.contains("charset=")) {
            String[] tokens = lower.split("charset=");
            if (tokens.length > 1) {
                String charsetName = tokens[1].replace(';', ' ').trim();
                try {
                    return Charset.forName(charsetName);
                } catch (Exception ignored) {
                    return StandardCharsets.UTF_8;
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    private String buildMetadataOnlySnippet(final ChatAttachment attachment) {
        return "此附件为非文本文件，请结合以下元信息处理:\n"
            + "文件名: " + attachment.getFileName() + "\n"
            + "类型: " + attachment.getMimeType() + "\n"
            + "大小: " + attachment.describe();
    }
}
