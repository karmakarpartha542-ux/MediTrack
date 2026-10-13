package com.meditrack.server;

import com.meditrack.util.JsonUtils;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * HTTP helper utilities for request parsing, JSON response formatting,
 * and Cross-Origin Resource Sharing (CORS) header configuration.
 */
public class HttpUtils {

    /**
     * Appends standard permissive CORS headers to the HTTP exchange.
     *
     * @param e the HTTP exchange
     */
    public static void addCorsHeaders(HttpExchange e) {
        e.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        e.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        e.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        e.getResponseHeaders().set("Access-Control-Max-Age", "86400");
    }

    /**
     * Sends a JSON response with specified status code and CORS headers.
     *
     * @param e    the HTTP exchange
     * @param code HTTP status code (e.g. 200, 201, 400, 404, 500)
     * @param body JSON response payload string
     * @throws IOException on socket transmission error
     */
    public static void sendJson(HttpExchange e, int code, String body) throws IOException {
        addCorsHeaders(e);
        e.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = (body != null ? body : "").getBytes(StandardCharsets.UTF_8);
        e.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = e.getResponseBody()) {
            os.write(bytes);
            os.flush();
        }
    }

    /**
     * Sends a standardized JSON error response.
     *
     * @param e    the HTTP exchange
     * @param code HTTP error code
     * @param msg  descriptive error message
     * @throws IOException on socket transmission error
     */
    public static void sendError(HttpExchange e, int code, String msg) throws IOException {
        sendJson(e, code, error(msg));
    }

    /**
     * Reads the entire HTTP request body as a UTF-8 string.
     *
     * @param e the HTTP exchange
     * @return body content string
     * @throws IOException on stream read failure
     */
    public static String readBody(HttpExchange e) throws IOException {
        try (InputStream is = e.getRequestBody()) {
            byte[] bytes = is.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    /**
     * Extracts the value of a specific query parameter from the request URL.
     *
     * @param e   the HTTP exchange
     * @param key query parameter name
     * @return decoded query parameter value, or null if parameter not present
     */
    public static String getQueryParam(HttpExchange e, String key) {
        if (e == null || e.getRequestURI() == null || key == null) {
            return null;
        }
        String query = e.getRequestURI().getRawQuery();
        if (query == null || query.isEmpty()) {
            return null;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            String paramKey;
            String paramVal = "";
            if (idx > -1) {
                paramKey = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                paramVal = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            } else {
                paramKey = URLDecoder.decode(pair, StandardCharsets.UTF_8);
            }
            if (paramKey.equalsIgnoreCase(key)) {
                return paramVal;
            }
        }
        return null;
    }

    /**
     * Formats an object JSON string inside standard success envelope.
     *
     * @param dataJson JSON string of payload object
     * @return wrapped JSON string
     */
    public static String success(String dataJson) {
        String data = (dataJson != null && !dataJson.trim().isEmpty()) ? dataJson.trim() : "{}";
        return "{\"success\":true,\"data\":" + data + "}";
    }

    /**
     * Formats an array JSON string inside standard success envelope.
     *
     * @param arrayJson JSON string of payload array
     * @return wrapped JSON string
     */
    public static String successArray(String arrayJson) {
        String data = (arrayJson != null && !arrayJson.trim().isEmpty()) ? arrayJson.trim() : "[]";
        return "{\"success\":true,\"data\":" + data + "}";
    }

    /**
     * Formats an error message inside standard error envelope.
     *
     * @param msg error message text
     * @return wrapped error JSON string
     */
    public static String error(String msg) {
        return "{\"success\":false,\"error\":\"" + JsonUtils.escape(msg) + "\"}";
    }
}
