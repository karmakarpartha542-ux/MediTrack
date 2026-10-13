package com.meditrack.model.room;

import com.meditrack.util.JsonUtils;

/**
 * Concrete implementation of {@link Room} representing a private cabin.
 *
 * Demonstrates:
 * - INHERITANCE: Specializes generic room concepts for single-occupancy cabins.
 * - POLYMORPHISM: Calculates charges factoring in Air Conditioning (AC) surcharges.
 */
public class Cabin extends Room {

    private boolean hasAC;
    private double dailyRate;

    /**
     * Default no-arg constructor.
     */
    public Cabin() {
        super();
        this.hasAC = false;
        this.dailyRate = 1500.0;
    }

    /**
     * Parameterized constructor for private cabin.
     *
     * @param roomId     unique room identifier
     * @param roomNumber display room number
     * @param floor      building floor number
     * @param hasAC      true if equipped with air conditioning
     */
    public Cabin(String roomId, String roomNumber, int floor, boolean hasAC) {
        super(roomId, roomNumber, floor);
        this.hasAC = hasAC;
        this.dailyRate = hasAC ? 2000.0 : 1500.0;
    }

    /**
     * Full parameterized constructor including occupancy state.
     */
    public Cabin(String roomId, String roomNumber, int floor, boolean hasAC,
                 boolean isOccupied, String patientId) {
        super(roomId, roomNumber, floor, isOccupied, patientId);
        this.hasAC = hasAC;
        this.dailyRate = hasAC ? 2000.0 : 1500.0;
    }

    @Override
    public String getRoomType() {
        return "Private Cabin";
    }

    /**
     * Calculates accommodation charges:
     * base daily rate * days + additional AC maintenance surcharge ($200/day) if AC is present.
     *
     * @param days number of days
     * @return total cabin charge
     */
    @Override
    public double calculateCharges(int days) {
        int d = Math.max(1, days);
        return (dailyRate * d) + (hasAC ? 200.0 * d : 0.0);
    }

    public boolean isHasAC() {
        return hasAC;
    }

    public void setHasAC(boolean hasAC) {
        this.hasAC = hasAC;
        this.dailyRate = hasAC ? 2000.0 : 1500.0;
    }

    public double getDailyRate() {
        return dailyRate;
    }

    public void setDailyRate(double dailyRate) {
        this.dailyRate = dailyRate;
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
                + "\"roomType\":\"CABIN\","
                + "\"roomTypeDisplay\":\"" + JsonUtils.escape(getRoomType()) + "\","
                + "\"hasAC\":" + hasAC + ","
                + "\"dailyRate\":" + dailyRate
                + "}";
    }

    /**
     * Deserializes a Cabin JSON string into a Cabin instance.
     *
     * @param json serialized JSON string
     * @return Cabin object
     */
    public static Cabin fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        String roomId = JsonUtils.getString(json, "roomId");
        String roomNumber = JsonUtils.getString(json, "roomNumber");
        int floor = JsonUtils.getInt(json, "floor");
        boolean hasAC = JsonUtils.getBoolean(json, "hasAC");
        boolean isOccupied = JsonUtils.getBoolean(json, "isOccupied");
        String patientId = JsonUtils.getString(json, "patientId");
        if ("null".equalsIgnoreCase(patientId) || patientId.isEmpty()) {
            patientId = null;
        }

        Cabin cabin = new Cabin(roomId, roomNumber, floor, hasAC, isOccupied, patientId);
        double rate = JsonUtils.getDouble(json, "dailyRate");
        if (rate > 0) {
            cabin.setDailyRate(rate);
        }
        return cabin;
    }
}
