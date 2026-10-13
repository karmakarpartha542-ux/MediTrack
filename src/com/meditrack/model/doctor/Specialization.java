package com.meditrack.model.doctor;

/**
 * Enumeration representing medical specializations within MediTrack.
 * Encapsulates the display name of each specialty and provides safe conversion.
 */
public enum Specialization {
    CARDIOLOGY("Cardiology"),
    NEUROLOGY("Neurology"),
    ORTHOPEDICS("Orthopedics"),
    GENERAL_MEDICINE("General Medicine"),
    PEDIATRICS("Pediatrics"),
    ONCOLOGY("Oncology"),
    RADIOLOGY("Radiology"),
    EMERGENCY_MEDICINE("Emergency Medicine");

    private final String displayName;

    Specialization(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Resolves an enum constant from a string matching either the name or display name.
     * Defaults to {@code GENERAL_MEDICINE} if no match is found.
     *
     * @param s input string (e.g. "CARDIOLOGY", "Cardiology", or "cardiology")
     * @return matching Specialization or GENERAL_MEDICINE
     */
    public static Specialization fromString(String s) {
        if (s == null || s.trim().isEmpty()) {
            return GENERAL_MEDICINE;
        }
        String clean = s.trim().replace(" ", "_");
        for (Specialization spec : values()) {
            if (spec.name().equalsIgnoreCase(clean) || spec.displayName.equalsIgnoreCase(s.trim())) {
                return spec;
            }
        }
        return GENERAL_MEDICINE;
    }
}
