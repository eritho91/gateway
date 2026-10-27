
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

        return null;
    }

    // Hämtar alla produkter
    @GetMapping("/products")
    public ResponseEntity<String> products(HttpServletRequest request) {
        String authorization = getAuthorization(request);

        if (authorization == null) {
            return ResponseEntity.status(401).build();
        }

        return proxyService.products(authorization);
    }

    // Hämtar en specifik produkt
    @GetMapping("/products/{id}")
    public ResponseEntity<String> productById(
            @PathVariable Long id,
            HttpServletRequest request) {

        String authorization = getAuthorization(request);

        if (authorization == null) {
            return ResponseEntity.status(401).build();
        }

        return proxyService.productById(id, authorization);
    }

    // Skapar en produkt
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

    // Loggar in och skapar HttpOnly-cookie
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(
            @RequestBody String body,
            HttpServletRequest request) {

        return proxyService.loginWithCookie(
                body,
                request.getHeader(HttpHeaders.CONTENT_TYPE)
        );
    }

    // Registrerar användare
    @PostMapping("/auth/register")
    public ResponseEntity<String> register(
            @RequestBody String body,
            HttpServletRequest request) {

        return proxyService.register(
                body,
                request.getHeader(HttpHeaders.CONTENT_TYPE)
        );
    }

    // Loggar ut användaren
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout() {
        return proxyService.logout();
    }

    // Kontrollerar befintlig session med JWT från HttpOnly-cookie
    @GetMapping("/auth/me")
    public ResponseEntity<String> session(
            @CookieValue(name = "access_token", required = false)
            String accessToken) {

        return proxyService.session(accessToken);
    }

    // Skapar en order
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
