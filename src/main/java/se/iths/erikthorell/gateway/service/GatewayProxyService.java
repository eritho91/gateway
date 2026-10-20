
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

// Hanterar adresserna till mikrotjänsterna
// och vidarebefordrar HTTP-anrop
@Service
public class GatewayProxyService {

    private final String authServerUrl;
    private final String productServiceUrl;
    private final String orderServiceUrl;
    private final HttpClient httpClient;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Hämtar inställningar från application.properties
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
        this.httpClient = httpClient;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    // Hämtar alla produkter
    public ResponseEntity<String> products(String authorization) {
        return forwardGet(
                productServiceUrl,
                "/products",
                authorization
        );
    }

    // Hämtar en specifik produkt
    public ResponseEntity<String> productById(
            Long id,
            String authorization) {

        return forwardGet(
                productServiceUrl,
                "/products/" + id,
                authorization
        );
    }

    // Skapar en produkt
    public ResponseEntity<String> createProduct(
            String requestBody,
            String authorization,
            String contentType) {

        return forwardPost(
                productServiceUrl,
                "/products",
                requestBody,
                authorization,
                contentType
        );
    }

    // Skapar en order
    public ResponseEntity<String> createOrder(
            String requestBody,
            String authorization,
            String contentType) {

        return forwardPost(
                orderServiceUrl,
                "/orders",
                requestBody,
                authorization,
                contentType
        );
    }

    // Vidarebefordrar registrering till auth-server
    public ResponseEntity<String> register(
            String requestBody,
            String contentType) {

        return forwardPost(
                authServerUrl,
                "/auth/register",
                requestBody,
                null,
                contentType
        );
    }

    // Skickar inloggningsuppgifter till auth-server
    public ResponseEntity<String> login(
            String requestBody,
            String contentType) {

        return forwardPost(
                authServerUrl,
                "/auth/login",
                requestBody,
                null,
                contentType
        );
    }

    // Loggar in och skapar en HttpOnly-cookie
    public ResponseEntity<?> loginWithCookie(
            String requestBody,
            String contentType) {

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(authServerUrl + "/auth/login"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        if (contentType != null && !contentType.isBlank()) {
            builder.header(HttpHeaders.CONTENT_TYPE, contentType);
        }

        return sendLogin(builder.build());
    }

    // Skickar inloggningsanropet till auth-server
    private ResponseEntity<?> sendLogin(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                return ResponseEntity
                        .status(response.statusCode())
                        .body(response.body());
            }

            return createLoginResponse(response.body());

        } catch (IOException exception) {
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body("Kunde inte nå auth-servern: "
                            + exception.getMessage());

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Anrop avbröts: "
                            + exception.getMessage());
        }
    }

    // Omvandlar auth-serverns svar och skapar en cookie
    private ResponseEntity<?> createLoginResponse(String body) {
        try {
            AuthServiceResponse authResponse =
                    objectMapper.readValue(
                            body,
                            AuthServiceResponse.class
                    );

            // JWT lagras i en HttpOnly-cookie
            ResponseCookie cookie = ResponseCookie
                    .from("access_token", authResponse.accessToken())
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite(cookieSameSite)
                    .path("/")
                    .maxAge(Duration.ofSeconds(authResponse.expiresIn()))
                    .build();

            // Frontend får användarinfo men inte JWT
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
                    .body("Kunde inte tolka svaret från auth-servern: "
                            + exception.getMessage());
        }
    }

    // Loggar ut genom att radera autentiseringscookien
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

    // Bygger GET-anrop och vidarebefordrar dem
    private ResponseEntity<String> forwardGet(
            String baseUrl,
            String path,
            String authorization) {

        HttpRequest.Builder builder = HttpRequest.newBuilder(
                URI.create(baseUrl + path)
        ).GET();

        return send(builder, authorization);
    }

    // Bygger POST-anrop med JSON och eventuell JWT
    private ResponseEntity<String> forwardPost(
            String baseUrl,
            String path,
            String requestBody,
            String authorization,
            String contentType) {

        HttpRequest.Builder builder = HttpRequest.newBuilder(
                URI.create(baseUrl + path)
        ).POST(HttpRequest.BodyPublishers.ofString(requestBody));

        if (contentType != null && !contentType.isBlank()) {
            builder.header(HttpHeaders.CONTENT_TYPE, contentType);
        }

        return send(builder, authorization);
    }

    // Skickar HTTP-anrop och returnerar mikrotjänstens svar
    private ResponseEntity<String> send(
            HttpRequest.Builder builder,
            String authorization) {

        // Skickar JWT vidare som Authorization-header
        if (authorization != null && !authorization.isBlank()) {
            builder.header(HttpHeaders.AUTHORIZATION, authorization);
        }

        try {
            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            // Behåller mikrotjänstens statuskod
            ResponseEntity.BodyBuilder result =
                    ResponseEntity.status(response.statusCode());

            // Behåller Content-Type
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

    //Skickar produktdata till product-service för att skapa en produkt
    public ResponseEntity<String> createProduct(
            String requestBody,
            String authorization,
            String contentType
    ) {
        return forwardPost(
                productServiceUrl,
                "/products",
                requestBody,
                authorization,
                contentType
        );
    }

    
// Vidarebefordrar registrering till auth-server
public ResponseEntity<String> register(
        String requestBody,
        String contentType) {

    return forwardPost(
            authServerUrl,
            "/auth/register",
            requestBody,
            null,
            contentType
    );
}

    // Raderar autentiseringscookien
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

}
