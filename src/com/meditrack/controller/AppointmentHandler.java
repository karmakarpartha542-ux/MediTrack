package com.meditrack.controller;

import com.meditrack.service.AppointmentService;
import com.meditrack.model.appointment.Appointment;
import com.meditrack.server.HttpUtils;
import com.meditrack.util.JsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * REST handler for /api/appointments
 * GET  /api/appointments                        → all
 * GET  /api/appointments?today=true             → today's
 * GET  /api/appointments?patientId=...          → by patient
 * GET  /api/appointments?id=...                 → by id
 * POST /api/appointments                        → book
 * PUT  /api/appointments?id=...&action=cancel   → cancel
 * PUT  /api/appointments?id=...&action=complete → complete
 */
public class AppointmentHandler implements HttpHandler {

    private final AppointmentService appointmentService;

    public AppointmentHandler(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        HttpUtils.addCorsHeaders(exchange);

        try {
            switch (method) {
                case "OPTIONS": exchange.sendResponseHeaders(204, -1); break;
                case "GET":     handleGet(exchange); break;
                case "POST":    handlePost(exchange); break;
                case "PUT":     handlePut(exchange); break;
                default:        HttpUtils.sendError(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal error: " + e.getMessage());
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        String today     = HttpUtils.getQueryParam(exchange, "today");
        String patientId = HttpUtils.getQueryParam(exchange, "patientId");
        String id        = HttpUtils.getQueryParam(exchange, "id");

        if (!id.isEmpty()) {
            Optional<Appointment> a = appointmentService.getAppointmentById(id);
            if (a.isPresent()) {
                HttpUtils.sendJson(exchange, 200, HttpUtils.success(a.get().toJson()));
            } else {
                HttpUtils.sendError(exchange, 404, "Appointment not found");
            }
        } else if ("true".equalsIgnoreCase(today)) {
            List<Appointment> list = appointmentService.getTodayAppointments();
            HttpUtils.sendJson(exchange, 200, HttpUtils.successArray(listToJsonArray(list)));
        } else if (!patientId.isEmpty()) {
            List<Appointment> list = appointmentService.getAppointmentsByPatient(patientId);
            HttpUtils.sendJson(exchange, 200, HttpUtils.successArray(listToJsonArray(list)));
        } else {
            List<Appointment> list = appointmentService.getAllAppointments();
            HttpUtils.sendJson(exchange, 200, HttpUtils.successArray(listToJsonArray(list)));
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body       = HttpUtils.readBody(exchange);
        String patientId  = JsonUtils.getString(body, "patientId");
        String patientName= JsonUtils.getString(body, "patientName");
        String doctorId   = JsonUtils.getString(body, "doctorId");
        String dateTime   = JsonUtils.getString(body, "dateTime");
        String notes      = JsonUtils.getString(body, "notes");

        if (patientId.isEmpty() || doctorId.isEmpty()) {
            HttpUtils.sendError(exchange, 400, "patientId and doctorId are required");
            return;
        }

        try {
            Appointment a = appointmentService.bookAppointment(
                    patientId, patientName, doctorId, dateTime, notes);
            HttpUtils.sendJson(exchange, 201, HttpUtils.success(a.toJson()));
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        }
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        String id     = HttpUtils.getQueryParam(exchange, "id");
        String action = HttpUtils.getQueryParam(exchange, "action");

        if (id.isEmpty()) { HttpUtils.sendError(exchange, 400, "id is required"); return; }

        // First fetch the appointment so we can return it
        Optional<Appointment> opt = appointmentService.getAppointmentById(id);
        if (opt.isEmpty()) { HttpUtils.sendError(exchange, 404, "Appointment not found"); return; }

        boolean done;
        if ("cancel".equalsIgnoreCase(action)) {
            done = appointmentService.cancelAppointment(id);
        } else if ("complete".equalsIgnoreCase(action)) {
            done = appointmentService.completeAppointment(id);
        } else {
            HttpUtils.sendError(exchange, 400, "Invalid action. Use cancel or complete");
            return;
        }

        if (done) {
            // Reload from repo to get updated status
            Optional<Appointment> updated = appointmentService.getAppointmentById(id);
            if (updated.isPresent()) {
                HttpUtils.sendJson(exchange, 200, HttpUtils.success(updated.get().toJson()));
            } else {
                HttpUtils.sendJson(exchange, 200, HttpUtils.success("{\"status\":\"done\"}"));
            }
        } else {
            HttpUtils.sendError(exchange, 500, "Failed to update appointment");
        }
    }


    private String listToJsonArray(List<Appointment> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toJson());
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
