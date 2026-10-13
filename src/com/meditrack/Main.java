package com.meditrack;

import com.meditrack.repository.*;
import com.meditrack.service.*;
import com.meditrack.controller.*;
import com.meditrack.server.HttpServerApp;
import com.meditrack.util.SeedDataLoader;

/**
 * Main — Entry point for MediTrack Hospital Management System
 *
 * Wires all layers together:
 *   Repository → Service → Controller → HttpServer
 */
public class Main {

    public static void main(String[] args) throws Exception {

        // ─────────────────────────────────────────────
        // ASCII Banner
        // ─────────────────────────────────────────────
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║  MediTrack — Hospital Management System      ║");
        System.out.println("║  Java OOP Project   |   Port: 8080           ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();

        // ─────────────────────────────────────────────
        // Repositories (File-based data layer)
        // ─────────────────────────────────────────────
        FilePatientRepository     patientRepo     = new FilePatientRepository();
        FileDoctorRepository      doctorRepo      = new FileDoctorRepository();
        FileRoomRepository        roomRepo        = new FileRoomRepository();
        FileAppointmentRepository appointmentRepo = new FileAppointmentRepository();
        FileBillingRepository     billingRepo     = new FileBillingRepository();

        // ─────────────────────────────────────────────
        // Seed demo data (only if files are empty)
        // ─────────────────────────────────────────────
        System.out.println("Loading data...");
        SeedDataLoader.seed(patientRepo, doctorRepo, roomRepo, appointmentRepo, billingRepo);

        // ─────────────────────────────────────────────
        // Services (Business logic layer)
        // ─────────────────────────────────────────────
        PatientService     patientService     = new PatientService(patientRepo);
        DoctorService      doctorService      = new DoctorService(doctorRepo);
        RoomService        roomService        = new RoomService(roomRepo);
        AppointmentService appointmentService = new AppointmentService(appointmentRepo, doctorRepo);
        BillingService     billingService     = new BillingService(billingRepo, roomRepo);

        // ─────────────────────────────────────────────
        // Controllers (HTTP handler layer)
        // ─────────────────────────────────────────────
        PatientHandler     patientHandler     = new PatientHandler(patientService);
        DoctorHandler      doctorHandler      = new DoctorHandler(doctorService);
        AppointmentHandler appointmentHandler = new AppointmentHandler(appointmentService);
        RoomHandler        roomHandler        = new RoomHandler(roomService);
        BillingHandler     billingHandler     = new BillingHandler(billingService);
        DashboardHandler   dashboardHandler   = new DashboardHandler(
                patientService, doctorService, appointmentService, roomService, billingService);

        // ─────────────────────────────────────────────
        // Start HTTP Server
        // ─────────────────────────────────────────────
        HttpServerApp server = new HttpServerApp(8080,
                patientHandler, doctorHandler, appointmentHandler,
                roomHandler, billingHandler, dashboardHandler);
        server.start();

        System.out.println();
        System.out.println("🌐 Open frontend/index.html in your browser");
        System.out.println("   (or open it via Live Server in VS Code)");
        System.out.println();
        System.out.println("Press Ctrl+C to stop the server.");
    }
}
