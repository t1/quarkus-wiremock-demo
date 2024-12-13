package com.github.t1;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Slf4j
public class GreetingsResource implements GreetingsApi {
    @Inject @RestClient HellosApi hellos;

    @Inject @RestClient NamesApi names;

    @PostConstruct void init() {log.info("################### started greetings resource");}

    @Override public Greeting greeting() {
        var hello = hellos.hello();
        var name = names.name();
        return new Greeting(hello, name);
    }
}
