package com.github.t1;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

@RegisterRestClient(baseUri = "http://localhost:8081", configKey = "hellos")
public interface HellosApi {
    @Produces(TEXT_PLAIN)
    @GET @Path("/hello") String hello();
}
