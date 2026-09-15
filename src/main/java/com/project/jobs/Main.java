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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class Main {

    private static final int PORT = 8080;

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        InMemoryJobRepository repository = new InMemoryJobRepository();
        ExecutorService jobExecutor = Executors.newFixedThreadPool(2);
        
        Runtime.getRuntime().addShutdownHook(
        new Thread(
                () -> shutdownExecutor(jobExecutor),
                "application-shutdown"
        )
        );

        JobService service = new JobService(repository, jobExecutor);
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

    private static void shutdownExecutor(ExecutorService executor) {
        System.out.println("Encerrando executor de Jobs...");

        executor.shutdown();

        try {
            boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);

            if (!finished) {
                System.out.println("Forçando encerramento das tarefas...");

                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

    }

}
