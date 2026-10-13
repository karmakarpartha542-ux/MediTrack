package com.meditrack.model.room;

import com.meditrack.model.common.Billable;
import com.meditrack.util.JsonUtils;

/**
 * ============================================================================
 * OOP CONCEPTS: ABSTRACTION & POLYMORPHISM
 * ============================================================================
 * 1. ABSTRACTION:
 * {@code Room} is declared as an {@code abstract} class representing the generic
 * concept of a hospital room.
 * - It defines the common structure (roomId, roomNumber, floor, occupancy state).
 * - It leaves specialized behavior unspecified by declaring abstract methods:
 *   {@link #calculateCharges(int)} and {@link #getRoomType()}.
 * - Abstract classes prevent direct instantiation of incomplete models: you
 *   cannot instantiate a generic "Room", only a concrete room type such as
 *   {@code GeneralWard}, {@code Cabin}, or {@code ICU}.
 *
 * 2. POLYMORPHISM:
 * By implementing the {@link Billable} contract, any room subtype can be treated
 * uniformly as a billable item.
 * - Calling {@link #getCharge()} invokes {@link #calculateCharges(1)}, dynamically
 *   delegating to the subclass's charge calculation formula at runtime.
 * - A billing service can process collections of {@code Room} objects polymorphically
 *   without checking concrete class types.
 * ============================================================================
 */
public abstract class Room implements Billable {

    /*
     * ========================================================================
     * ABSTRACTION & ENCAPSULATION:
     * Base attributes common to all rooms in the hospital facility.
     * ========================================================================
     */
    private String roomId;
    private String roomNumber;
    private int floor;
    private boolean isOccupied;
    private String patientId; // null if room is vacant

    /**
     * Protected no-arg constructor.
     */
    protected Room() {
    }

    /**
     * Base constructor for hospital rooms.
     *
     * @param roomId     unique identifier for the room
     * @param roomNumber display room number (e.g., "GW-101", "ICU-301")
     * @param floor      building floor number
     */
    public Room(String roomId, String roomNumber, int floor) {
        this.roomId = roomId != null ? roomId.trim() : "";
        this.roomNumber = roomNumber != null ? roomNumber.trim() : "";
        this.floor = floor;
        this.isOccupied = false;
        this.patientId = null;
    }

    /**
     * Parameterized constructor with full state.
     */
    public Room(String roomId, String roomNumber, int floor, boolean isOccupied, String patientId) {
        this.roomId = roomId != null ? roomId.trim() : "";
        this.roomNumber = roomNumber != null ? roomNumber.trim() : "";
        this.floor = floor;
        this.isOccupied = isOccupied;
        this.patientId = (patientId != null && !patientId.trim().isEmpty() && !patientId.equalsIgnoreCase("null"))
                ? patientId.trim() : null;
    }

    /**
     * Abstract method: Calculates accommodation charges for a given stay duration.
     * Each concrete room type implements its unique pricing formula.
     *
     * @param days number of days the room is occupied
     * @return total room accommodation fee
     */
    public abstract double calculateCharges(int days);

    /**
     * Abstract method: Returns the descriptive room classification.
     *
     * @return readable name of the room tier
     */
    public abstract String getRoomType();

    /**
     * Implements {@link Billable#getCharge()} polymorphically by calculating
     * the standard single-day rate.
     *
     * @return 1-day room rate
     */
    @Override
    public double getCharge() {
        return calculateCharges(1);
    }

    /**
     * Implements {@link Billable#getBillingDescription()} describing this room charge.
     *
     * @return descriptive line item string
     */
    @Override
    public String getBillingDescription() {
        return getRoomType() + " - Room " + roomNumber;
    }

    /**
     * Allocates this room to an admitted patient.
     *
     * @param patientId identifier of the patient occupying the room
     */
    public void allocate(String patientId) {
        this.isOccupied = true;
        this.patientId = patientId != null ? patientId.trim() : null;
    }

    /**
     * Vacates the room, marking it available for subsequent admissions.
     */
    public void vacate() {
        this.isOccupied = false;
        this.patientId = null;
    }

    // ------------------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------------------

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId != null ? roomId.trim() : "";
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber != null ? roomNumber.trim() : "";
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public void setOccupied(boolean occupied) {
        this.isOccupied = occupied;
        if (!occupied) {
            this.patientId = null;
        }
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = (patientId != null && !patientId.trim().isEmpty() && !patientId.equalsIgnoreCase("null"))
                ? patientId.trim() : null;
    }

    /**
     * Serializes common room attributes to JSON format.
     * Subclasses can call or extend this formatting.
     *
     * @return JSON string containing room data
     */
    public String toJson() {
        String patientIdVal = (patientId != null && !patientId.isEmpty())
                ? "\"" + JsonUtils.escape(patientId) + "\""
                : "null";

        return "{"
                + "\"roomId\":\"" + JsonUtils.escape(roomId) + "\","
                + "\"roomNumber\":\"" + JsonUtils.escape(roomNumber) + "\","
                + "\"floor\":" + floor + ","
                + "\"isOccupied\":" + isOccupied + ","
                + "\"patientId\":" + patientIdVal + ","
                + "\"roomType\":\"" + JsonUtils.escape(getRoomType()) + "\","
                + "\"dailyRate\":" + getCharge()
                + "}";
    }

    @Override
    public String toString() {
        return "Room{" +
                "roomId='" + roomId + '\'' +
                ", roomNumber='" + roomNumber + '\'' +
                ", floor=" + floor +
                ", isOccupied=" + isOccupied +
                ", patientId='" + patientId + '\'' +
                ", type='" + getRoomType() + '\'' +
                '}';
    }
}
