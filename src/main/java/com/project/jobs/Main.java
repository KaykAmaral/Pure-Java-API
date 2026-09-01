package com.project.jobs;

import com.project.jobs.http.HttpResponse;
import com.project.jobs.http.Router;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public final class Main {

    private static final int PORT = 8080;

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        Router router = new Router();
        router.addRoute("GET", "/health", Main::handleHealth);
        router.addRoute("GET", "/hello", Main::handleHello);

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
}
