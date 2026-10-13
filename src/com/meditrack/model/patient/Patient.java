package com.meditrack.model.patient;

import com.meditrack.model.common.Person;
import com.meditrack.util.JsonUtils;

/**
 * =====================================================================
 * OOP CONCEPT: INHERITANCE + ENCAPSULATION
 * =====================================================================
 * Patient EXTENDS Person — it INHERITS id, name, phone, email, age
 * and adds hospital-specific fields.
 *
 * INHERITANCE demonstrated:
 * - Patient reuses Person's constructor and getters/setters.
 * - Patient OVERRIDES getRole() to return "Patient".
 * - Patient OVERRIDES toJson() to add its own fields after calling
 *   super.toJson() to include inherited fields.
 *
 * ENCAPSULATION demonstrated:
 * - New fields (bloodGroup, medicalHistory, etc.) are private.
 * - State change methods (setAdmitted, setAssignedRoomId) control
 *   when and how the patient's admission status changes.
 * =====================================================================
 */
public class Patient extends Person {

    // -------------------------
    // Patient-specific private fields (ENCAPSULATION)
    // -------------------------
    private String  bloodGroup;
    private String  medicalHistory;
    private String  admissionDate;
    private boolean isAdmitted;
    private String  assignedRoomId;   // null if not admitted to a room

    // -------------------------
    // Constructors
    // -------------------------

    public Patient() { super(); }

    /**
     * Full constructor.
     */
    public Patient(String id, String name, String phone, String email,
                   int age, String bloodGroup, String medicalHistory) {
        super(id, name, phone, email, age);
        this.bloodGroup     = bloodGroup;
        this.medicalHistory = medicalHistory;
        this.isAdmitted     = false;
        this.assignedRoomId = null;
        this.admissionDate  = java.time.LocalDateTime.now().toString();
    }

    // -------------------------
    // INHERITANCE: override abstract method from Person
    // -------------------------

    @Override
    public String getRole() {
        return "Patient";
    }

    // -------------------------
    // Business state methods (ENCAPSULATION)
    // -------------------------

    public void admit(String roomId) {
        this.isAdmitted     = true;
        this.assignedRoomId = roomId;
    }

    public void discharge() {
        this.isAdmitted     = false;
        this.assignedRoomId = null;
    }

    // -------------------------
    // Getters & Setters
    // -------------------------

    public String  getBloodGroup()     { return bloodGroup; }
    public String  getMedicalHistory() { return medicalHistory; }
    public String  getAdmissionDate()  { return admissionDate; }
    public boolean isAdmitted()        { return isAdmitted; }
    public String  getAssignedRoomId() { return assignedRoomId; }

    public void setBloodGroup(String bg)         { this.bloodGroup = bg; }
    public void setMedicalHistory(String mh)     { this.medicalHistory = mh; }
    public void setAdmissionDate(String date)    { this.admissionDate = date; }
    public void setAdmitted(boolean admitted)    { this.isAdmitted = admitted; }
    public void setAssignedRoomId(String roomId) { this.assignedRoomId = roomId; }

    // -------------------------
    // JSON Serialization (INHERITANCE: calls super.toJson())
    // -------------------------

    @Override
    public String toJson() {
        return "{" + super.toJson() + "," +
               "\"bloodGroup\":\""     + escape(bloodGroup)     + "\"," +
               "\"medicalHistory\":\"" + escape(medicalHistory) + "\"," +
               "\"admissionDate\":\""  + escape(admissionDate)  + "\"," +
               "\"isAdmitted\":"       + isAdmitted             + "," +
               "\"assignedRoomId\":"   + (assignedRoomId == null ? "null" : "\"" + escape(assignedRoomId) + "\"") +
               "}";
    }

    /**
     * Parses a Patient from a JSON string.
     * Uses JsonUtils for simple key-value extraction.
     */
    public static Patient fromJson(String json) {
        Patient p = new Patient();
        p.setId(JsonUtils.getString(json, "id"));
        p.setName(JsonUtils.getString(json, "name"));
        p.setPhone(JsonUtils.getString(json, "phone"));
        p.setEmail(JsonUtils.getString(json, "email"));
        p.setAge(JsonUtils.getInt(json, "age"));
        p.setBloodGroup(JsonUtils.getString(json, "bloodGroup"));
        p.setMedicalHistory(JsonUtils.getString(json, "medicalHistory"));
        p.setAdmissionDate(JsonUtils.getString(json, "admissionDate"));
        p.setAdmitted(JsonUtils.getBoolean(json, "isAdmitted"));
        String roomId = JsonUtils.getString(json, "assignedRoomId");
        p.setAssignedRoomId(roomId.isEmpty() || roomId.equals("null") ? null : roomId);
        return p;
    }
}
