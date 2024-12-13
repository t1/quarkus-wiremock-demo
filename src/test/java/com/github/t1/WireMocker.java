package com.github.t1;

import com.github.tomakehurst.wiremock.client.WireMock;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;
import lombok.extern.slf4j.Slf4j;
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
import static jakarta.ws.rs.core.Response.Status;
import static jakarta.ws.rs.core.Response.Status.OK;

/**
 * This JUnit 5 extension allows to stub REST API calls using WireMock,
 * using the JAX-RS / Jakarta REST annotations in an API interface.
 * It works similar to Mockito, but for REST APIs.
 * <p>
 * <h1>Usage</h1>
 * <pre>{@code
 * @Path("/api")
 * interface Api {
 *     @GET
 *     @Path("/someMethod/{id}")
 *     String someMethod(@PathParam("id") String id);
 * }
 *
 * @RegisterExtension
 * WireMockExtension<Api> service = new WireMockExtension<>(Api.class);
 *
 * given(service.api.someMethod("123"))
 *         .returns("some response");
 * }</pre>
 *
 * <h1>Client</h1>
 * There's also a convenient {@link #client()} method to create a REST client for the provided API.
 * So you can easily do:
 * <pre>{@code
 * var client = service.client();
 *
 * var response = client.someMethod("123");
 *
 * then(response).isEqualTo("some response");
 * }</pre>
 *
 * <h1>Things left to do</h1>
 * <ul>
 *     <li>Support {@link jakarta.ws.rs.QueryParam}</li>
 *     <li>Support {@link jakarta.ws.rs.HeaderParam}</li>
 *     <li>Support {@link jakarta.ws.rs.MatrixParam}</li>
 *     <li>Support {@link jakarta.ws.rs.FormParam}</li>
 *     <li>Support {@link jakarta.ws.rs.CookieParam}</li>
 *     <li>Support {@link jakarta.ws.rs.BeanParam}</li>
 *     <li>Support {@link jakarta.ws.rs.Consumes}</li>
 *     <li>Support {@link jakarta.ws.rs.Produces}</li>
 * </ul>
 */
@Slf4j
public class WireMocker<T> {
    public static final Jsonb JSONB = JsonbBuilder.create(new JsonbConfig().withFormatting(true));
    private static final Object[] NO_ARGS = new Object[0];
    private static final ResponseBodyHandler ERROR_HANDLER = new ResponseBodyHandler(APPLICATION_JSON_TYPE, WireMocker::errorBodyConverter);

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

    /// Strings are probably already full JSON strings... don't wrap them into a JSON string
    private static String errorBodyConverter(Object object) {
        return (object instanceof String s) ? s : JSONB.toJson(object);
    }

    @SuppressWarnings("unchecked")
    private T proxy(Class<T> apiClass) {
        return (T) Proxy.newProxyInstance(apiClass.getClassLoader(), new Class[]{apiClass}, this::handleInvocation);
    }

    public static <U> StubBuilder<U> given(@SuppressWarnings("unused") U methodCall) {
        return new StubBuilder<>();
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
        given = new Given(this, method, (args == null) ? NO_ARGS : args); // avoid NPE in UriBuilder
        // we must return null here, as it could be primitive
        // the data required to build the stub is stored in the `given` field
        return null;
    }

    private record Given(WireMocker<?> extension, Method method, Object... args) {}

    public record StubBuilder<U>() {
        public void returns(U body) {
            returns(OK, body);
        }

        public void returns(Status status, U body) {
            handle(status, body);
        }

        public void returns(Status status, String body) {
            handle(status, body);
        }

        private void handle(Status status, Object body) {
            if (given == null) {
                throw new IllegalStateException("No stubbing in progress");
            }
            given.extension.stub(status, body, given.method, given.args);
            given = null;
        }
    }

    private void stub(Status status, Object body, Method method, Object... args) {
        var httpMethod = findHttpMethod(method);
        var uri = uriBuilder(method).build(args);
        log.info("stubbing {} {} -> {} {}: {}", httpMethod, uri, status.getStatusCode(), status.getReasonPhrase(), body);
        var request = request(httpMethod, urlPathTemplate(uri.toString()));

        var responseBodyHandler = method.getReturnType().isInstance(body) ? responseBodyHandler(method) : ERROR_HANDLER;
        var response = aResponse()
                .withStatus(status.getStatusCode())
                .withHeader("Content-Type", responseBodyHandler.contentType.toString())
                .withBody(responseBodyHandler.convert.apply(body));
        wireMock.register(request.willReturn(response));
    }

    private UriBuilder uriBuilder(Method method) {
        if (apiClass.isAnnotationPresent(Path.class)) {
            var builder = UriBuilder.fromResource(apiClass);
            if (method.isAnnotationPresent(Path.class)) builder.path(method);
            return builder;
        } else {
            return UriBuilder.fromMethod(apiClass, method.getName());
        }
    }

    private ResponseBodyHandler responseBodyHandler(Method method) {
        var contentType = method.isAnnotationPresent(Produces.class)
                ? method.getAnnotation(Produces.class).value()[0]
                : APPLICATION_JSON;
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
    public T client() {
        return RestClientBuilder.newBuilder().baseUri(uri()).build(apiClass);
    }

    public URI uri() {
        return URI.create("http://localhost:" + ConfigProvider.getConfig().getValue(PORT, Integer.class));
    }

    private record ResponseBodyHandler(MediaType contentType, Function<Object, String> convert) {}
}
