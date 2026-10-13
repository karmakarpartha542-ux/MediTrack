package com.meditrack.model.common;

/**
 * =====================================================================
 * OOP CONCEPT: ABSTRACTION (Interface)
 * =====================================================================
 * Billable is an interface that defines a CONTRACT for billing.
 * Any class that can be billed MUST implement this interface.
 *
 * WHY ABSTRACTION?
 * - We hide HOW the charge is calculated behind this interface.
 * - BillingService works with List<Billable> without caring whether
 *   the item is a GeneralWard, Cabin, ICU, or a Doctor consultation.
 * - This is the foundation of POLYMORPHISM in our billing system.
 *
 * IMPLEMENTING CLASSES:
 * - Doctor       → consultationFee
 * - GeneralWard  → 500.0 * days
 * - Cabin        → (1500 or 2000) * days + AC surcharge
 * - ICU          → 5000 * days + ventilator surcharge
 * =====================================================================
 */
public interface Billable {

    /**
     * Returns the base charge for this billable entity.
     * Each class implements this differently — POLYMORPHISM.
     *
     * @return the charge amount in USD
     */
    double getCharge();

    /**
     * Returns a human-readable description for the bill line item.
     *
     * @return description string (e.g., "Consultation - Dr. Ahmed (Cardiology)")
     */
    String getBillingDescription();
}
