package com.smartdesk.core.task.model;

/**
 * Logical columns used in the dashboard view. Each lane groups tasks sharing a
 * similar scheduling context.
 */
public enum TaskLane {
    TODAY,
    UPCOMING,
    SOMEDAY,
    COURSE,
    ANNIVERSARY,
    OVERDUE,
    COMPLETED
}
