package com.github.t1;

import com.github.tomakehurst.wiremock.client.WireMock;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.function.Function;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.request;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;
import static io.quarkiverse.wiremock.devservice.WireMockConfigKey.PORT;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static jakarta.ws.rs.core.MediaType.WILDCARD;

public class WireMocker<T> {
    private static final Jsonb JSONB = JsonbBuilder.create(new JsonbConfig().withFormatting(true));
    private static final Object[] NO_ARGS = new Object[0];

    private static Given given;

    @SuppressWarnings("checkstyle:visibilitymodifier")
    public final T api;

    private final Class<T> apiClass;
    private final WireMock wireMock;

    public WireMocker(WireMock wireMock, Class<T> apiClass) {
        this.wireMock = wireMock;
        this.apiClass = apiClass;
        this.api = proxy(apiClass);
    }

    @SuppressWarnings("unchecked")
    private T proxy(Class<T> apiClass) {
        return (T) Proxy.newProxyInstance(apiClass.getClassLoader(), new Class[]{apiClass}, this::handleInvocation);
    }

    public static StubBuilder given(@SuppressWarnings("unused") Object methodCall) {
        return new StubBuilder();
    }

    private Object handleInvocation(Object proxy, Method method, Object... args) {
        if ("equals".equals(method.getName()) && args.length == 1) {
            return false;
        }
        if ("hashCode".equals(method.getName()) && args.length == 0) {
            return 0;
        }
        if ("toString".equals(method.getName()) && args.length == 0) {
            return "\"Given\" proxy for " + method;
        }
        if (given != null) {
            throw new IllegalStateException("Already building a stub");
        }
        if (args == null) args = NO_ARGS; // avoid NPE in UriBuilder
        given = new Given(this, method, args);
        // we must return null here, as it could be primitive
        // the data required to build the stub is stored in the `given` field
        return null;
    }

    private record Given(WireMocker<?> extension, Method method, Object... args) {}

    public record StubBuilder() {
        public void returns(Object body) {
            if (given == null) {
                throw new IllegalStateException("No stubbing in progress");
            }
            given.extension.stub(body, given.method, given.args);
            given = null;
        }
    }

    private void stub(Object body, Method method, Object... args) {
        var httpMethod = findHttpMethod(method);
        var produced = findProducedContentType(method);
        var uri = UriBuilder.fromMethod(apiClass, method.getName()).build(args).toString();
        var request = request(httpMethod, urlPathTemplate(uri));

        var response = aResponse()
                .withHeader("Content-Type", produced.contentType.toString())
                .withBody(produced.convert.apply(body));
        wireMock.register(request.willReturn(response));
    }

    private ResponseBodyHandler findProducedContentType(Method method) {
        var contentType = (method.isAnnotationPresent(Produces.class)) ?
                method.getAnnotation(Produces.class).value()[0] :
                APPLICATION_JSON;
        return switch (contentType) {
            case APPLICATION_JSON, WILDCARD -> new ResponseBodyHandler(APPLICATION_JSON_TYPE, JSONB::toJson);
            case TEXT_PLAIN -> new ResponseBodyHandler(TEXT_PLAIN_TYPE, Object::toString);
            default -> throw new IllegalArgumentException("Unsupported content type: " + contentType);
        };
    }

    private String findHttpMethod(Method method) {
        for (var annotation : method.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(HttpMethod.class)) {
                return annotation.annotationType().getSimpleName();
            }
        }
        throw new IllegalArgumentException("No HTTP method annotation found on " + method);
    }


    /** This is a real REST client for the provided API */
    @SuppressWarnings("unused")
    public T client() {return RestClientBuilder.newBuilder().baseUri(uri()).build(apiClass);}

    public URI uri() {
        return URI.create("http://localhost:" + ConfigProvider.getConfig().getValue(PORT, Integer.class));
    }

    private record ResponseBodyHandler(MediaType contentType, Function<Object, String> convert) {}
}
