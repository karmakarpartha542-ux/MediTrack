package com.meditrack.model.room;

import com.meditrack.util.JsonUtils;

/**
 * Concrete implementation of {@link Room} representing an Intensive Care Unit (ICU).
 *
 * Demonstrates:
 * - INHERITANCE: Builds upon base Room structure.
 * - POLYMORPHISM: Dynamic fee computation including high-dependency ventilator surcharges.
 */
public class ICU extends Room {

    private boolean ventilatorRequired;
    private double dailyRate = 5000.0;
    private double ventilatorSurcharge = 2000.0;

    /**
     * Default no-arg constructor.
     */
    public ICU() {
        super();
        this.ventilatorRequired = false;
        this.dailyRate = 5000.0;
        this.ventilatorSurcharge = 2000.0;
    }

    /**
     * Parameterized constructor for ICU room.
     *
     * @param roomId             unique room identifier
     * @param roomNumber         display room number
     * @param floor              floor number
     * @param ventilatorRequired whether advanced life-support ventilator is equipped
     */
    public ICU(String roomId, String roomNumber, int floor, boolean ventilatorRequired) {
        super(roomId, roomNumber, floor);
        this.ventilatorRequired = ventilatorRequired;
        this.dailyRate = 5000.0;
        this.ventilatorSurcharge = 2000.0;
    }

    /**
     * Full parameterized constructor including occupancy status.
     */
    public ICU(String roomId, String roomNumber, int floor, boolean ventilatorRequired,
               boolean isOccupied, String patientId) {
        super(roomId, roomNumber, floor, isOccupied, patientId);
        this.ventilatorRequired = ventilatorRequired;
        this.dailyRate = 5000.0;
        this.ventilatorSurcharge = 2000.0;
    }

    @Override
    public String getRoomType() {
        return "Intensive Care Unit (ICU)";
    }

    /**
     * Calculates ICU charges:
     * base daily rate * days + (ventilatorRequired ? ventilator surcharge * days : 0).
     *
     * @param days number of days
     * @return total ICU stay charges
     */
    @Override
    public double calculateCharges(int days) {
        int d = Math.max(1, days);
        return (dailyRate * d) + (ventilatorRequired ? ventilatorSurcharge * d : 0.0);
    }

    public boolean isVentilatorRequired() {
        return ventilatorRequired;
    }

    public void setVentilatorRequired(boolean ventilatorRequired) {
        this.ventilatorRequired = ventilatorRequired;
    }

    public double getDailyRate() {
        return dailyRate;
    }

    public void setDailyRate(double dailyRate) {
        this.dailyRate = dailyRate;
    }

    public double getVentilatorSurcharge() {
        return ventilatorSurcharge;
    }

    public void setVentilatorSurcharge(double ventilatorSurcharge) {
        this.ventilatorSurcharge = ventilatorSurcharge;
    }

    @Override
    public String toJson() {
        String patientIdVal = (getPatientId() != null && !getPatientId().isEmpty())
                ? "\"" + JsonUtils.escape(getPatientId()) + "\""
                : "null";

        return "{"
                + "\"roomId\":\"" + JsonUtils.escape(getRoomId()) + "\","
                + "\"roomNumber\":\"" + JsonUtils.escape(getRoomNumber()) + "\","
                + "\"floor\":" + getFloor() + ","
                + "\"isOccupied\":" + isOccupied() + ","
                + "\"patientId\":" + patientIdVal + ","
                + "\"roomType\":\"ICU\","
                + "\"roomTypeDisplay\":\"" + JsonUtils.escape(getRoomType()) + "\","
                + "\"ventilatorRequired\":" + ventilatorRequired + ","
                + "\"dailyRate\":" + dailyRate + ","
                + "\"ventilatorSurcharge\":" + ventilatorSurcharge
                + "}";
    }

    /**
     * Reconstructs an ICU instance from JSON format.
     *
     * @param json serialized JSON string
     * @return ICU object
     */
    public static ICU fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        String roomId = JsonUtils.getString(json, "roomId");
        String roomNumber = JsonUtils.getString(json, "roomNumber");
        int floor = JsonUtils.getInt(json, "floor");
        boolean ventilatorRequired = JsonUtils.getBoolean(json, "ventilatorRequired");
        boolean isOccupied = JsonUtils.getBoolean(json, "isOccupied");
        String patientId = JsonUtils.getString(json, "patientId");
        if ("null".equalsIgnoreCase(patientId) || patientId.isEmpty()) {
            patientId = null;
        }

        ICU icu = new ICU(roomId, roomNumber, floor, ventilatorRequired, isOccupied, patientId);
        double rate = JsonUtils.getDouble(json, "dailyRate");
        if (rate > 0) {
            icu.setDailyRate(rate);
        }
        double surcharge = JsonUtils.getDouble(json, "ventilatorSurcharge");
        if (surcharge > 0) {
            icu.setVentilatorSurcharge(surcharge);
        }
        return icu;
    }
}
