package com.meditrack.repository;

import com.meditrack.model.billing.Bill;
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
 * File-backed persistence implementation for {@link Bill} invoices.
 */
public class FileBillingRepository implements CrudRepository<Bill, String> {

    private final Path filePath;

    public FileBillingRepository() {
        this("data/bills.json");
    }

    public FileBillingRepository(String path) {
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
            System.err.println("Could not initialize bills file: " + e.getMessage());
        }
    }

    public synchronized List<Bill> loadFromFile() {
        try {
            if (!Files.exists(filePath)) {
                initFile();
                return new ArrayList<>();
            }
            String content = Files.readString(filePath);
            List<String> jsonList = JsonUtils.parseJsonArray(content);
            List<Bill> bills = new ArrayList<>();
            for (String json : jsonList) {
                Bill b = Bill.fromJson(json);
                if (b != null) {
                    bills.add(b);
                }
            }
            return bills;
        } catch (IOException e) {
            System.err.println("Error reading bills file: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public synchronized void saveToFile(List<Bill> bills) {
        try {
            if (filePath.getParent() != null && !Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < bills.size(); i++) {
                sb.append("  ").append(bills.get(i).toJson());
                if (i < bills.size() - 1) {
                    sb.append(",\n");
                } else {
                    sb.append("\n");
                }
            }
            sb.append("]");
            Files.writeString(filePath, sb.toString());
        } catch (IOException e) {
            System.err.println("Error writing bills to file: " + e.getMessage());
        }
    }

    @Override
    public synchronized void save(Bill entity) {
        if (entity == null) return;
        List<Bill> bills = loadFromFile();
        boolean replaced = false;
        for (int i = 0; i < bills.size(); i++) {
            if (bills.get(i).getBillId().equalsIgnoreCase(entity.getBillId())) {
                bills.set(i, entity);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            bills.add(entity);
        }
        saveToFile(bills);
    }

    @Override
    public synchronized Optional<Bill> findById(String id) {
        if (id == null) return Optional.empty();
        List<Bill> bills = loadFromFile();
        return bills.stream()
                .filter(b -> b.getBillId().equalsIgnoreCase(id.trim()))
                .findFirst();
    }

    @Override
    public synchronized List<Bill> findAll() {
        return loadFromFile();
    }

    @Override
    public synchronized void update(Bill entity) {
        if (entity == null) return;
        List<Bill> bills = loadFromFile();
        for (int i = 0; i < bills.size(); i++) {
            if (bills.get(i).getBillId().equalsIgnoreCase(entity.getBillId())) {
                bills.set(i, entity);
                saveToFile(bills);
                return;
            }
        }
        bills.add(entity);
        saveToFile(bills);
    }

    @Override
    public synchronized boolean deleteById(String id) {
        if (id == null) return false;
        List<Bill> bills = loadFromFile();
        boolean removed = bills.removeIf(b -> b.getBillId().equalsIgnoreCase(id.trim()));
        if (removed) {
            saveToFile(bills);
        }
        return removed;
    }

    /**
     * Retrieves all pending (unpaid) bills.
     *
     * @return list of unpaid invoices
     */
    public synchronized List<Bill> findUnpaid() {
        return loadFromFile().stream()
                .filter(b -> !b.isPaid())
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all invoices associated with a specific patient.
     *
     * @param patientId patient identifier
     * @return list of patient invoices
     */
    public synchronized List<Bill> findByPatientId(String patientId) {
        if (patientId == null) return new ArrayList<>();
        return loadFromFile().stream()
                .filter(b -> b.getPatientId().equalsIgnoreCase(patientId.trim()))
                .collect(Collectors.toList());
    }
}
