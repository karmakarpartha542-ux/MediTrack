package com.meditrack.controller;

import com.meditrack.service.*;
import com.meditrack.model.patient.Patient;
import com.meditrack.model.appointment.Appointment;
import com.meditrack.model.billing.Bill;
import com.meditrack.server.HttpUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

/**
 * REST handler for /api/dashboard
 * GET /api/dashboard → aggregated KPI data for the dashboard UI
 */
public class DashboardHandler implements HttpHandler {

    private final PatientService     patientService;
    private final DoctorService      doctorService;
    private final AppointmentService appointmentService;
    private final RoomService        roomService;
    private final BillingService     billingService;

    public DashboardHandler(PatientService patientService,
                            DoctorService doctorService,
                            AppointmentService appointmentService,
                            RoomService roomService,
                            BillingService billingService) {
        this.patientService     = patientService;
        this.doctorService      = doctorService;
        this.appointmentService = appointmentService;
        this.roomService        = roomService;
        this.billingService     = billingService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpUtils.addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            // Gather all data
            int totalPatients          = patientService.getAllPatients().size();
            int totalDoctors           = doctorService.getAllDoctors().size();
            List<Appointment> todayAppts = appointmentService.getTodayAppointments();
            int todayCount             = todayAppts.size();
            java.util.Map<String, Object> roomStats = roomService.getRoomStats();
            int occupiedRooms          = ((Number) roomStats.getOrDefault("occupiedCount", 0)).intValue();
            int totalRooms             = ((Number) roomStats.getOrDefault("totalRooms", 0)).intValue();
            List<Bill> pendingBills    = billingService.getPendingBills();
            double pendingAmount       = billingService.getTotalPendingAmount();

            // Recent admissions — last 4 admitted patients
            List<Patient> allPatients    = patientService.getAllPatients();
            List<Patient> admittedList   = new java.util.ArrayList<>();
            for (Patient p : allPatients) {
                if (p.isAdmitted()) admittedList.add(p);
            }
            // Take last 4 (most recently added)
            int fromIdx = Math.max(0, admittedList.size() - 4);
            List<Patient> recent = admittedList.subList(fromIdx, admittedList.size());

            // Build JSON manually
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"totalPatients\":").append(totalPatients).append(",");
            sb.append("\"totalDoctors\":").append(totalDoctors).append(",");
            sb.append("\"todayAppointments\":").append(todayCount).append(",");
            sb.append("\"occupiedRooms\":").append(occupiedRooms).append(",");
            sb.append("\"totalRooms\":").append(totalRooms).append(",");
            sb.append("\"pendingBillsAmount\":").append(pendingAmount).append(",");

            // recentAdmissions array
            sb.append("\"recentAdmissions\":[");
            for (int i = 0; i < recent.size(); i++) {
                sb.append(recent.get(i).toJson());
                if (i < recent.size() - 1) sb.append(",");
            }
            sb.append("],");

            // pendingBills array (max 5)
            sb.append("\"pendingBills\":[");
            int billLimit = Math.min(5, pendingBills.size());
            for (int i = 0; i < billLimit; i++) {
                sb.append(pendingBills.get(i).toJson());
                if (i < billLimit - 1) sb.append(",");
            }
            sb.append("],");

            // todayAppointmentsList array
            sb.append("\"todayAppointmentsList\":[");
            for (int i = 0; i < todayAppts.size(); i++) {
                sb.append(todayAppts.get(i).toJson());
                if (i < todayAppts.size() - 1) sb.append(",");
            }
            sb.append("]");

            sb.append("}");

            HttpUtils.sendJson(exchange, 200, HttpUtils.success(sb.toString()));
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Dashboard error: " + e.getMessage());
        }
    }
}
