package com.github.t1;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "name-service")
public interface NameServiceApi {
    @GET @Path("/name") String name();
}
