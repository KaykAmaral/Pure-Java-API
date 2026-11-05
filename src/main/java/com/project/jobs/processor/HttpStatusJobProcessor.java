package com.project.jobs.processor;

import com.project.jobs.model.result.HttpStatusResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class HttpStatusJobProcessor
        implements JobProcessor<HttpStatusResult> {

    private static final Set<String> ALLOWED_SCHEMES =
            Set.of("http", "https");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;

    public HttpStatusJobProcessor(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(
                httpClient,
                "httpClient must not be null"
        );
    }

    @Override
    public HttpStatusResult process(String content) {
        Objects.requireNonNull(content, "content must not be null");

        URI uri = parseHttpUri(content);
        HttpRequest request = HttpRequest
                .newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<Void> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            );

            return new HttpStatusResult(response.statusCode());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "HTTP request was interrupted",
                    exception
            );
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "HTTP request failed",
                    exception
            );
        }
    }

    private static URI parseHttpUri(String content) {
        URI uri;

        try {
            uri = URI.create(content.strip());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "content must be a valid HTTP URL",
                    exception
            );
        }

        String scheme = uri.getScheme();

        if (scheme == null || !ALLOWED_SCHEMES.contains(
                scheme.toLowerCase(Locale.ROOT)
        )) {
            throw new IllegalArgumentException(
                    "URL scheme must be http or https"
            );
        }

        if (uri.getHost() == null) {
            throw new IllegalArgumentException(
                    "URL must contain a host"
            );
        }

        return uri;
    }
}
