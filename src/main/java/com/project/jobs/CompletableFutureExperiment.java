package com.project.jobs;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class CompletableFutureExperiment {

    private CompletableFutureExperiment() {
    }

    public static void main(String[] args) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            
            demonstrateThenCompose(executor);
            demonstrateThenCombine(executor);

        }
    }

    private static void log(String message) {
        Thread currentThread = Thread.currentThread();

        System.out.printf(
                "[thread=%s, virtual=%s] %s%n",
                currentThread,
                currentThread.isVirtual(),
                message);
    }

    private static String loadText(boolean shouldFail) {
        log("carregamento iniciado");

        try {
            Thread.sleep(300);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "carregamento interrompido",
                    exception);
        }

        if (shouldFail) {
            throw new IllegalStateException(
                    "falha simulada no carregamento");
        }

        return "Lorem ipsum dolor sit amet";
    }

    private static void demonstrateWhenComplete(
            ExecutorService executor) {
        log("--- whenComplete ---");

        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(
                        () -> loadText(true),
                        executor)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log(
                                "whenComplete observou: "
                                        + exception.getMessage());
                    }
                });

        try {
            future.join();
        } catch (CompletionException exception) {
            log(
                    "join ainda recebeu a falha: "
                            + exception.getCause().getMessage());
        }
    }

    private static void demonstrateExceptionally(
            ExecutorService executor) {
        log("--- exceptionally ---");

        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(
                        () -> loadText(true),
                        executor)
                .exceptionally(exception -> {
                    log(
                            "exceptionally recuperou: "
                                    + exception.getMessage());

                    return "Texto alternativo";
                });

        String result = future.join();

        log("resultado recuperado: " + result);
    }

    private static void demonstrateHandle(
            ExecutorService executor,
            boolean shouldFail) {
        log("--- handle: shouldFail=" + shouldFail + " ---");

        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(
                        () -> loadText(shouldFail),
                        executor)
                .handle((text, exception) -> {
                    if (exception != null) {
                        log("handle recebeu uma falha");
                        return "Texto criado pelo handle";
                    }

                    log("handle recebeu sucesso");
                    return text.toUpperCase();
                });

        log("resultado do handle: " + future.join());
    }

    private static void demonstrateThenCompose(
            ExecutorService executor) {
        log("--- thenCompose ---");

        CompletableFuture<Integer> wordCountFuture = CompletableFuture
                .supplyAsync(
                        CompletableFutureExperiment::findDocumentId,
                        executor)
                .thenCompose(
                        documentId -> loadDocumentAsync(
                                documentId,
                                executor))
                .thenApply(
                        CompletableFutureExperiment::countWords);

        int wordCount = wordCountFuture.join();

        log("quantidade recebida: " + wordCount);
    }

    private static String findDocumentId() {
        log("buscando ID do documento");
        sleep(300);

        return "document-123";
    }

    private static CompletableFuture<String> loadDocumentAsync(
            String documentId,
            ExecutorService executor) {
        return CompletableFuture.supplyAsync(
                () -> {
                    log("carregando documento " + documentId);
                    sleep(500);

                    return "Java Virtual Threads CompletableFuture";
                },
                executor);
    }

    private static void demonstrateThenCombine(
            ExecutorService executor) {
        log("--- thenCombine ---");

        long startedAt = System.nanoTime();

        CompletableFuture<String> titleFuture = CompletableFuture.supplyAsync(
                () -> {
                    log("carregando título");
                    sleep(1_000);

                    return "Título do documento";
                },
                executor);

        CompletableFuture<String> contentFuture = CompletableFuture.supplyAsync(
                () -> {
                    log("carregando conteúdo");
                    sleep(1_000);

                    return "Conteúdo do documento";
                },
                executor);

        CompletableFuture<String> documentFuture = titleFuture.thenCombine(
                contentFuture,
                (title, content) -> title + System.lineSeparator() + content);

        String document = documentFuture.join();

        long elapsedMilliseconds = TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startedAt);

        log(
                "documento combinado em "
                        + elapsedMilliseconds
                        + " ms:"
                        + System.lineSeparator()
                        + document);
    }

    private static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "operação interrompida",
                    exception);
        }
    }

    private static int countWords(String text) {
        log("contagem de palavras iniciada");

        if (text.isBlank()) {
            return 0;
        }

        return text.trim().split("\\s+").length;
    }
}