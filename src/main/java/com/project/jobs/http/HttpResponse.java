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
        send(
                exchange,
                statusCode,
                "application/json; charset=UTF-8",
                json
        );
    }

    public static void sendText(HttpExchange exchange, int statusCode, String text)
            throws IOException {
        send(
                exchange,
                statusCode,
                "text/plain; charset=UTF-8",
                text
        );
    }

    private static void send(
            HttpExchange exchange,
            int statusCode,
            String contentType,
            String body
    ) throws IOException {
        byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, responseBody.length);

        try (OutputStream responseStream = exchange.getResponseBody()) {
            responseStream.write(responseBody);
        }
    }
}
