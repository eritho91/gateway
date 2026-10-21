
package se.iths.erikthorell.gateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import se.iths.erikthorell.gateway.dto.AuthServiceResponse;
import se.iths.erikthorell.gateway.dto.LoginResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class GatewayProxyService {

    private final String authServerUrl;
    private final String productServiceUrl;
    private final String orderServiceUrl;
    private final HttpClient httpClient;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GatewayProxyService(
            @Value("${app.auth-server-url}") String authServerUrl,
            @Value("${app.product-service-url}") String productServiceUrl,
            @Value("${app.order-service-url}") String orderServiceUrl,
            @Value("${app.cookie.secure}") boolean cookieSecure,
            @Value("${app.cookie.same-site}") String cookieSameSite,
            HttpClient httpClient) {

        this.authServerUrl = authServerUrl;
        this.productServiceUrl = productServiceUrl;
        this.orderServiceUrl = orderServiceUrl;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.httpClient = httpClient;
    }

    public ResponseEntity<String> products(String authorization) {
        return forwardGet(productServiceUrl, "/products", authorization);
    }

    public ResponseEntity<String> productById(
            Long id,
            String authorization) {

        return forwardGet(
                productServiceUrl,
                "/products/" + id,
                authorization
        );
    }

    public ResponseEntity<String> createProduct(
            String body,
            String authorization,
            String contentType) {

        return forwardPost(
                productServiceUrl,
                "/products",
                body,
                authorization,
                contentType
        );
    }

    public ResponseEntity<String> createOrder(
            String body,
            String authorization,
            String contentType) {

        return forwardPost(
                orderServiceUrl,
                "/orders",
                body,
                authorization,
                contentType
        );
    }

    public ResponseEntity<String> register(
            String body,
            String contentType) {

        return forwardPost(
                authServerUrl,
                "/auth/register",
                body,
                null,
                contentType
        );
    }

    public ResponseEntity<?> loginWithCookie(
            String body,
            String contentType) {

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(authServerUrl + "/auth/login"))
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (contentType != null && !contentType.isBlank()) {
            builder.header(HttpHeaders.CONTENT_TYPE, contentType);
        }

        try {
            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {
                return ResponseEntity
                        .status(response.statusCode())
                        .body(response.body());
            }

            AuthServiceResponse authResponse = objectMapper.readValue(
                    response.body(),
                    AuthServiceResponse.class
            );

            ResponseCookie cookie = ResponseCookie
                    .from("access_token", authResponse.accessToken())
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite(cookieSameSite)
                    .path("/")
                    .maxAge(Duration.ofSeconds(authResponse.expiresIn()))
                    .build();

            LoginResponse loginResponse = new LoginResponse(
                    authResponse.expiresIn(),
                    authResponse.username(),
                    authResponse.roles()
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(loginResponse);

        } catch (JacksonException exception) {
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body("Ogiltigt svar från auth-servern");

        } catch (IOException exception) {
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body("Kunde inte nå auth-servern");

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Anropet avbröts");
        }
    }

    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = ResponseCookie
                .from("access_token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    private ResponseEntity<String> forwardGet(
            String baseUrl,
            String path,
            String authorization) {

        HttpRequest.Builder builder = HttpRequest.newBuilder(
                URI.create(baseUrl + path)
        ).GET();

        return send(builder, authorization);
    }

    private ResponseEntity<String> forwardPost(
            String baseUrl,
            String path,
            String body,
            String authorization,
            String contentType) {

        HttpRequest.Builder builder = HttpRequest.newBuilder(
                URI.create(baseUrl + path)
        ).POST(HttpRequest.BodyPublishers.ofString(body));

        if (contentType != null && !contentType.isBlank()) {
            builder.header(HttpHeaders.CONTENT_TYPE, contentType);
        }

        return send(builder, authorization);
    }

    private ResponseEntity<String> send(
            HttpRequest.Builder builder,
            String authorization) {

        if (authorization != null && !authorization.isBlank()) {
            builder.header(HttpHeaders.AUTHORIZATION, authorization);
        }

        try {
            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            ResponseEntity.BodyBuilder result =
                    ResponseEntity.status(response.statusCode());

            response.headers()
                    .firstValue(HttpHeaders.CONTENT_TYPE)
                    .ifPresent(value ->
                            result.header(HttpHeaders.CONTENT_TYPE, value)
                    );

            return result.body(response.body());

        } catch (IOException exception) {
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body("Kunde inte nå bakomliggande tjänst");

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Anropet avbröts");
        }
    }
}
