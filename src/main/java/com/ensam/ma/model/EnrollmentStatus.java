package com.ensam.ma.model;

/**
 * Enrollment status for course validation tracking
 * Used by Agentic Supervisor to recommend validation
 */
public enum EnrollmentStatus {
    IN_PROGRESS, // Student is still learning
    PENDING_VALIDATION, // Agent recommends validation, awaiting admin approval
    VALIDATED // Admin approved validation
}
