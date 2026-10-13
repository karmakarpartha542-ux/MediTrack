package com.meditrack.model.room;

import com.meditrack.util.JsonUtils;

/**
 * Concrete implementation of {@link Room} representing a shared General Ward.
 *
 * Demonstrates:
 * - INHERITANCE: Inherits core room infrastructure.
 * - POLYMORPHISM: Implements {@code calculateCharges(int)} with flat per-day ward pricing.
 */
public class GeneralWard extends Room {

    private String bedNumber;
    private double dailyRate = 500.0;

    /**
     * Default no-arg constructor.
     */
    public GeneralWard() {
        super();
        this.bedNumber = "";
        this.dailyRate = 500.0;
    }

    /**
     * Constructor for creating a General Ward room.
     *
     * @param roomId     unique room identifier
     * @param roomNumber room number
     * @param floor      floor number
     * @param bedNumber  specific bed designation (e.g. "B1", "B2")
     */
    public GeneralWard(String roomId, String roomNumber, int floor, String bedNumber) {
        super(roomId, roomNumber, floor);
        this.bedNumber = bedNumber != null ? bedNumber.trim() : "";
        this.dailyRate = 500.0;
    }

    /**
     * Parameterized constructor with occupancy state.
     */
    public GeneralWard(String roomId, String roomNumber, int floor, String bedNumber,
                       boolean isOccupied, String patientId) {
        super(roomId, roomNumber, floor, isOccupied, patientId);
        this.bedNumber = bedNumber != null ? bedNumber.trim() : "";
        this.dailyRate = 500.0;
    }

    @Override
    public String getRoomType() {
        return "General Ward";
    }

    /**
     * Calculates ward charges as flat daily rate * days.
     *
     * @param days number of days
     * @return total charge
     */
    @Override
    public double calculateCharges(int days) {
        return dailyRate * Math.max(1, days);
    }

    public String getBedNumber() {
        return bedNumber;
    }

    public void setBedNumber(String bedNumber) {
        this.bedNumber = bedNumber != null ? bedNumber.trim() : "";
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
                + "\"roomType\":\"GENERAL_WARD\","
                + "\"roomTypeDisplay\":\"" + JsonUtils.escape(getRoomType()) + "\","
                + "\"bedNumber\":\"" + JsonUtils.escape(bedNumber) + "\","
                + "\"dailyRate\":" + dailyRate
                + "}";
    }

    /**
     * Deserializes a General Ward JSON representation.
     *
     * @param json JSON input string
     * @return GeneralWard instance
     */
    public static GeneralWard fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        String roomId = JsonUtils.getString(json, "roomId");
        String roomNumber = JsonUtils.getString(json, "roomNumber");
        int floor = JsonUtils.getInt(json, "floor");
        String bedNumber = JsonUtils.getString(json, "bedNumber");
        boolean isOccupied = JsonUtils.getBoolean(json, "isOccupied");
        String patientId = JsonUtils.getString(json, "patientId");
        if ("null".equalsIgnoreCase(patientId) || patientId.isEmpty()) {
            patientId = null;
        }

        GeneralWard ward = new GeneralWard(roomId, roomNumber, floor, bedNumber, isOccupied, patientId);
        double rate = JsonUtils.getDouble(json, "dailyRate");
        if (rate > 0) {
            ward.setDailyRate(rate);
        }
        return ward;
    }
}
