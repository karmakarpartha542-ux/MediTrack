package com.meditrack.service;

import com.meditrack.model.doctor.Doctor;
import com.meditrack.model.doctor.Specialization;
import com.meditrack.repository.FileDoctorRepository;
import com.meditrack.util.JsonUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service managing doctor staff records, consultation fees, specialties,
 * and roster availability.
 */
public class DoctorService {

    private final FileDoctorRepository doctorRepo;

    public DoctorService(FileDoctorRepository doctorRepo) {
        this.doctorRepo = doctorRepo;
    }

    /**
     * Registers a new doctor into the hospital system.
     *
     * @param name           doctor's full name
     * @param phone          phone contact
     * @param email          email address
     * @param age            doctor age
     * @param specialization medical specialty
     * @param fee            consultation fee
     * @param department     department name
     * @return newly created Doctor
     */
    public Doctor addDoctor(String name, String phone, String email, int age,
                            Specialization specialization, double fee, String department) {
        String id = JsonUtils.generateId("DOC");
        Doctor doctor = new Doctor(id, name, phone, email, age, specialization, fee, true, department);
        doctorRepo.save(doctor);
        return doctor;
    }

    /**
     * Retrieves all registered doctors.
     *
     * @return list of doctors
     */
    public List<Doctor> getAllDoctors() {
        return doctorRepo.findAll();
    }

    /**
     * Looks up a doctor by unique ID.
     *
     * @param id doctor ID
     * @return Optional containing Doctor if found
     */
    public Optional<Doctor> getDoctorById(String id) {
        return doctorRepo.findById(id);
    }

    /**
     * Retrieves only doctors who are currently marked as available for consultation.
     *
     * @return list of active/available doctors
     */
    public List<Doctor> getAvailableDoctors() {
        return doctorRepo.findAll().stream()
                .filter(Doctor::isAvailable)
                .collect(Collectors.toList());
    }

    /**
     * Searches doctors by name, department, or specialization (case-insensitive substring match).
     *
     * @param query search query
     * @return list of matching doctors
     */
    public List<Doctor> searchDoctors(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllDoctors();
        }
        String q = query.trim().toLowerCase();
        return doctorRepo.findAll().stream()
                .filter(d -> (d.getName() != null && d.getName().toLowerCase().contains(q))
                        || (d.getDepartment() != null && d.getDepartment().toLowerCase().contains(q))
                        || (d.getSpecialization() != null && d.getSpecialization().getDisplayName().toLowerCase().contains(q)))
                .collect(Collectors.toList());
    }

    /**
     * Toggles the duty availability flag for a specific doctor.
     *
     * @param doctorId doctor identifier
     * @return updated Doctor if found, or null
     */
    public Doctor toggleAvailability(String doctorId) {
        Optional<Doctor> opt = doctorRepo.findById(doctorId);
        if (opt.isPresent()) {
            Doctor doctor = opt.get();
            doctor.toggleAvailability();
            doctorRepo.update(doctor);
            return doctor;
        }
        return null;
    }

    /**
     * Updates an existing doctor's profile.
     *
     * @param doctor Doctor instance with updated fields
     */
    public void updateDoctor(Doctor doctor) {
        doctorRepo.update(doctor);
    }
}
