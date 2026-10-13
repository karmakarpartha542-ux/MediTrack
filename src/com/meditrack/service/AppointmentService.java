package com.meditrack.service;

import com.meditrack.model.appointment.Appointment;
import com.meditrack.model.appointment.AppointmentStatus;
import com.meditrack.model.doctor.Doctor;
import com.meditrack.repository.FileAppointmentRepository;
import com.meditrack.repository.FileDoctorRepository;
import com.meditrack.util.JsonUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service managing outpatient appointments, scheduling workflows,
 * and status transitions.
 */
public class AppointmentService {

    private final FileAppointmentRepository appointmentRepo;
    private final FileDoctorRepository doctorRepo;

    public AppointmentService(FileAppointmentRepository appointmentRepo, FileDoctorRepository doctorRepo) {
        this.appointmentRepo = appointmentRepo;
        this.doctorRepo = doctorRepo;
    }

    /**
     * Books a new outpatient consultation appointment.
     * Validates that the requested doctor exists in the system.
     *
     * @param patientId   patient identifier
     * @param patientName patient full name
     * @param doctorId    assigned doctor identifier
     * @param dateTime    scheduled date-time string
     * @param notes       consultation reason or medical notes
     * @return newly scheduled Appointment
     * @throws IllegalArgumentException if doctor does not exist
     */
    public Appointment bookAppointment(String patientId, String patientName,
                                       String doctorId, String dateTime, String notes) {
        Optional<Doctor> doctorOpt = doctorRepo.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            throw new IllegalArgumentException("Doctor with ID " + doctorId + " does not exist.");
        }
        Doctor doctor = doctorOpt.get();

        String appointmentId = JsonUtils.generateId("APT");
        Appointment appointment = new Appointment(
                appointmentId,
                patientId,
                patientName,
                doctorId,
                doctor.getName(),
                doctor.getSpecialization().getDisplayName(),
                dateTime,
                AppointmentStatus.SCHEDULED,
                notes
        );

        appointmentRepo.save(appointment);
        return appointment;
    }

    /**
     * Retrieves all appointments recorded in the system.
     *
     * @return list of appointments
     */
    public List<Appointment> getAllAppointments() {
        return appointmentRepo.findAll();
    }

    /**
     * Retrieves appointments scheduled for today's date.
     *
     * @return list of today's appointments
     */
    public List<Appointment> getTodayAppointments() {
        String today = LocalDate.now().toString();
        return appointmentRepo.findByDate(today);
    }

    /**
     * Looks up an appointment by unique identifier.
     *
     * @param id appointment ID
     * @return Optional containing Appointment if found
     */
    public Optional<Appointment> getAppointmentById(String id) {
        return appointmentRepo.findById(id);
    }

    /**
     * Cancels an appointment.
     *
     * @param id appointment identifier
     * @return true if appointment found and cancelled, false otherwise
     */
    public boolean cancelAppointment(String id) {
        Optional<Appointment> opt = appointmentRepo.findById(id);
        if (opt.isPresent()) {
            Appointment apt = opt.get();
            apt.cancel();
            appointmentRepo.update(apt);
            return true;
        }
        return false;
    }

    /**
     * Marks an appointment as completed.
     *
     * @param id appointment identifier
     * @return true if appointment found and completed, false otherwise
     */
    public boolean completeAppointment(String id) {
        Optional<Appointment> opt = appointmentRepo.findById(id);
        if (opt.isPresent()) {
            Appointment apt = opt.get();
            apt.complete();
            appointmentRepo.update(apt);
            return true;
        }
        return false;
    }

    /**
     * Retrieves all appointments booked by a specific patient.
     *
     * @param patientId patient identifier
     * @return list of matching appointments
     */
    public List<Appointment> getAppointmentsByPatient(String patientId) {
        return appointmentRepo.findByPatientId(patientId);
    }
}
