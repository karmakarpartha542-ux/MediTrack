package com.meditrack.controller;

import com.meditrack.service.DoctorService;
import com.meditrack.model.doctor.Doctor;
import com.meditrack.model.doctor.Specialization;
import com.meditrack.server.HttpUtils;
import com.meditrack.util.JsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * REST handler for /api/doctors
 * GET /api/doctors                  → all doctors
 * GET /api/doctors?available=true   → available only
 * GET /api/doctors?search=...       → search
 * GET /api/doctors?id=...           → by id
 * POST /api/doctors                 → add doctor
 * PUT /api/doctors?toggle=id        → toggle availability
 */
public class DoctorHandler implements HttpHandler {

    private final DoctorService doctorService;

    public DoctorHandler(DoctorService doctorService) {
        this.doctorService = doctorService;
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
        String available = HttpUtils.getQueryParam(exchange, "available");
        String search    = HttpUtils.getQueryParam(exchange, "search");
        String id        = HttpUtils.getQueryParam(exchange, "id");

        if (!id.isEmpty()) {
            Optional<Doctor> d = doctorService.getDoctorById(id);
            if (d.isPresent()) {
                HttpUtils.sendJson(exchange, 200, HttpUtils.success(d.get().toJson()));
            } else {
                HttpUtils.sendError(exchange, 404, "Doctor not found");
            }
        } else if ("true".equalsIgnoreCase(available)) {
            List<Doctor> docs = doctorService.getAvailableDoctors();
            HttpUtils.sendJson(exchange, 200, HttpUtils.successArray(listToJsonArray(docs)));
        } else if (!search.isEmpty()) {
            List<Doctor> docs = doctorService.searchDoctors(search);
            HttpUtils.sendJson(exchange, 200, HttpUtils.successArray(listToJsonArray(docs)));
        } else {
            List<Doctor> docs = doctorService.getAllDoctors();
            HttpUtils.sendJson(exchange, 200, HttpUtils.successArray(listToJsonArray(docs)));
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body   = HttpUtils.readBody(exchange);
        String name   = JsonUtils.getString(body, "name");
        String phone  = JsonUtils.getString(body, "phone");
        String email  = JsonUtils.getString(body, "email");
        int    age    = JsonUtils.getInt(body, "age");
        String spec   = JsonUtils.getString(body, "specialization");
        double fee    = JsonUtils.getDouble(body, "consultationFee");
        String dept   = JsonUtils.getString(body, "department");

        if (name.isEmpty()) { HttpUtils.sendError(exchange, 400, "Name is required"); return; }

        Doctor d = doctorService.addDoctor(name, phone, email, age,
                Specialization.fromString(spec), fee, dept);
        HttpUtils.sendJson(exchange, 201, HttpUtils.success(d.toJson()));
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        String toggleId = HttpUtils.getQueryParam(exchange, "toggle");
        if (!toggleId.isEmpty()) {
            Doctor d = doctorService.toggleAvailability(toggleId);
            if (d != null) {
                HttpUtils.sendJson(exchange, 200, HttpUtils.success(d.toJson()));
            } else {
                HttpUtils.sendError(exchange, 404, "Doctor not found");
            }
            return;
        }
        // General update
        String body = HttpUtils.readBody(exchange);
        String id   = JsonUtils.getString(body, "id");
        Optional<Doctor> opt = doctorService.getDoctorById(id);
        if (opt.isEmpty()) { HttpUtils.sendError(exchange, 404, "Doctor not found"); return; }

        Doctor d = opt.get();
        String name  = JsonUtils.getString(body, "name");
        String phone = JsonUtils.getString(body, "phone");
        String email = JsonUtils.getString(body, "email");
        int age      = JsonUtils.getInt(body, "age");
        double fee   = JsonUtils.getDouble(body, "consultationFee");

        if (!name.isEmpty())  d.setName(name);
        if (!phone.isEmpty()) d.setPhone(phone);
        if (!email.isEmpty()) d.setEmail(email);
        if (age > 0)          d.setAge(age);
        if (fee > 0)          d.setConsultationFee(fee);

        doctorService.updateDoctor(d);
        HttpUtils.sendJson(exchange, 200, HttpUtils.success(d.toJson()));
    }

    private String listToJsonArray(List<Doctor> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toJson());
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
