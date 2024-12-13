package com.github.t1;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import lombok.extern.slf4j.Slf4j;

import java.util.Stack;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

@Startup
@Singleton
@Path("/hello")
@Slf4j
public class HellosResource {
    private final Stack<String> hellos = new Stack<>();

    @PostConstruct void init() {log.warn("################### started hello resource");}

    @GET @Produces(TEXT_PLAIN)
    public String hello() {return hellos.isEmpty() ? "Hello" : hellos.pop();}

    public void pushResponse(String hello) {this.hellos.push(hello);}

    public void reset() {this.hellos.clear();}
}
