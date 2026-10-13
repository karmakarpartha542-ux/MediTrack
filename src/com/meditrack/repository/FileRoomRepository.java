package com.meditrack.repository;

import com.meditrack.model.room.Cabin;
import com.meditrack.model.room.GeneralWard;
import com.meditrack.model.room.ICU;
import com.meditrack.model.room.Room;
import com.meditrack.util.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * File-backed persistence implementation for {@link Room} records.
 * Uses polymorphic deserialization based on the {@code roomType} discriminator.
 */
public class FileRoomRepository implements CrudRepository<Room, String> {

    private final Path filePath;

    public FileRoomRepository() {
        this("data/rooms.json");
    }

    public FileRoomRepository(String path) {
        this.filePath = Paths.get(path);
        initFile();
    }

    private void initFile() {
        try {
            if (filePath.getParent() != null && !Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            if (!Files.exists(filePath)) {
                Files.writeString(filePath, "[]");
            }
        } catch (IOException e) {
            System.err.println("Could not initialize room file: " + e.getMessage());
        }
    }

    /**
     * Reads rooms from JSON and dispatches to specific room subclasses polymorphically.
     *
     * @return list of Room instances
     */
    public synchronized List<Room> loadFromFile() {
        try {
            if (!Files.exists(filePath)) {
                initFile();
                return new ArrayList<>();
            }
            String content = Files.readString(filePath);
            List<String> jsonList = JsonUtils.parseJsonArray(content);
            List<Room> rooms = new ArrayList<>();

            for (String json : jsonList) {
                String type = JsonUtils.getString(json, "roomType");
                Room room = null;
                if ("ICU".equalsIgnoreCase(type) || type.toUpperCase().contains("ICU")) {
                    room = ICU.fromJson(json);
                } else if ("CABIN".equalsIgnoreCase(type) || type.toUpperCase().contains("CABIN")) {
                    room = Cabin.fromJson(json);
                } else {
                    room = GeneralWard.fromJson(json);
                }

                if (room != null) {
                    rooms.add(room);
                }
            }
            return rooms;
        } catch (IOException e) {
            System.err.println("Error reading rooms file: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public synchronized void saveToFile(List<Room> rooms) {
        try {
            if (filePath.getParent() != null && !Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < rooms.size(); i++) {
                sb.append("  ").append(rooms.get(i).toJson());
                if (i < rooms.size() - 1) {
                    sb.append(",\n");
                } else {
                    sb.append("\n");
                }
            }
            sb.append("]");
            Files.writeString(filePath, sb.toString());
        } catch (IOException e) {
            System.err.println("Error writing rooms to file: " + e.getMessage());
        }
    }

    @Override
    public synchronized void save(Room entity) {
        if (entity == null) return;
        List<Room> rooms = loadFromFile();
        boolean replaced = false;
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).getRoomId().equalsIgnoreCase(entity.getRoomId())) {
                rooms.set(i, entity);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            rooms.add(entity);
        }
        saveToFile(rooms);
    }

    @Override
    public synchronized Optional<Room> findById(String id) {
        if (id == null) return Optional.empty();
        List<Room> rooms = loadFromFile();
        return rooms.stream()
                .filter(r -> r.getRoomId().equalsIgnoreCase(id.trim()))
                .findFirst();
    }

    @Override
    public synchronized List<Room> findAll() {
        return loadFromFile();
    }

    @Override
    public synchronized void update(Room entity) {
        if (entity == null) return;
        List<Room> rooms = loadFromFile();
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).getRoomId().equalsIgnoreCase(entity.getRoomId())) {
                rooms.set(i, entity);
                saveToFile(rooms);
                return;
            }
        }
        rooms.add(entity);
        saveToFile(rooms);
    }

    @Override
    public synchronized boolean deleteById(String id) {
        if (id == null) return false;
        List<Room> rooms = loadFromFile();
        boolean removed = rooms.removeIf(r -> r.getRoomId().equalsIgnoreCase(id.trim()));
        if (removed) {
            saveToFile(rooms);
        }
        return removed;
    }

    /**
     * Filters and returns all currently unoccupied rooms.
     *
     * @return list of available rooms
     */
    public synchronized List<Room> findAvailable() {
        return loadFromFile().stream()
                .filter(r -> !r.isOccupied())
                .collect(Collectors.toList());
    }

    /**
     * Filters and returns all currently occupied rooms.
     *
     * @return list of occupied rooms
     */
    public synchronized List<Room> findOccupied() {
        return loadFromFile().stream()
                .filter(Room::isOccupied)
                .collect(Collectors.toList());
    }
}
