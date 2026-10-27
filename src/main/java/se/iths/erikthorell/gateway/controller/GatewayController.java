package se.iths.erikthorell.gateway.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.iths.erikthorell.gateway.service.GatewayProxyService;

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    private final GatewayProxyService proxyService;

    public GatewayController(GatewayProxyService proxyService) {
        this.proxyService = proxyService;
    }

    // Hämtar JWT från HttpOnly-cookie
    private String getAuthorization(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())
                        && !cookie.getValue().isBlank()) {
                    return "Bearer " + cookie.getValue();
                }
            }
        }

        // nu hämtas gatewayen jwt bara från httponly cookien
        return null;
    }

    //access token hämtas nu från cookie, skickas vidare till service
    @GetMapping("/products")
    public ResponseEntity<String> products(HttpServletRequest request) {
        String authorization = getAuthorization(request);

        //kontroll för att se till att cookien finns
        if (authorization == null) {
            return ResponseEntity.status(401).build();
        }
        return proxyService.products(authorization);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<String> productById(
            @PathVariable Long id,
            HttpServletRequest request) {

        String authorization = getAuthorization(request);
        if (authorization == null) {
            return ResponseEntity.status(401).build();
        }

        return proxyService.productById(
                id, authorization);
    }

    @PostMapping("/products")
    public ResponseEntity<String> createProduct(
            @RequestBody String body,
            HttpServletRequest request) {

        String authorization = getAuthorization(request);
        if (authorization == null) {
            return ResponseEntity.status(401).build();
        }

        return proxyService.createProduct(
                body,
                authorization,
                request.getHeader(HttpHeaders.CONTENT_TYPE)
        );
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(
            @RequestBody String body,
            HttpServletRequest request) {

        return proxyService.loginWithCookie(
                body,
                request.getHeader(HttpHeaders.CONTENT_TYPE)
        );
    }

    @PostMapping("/auth/register")
    public ResponseEntity<String> register(
            @RequestBody String body,
            HttpServletRequest request) {

        return proxyService.register(
                body,
                request.getHeader(HttpHeaders.CONTENT_TYPE)
        );
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout() {
        return proxyService.logout();
    }

    @PostMapping("/orders")
    public ResponseEntity<String> createOrder(
            @RequestBody String body,
            HttpServletRequest request) {

        String authorization = getAuthorization(request);
        if (authorization == null) {
            return ResponseEntity.status(401).build();
        }

        return proxyService.createOrder(
                body,
                authorization,
                request.getHeader(HttpHeaders.CONTENT_TYPE)
        );
    }
}
