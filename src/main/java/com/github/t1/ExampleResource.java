package com.github.t1;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/hello")
public class ExampleResource {

    @GET public Greeting hello() {return new Greeting("Hello", "World");}
}
