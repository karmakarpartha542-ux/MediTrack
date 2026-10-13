package com.meditrack.service;

import com.meditrack.model.room.Cabin;
import com.meditrack.model.room.GeneralWard;
import com.meditrack.model.room.ICU;
import com.meditrack.model.room.Room;
import com.meditrack.repository.FileRoomRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service managing hospital room allocations, vacating, and occupancy metrics.
 */
public class RoomService {

    private final FileRoomRepository roomRepo;

    public RoomService(FileRoomRepository roomRepo) {
        this.roomRepo = roomRepo;
    }

    /**
     * Retrieves all rooms in the hospital.
     *
     * @return list of rooms
     */
    public List<Room> getAllRooms() {
        return roomRepo.findAll();
    }

    /**
     * Retrieves all unoccupied rooms available for patient assignment.
     *
     * @return list of available rooms
     */
    public List<Room> getAvailableRooms() {
        return roomRepo.findAvailable();
    }

    /**
     * Finds a room by ID.
     *
     * @param roomId room identifier
     * @return Optional containing Room if found
     */
    public Optional<Room> getRoomById(String roomId) {
        return roomRepo.findById(roomId);
    }

    /**
     * Assigns an available room to a patient.
     *
     * @param roomId    room identifier
     * @param patientId patient identifier
     * @return true if room was successfully allocated, false if room not found
     */
    public boolean assignRoom(String roomId, String patientId) {
        Optional<Room> opt = roomRepo.findById(roomId);
        if (opt.isPresent()) {
            Room room = opt.get();
            room.allocate(patientId);
            roomRepo.update(room);
            return true;
        }
        return false;
    }

    /**
     * Vacates a room and makes it available for new patients.
     *
     * @param roomId room identifier
     * @return true if vacated successfully, false if room not found
     */
    public boolean vacateRoom(String roomId) {
        Optional<Room> opt = roomRepo.findById(roomId);
        if (opt.isPresent()) {
            Room room = opt.get();
            room.vacate();
            roomRepo.update(room);
            return true;
        }
        return false;
    }

    /**
     * Aggregates real-time room occupancy statistics broken down by room category.
     *
     * @return map with total and category-specific room counts
     */
    public Map<String, Object> getRoomStats() {
        List<Room> allRooms = roomRepo.findAll();

        int totalRooms = allRooms.size();
        int occupiedCount = 0;
        int availableCount = 0;

        int generalWardCount = 0;
        int generalWardOccupied = 0;

        int cabinCount = 0;
        int cabinOccupied = 0;

        int icuCount = 0;
        int icuOccupied = 0;

        for (Room r : allRooms) {
            boolean occupied = r.isOccupied();
            if (occupied) {
                occupiedCount++;
            } else {
                availableCount++;
            }

            if (r instanceof ICU || r.getRoomType().toUpperCase().contains("ICU")) {
                icuCount++;
                if (occupied) icuOccupied++;
            } else if (r instanceof Cabin || r.getRoomType().toUpperCase().contains("CABIN")) {
                cabinCount++;
                if (occupied) cabinOccupied++;
            } else {
                generalWardCount++;
                if (occupied) generalWardOccupied++;
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRooms", totalRooms);
        stats.put("occupiedCount", occupiedCount);
        stats.put("availableCount", availableCount);
        stats.put("generalWardCount", generalWardCount);
        stats.put("generalWardOccupied", generalWardOccupied);
        stats.put("cabinCount", cabinCount);
        stats.put("cabinOccupied", cabinOccupied);
        stats.put("icuCount", icuCount);
        stats.put("icuOccupied", icuOccupied);

        return stats;
    }
}
