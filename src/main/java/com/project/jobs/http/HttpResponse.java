package com.project.jobs.http;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class HttpResponse {

    private HttpResponse() {
    }

    public static void sendJson(HttpExchange exchange, int statusCode, String json)
            throws IOException {
        byte[] responseBody = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBody.length);

        try (OutputStream responseStream = exchange.getResponseBody()) {
            responseStream.write(responseBody);
        }
    }
}
