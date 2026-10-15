package se.iths.erikthorell.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        //Frontend från de här portarna får anropa gatewayen
        corsConfiguration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:5175"
        ));

        //Tillåtna HTTP-metoder
        corsConfiguration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));

        //Tillåtna headers
        corsConfiguration.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept"
        ));

        UrlBasedCorsConfigurationSource urlSource =
                new UrlBasedCorsConfigurationSource();

        urlSource.registerCorsConfiguration("/**", corsConfiguration);

        return urlSource;
    }

    @Bean
    public CorsFilter corsFilter(
            UrlBasedCorsConfigurationSource source) {
        return new CorsFilter(source);
    }
}