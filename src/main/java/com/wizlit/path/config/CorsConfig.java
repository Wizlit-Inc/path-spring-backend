package com.wizlit.path.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.addAllowedOrigin("http://localhost:3001"); // Add your React frontend's URL (or more domains if applicable)
        corsConfig.addAllowedHeader("*"); // Allow all headers
        corsConfig.addAllowedMethod("*"); // Allow all HTTP methods
        corsConfig.setAllowCredentials(true); // You can enable credentials (cookies, etc.) if needed.

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}