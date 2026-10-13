package com.meditrack.controller;

import com.meditrack.service.RoomService;
import com.meditrack.model.room.Room;
import com.meditrack.server.HttpUtils;
import com.meditrack.util.JsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST handler for /api/rooms and /api/rooms/stats
 * GET  /api/rooms               → all rooms
 * GET  /api/rooms?available=true → available rooms
 * GET  /api/rooms/stats         → occupancy statistics
 * POST /api/rooms               → assign (body has roomId+patientId) or vacate (body has roomId only)
 */
public class RoomHandler implements HttpHandler {

    private final RoomService roomService;

    public RoomHandler(RoomService roomService) {
        this.roomService = roomService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path   = exchange.getRequestURI().getPath();
        HttpUtils.addCorsHeaders(exchange);

        try {
            if (method.equals("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (method.equals("GET")) {
                if (path.endsWith("/stats")) {
                    handleStats(exchange);
                } else {
                    handleGet(exchange);
                }
            } else if (method.equals("POST")) {
                handlePost(exchange);
            } else {
                HttpUtils.sendError(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal error: " + e.getMessage());
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        String available = HttpUtils.getQueryParam(exchange, "available");
        String id        = HttpUtils.getQueryParam(exchange, "id");

        if (!id.isEmpty()) {
            Optional<Room> r = roomService.getRoomById(id);
            if (r.isPresent()) {
                HttpUtils.sendJson(exchange, 200, HttpUtils.success(r.get().toJson()));
            } else {
                HttpUtils.sendError(exchange, 404, "Room not found");
            }
        } else if ("true".equalsIgnoreCase(available)) {
            List<Room> rooms = roomService.getAvailableRooms();
            HttpUtils.sendJson(exchange, 200, HttpUtils.successArray(listToJsonArray(rooms)));
        } else {
            List<Room> rooms = roomService.getAllRooms();
            HttpUtils.sendJson(exchange, 200, HttpUtils.successArray(listToJsonArray(rooms)));
        }
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        java.util.Map<String, Object> stats = roomService.getRoomStats();
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (java.util.Map.Entry<String, Object> e : stats.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":").append(e.getValue());
            first = false;
        }
        sb.append("}");
        HttpUtils.sendJson(exchange, 200, HttpUtils.success(sb.toString()));
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body      = HttpUtils.readBody(exchange);
        String roomId    = JsonUtils.getString(body, "roomId");
        String patientId = JsonUtils.getString(body, "patientId");

        if (roomId.isEmpty()) { HttpUtils.sendError(exchange, 400, "roomId is required"); return; }

        // Fetch the room first so we can return it
        Optional<Room> roomOpt = roomService.getRoomById(roomId);
        if (roomOpt.isEmpty()) { HttpUtils.sendError(exchange, 404, "Room not found"); return; }

        boolean done;
        if (!patientId.isEmpty() && !patientId.equals("null")) {
            done = roomService.assignRoom(roomId, patientId);
        } else {
            done = roomService.vacateRoom(roomId);
        }

        if (done) {
            Optional<Room> updated = roomService.getRoomById(roomId);
            HttpUtils.sendJson(exchange, 200, HttpUtils.success(
                    updated.isPresent() ? updated.get().toJson() : roomOpt.get().toJson()));
        } else {
            HttpUtils.sendError(exchange, 500, "Operation failed");
        }
    }


    private String listToJsonArray(List<Room> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toJson());
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
