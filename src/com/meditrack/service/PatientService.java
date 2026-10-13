package com.meditrack.service;

import com.meditrack.model.patient.Patient;
import com.meditrack.repository.FilePatientRepository;
import com.meditrack.util.JsonUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Business service layer managing patient lifecycles, admissions, discharges,
 * and profile lookups.
 */
public class PatientService {

    private final FilePatientRepository patientRepo;

    public PatientService(FilePatientRepository patientRepo) {
        this.patientRepo = patientRepo;
    }

    /**
     * Registers a new patient in the hospital directory.
     *
     * @param name           full name
     * @param phone          phone number
     * @param email          email address
     * @param age            patient age
     * @param bloodGroup     blood group string (e.g. "O+", "A-")
     * @param medicalHistory initial symptoms or medical history
     * @return newly created and persisted Patient
     */
    public Patient registerPatient(String name, String phone, String email, int age,
                                   String bloodGroup, String medicalHistory) {
        String id = JsonUtils.generateId("PAT");
        Patient patient = new Patient(id, name, phone, email, age, bloodGroup, medicalHistory);
        patientRepo.save(patient);
        return patient;
    }

    /**
     * Retrieves all registered patients.
     *
     * @return list of patients
     */
    public List<Patient> getAllPatients() {
        return patientRepo.findAll();
    }

    /**
     * Looks up a single patient by ID.
     *
     * @param id patient ID
     * @return Optional containing Patient if found
     */
    public Optional<Patient> getPatientById(String id) {
        return patientRepo.findById(id);
    }

    /**
     * Searches patients by name, telephone, or ID (case-insensitive substring match).
     *
     * @param query search query
     * @return filtered list of matching patients
     */
    public List<Patient> searchPatients(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllPatients();
        }
        String q = query.trim().toLowerCase();
        return patientRepo.findAll().stream()
                .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(q))
                        || (p.getPhone() != null && p.getPhone().toLowerCase().contains(q))
                        || (p.getId() != null && p.getId().toLowerCase().contains(q))
                        || (p.getBloodGroup() != null && p.getBloodGroup().toLowerCase().contains(q)))
                .collect(Collectors.toList());
    }

    /**
     * Updates an existing patient record.
     *
     * @param patient patient with updated fields
     */
    public void updatePatient(Patient patient) {
        patientRepo.update(patient);
    }

    /**
     * Marks a patient as admitted and links them to an assigned hospital room.
     *
     * @param patientId patient identifier
     * @param roomId    assigned room identifier
     * @return true if patient was found and updated, false otherwise
     */
    public boolean admitPatient(String patientId, String roomId) {
        Optional<Patient> opt = patientRepo.findById(patientId);
        if (opt.isPresent()) {
            Patient p = opt.get();
            p.setAdmitted(true);
            p.setAssignedRoomId(roomId);
            if (p.getAdmissionDate() == null || p.getAdmissionDate().isEmpty()) {
                p.setAdmissionDate(LocalDate.now().toString());
            }
            patientRepo.update(p);
            return true;
        }
        return false;
    }

    /**
     * Discharges an admitted patient, clearing room assignment.
     *
     * @param patientId patient identifier
     * @return true if successfully discharged, false otherwise
     */
    public boolean dischargePatient(String patientId) {
        Optional<Patient> opt = patientRepo.findById(patientId);
        if (opt.isPresent()) {
            Patient p = opt.get();
            p.setAdmitted(false);
            p.setAssignedRoomId(null);
            patientRepo.update(p);
            return true;
        }
        return false;
    }
}
