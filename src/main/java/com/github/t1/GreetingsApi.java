package com.github.t1;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/greeting")
public interface GreetingsApi {
    @GET Greeting greeting();
}
