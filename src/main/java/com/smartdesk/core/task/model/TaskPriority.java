package com.smartdesk.core.task.model;

import java.util.Arrays;

/**
 * Enumeration representing the priority of a task. The levels intentionally
 * mirror common productivity systems such as the Eisenhower matrix and can be
 * mapped to UI colours or icons.
 */
public enum TaskPriority {
    LOW(1),
    NORMAL(2),
    HIGH(3),
    URGENT(4),
    CRITICAL(5);

    private final int level;

    TaskPriority(final int level) {
        this.level = level;
    }

    /**
     * Numeric priority level that is persisted in the database.
     *
     * @return the priority level in the range {@code 1..5}
     */
    public int getLevel() {
        return level;
    }

    /**
     * Resolves a {@link TaskPriority} from a numeric value stored in the
     * database. Values outside of the supported range result in
     * {@link TaskPriority#NORMAL}.
     *
     * @param level stored numeric level
     * @return the resolved priority
     */
    public static TaskPriority fromLevel(final int level) {
        return Arrays.stream(values())
            .filter(priority -> priority.level == level)
            .findFirst()
            .orElse(NORMAL);
    }
}
