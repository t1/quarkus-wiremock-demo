package com.github.t1;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.quarkiverse.wiremock.devservice.ConnectWireMock;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static com.github.t1.WireMockExtension.given;
import static org.assertj.core.api.BDDAssertions.then;

@QuarkusTest
@ConnectWireMock
class GreetingResourceTest {
    @TestHTTPResource
    URI baseUri;
    GreetingApi greetingApi;

    WireMock wireMock;
    WireMockExtension<NameServiceApi> nameService;

    @BeforeEach void setup() {
        greetingApi = RestClientBuilder.newBuilder().baseUri(baseUri).build(GreetingApi.class);

        wireMock.resetMappings();
        nameService = new WireMockExtension<>(wireMock, NameServiceApi.class);
    }

    @Test
    void testHelloEndpoint() {
        given(nameService.api.name()).returns("World");

        var response = greetingApi.hello();

        then(response).isEqualTo(new Greeting("Hello", "World"));
    }
}
