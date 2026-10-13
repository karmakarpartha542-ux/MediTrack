package com.meditrack.repository;

import com.meditrack.model.doctor.Doctor;
import com.meditrack.util.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * File-backed persistence implementation for {@link Doctor} records.
 */
public class FileDoctorRepository implements CrudRepository<Doctor, String> {

    private final Path filePath;

    public FileDoctorRepository() {
        this("data/doctors.json");
    }

    public FileDoctorRepository(String path) {
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
            System.err.println("Could not initialize doctor file: " + e.getMessage());
        }
    }

    public synchronized List<Doctor> loadFromFile() {
        try {
            if (!Files.exists(filePath)) {
                initFile();
                return new ArrayList<>();
            }
            String content = Files.readString(filePath);
            List<String> jsonList = JsonUtils.parseJsonArray(content);
            List<Doctor> doctors = new ArrayList<>();
            for (String json : jsonList) {
                Doctor d = Doctor.fromJson(json);
                if (d != null) {
                    doctors.add(d);
                }
            }
            return doctors;
        } catch (IOException e) {
            System.err.println("Error reading doctors file: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public synchronized void saveToFile(List<Doctor> doctors) {
        try {
            if (filePath.getParent() != null && !Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < doctors.size(); i++) {
                sb.append("  ").append(doctors.get(i).toJson());
                if (i < doctors.size() - 1) {
                    sb.append(",\n");
                } else {
                    sb.append("\n");
                }
            }
            sb.append("]");
            Files.writeString(filePath, sb.toString());
        } catch (IOException e) {
            System.err.println("Error writing doctors to file: " + e.getMessage());
        }
    }

    @Override
    public synchronized void save(Doctor entity) {
        if (entity == null) return;
        List<Doctor> doctors = loadFromFile();
        boolean replaced = false;
        for (int i = 0; i < doctors.size(); i++) {
            if (doctors.get(i).getId().equalsIgnoreCase(entity.getId())) {
                doctors.set(i, entity);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            doctors.add(entity);
        }
        saveToFile(doctors);
    }

    @Override
    public synchronized Optional<Doctor> findById(String id) {
        if (id == null) return Optional.empty();
        List<Doctor> doctors = loadFromFile();
        return doctors.stream()
                .filter(d -> d.getId().equalsIgnoreCase(id.trim()))
                .findFirst();
    }

    @Override
    public synchronized List<Doctor> findAll() {
        return loadFromFile();
    }

    @Override
    public synchronized void update(Doctor entity) {
        if (entity == null) return;
        List<Doctor> doctors = loadFromFile();
        for (int i = 0; i < doctors.size(); i++) {
            if (doctors.get(i).getId().equalsIgnoreCase(entity.getId())) {
                doctors.set(i, entity);
                saveToFile(doctors);
                return;
            }
        }
        doctors.add(entity);
        saveToFile(doctors);
    }

    @Override
    public synchronized boolean deleteById(String id) {
        if (id == null) return false;
        List<Doctor> doctors = loadFromFile();
        boolean removed = doctors.removeIf(d -> d.getId().equalsIgnoreCase(id.trim()));
        if (removed) {
            saveToFile(doctors);
        }
        return removed;
    }
}
