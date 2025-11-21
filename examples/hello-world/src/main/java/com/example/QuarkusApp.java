package com.example;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

/**
 * Quarkus application that demonstrates CDI integration with Vert.x HTTP server.
 *
 * This application:
 * 1. Initializes the Quarkus ArC (CDI) container
 * 2. Creates a Vert.x HTTP server
 * 3. Uses CDI beans to handle HTTP requests
 */
public class QuarkusApp {

    public static void main(String... args) {
        System.out.println("Starting Quarkus application with CDI support...");

        // Initialize Quarkus ArC (CDI container)
        System.out.println("Initializing ArC CDI container...");
        ArcContainer container = Arc.initialize();

        if (container == null) {
            System.err.println("Failed to initialize ArC container");
            System.exit(1);
        }

        // Get the CDI bean instance
        GreetingService greetingService = container.instance(GreetingService.class).get();

        if (greetingService == null) {
            System.err.println("Failed to get GreetingService bean");
            System.exit(1);
        }

        System.out.println("CDI container initialized successfully!");

        // Create Vert.x instance (what Quarkus uses for HTTP)
        Vertx vertx = Vertx.vertx();

        // Create HTTP server
        HttpServer server = vertx.createHttpServer();

        // Set up router for endpoints
        server.requestHandler(request -> {
            String path = request.path();

            if (path.equals("/hello")) {
                String name = request.getParam("name");
                String greeting = greetingService.greet(name);
                request.response()
                    .putHeader("content-type", "text/plain")
                    .end(greeting);
            } else if (path.equals("/hello/count")) {
                String countMsg = "Total requests: " + greetingService.getCounter();
                request.response()
                    .putHeader("content-type", "text/plain")
                    .end(countMsg);
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
                System.out.println("Endpoints:");
                System.out.println("  - curl http://localhost:" + port + "/hello");
                System.out.println("  - curl http://localhost:" + port + "/hello?name=YourName");
                System.out.println("  - curl http://localhost:" + port + "/hello/count");
            } else {
                System.err.println("Failed to start server: " + result.cause());
                System.exit(1);
            }
        });

        // Keep the application running
        System.out.println("Application is running. Press Ctrl+C to stop.");

        // Register shutdown hook to properly close ArC container
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down ArC container...");
            Arc.shutdown();
        }));
    }
}
