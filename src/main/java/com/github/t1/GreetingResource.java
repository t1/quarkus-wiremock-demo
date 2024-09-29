package com.github.t1;

import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

public class GreetingResource implements GreetingApi {
    @Inject @RestClient NameServiceApi nameService;

    @Override public Greeting hello() {return new Greeting("Hello", nameService.name());}
}
