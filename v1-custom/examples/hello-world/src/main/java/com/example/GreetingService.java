package com.example;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * A CDI bean that provides greeting functionality.
 *
 * @ApplicationScoped makes this a singleton CDI bean that can be injected
 * into other beans throughout the application lifecycle.
 */
@ApplicationScoped
public class GreetingService {

    private int counter = 0;

    public String greet(String name) {
        counter++;
        if (name == null || name.isEmpty()) {
            return String.format("Hello from Quarkus CDI! (request #%d)", counter);
        }
        return String.format("Hello %s from Quarkus CDI! (request #%d)", name, counter);
    }

    public int getCounter() {
        return counter;
    }
}
