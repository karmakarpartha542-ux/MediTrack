package com.meditrack.util;

import com.meditrack.model.patient.Patient;
import com.meditrack.model.doctor.Doctor;
import com.meditrack.model.doctor.Specialization;
import com.meditrack.model.room.*;
import com.meditrack.model.appointment.Appointment;
import com.meditrack.model.appointment.AppointmentStatus;
import com.meditrack.model.billing.Bill;
import com.meditrack.model.billing.BillItem;
import com.meditrack.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SeedDataLoader — Seeds demo data into empty JSON files on first startup.
 * Checks if each file is empty before seeding, so it won't overwrite real data.
 */
public class SeedDataLoader {

    public static void seed(FilePatientRepository patientRepo,
                            FileDoctorRepository doctorRepo,
                            FileRoomRepository roomRepo,
                            FileAppointmentRepository apptRepo,
                            FileBillingRepository billingRepo) {

        seedDoctors(doctorRepo);
        seedPatients(patientRepo);
        seedRooms(roomRepo, patientRepo);
        seedAppointments(apptRepo, patientRepo, doctorRepo);
        seedBills(billingRepo);
    }

    // -------------------------
    // DOCTORS
    // -------------------------
    private static void seedDoctors(FileDoctorRepository repo) {
        if (!repo.findAll().isEmpty()) return;

        repo.save(makeDoctor("DOC-001", "Dr. Farhan Ahmed",   "+880-1711-001",  "farhan@meditrack.io",  45, Specialization.CARDIOLOGY,        800.0,  "Cardiology Unit"));
        repo.save(makeDoctor("DOC-002", "Dr. Elena Thornton", "+1-555-2002",    "elena@meditrack.io",   38, Specialization.NEUROLOGY,           1200.0, "Neurology Dept"));
        repo.save(makeDoctor("DOC-003", "Dr. Maria Santos",   "+63-917-3003",   "maria@meditrack.io",   50, Specialization.ORTHOPEDICS,         900.0,  "Orthopedics Dept"));
        repo.save(makeDoctor("DOC-004", "Dr. James Wilson",   "+44-7700-4004",  "james@meditrack.io",   55, Specialization.GENERAL_MEDICINE,    500.0,  "General Medicine"));
        repo.save(makeDoctor("DOC-005", "Dr. Priya Sharma",   "+91-9876-5005",  "priya@meditrack.io",   35, Specialization.PEDIATRICS,          600.0,  "Pediatrics Ward"));

        System.out.println("✔ Seeded 5 doctors.");
    }

    private static Doctor makeDoctor(String id, String name, String phone, String email,
                                     int age, Specialization spec, double fee, String dept) {
        Doctor d = new Doctor(id, name, phone, email, age, spec, fee, dept);
        d.setAvailable(true);
        return d;
    }

    // -------------------------
    // PATIENTS
    // -------------------------
    private static void seedPatients(FilePatientRepository repo) {
        if (!repo.findAll().isEmpty()) return;

        String today = LocalDate.now().toString();

        Patient p1 = new Patient("PAT-1001", "Tanvir Hassan",  "+880-1888-101", "tanvir@mail.com",  32, "B+",  "Acute Appendicitis");
        Patient p2 = new Patient("PAT-1002", "Clara Chan",     "+1-650-2020",   "clara@mail.com",   27, "A-",  "Post-Op Recovery");
        Patient p3 = new Patient("PAT-1003", "Samuel Okafor",  "+234-803-303",  "samuel@mail.com",  48, "O+",  "Pulmonary Condition");
        Patient p4 = new Patient("PAT-1004", "Nusrat Jahan",   "+880-1744-404", "nusrat@mail.com",  24, "AB+", "General Checkup");

        p1.setAdmissionDate(today + "T08:30:00");
        p2.setAdmissionDate(today + "T09:00:00");
        p3.setAdmissionDate(today + "T07:45:00");
        p4.setAdmissionDate(today + "T10:15:00");

        // Mark some as admitted
        p1.admit("GW-001");
        p2.admit("CAB-001");
        p3.admit("ICU-001");

        repo.save(p1);
        repo.save(p2);
        repo.save(p3);
        repo.save(p4);

        System.out.println("✔ Seeded 4 patients.");
    }

    // -------------------------
    // ROOMS
    // -------------------------
    private static void seedRooms(FileRoomRepository repo, FilePatientRepository patientRepo) {
        if (!repo.findAll().isEmpty()) return;

        // General Wards
        GeneralWard gw1 = new GeneralWard("GW-001", "101-A", 1, "Bed-1");
        GeneralWard gw2 = new GeneralWard("GW-002", "101-B", 1, "Bed-2");
        GeneralWard gw3 = new GeneralWard("GW-003", "102-A", 1, "Bed-3");

        // Cabins
        Cabin cab1 = new Cabin("CAB-001", "201",  2, true);   // with AC
        Cabin cab2 = new Cabin("CAB-002", "202",  2, false);  // no AC
        Cabin cab3 = new Cabin("CAB-003", "203",  2, true);   // with AC

        // ICU
        ICU icu1 = new ICU("ICU-001", "301", 3, true);   // with ventilator
        ICU icu2 = new ICU("ICU-002", "302", 3, false);  // no ventilator

        // Occupy matching patient assignments
        gw1.allocate("PAT-1001");
        cab1.allocate("PAT-1002");
        icu1.allocate("PAT-1003");

        repo.save(gw1);
        repo.save(gw2);
        repo.save(gw3);
        repo.save(cab1);
        repo.save(cab2);
        repo.save(cab3);
        repo.save(icu1);
        repo.save(icu2);

        System.out.println("✔ Seeded 8 rooms (3 GW, 3 Cabin, 2 ICU).");
    }

    // -------------------------
    // APPOINTMENTS
    // -------------------------
    private static void seedAppointments(FileAppointmentRepository repo,
                                         FilePatientRepository patientRepo,
                                         FileDoctorRepository doctorRepo) {
        if (!repo.findAll().isEmpty()) return;

        String today = LocalDate.now().toString();

        Appointment a1 = new Appointment("APT-001", "PAT-1001", "Tanvir Hassan",
                "DOC-001", "Dr. Farhan Ahmed",   "Cardiology",       today + " 09:00", AppointmentStatus.SCHEDULED,  "Follow-up cardio check");
        Appointment a2 = new Appointment("APT-002", "PAT-1002", "Clara Chan",
                "DOC-003", "Dr. Maria Santos",   "Orthopedics",      today + " 10:30", AppointmentStatus.COMPLETED,  "Post-op assessment");
        Appointment a3 = new Appointment("APT-003", "PAT-1003", "Samuel Okafor",
                "DOC-002", "Dr. Elena Thornton", "Neurology",        today + " 11:00", AppointmentStatus.SCHEDULED,  "Pulmonary neuro consult");
        Appointment a4 = new Appointment("APT-004", "PAT-1004", "Nusrat Jahan",
                "DOC-005", "Dr. Priya Sharma",   "Pediatrics",       today + " 14:00", AppointmentStatus.COMPLETED,  "General wellness visit");

        repo.save(a1);
        repo.save(a2);
        repo.save(a3);
        repo.save(a4);

        System.out.println("✔ Seeded 4 appointments for today.");
    }

    // -------------------------
    // BILLS
    // -------------------------
    private static void seedBills(FileBillingRepository repo) {
        if (!repo.findAll().isEmpty()) return;

        String today = LocalDate.now().toString();

        Bill b1 = new Bill("BILL-001", "PAT-1004", "Nusrat Jahan", today, "General Checkup");
        b1.addItem(new BillItem("Consultation - Dr. Priya Sharma (Pediatrics)", 600.0));

        Bill b2clean = new Bill("BILL-002", "PAT-EXT-01", "Marcus Brody", today, "Outpatient neuro visit");
        b2clean.addItem(new BillItem("Consultation - Dr. Elena Thornton (Neurology)", 800.0));
        b2clean.addItem(new BillItem("Diagnostic Imaging", 320.0));

        Bill b3 = new Bill("BILL-003", "PAT-EXT-02", "Eileen Zhou", today, "Cardiology screening");
        b3.addItem(new BillItem("Consultation - Dr. Farhan Ahmed (Cardiology)", 800.0));
        b3.addItem(new BillItem("ECG & Cardiology Tests", 90.0));

        repo.save(b1);
        repo.save(b2clean);
        repo.save(b3);

        System.out.println("✔ Seeded 3 pending bills.");
    }
}
