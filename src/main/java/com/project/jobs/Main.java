package com.project.jobs;

import com.project.jobs.dto.CreateJobRequest;
import com.project.jobs.http.HttpResponse;
import com.project.jobs.http.Router;
import com.project.jobs.json.CreateJobRequestJsonDecoder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class Main {

    private static final int PORT = 8080;

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        Router router = new Router();
        router.addRoute("GET", "/health", Main::handleHealth);
        router.addRoute("GET", "/hello", Main::handleHello);
        router.addRoute("POST", "/echo", Main::handleEcho);

        server.createContext("/", router);
        server.start();

        System.out.printf("Servidor iniciado em http://localhost:%d%n", PORT);
    }

    private static void handleHealth(HttpExchange exchange) throws IOException {
        HttpResponse.sendJson(exchange, 200, "{\"status\":\"UP\"}");
    }

    private static void handleHello(HttpExchange exchange) throws IOException {
        HttpResponse.sendJson(exchange, 200, "{\"message\":\"hello\"}");
    }

    private static void handleEcho(HttpExchange exchange) throws IOException {
        try (InputStream requestBody = exchange.getRequestBody()) {
            byte[] contentBytes = requestBody.readAllBytes();
            String json = new String(contentBytes, StandardCharsets.UTF_8);
            CreateJobRequest request = CreateJobRequestJsonDecoder.decode(json);

            System.out.println("Valid create-job request: " + request);

            HttpResponse.sendJson(exchange, 200, json);
        }
    }
}
