package se.iths.erikthorell.gateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

//Den här klassen hanterar adresserna till mikrotjänsterna
//Samt vidarebefordrar Http-anrop till dem
@Service
public class GatewayProxyService {
    private final String authServerUrl;
    private final String productServiceUrl;
    private final String orderServiceUrl;
    private final HttpClient httpClient;

    public GatewayProxyService(
            @Value("${app.auth-server-url}") String authServerUrl,
            @Value("${app.product-service-url}") String productServiceUrl,
            @Value("${app.order-service-url}") String orderServiceUrl,
            HttpClient httpClient) {
        this.authServerUrl = authServerUrl;
        this.productServiceUrl = productServiceUrl;
        this.orderServiceUrl = orderServiceUrl;
        this.httpClient = httpClient;
    }

    public ResponseEntity<String> products(String authorization) {
        ResponseEntity<String> getResponse = forwardGet(productServiceUrl, "/products", authorization);
        return getResponse;
    }

    private ResponseEntity<String> forwardGet(
            String baseUrl,
            String path,
            String authorization) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET();

        ResponseEntity<String> sendResponse = send(builder, authorization);
        return sendResponse;
    }

    public ResponseEntity<String> productById(Long id, String authorization) {
        ResponseEntity<String> getResponse = forwardGet(productServiceUrl, "/products/" + id, authorization);
        return getResponse;
    }

    private ResponseEntity<String> forwardPost(
            String baseUrl,
            String path,
            String requestBody,
            String authorization,
            String contentType) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        if (contentType != null && !contentType.isBlank()) {
            builder.header(HttpHeaders.CONTENT_TYPE, contentType);
        }

        return send(builder, authorization);
    }

    public ResponseEntity<String> login(String requestBody, String contentType) {
        return forwardPost(
                authServerUrl,
                "/auth/login",
                requestBody,
                null,
                contentType
        );
    }

    private ResponseEntity<String> send(HttpRequest.Builder builder, String authorization) {
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

            response.headers().firstValue(HttpHeaders.CONTENT_TYPE)
                    .ifPresent(value -> result.header(
                            HttpHeaders.CONTENT_TYPE, value
                    ));

            return result.body(response.body());

        } catch (IOException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Kunde inte nå bakomliggande tjänst");

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Anropet avbröts");
        }
    }

}