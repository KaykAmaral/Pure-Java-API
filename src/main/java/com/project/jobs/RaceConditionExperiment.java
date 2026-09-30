package com.project.jobs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class RaceConditionExperiment {

    private static int counter = 0;

    private RaceConditionExperiment() {
    }

    public static void main(String[] args)
            throws InterruptedException {

        int taskCount = 8;
        int incrementsPerTask = 1_000_000;
        int expected = taskCount * incrementsPerTask;

        ExecutorService executor =
                Executors.newFixedThreadPool(taskCount);

        for (int task = 0; task < taskCount; task++) {
            executor.execute(() -> incrementManyTimes(
                    incrementsPerTask
            ));
        }

        executor.shutdown();

        boolean finished = executor.awaitTermination(
                30,
                TimeUnit.SECONDS
        );

        System.out.println("Executor terminou: " + finished);
        System.out.println("Esperado: " + expected);
        System.out.println("Obtido:   " + counter);
        System.out.println("Perdidos: " + (expected - counter));
    }

    private static void incrementManyTimes(int amount) {
        for (int increment = 0; increment < amount; increment++) {
            increment();
        }
    }

    private static synchronized void increment() {
        counter++;
    }
}