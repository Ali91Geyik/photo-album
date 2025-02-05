// src/main/java/com/example/photo_album/config/SecurityConfig.java
package com.example.photo_album.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())  // Multipart file upload için CSRF'i disable ediyoruz
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").permitAll()  // API endpointlerini public yapıyoruz
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}