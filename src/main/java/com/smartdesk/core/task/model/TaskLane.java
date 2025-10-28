package com.smartdesk.core.task.model;

/**
 * Logical columns used in the dashboard view. Each lane groups tasks sharing a
 * similar scheduling context.
 */
public enum TaskLane {
    TODAY("今日必做", "今天必须完成或即将开始的任务", "#ff7a45", "calendar-today"),
    UPCOMING("即将到来", "未来几天需要关注的任务", "#40a9ff", "calendar"),
    SOMEDAY("待安排", "尚未安排具体日期的想法或长期目标", "#595959", "inbox"),
    COURSE("课程任务", "课程或学习计划相关的任务", "#73d13d", "book"),
    ANNIVERSARY("纪念日", "纪念日、生日等特殊提醒", "#9254de", "gift"),
    OVERDUE("已逾期", "超过截止时间仍未完成的任务", "#f5222d", "alert"),
    COMPLETED("已完成", "所有标记为完成的任务归档区", "#52c41a", "check-circle");

    private final String displayName;
    private final String description;
    private final String accentColor;
    private final String icon;

    TaskLane(final String displayName, final String description,
             final String accentColor, final String icon) {
        this.displayName = displayName;
        this.description = description;
        this.accentColor = accentColor;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public String getIcon() {
        return icon;
    }
}
