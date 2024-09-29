package com.github.t1;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

@RegisterRestClient(configKey = "name-service")
public interface NameServiceApi {
    @Produces(TEXT_PLAIN)
    @GET @Path("/name") String name();
}
