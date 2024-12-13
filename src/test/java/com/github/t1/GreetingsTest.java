package com.github.t1;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.quarkiverse.wiremock.devservice.ConnectWireMock;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;

import static com.github.t1.WireMocker.given;
import static org.assertj.core.api.BDDAssertions.then;

@QuarkusTest
@ConnectWireMock
class GreetingsTest {
    @TestHTTPResource
    URI baseUri;
    HellosApi hellos;
    WireMocker<NamesApi> names;
    GreetingsApi greetings;

    WireMock wireMock;

    @Inject HellosResource hellosResource;


    @BeforeEach void setup() {
        hellos = RestClientBuilder.newBuilder().baseUri(baseUri).build(HellosApi.class);
        greetings = RestClientBuilder.newBuilder().baseUri(baseUri).build(GreetingsApi.class);

        wireMock.resetMappings();
        names = new WireMocker<>(wireMock, NamesApi.class);
    }

    @AfterEach void tearDown() {hellosResource.reset();}

    @Test void testHellos() {
        hellosResource.pushResponse("Mock-Hello");

        var response = hellos.hello();

        then(response).isEqualTo("Mock-Hello");
    }

    @Test void testHellosDefault() {
        var response = hellos.hello();

        then(response).isEqualTo("Hello");
    }

    @Test void testNameViaWireMockerGiven() {
        given(names.api.name()).returns("WireMocker");

        var response = names.client().name();

        then(response).isEqualTo("WireMocker");
    }

    @Test void testNameViaFileMapping() {
        var response = names.client().name();

        then(response).isEqualTo("File");
    }

    @Test void testNameViaMappingsLoaderExtension() throws Exception {
        var inputStream = (InputStream) URI.create(names.uri() + "/foo").toURL().getContent();
        var body = new String(inputStream.readAllBytes());

        then(body).isEqualTo("{\"foo\":\"bar\"}");
    }

    @Test void testGreetings() {
        hellosResource.pushResponse("Full-Hello");
        given(names.api.name()).returns("Whole-World");

        var response = greetings.greeting();

        then(response).isEqualTo(new Greeting("Full-Hello", "Whole-World"));
    }
}
