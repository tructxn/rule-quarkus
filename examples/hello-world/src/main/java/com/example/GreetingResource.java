package com.example;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * REST endpoint that demonstrates Quarkus CDI dependency injection.
 *
 * The GreetingService is injected using @Inject annotation,
 * which is provided by Quarkus ArC (CDI implementation).
 */
@Path("/hello")
public class GreetingResource {

    @Inject
    GreetingService greetingService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@QueryParam("name") String name) {
        return greetingService.greet(name);
    }

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    public String count() {
        return "Total requests: " + greetingService.getCounter();
    }
}
