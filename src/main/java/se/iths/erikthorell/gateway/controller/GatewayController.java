package se.iths.erikthorell.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import java.net.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.iths.erikthorell.gateway.service.GatewayProxyService;

import java.net.URI;

//Tar emot frontends Http-anrop
@RestController
@RequestMapping("/gateway")
public class GatewayController {
    private final GatewayProxyService proxyService;

    public GatewayController(GatewayProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @GetMapping("/products")
    public ResponseEntity<String> products(HttpServletRequest request) {
        ResponseEntity<String> response = proxyService.products(request.getHeader(HttpHeaders.AUTHORIZATION));
        return response;
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<String> productById(@PathVariable Long id, HttpServletRequest request) {
        ResponseEntity<String> response = proxyService.productById(id, request.getHeader(HttpHeaders.AUTHORIZATION));
        return response;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<String> login(
            @RequestBody String requestBody,
            HttpServletRequest request) {
        return proxyService.login(
                requestBody,
                request.getHeader(HttpHeaders.CONTENT_TYPE)
        );
    }
}