package com.meditrack.server;

import com.meditrack.controller.*;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * HttpServerApp — Starts the Java HttpServer on port 8080
 * and registers all REST API context paths.
 */
public class HttpServerApp {

    private final int               port;
    private final PatientHandler    patientHandler;
    private final DoctorHandler     doctorHandler;
    private final AppointmentHandler appointmentHandler;
    private final RoomHandler       roomHandler;
    private final BillingHandler    billingHandler;
    private final DashboardHandler  dashboardHandler;

    public HttpServerApp(int port,
                         PatientHandler patientHandler,
                         DoctorHandler doctorHandler,
                         AppointmentHandler appointmentHandler,
                         RoomHandler roomHandler,
                         BillingHandler billingHandler,
                         DashboardHandler dashboardHandler) {
        this.port               = port;
        this.patientHandler     = patientHandler;
        this.doctorHandler      = doctorHandler;
        this.appointmentHandler = appointmentHandler;
        this.roomHandler        = roomHandler;
        this.billingHandler     = billingHandler;
        this.dashboardHandler   = dashboardHandler;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Register all REST endpoints
        server.createContext("/api/patients",     patientHandler);
        server.createContext("/api/doctors",      doctorHandler);
        server.createContext("/api/appointments", appointmentHandler);
        server.createContext("/api/rooms",        roomHandler);
        server.createContext("/api/billing",      billingHandler);
        server.createContext("/api/dashboard",    dashboardHandler);

        // Thread pool for concurrent request handling
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("✅ MediTrack server running at http://localhost:" + port);
        System.out.println("   Endpoints registered:");
        System.out.println("   → GET/POST/PUT  /api/patients");
        System.out.println("   → GET/POST/PUT  /api/doctors");
        System.out.println("   → GET/POST/PUT  /api/appointments");
        System.out.println("   → GET/POST      /api/rooms");
        System.out.println("   → GET           /api/rooms/stats");
        System.out.println("   → GET/POST/PUT  /api/billing");
        System.out.println("   → POST          /api/billing/generate");
        System.out.println("   → GET           /api/dashboard");
    }
}
