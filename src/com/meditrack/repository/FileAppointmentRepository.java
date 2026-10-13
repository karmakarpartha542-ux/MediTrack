package com.meditrack.repository;

import com.meditrack.model.appointment.Appointment;
import com.meditrack.model.appointment.AppointmentStatus;
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
 * File-backed persistence implementation for {@link Appointment} records.
 */
public class FileAppointmentRepository implements CrudRepository<Appointment, String> {

    private final Path filePath;

    public FileAppointmentRepository() {
        this("data/appointments.json");
    }

    public FileAppointmentRepository(String path) {
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
            System.err.println("Could not initialize appointments file: " + e.getMessage());
        }
    }

    public synchronized List<Appointment> loadFromFile() {
        try {
            if (!Files.exists(filePath)) {
                initFile();
                return new ArrayList<>();
            }
            String content = Files.readString(filePath);
            List<String> jsonList = JsonUtils.parseJsonArray(content);
            List<Appointment> appointments = new ArrayList<>();
            for (String json : jsonList) {
                Appointment a = Appointment.fromJson(json);
                if (a != null) {
                    appointments.add(a);
                }
            }
            return appointments;
        } catch (IOException e) {
            System.err.println("Error reading appointments file: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public synchronized void saveToFile(List<Appointment> appointments) {
        try {
            if (filePath.getParent() != null && !Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < appointments.size(); i++) {
                sb.append("  ").append(appointments.get(i).toJson());
                if (i < appointments.size() - 1) {
                    sb.append(",\n");
                } else {
                    sb.append("\n");
                }
            }
            sb.append("]");
            Files.writeString(filePath, sb.toString());
        } catch (IOException e) {
            System.err.println("Error writing appointments to file: " + e.getMessage());
        }
    }

    @Override
    public synchronized void save(Appointment entity) {
        if (entity == null) return;
        List<Appointment> appointments = loadFromFile();
        boolean replaced = false;
        for (int i = 0; i < appointments.size(); i++) {
            if (appointments.get(i).getAppointmentId().equalsIgnoreCase(entity.getAppointmentId())) {
                appointments.set(i, entity);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            appointments.add(entity);
        }
        saveToFile(appointments);
    }

    @Override
    public synchronized Optional<Appointment> findById(String id) {
        if (id == null) return Optional.empty();
        List<Appointment> appointments = loadFromFile();
        return appointments.stream()
                .filter(a -> a.getAppointmentId().equalsIgnoreCase(id.trim()))
                .findFirst();
    }

    @Override
    public synchronized List<Appointment> findAll() {
        return loadFromFile();
    }

    @Override
    public synchronized void update(Appointment entity) {
        if (entity == null) return;
        List<Appointment> appointments = loadFromFile();
        for (int i = 0; i < appointments.size(); i++) {
            if (appointments.get(i).getAppointmentId().equalsIgnoreCase(entity.getAppointmentId())) {
                appointments.set(i, entity);
                saveToFile(appointments);
                return;
            }
        }
        appointments.add(entity);
        saveToFile(appointments);
    }

    @Override
    public synchronized boolean deleteById(String id) {
        if (id == null) return false;
        List<Appointment> appointments = loadFromFile();
        boolean removed = appointments.removeIf(a -> a.getAppointmentId().equalsIgnoreCase(id.trim()));
        if (removed) {
            saveToFile(appointments);
        }
        return removed;
    }

    public synchronized List<Appointment> findByPatientId(String patientId) {
        if (patientId == null) return new ArrayList<>();
        return loadFromFile().stream()
                .filter(a -> a.getPatientId().equalsIgnoreCase(patientId.trim()))
                .collect(Collectors.toList());
    }

    public synchronized List<Appointment> findByDoctorId(String doctorId) {
        if (doctorId == null) return new ArrayList<>();
        return loadFromFile().stream()
                .filter(a -> a.getDoctorId().equalsIgnoreCase(doctorId.trim()))
                .collect(Collectors.toList());
    }

    public synchronized List<Appointment> findByStatus(AppointmentStatus status) {
        if (status == null) return new ArrayList<>();
        return loadFromFile().stream()
                .filter(a -> a.getStatus() == status)
                .collect(Collectors.toList());
    }

    public synchronized List<Appointment> findByDate(String date) {
        if (date == null) return new ArrayList<>();
        String cleanDate = date.trim();
        return loadFromFile().stream()
                .filter(a -> a.getDateTime() != null && a.getDateTime().startsWith(cleanDate))
                .collect(Collectors.toList());
    }
}
