package com.github.t1;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.quarkiverse.wiremock.devservice.ConnectWireMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@ConnectWireMock
class ExampleResourceTest {
    WireMock wireMock;

    @BeforeEach void setup() {
        wireMock.resetMappings();
    }

    @Test
    void testHelloEndpoint() {
        wireMock.register(get(urlEqualTo("/name"))
                .willReturn(aResponse().withBody("World")));

        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("{" +
                         "\"address\":\"Hello\"," +
                         "\"name\":\"World\"" +
                         "}"));
    }
}