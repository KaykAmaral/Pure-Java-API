package com.project.jobs;

import com.project.jobs.controller.JobController;
import com.project.jobs.http.HttpResponse;
import com.project.jobs.http.Router;
import com.project.jobs.repository.InMemoryJobRepository;
import com.project.jobs.service.JobService;
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

        InMemoryJobRepository repository = new InMemoryJobRepository();
        JobService service = new JobService(repository);
        JobController jobController = new JobController(service);

        Router router = new Router();
        router.addRoute("GET", "/health", Main::handleHealth);
        router.addRoute("POST", "/jobs", jobController::create);
        router.addRoute("GET", "/jobs", jobController::findAll);
        router.addRoute("GET", "/jobs/{id}", jobController::findById);
        router.addRoute("DELETE", "/jobs/{id}", jobController::deleteById);

        server.createContext("/", router);
        server.start();

        System.out.printf("Servidor iniciado em http://localhost:%d%n", PORT);
    }

    private static void handleHealth(HttpExchange exchange) throws IOException {
        HttpResponse.sendJson(exchange, 200, "{\"status\":\"UP\"}");
    }

}
