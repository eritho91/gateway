package se.iths.erikthorell.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

//Konfigurationsklass för Http-klienten som används för kommunikation med de andra tjänsterna
@Configuration
public class HttpClientConfig {
    //Skapar en HTTP-klient med 5 sekunders timeout
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }
}