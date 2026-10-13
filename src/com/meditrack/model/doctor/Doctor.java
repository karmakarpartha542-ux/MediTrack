package com.meditrack.model.doctor;

import com.meditrack.model.common.Billable;
import com.meditrack.model.common.Person;
import com.meditrack.util.JsonUtils;

/**
 * ============================================================================
 * OOP CONCEPT: INHERITANCE
 * ============================================================================
 * Inheritance is the mechanism where a new class (subclass/derived class) derives
 * properties and behaviors from an existing class (superclass/base class).
 *
 * Here, {@code Doctor} extends {@code Person}:
 * - It inherits baseline human attributes (id, name, phone, email, age) and methods.
 * - It avoids code duplication across human actors in the hospital.
 * - It specializes {@code Person} by introducing medical attributes like
 *   {@code specialization}, {@code consultationFee}, {@code isAvailable}, and {@code department}.
 *
 * Furthermore, {@code Doctor} implements {@link Billable}, enabling doctors
 * to be treated as billing items polymorphically alongside rooms and services.
 * ============================================================================
 */
public class Doctor extends Person implements Billable {

    private Specialization specialization;
    private double consultationFee;
    private boolean isAvailable;
    private String department;

    /**
     * Default no-arg constructor.
     */
    public Doctor() {
        super();
        this.specialization = Specialization.GENERAL_MEDICINE;
        this.consultationFee = 0.0;
        this.isAvailable = true;
        this.department = "General Medicine";
    }

    /**
     * Convenience constructor with default availability set to true.
     */
    public Doctor(String id, String name, String phone, String email, int age,
                  Specialization specialization, double consultationFee, String department) {
        this(id, name, phone, email, age, specialization, consultationFee, true, department);
    }

    /**
     * Full parameterized constructor for doctor.
     */
    public Doctor(String id, String name, String phone, String email, int age,
                  Specialization specialization, double consultationFee, boolean isAvailable,
                  String department) {
        super(id, name, phone, email, age > 0 ? age : 1);
        this.specialization = specialization != null ? specialization : Specialization.GENERAL_MEDICINE;
        this.consultationFee = Math.max(0.0, consultationFee);
        this.isAvailable = isAvailable;
        this.department = (department != null && !department.trim().isEmpty())
                ? department.trim()
                : this.specialization.getDisplayName() + " Department";
    }

    @Override
    public String getRole() {
        return "Doctor";
    }

    /**
     * ========================================================================
     * OOP CONCEPT: POLYMORPHISM (Interface Implementation)
     * ========================================================================
     * Polymorphism allows an object to take many forms.
     * By implementing {@link Billable#getCharge()}, {@code Doctor} provides its own
     * specific implementation of monetary calculation (its consultation fee).
     *
     * The billing engine can invoke {@code billable.getCharge()} on any object
     * implementing {@code Billable} (e.g. Doctor or Room) and dynamic dispatch
     * ensures the appropriate calculation is executed at runtime.
     * ========================================================================
     *
     * @return consultation fee for this doctor
     */
    @Override
    public double getCharge() {
        return consultationFee;
    }

    @Override
    public String getBillingDescription() {
        return "Consultation - Dr. " + getName() + " (" + specialization.getDisplayName() + ")";
    }

    /**
     * Toggles the availability status of the doctor for taking appointments.
     */
    public void toggleAvailability() {
        this.isAvailable = !this.isAvailable;
    }

    // ------------------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------------------

    public Specialization getSpecialization() {
        return specialization;
    }

    public void setSpecialization(Specialization specialization) {
        this.specialization = specialization != null ? specialization : Specialization.GENERAL_MEDICINE;
    }

    public double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(double consultationFee) {
        this.consultationFee = Math.max(0.0, consultationFee);
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        this.isAvailable = available;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department != null ? department.trim() : "";
    }

    @Override
    public String toJson() {
        return "{"
                + "\"id\":\"" + JsonUtils.escape(getId()) + "\","
                + "\"name\":\"" + JsonUtils.escape(getName()) + "\","
                + "\"phone\":\"" + JsonUtils.escape(getPhone()) + "\","
                + "\"email\":\"" + JsonUtils.escape(getEmail()) + "\","
                + "\"age\":" + getAge() + ","
                + "\"role\":\"" + JsonUtils.escape(getRole()) + "\","
                + "\"specialization\":\"" + specialization.name() + "\","
                + "\"specializationDisplayName\":\"" + JsonUtils.escape(specialization.getDisplayName()) + "\","
                + "\"consultationFee\":" + consultationFee + ","
                + "\"isAvailable\":" + isAvailable + ","
                + "\"department\":\"" + JsonUtils.escape(department) + "\""
                + "}";
    }

    /**
     * Reconstructs a Doctor instance from a JSON string.
     *
     * @param json serialized Doctor JSON
     * @return Doctor instance
     */
    public static Doctor fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        String id = JsonUtils.getString(json, "id");
        String name = JsonUtils.getString(json, "name");
        String phone = JsonUtils.getString(json, "phone");
        String email = JsonUtils.getString(json, "email");
        int age = JsonUtils.getInt(json, "age");
        String specStr = JsonUtils.getString(json, "specialization");
        Specialization specialization = Specialization.fromString(specStr);
        double fee = JsonUtils.getDouble(json, "consultationFee");
        boolean isAvailable = JsonUtils.getBoolean(json, "isAvailable");
        String dept = JsonUtils.getString(json, "department");

        return new Doctor(id, name, phone, email, age, specialization, fee, isAvailable, dept);
    }
}
