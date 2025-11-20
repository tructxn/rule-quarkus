package com.example;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

/**
 * Simple Quarkus application that starts a Vert.x HTTP server.
 *
 * Note: This is a simplified version that doesn't use the full Quarkus runtime.
 * It directly uses Vert.x to create an HTTP server, which is what Quarkus uses under the hood.
 */
public class QuarkusApp {

    public static void main(String... args) {
        System.out.println("Starting Quarkus-style application with Bazel...");

        // Create Vert.x instance (what Quarkus uses for HTTP)
        Vertx vertx = Vertx.vertx();

        // Create HTTP server
        HttpServer server = vertx.createHttpServer();

        // Set up router for /hello endpoint
        server.requestHandler(request -> {
            if (request.path().equals("/hello")) {
                request.response()
                    .putHeader("content-type", "text/plain")
                    .end("Hello from Quarkus with Bazel!");
            } else {
                request.response()
                    .setStatusCode(404)
                    .end("Not Found");
            }
        });

        // Start server on port 8080
        int port = 8080;
        server.listen(port, result -> {
            if (result.succeeded()) {
                System.out.println("HTTP server started on port " + port);
                System.out.println("Try: curl http://localhost:" + port + "/hello");
            } else {
                System.err.println("Failed to start server: " + result.cause());
                System.exit(1);
            }
        });

        // Keep the application running
        System.out.println("Application is running. Press Ctrl+C to stop.");
    }
}
