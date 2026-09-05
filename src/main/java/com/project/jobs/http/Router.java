package com.project.jobs.http;

import com.project.jobs.exception.InvalidRequestException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class Router implements HttpHandler {

    private final Map<String, Map<String, HttpHandler>> routes = new HashMap<>();

    public void addRoute(String method, String path, HttpHandler handler) {
        Map<String, HttpHandler> handlersByMethod = routes.get(path);

        if (handlersByMethod == null) {
            handlersByMethod = new HashMap<>();
            routes.put(path, handlersByMethod);
        }

        handlersByMethod.put(method, handler);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } catch (InvalidRequestException exception) {
            logInvalidRequest(exchange, exception);
            HttpResponse.sendJson(exchange, 400, "{\"error\":\"Bad Request\"}");
        } catch (RuntimeException exception) {
            logUnexpectedError(exchange, exception);
            HttpResponse.sendJson(exchange, 500, "{\"error\":\"Internal Server Error\"}");
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        logRequest(exchange);

        String path = exchange.getRequestURI().getPath();
        Map<String, HttpHandler> handlersByMethod = routes.get(path);

        if (handlersByMethod == null) {
            HttpResponse.sendJson(exchange, 404, "{\"error\":\"Not Found\"}");
            return;
        }

        String method = exchange.getRequestMethod();
        HttpHandler handler = handlersByMethod.get(method);

        if (handler == null) {
            exchange.getResponseHeaders().set(
                    "Allow",
                    String.join(", ", handlersByMethod.keySet())
            );
            HttpResponse.sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        handler.handle(exchange);
    }

    private void logInvalidRequest(
            HttpExchange exchange,
            InvalidRequestException exception
    ) {
        System.err.printf(
                "Rejected request %s %s: %s%n",
                exchange.getRequestMethod(),
                exchange.getRequestURI(),
                exception.getMessage()
        );

        if (exception.getCause() != null) {
            System.err.printf(
                    "Cause: %s: %s%n",
                    exception.getCause().getClass().getSimpleName(),
                    exception.getCause().getMessage()
            );
        }
    }

    private void logUnexpectedError(
            HttpExchange exchange,
            RuntimeException exception
    ) {
        System.err.printf(
                "Unexpected error while handling %s %s%n",
                exchange.getRequestMethod(),
                exchange.getRequestURI()
        );
        exception.printStackTrace(System.err);
    }

    private void logRequest(HttpExchange exchange) {
        String userAgent = exchange.getRequestHeaders().getFirst("User-Agent");

        if (userAgent == null) {
            userAgent = "não informado";
        }

        System.out.printf(
                "%s %s %s | User-Agent: %s%n",
                exchange.getRequestMethod(),
                exchange.getRequestURI(),
                exchange.getProtocol(),
                userAgent
        );
    }
}
