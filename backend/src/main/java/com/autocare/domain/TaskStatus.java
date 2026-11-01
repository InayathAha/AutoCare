package com.autocare.domain;

/**
 * Task lifecycle states.
 * Allowed forward transitions: PENDING -> IN_PROGRESS -> COMPLETED.
 */
public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}
