package com.meditrack.repository;

import com.meditrack.model.patient.Patient;
import com.meditrack.util.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * File-backed persistence implementation for {@link Patient} records.
 * Serializes and deserializes patient lists to/from JSON storage.
 */
public class FilePatientRepository implements CrudRepository<Patient, String> {

    private final Path filePath;

    public FilePatientRepository() {
        this("data/patients.json");
    }

    public FilePatientRepository(String path) {
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
            System.err.println("Could not initialize patient file: " + e.getMessage());
        }
    }

    /**
     * Reads and parses all patients from the JSON storage file.
     *
     * @return list of loaded patients
     */
    public synchronized List<Patient> loadFromFile() {
        try {
            if (!Files.exists(filePath)) {
                initFile();
                return new ArrayList<>();
            }
            String content = Files.readString(filePath);
            List<String> jsonList = JsonUtils.parseJsonArray(content);
            List<Patient> patients = new ArrayList<>();
            for (String json : jsonList) {
                Patient p = Patient.fromJson(json);
                if (p != null) {
                    patients.add(p);
                }
            }
            return patients;
        } catch (IOException e) {
            System.err.println("Error reading patients file: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Writes the current list of patients to the JSON storage file.
     *
     * @param patients list of patients to persist
     */
    public synchronized void saveToFile(List<Patient> patients) {
        try {
            if (filePath.getParent() != null && !Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < patients.size(); i++) {
                sb.append("  ").append(patients.get(i).toJson());
                if (i < patients.size() - 1) {
                    sb.append(",\n");
                } else {
                    sb.append("\n");
                }
            }
            sb.append("]");
            Files.writeString(filePath, sb.toString());
        } catch (IOException e) {
            System.err.println("Error writing patients to file: " + e.getMessage());
        }
    }

    @Override
    public synchronized void save(Patient entity) {
        if (entity == null) return;
        List<Patient> patients = loadFromFile();
        boolean replaced = false;
        for (int i = 0; i < patients.size(); i++) {
            if (patients.get(i).getId().equalsIgnoreCase(entity.getId())) {
                patients.set(i, entity);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            patients.add(entity);
        }
        saveToFile(patients);
    }

    @Override
    public synchronized Optional<Patient> findById(String id) {
        if (id == null) return Optional.empty();
        List<Patient> patients = loadFromFile();
        return patients.stream()
                .filter(p -> p.getId().equalsIgnoreCase(id.trim()))
                .findFirst();
    }

    @Override
    public synchronized List<Patient> findAll() {
        return loadFromFile();
    }

    @Override
    public synchronized void update(Patient entity) {
        if (entity == null) return;
        List<Patient> patients = loadFromFile();
        for (int i = 0; i < patients.size(); i++) {
            if (patients.get(i).getId().equalsIgnoreCase(entity.getId())) {
                patients.set(i, entity);
                saveToFile(patients);
                return;
            }
        }
        // If not found, persist as new
        patients.add(entity);
        saveToFile(patients);
    }

    @Override
    public synchronized boolean deleteById(String id) {
        if (id == null) return false;
        List<Patient> patients = loadFromFile();
        boolean removed = patients.removeIf(p -> p.getId().equalsIgnoreCase(id.trim()));
        if (removed) {
            saveToFile(patients);
        }
        return removed;
    }
}
