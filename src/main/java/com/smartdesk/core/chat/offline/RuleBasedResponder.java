package com.smartdesk.core.chat.offline;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Lightweight rule based responder for offline usage.
 */
public final class RuleBasedResponder {

    private final Map<String, String> keywordResponses = new LinkedHashMap<>();

    public RuleBasedResponder() {
        keywordResponses.put("你好", "你好，我是你的智能办公助手，有什么可以帮你吗？");
        keywordResponses.put("任务", "你可以在任务面板中创建、编辑和跟踪任务，必要时我也可以帮你总结。");
        keywordResponses.put("总结", "总结功能可以帮助你快速梳理重点，目前仍在迭代中。");
        keywordResponses.put("帮助", "如果需要帮助，可以告诉我你的问题，我会给出操作提示。");
        keywordResponses.put("会议", "建议提前梳理议程并准备纪要模版，这样可以提升会议效率。");
        keywordResponses.put("提醒", "设置提醒可以帮助你按时完成关键事项，别忘了为重要任务设置提醒哦！");
    }

    public String respond(final String message) {
        if (message == null || message.isBlank()) {
            return "我没有听清，可以再说一次吗？";
        }
        String lower = message.toLowerCase(Locale.CHINA);
        for (Map.Entry<String, String> entry : keywordResponses.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        if (lower.endsWith("吗") || lower.endsWith("?")) {
            return "这是个好问题，我会继续学习，稍后也可以尝试在线模式获取更详细的答案。";
        }
        return "我已经记录下来了，如需更深入的建议可以切换到在线模式与大模型对话。";
    }
}
