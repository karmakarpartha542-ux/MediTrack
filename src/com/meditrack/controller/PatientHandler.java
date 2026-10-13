package com.meditrack.controller;

import com.meditrack.service.PatientService;
import com.meditrack.model.patient.Patient;
import com.meditrack.server.HttpUtils;
import com.meditrack.util.JsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * REST handler for /api/patients
 * GET    /api/patients            → all patients
 * GET    /api/patients?id=...     → by id
 * GET    /api/patients?search=... → search
 * POST   /api/patients            → register
 * PUT    /api/patients            → update
 */
public class PatientHandler implements HttpHandler {

    private final PatientService patientService;

    public PatientHandler(PatientService patientService) {
        this.patientService = patientService;
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
        String id     = HttpUtils.getQueryParam(exchange, "id");
        String search = HttpUtils.getQueryParam(exchange, "search");

        if (!id.isEmpty()) {
            Optional<Patient> p = patientService.getPatientById(id);
            if (p.isPresent()) {
                HttpUtils.sendJson(exchange, 200, HttpUtils.success(p.get().toJson()));
            } else {
                HttpUtils.sendError(exchange, 404, "Patient not found");
            }
        } else if (!search.isEmpty()) {
            List<Patient> results = patientService.searchPatients(search);
            HttpUtils.sendJson(exchange, 200, HttpUtils.successArray(listToJsonArray(results)));
        } else {
            List<Patient> all = patientService.getAllPatients();
            HttpUtils.sendJson(exchange, 200, HttpUtils.successArray(listToJsonArray(all)));
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = HttpUtils.readBody(exchange);
        String name    = JsonUtils.getString(body, "name");
        String phone   = JsonUtils.getString(body, "phone");
        String email   = JsonUtils.getString(body, "email");
        int    age     = JsonUtils.getInt(body, "age");
        String blood   = JsonUtils.getString(body, "bloodGroup");
        String history = JsonUtils.getString(body, "medicalHistory");

        if (name.isEmpty()) { HttpUtils.sendError(exchange, 400, "Name is required"); return; }

        Patient p = patientService.registerPatient(name, phone, email, age, blood, history);
        HttpUtils.sendJson(exchange, 201, HttpUtils.success(p.toJson()));
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        String body = HttpUtils.readBody(exchange);
        String id   = JsonUtils.getString(body, "id");

        Optional<Patient> opt = patientService.getPatientById(id);
        if (opt.isEmpty()) { HttpUtils.sendError(exchange, 404, "Patient not found"); return; }

        Patient p = opt.get();
        String name  = JsonUtils.getString(body, "name");
        String phone = JsonUtils.getString(body, "phone");
        String email = JsonUtils.getString(body, "email");
        int age      = JsonUtils.getInt(body, "age");

        if (!name.isEmpty())  p.setName(name);
        if (!phone.isEmpty()) p.setPhone(phone);
        if (!email.isEmpty()) p.setEmail(email);
        if (age > 0)          p.setAge(age);

        String blood   = JsonUtils.getString(body, "bloodGroup");
        String history = JsonUtils.getString(body, "medicalHistory");
        if (!blood.isEmpty())   p.setBloodGroup(blood);
        if (!history.isEmpty()) p.setMedicalHistory(history);

        patientService.updatePatient(p);
        HttpUtils.sendJson(exchange, 200, HttpUtils.success(p.toJson()));
    }

    private String listToJsonArray(List<Patient> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toJson());
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
