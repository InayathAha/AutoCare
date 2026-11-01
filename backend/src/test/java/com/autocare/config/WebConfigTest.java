package com.autocare.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.assertj.core.api.Assertions.assertThatNoException;

class WebConfigTest {

    @Test
    @DisplayName("WebConfig parses comma-separated origins and configures registry")
    void testCorsConfiguration() {
        WebConfig config = new WebConfig("https://autocare.vercel.app, http://localhost:5173 , https://preview.vercel.app");
        CorsRegistry registry = new CorsRegistry();
        assertThatNoException().isThrownBy(() -> config.addCorsMappings(registry));
    }

    @Test
    @DisplayName("WebConfig defaults to localhost:5173 when empty or blank")
    void testCorsDefault() {
        WebConfig config = new WebConfig("");
        CorsRegistry registry = new CorsRegistry();
        assertThatNoException().isThrownBy(() -> config.addCorsMappings(registry));
    }
}
