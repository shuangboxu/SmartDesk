package com.smartdesk.core.chat;

/**
 * Builds textual snippets that describe attachments for providers requiring inline context.
 */
public final class AttachmentPromptFormatter {

    private static final AttachmentTextExtractor EXTRACTOR = new AttachmentTextExtractor();

    private AttachmentPromptFormatter() {
    }

    public static String buildContentWithAttachments(final ChatMessage message) {
        if (message == null || !message.hasAttachments()) {
            return message == null ? "" : message.getContent();
        }
        StringBuilder builder = new StringBuilder(message.getContent() == null ? "" : message.getContent());
        message.getAttachments().forEach(attachment -> {
            builder.append("\n\n[附件] ")
                .append(attachment.describe())
                .append("\n");
            try {
                builder.append(EXTRACTOR.extract(attachment));
            } catch (Exception ex) {
                builder.append("(提取附件内容失败: ").append(ex.getMessage()).append(')');
            }
        });
        return builder.toString();
    }
}
