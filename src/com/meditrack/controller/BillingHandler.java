package com.meditrack.controller;

import com.meditrack.service.BillingService;
import com.meditrack.model.billing.Bill;
import com.meditrack.server.HttpUtils;
import com.meditrack.util.JsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * REST handler for /api/billing and /api/billing/generate
 * GET  /api/billing               → all bills
 * GET  /api/billing?pending=true  → pending bills
 * GET  /api/billing?patientId=... → bills by patient
 * GET  /api/billing?id=...        → by id
 * POST /api/billing/generate      → generate bill
 * PUT  /api/billing?id=...&action=pay → mark paid
 */
public class BillingHandler implements HttpHandler {

    private final BillingService billingService;

    public BillingHandler(BillingService billingService) {
        this.billingService = billingService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path   = exchange.getRequestURI().getPath();
        HttpUtils.addCorsHeaders(exchange);

        try {
            switch (method) {
                case "OPTIONS": exchange.sendResponseHeaders(204, -1); break;
                case "GET":     handleGet(exchange); break;
                case "POST":
                    if (path.endsWith("/generate")) {
                        handleGenerate(exchange);
                    } else {
                        HttpUtils.sendError(exchange, 404, "Use /api/billing/generate to create bills");
                    }
                    break;
                case "PUT":     handlePut(exchange); break;
                default:        HttpUtils.sendError(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal error: " + e.getMessage());
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        String pending   = HttpUtils.getQueryParam(exchange, "pending");
        String patientId = HttpUtils.getQueryParam(exchange, "patientId");
        String id        = HttpUtils.getQueryParam(exchange, "id");

        if (!id.isEmpty()) {
            Optional<Bill> b = billingService.getBillById(id);
            if (b.isPresent()) {
                HttpUtils.sendJson(exchange, 200, HttpUtils.success(b.get().toJson()));
            } else {
                HttpUtils.sendError(exchange, 404, "Bill not found");
            }
        } else if ("true".equalsIgnoreCase(pending)) {
            List<Bill> bills = billingService.getPendingBills();
            HttpUtils.sendJson(exchange, 200, HttpUtils.successArray(listToJsonArray(bills)));
        } else if (!patientId.isEmpty()) {
            List<Bill> bills = billingService.getBillsByPatient(patientId);
            HttpUtils.sendJson(exchange, 200, HttpUtils.successArray(listToJsonArray(bills)));
        } else {
            List<Bill> bills = billingService.getAllBills();
            HttpUtils.sendJson(exchange, 200, HttpUtils.successArray(listToJsonArray(bills)));
        }
    }

    private void handleGenerate(HttpExchange exchange) throws IOException {
        String body         = HttpUtils.readBody(exchange);
        String patientId    = JsonUtils.getString(body, "patientId");
        String patientName  = JsonUtils.getString(body, "patientName");
        String roomId       = JsonUtils.getString(body, "roomId");
        int    daysStayed   = JsonUtils.getInt(body, "daysStayed");
        double consultFee   = JsonUtils.getDouble(body, "consultationFee");
        String doctorDesc   = JsonUtils.getString(body, "doctorDescription");

        if (patientId.isEmpty() || patientName.isEmpty()) {
            HttpUtils.sendError(exchange, 400, "patientId and patientName are required");
            return;
        }

        try {
            Bill bill = billingService.generateBill(
                    patientId, patientName, roomId, daysStayed, consultFee, doctorDesc);
            HttpUtils.sendJson(exchange, 201, HttpUtils.success(bill.toJson()));
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        }
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        String id     = HttpUtils.getQueryParam(exchange, "id");
        String action = HttpUtils.getQueryParam(exchange, "action");

        if (id.isEmpty()) { HttpUtils.sendError(exchange, 400, "id is required"); return; }

        if ("pay".equalsIgnoreCase(action)) {
            // Get the bill before marking paid so we can return it
            Optional<Bill> opt = billingService.getBillById(id);
            if (opt.isEmpty()) { HttpUtils.sendError(exchange, 404, "Bill not found"); return; }
            boolean done = billingService.markBillPaid(id);
            if (done) {
                Optional<Bill> updated = billingService.getBillById(id);
                HttpUtils.sendJson(exchange, 200, HttpUtils.success(
                        updated.isPresent() ? updated.get().toJson() : opt.get().toJson()));
            } else {
                HttpUtils.sendError(exchange, 500, "Could not mark bill as paid");
            }
        } else {
            HttpUtils.sendError(exchange, 400, "Invalid action. Use action=pay");
        }
    }


    private String listToJsonArray(List<Bill> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toJson());
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
