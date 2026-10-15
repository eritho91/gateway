
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
//samt vidarebefordrar Http-anrop till dem
@Service
public class GatewayProxyService {
    private final String authServerUrl;
    private final String productServiceUrl;
    private final String orderServiceUrl;
    private final HttpClient httpClient;

    //Hämtar mikrotjänsternas adresser från application.properties
    //HttpClient används för att skicka HTTP-anrop
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

    //Hämtar alla produkter från product-service
    public ResponseEntity<String> products(String authorization) {
        ResponseEntity<String> getResponse = forwardGet(productServiceUrl, "/products", authorization);
        return getResponse;
    }

    //Bygger ett GET-anrop med rätt adress och skickar det vidare
    private ResponseEntity<String> forwardGet(
            String baseUrl,
            String path,
            String authorization) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET();

        ResponseEntity<String> sendResponse = send(builder, authorization);
        return sendResponse;
    }

    //Hämtar en specifik produkt genom att lägga till produktens ID i adressen
    public ResponseEntity<String> productById(Long id, String authorization) {
        ResponseEntity<String> getResponse = forwardGet(productServiceUrl, "/products/" + id, authorization);
        return getResponse;
    }

    //Bygger ett POST-anrop och skickar med data, JWT-token och Content-Type
    private ResponseEntity<String> forwardPost(
            String baseUrl,
            String path,
            String requestBody,
            String authorization,
            String contentType) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        //Lägger till Content-Type om det finns, exempelvis application/json
        if (contentType != null && !contentType.isBlank()) {
            builder.header(HttpHeaders.CONTENT_TYPE, contentType);
        }

        return send(builder, authorization);
    }

    //Skickar inloggningsuppgifter till auth-server
    //Ingen JWT-token behövs eftersom användaren inte är inloggad ännu
    public ResponseEntity<String> login(String requestBody, String contentType) {
        return forwardPost(
                authServerUrl,
                "/auth/login",
                requestBody,
                null,
                contentType
        );
    }

    //Skickar orderinformation till order-service tillsammans med JWT-token
    public ResponseEntity<String> createOrder(
            String requestBody,
            String authorization,
            String contentType
    ) {
        return forwardPost(
                orderServiceUrl,
                "/orders",
                requestBody,
                authorization,
                contentType
        );
    }

    //Skickar Http-anrop till mikrotjänsten och returnerar svaret
    private ResponseEntity<String> send(HttpRequest.Builder builder, String authorization) {

        //Skickar vidare Authorization-headern om den finns och inte är tom
        if (authorization != null && !authorization.isBlank()) {
            builder.header(HttpHeaders.AUTHORIZATION, authorization);
        }

        try {
            //Skickar anropet och hämtar statuskod, response body och headers
            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            //Bevarar mikrotjänstens statuskod
            ResponseEntity.BodyBuilder result =
                    ResponseEntity.status(response.statusCode());

            //Skickar vidare Content-Type så frontend kan tolka svaret korrekt
            response.headers().firstValue(HttpHeaders.CONTENT_TYPE)
                    .ifPresent(value -> result.header(
                            HttpHeaders.CONTENT_TYPE, value
                    ));

            //Returnerar mikrotjänstens svar till frontend
            return result.body(response.body());

        } catch (IOException exception) {
            //Status 502 om kommunikationen med mikrotjänsten misslyckas
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Kunde inte nå bakomliggande tjänst");

        } catch (InterruptedException exception) {
            //Återställer trådens avbrottsstatus och returnerar 503
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Anropet avbröts");
        }
    }
}
