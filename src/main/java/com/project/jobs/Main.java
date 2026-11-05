package com.project.jobs;

import com.project.jobs.controller.JobController;
import com.project.jobs.controller.StatsController;
import com.project.jobs.http.HttpResponse;
import com.project.jobs.http.Router;
import com.project.jobs.model.JobType;
import com.project.jobs.model.result.JobResult;
import com.project.jobs.processor.CharacterCountJobProcessor;
import com.project.jobs.processor.JobProcessor;
import com.project.jobs.processor.HttpStatusJobProcessor;
import com.project.jobs.processor.WordCountJobProcessor;
import com.project.jobs.repository.InMemoryJobRepository;
import com.project.jobs.service.JobService;
import com.project.jobs.stats.JobStatistics;
import com.project.jobs.stats.JobStatisticsReporter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class Main {

    private static final int PORT = 8080;
    private static final long STATISTICS_INITIAL_DELAY_SECONDS = 5;
    private static final long STATISTICS_DELAY_SECONDS = 10;

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        InMemoryJobRepository repository = new InMemoryJobRepository();
        JobStatistics statistics = new JobStatistics();
        ExecutorService jobExecutor = Executors.newVirtualThreadPerTaskExecutor();
        HttpClient httpClient = HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        ThreadFactory statisticsThreadFactory = Thread
                .ofPlatform()
                .name("stats-reporter")
                .factory();

        ScheduledExecutorService statisticsScheduler =
                Executors.newSingleThreadScheduledExecutor(
                        statisticsThreadFactory
                );

        statisticsScheduler.scheduleWithFixedDelay(
                new JobStatisticsReporter(statistics),
                STATISTICS_INITIAL_DELAY_SECONDS,
                STATISTICS_DELAY_SECONDS,
                TimeUnit.SECONDS
        );

        Runtime.getRuntime().addShutdownHook(
                new Thread(
                        () -> {
                            shutdownExecutor(
                                    statisticsScheduler,
                                    "statistics scheduler"
                            );
                            shutdownExecutor(jobExecutor, "job executor");
                        },
                        "application-shutdown"
                )
        );

        Map<JobType, JobProcessor<? extends JobResult>> processors = Map.of(
            JobType.WORD_COUNT,
            new WordCountJobProcessor(),

            JobType.CHARACTER_COUNT,
            new CharacterCountJobProcessor(),

            JobType.HTTP_STATUS,
            new HttpStatusJobProcessor(httpClient)
        );

        JobService service = new JobService(
                repository,
                jobExecutor,
                processors,
                statistics
        );
        JobController jobController = new JobController(service);
        StatsController statsController = new StatsController(statistics);

        Router router = new Router();
        router.addRoute("GET", "/health", Main::handleHealth);
        router.addRoute("POST", "/jobs", jobController::create);
        router.addRoute("GET", "/jobs", jobController::findAll);
        router.addRoute("GET", "/jobs/{id}", jobController::findById);
        router.addRoute("DELETE", "/jobs/{id}", jobController::deleteById);
        router.addRoute("GET", "/stats", statsController::get);

        server.createContext("/", router);
        server.start();

        System.out.printf("Servidor iniciado em http://localhost:%d%n", PORT);
    }

    private static void handleHealth(HttpExchange exchange) throws IOException {
        HttpResponse.sendJson(exchange, 200, "{\"status\":\"UP\"}");
    }

    private static void shutdownExecutor(
            ExecutorService executor,
            String executorName
    ) {
        System.out.printf("Encerrando %s...%n", executorName);

        executor.shutdown();

        try {
            boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);

            if (!finished) {
                System.out.printf(
                        "Forçando encerramento de %s...%n",
                        executorName
                );

                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

    }

}
