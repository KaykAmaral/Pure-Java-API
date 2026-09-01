package com.project.jobs.http;

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
