package com.meditrack.model.appointment;

import com.meditrack.util.JsonUtils;

/**
 * Represents a medical appointment between a patient and a doctor.
 * Encapsulates the scheduling state, metadata, and lifecycle transitions.
 */
public class Appointment {

    private String appointmentId;
    private String patientId;
    private String patientName;
    private String doctorId;
    private String doctorName;
    private String specialization;
    private String dateTime;
    private AppointmentStatus status;
    private String notes;

    /**
     * Default no-arg constructor.
     */
    public Appointment() {
        this.status = AppointmentStatus.SCHEDULED;
    }

    /**
     * Parameterized constructor initializing all appointment details.
     *
     * @param appointmentId  unique appointment identifier
     * @param patientId      patient identifier
     * @param patientName    patient full name
     * @param doctorId       doctor identifier
     * @param doctorName     doctor full name
     * @param specialization medical specialty
     * @param dateTime       scheduled date/time string (ISO format or YYYY-MM-DD HH:mm)
     * @param status         appointment state
     * @param notes          clinical or consultation notes
     */
    public Appointment(String appointmentId, String patientId, String patientName,
                       String doctorId, String doctorName, String specialization,
                       String dateTime, AppointmentStatus status, String notes) {
        this.appointmentId = appointmentId != null ? appointmentId.trim() : "";
        this.patientId = patientId != null ? patientId.trim() : "";
        this.patientName = patientName != null ? patientName.trim() : "";
        this.doctorId = doctorId != null ? doctorId.trim() : "";
        this.doctorName = doctorName != null ? doctorName.trim() : "";
        this.specialization = specialization != null ? specialization.trim() : "";
        this.dateTime = dateTime != null ? dateTime.trim() : "";
        this.status = status != null ? status : AppointmentStatus.SCHEDULED;
        this.notes = notes != null ? notes.trim() : "";
    }

    /**
     * Cancels the scheduled appointment.
     */
    public void cancel() {
        this.status = AppointmentStatus.CANCELLED;
    }

    /**
     * Marks the appointment as successfully completed.
     */
    public void complete() {
        this.status = AppointmentStatus.COMPLETED;
    }

    // ------------------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------------------

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId != null ? appointmentId.trim() : "";
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId != null ? patientId.trim() : "";
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName != null ? patientName.trim() : "";
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId != null ? doctorId.trim() : "";
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName != null ? doctorName.trim() : "";
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization != null ? specialization.trim() : "";
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime != null ? dateTime.trim() : "";
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status != null ? status : AppointmentStatus.SCHEDULED;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes != null ? notes.trim() : "";
    }

    /**
     * Converts this appointment into a JSON formatted string.
     *
     * @return JSON string representing the appointment
     */
    public String toJson() {
        return "{"
                + "\"appointmentId\":\"" + JsonUtils.escape(appointmentId) + "\","
                + "\"patientId\":\"" + JsonUtils.escape(patientId) + "\","
                + "\"patientName\":\"" + JsonUtils.escape(patientName) + "\","
                + "\"doctorId\":\"" + JsonUtils.escape(doctorId) + "\","
                + "\"doctorName\":\"" + JsonUtils.escape(doctorName) + "\","
                + "\"specialization\":\"" + JsonUtils.escape(specialization) + "\","
                + "\"dateTime\":\"" + JsonUtils.escape(dateTime) + "\","
                + "\"status\":\"" + status.name() + "\","
                + "\"statusDisplayName\":\"" + JsonUtils.escape(status.getDisplayName()) + "\","
                + "\"notes\":\"" + JsonUtils.escape(notes) + "\""
                + "}";
    }

    /**
     * Reconstructs an Appointment instance from JSON format.
     *
     * @param json JSON input string
     * @return Appointment instance
     */
    public static Appointment fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        String appointmentId = JsonUtils.getString(json, "appointmentId");
        String patientId = JsonUtils.getString(json, "patientId");
        String patientName = JsonUtils.getString(json, "patientName");
        String doctorId = JsonUtils.getString(json, "doctorId");
        String doctorName = JsonUtils.getString(json, "doctorName");
        String specialization = JsonUtils.getString(json, "specialization");
        String dateTime = JsonUtils.getString(json, "dateTime");
        String statusStr = JsonUtils.getString(json, "status");
        AppointmentStatus status = AppointmentStatus.fromString(statusStr);
        String notes = JsonUtils.getString(json, "notes");

        return new Appointment(appointmentId, patientId, patientName, doctorId, doctorName, specialization, dateTime, status, notes);
    }

    @Override
    public String toString() {
        return "Appointment{" +
                "appointmentId='" + appointmentId + '\'' +
                ", patientName='" + patientName + '\'' +
                ", doctorName='" + doctorName + '\'' +
                ", dateTime='" + dateTime + '\'' +
                ", status=" + status +
                '}';
    }
}
