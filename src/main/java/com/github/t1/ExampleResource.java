package com.github.t1;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/hello")
public class ExampleResource {
    @RegisterRestClient(configKey = "name-service")
    public interface NameServiceApi {
        @GET @Path("/name") String name();
    }

    @Inject @RestClient NameServiceApi nameService;

    @GET public Greeting hello() {return new Greeting("Hello", nameService.name());}
}
