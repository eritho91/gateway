package se.iths.erikthorell.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.iths.erikthorell.gateway.service.GatewayProxyService;

//Tar emot frontends Http-anrop
@RestController
@RequestMapping("/gateway")
public class GatewayController {
    private final GatewayProxyService proxyService;

    public GatewayController(GatewayProxyService proxyService) {
        this.proxyService = proxyService;
    }

    //Tar emot GET-anrop för alla produkter och skickar med JWT-token
    @GetMapping("/products")
    public ResponseEntity<String> products(HttpServletRequest request) {
        ResponseEntity<String> response = proxyService.products(request.getHeader(HttpHeaders.AUTHORIZATION));
        return response;
    }

    //Tar emot GET-anrop för en specifik produkt
    //samt skickar vidare id och token
    @GetMapping("/products/{id}")
    public ResponseEntity<String> productById(@PathVariable Long id, HttpServletRequest request) {
        ResponseEntity<String> response = proxyService.productById(id, request.getHeader(HttpHeaders.AUTHORIZATION));
        return response;
    }

    //Tar emot inloggningsuppgifterna
    //samt skickar dessa till auth-server
    //bytt från string till ? då vi kan returnera olika typer
    //loginwithcookie använder då sendlogin och createloginresponse från service
    //test
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(
            @RequestBody String requestBody,
            HttpServletRequest request) {
        return proxyService.loginWithCookie(
                requestBody,
                request.getHeader(HttpHeaders.CONTENT_TYPE)
        );
    }

    //Tar emot en order
    //och skickar vidare token, orderdata och content type
    @PostMapping("/orders")
    public ResponseEntity<String> createOrder(
            @RequestBody String requestBody,
            HttpServletRequest request) {

        return proxyService.createOrder(
                requestBody,
                request.getHeader(HttpHeaders.AUTHORIZATION),
                request.getHeader(HttpHeaders.CONTENT_TYPE)
        );
    }

    //Tar emot POST-anrop för att skapa en produkt
//och skickar vidare produktdata och JWT-token
@PostMapping("/products")
public ResponseEntity<String> createProduct(
        @RequestBody String requestBody,
        HttpServletRequest request) {

    return proxyService.createProduct(
            requestBody,
            request.getHeader(HttpHeaders.AUTHORIZATION),
            request.getHeader(HttpHeaders.CONTENT_TYPE)
    );
}

    
@PostMapping("/auth/register")
public ResponseEntity<String> register(
        @RequestBody String requestBody,
        HttpServletRequest request) {

    return proxyService.register(
            requestBody,
                request.getHeader(HttpHeaders.CONTENT_TYPE)
        );
    }

}
