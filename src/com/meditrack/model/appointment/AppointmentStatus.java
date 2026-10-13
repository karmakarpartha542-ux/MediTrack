package com.meditrack.model.appointment;

/**
 * Status of an outpatient appointment in the MediTrack hospital system.
 */
public enum AppointmentStatus {
    SCHEDULED("Scheduled"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    AppointmentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Resolves an appointment status from string input.
     * Defaults to {@code SCHEDULED} if no direct match is found.
     *
     * @param s string representation
     * @return matching AppointmentStatus or SCHEDULED
     */
    public static AppointmentStatus fromString(String s) {
        if (s == null || s.trim().isEmpty()) {
            return SCHEDULED;
        }
        for (AppointmentStatus status : values()) {
            if (status.name().equalsIgnoreCase(s.trim()) || status.displayName.equalsIgnoreCase(s.trim())) {
                return status;
            }
        }
        return SCHEDULED;
    }
}
