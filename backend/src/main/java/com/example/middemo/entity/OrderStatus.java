package com.example.middemo.entity;

/**
 * Order lifecycle.
 *
 * <pre>
 * CREATED -> PAID -> COMPLETED
 * CREATED -> CANCELLED
 * </pre>
 *
 * Everything else (for example {@code CANCELLED -> PAID}) is rejected by the service layer.
 */
public enum OrderStatus {
    CREATED,
    PAID,
    CANCELLED,
    COMPLETED;

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case CREATED -> target == PAID || target == CANCELLED;
            case PAID -> target == COMPLETED;
            case CANCELLED, COMPLETED -> false;
        };
    }
}
