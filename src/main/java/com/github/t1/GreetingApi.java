package com.github.t1;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/hello")
public interface GreetingApi {
    @GET Greeting hello();
}
