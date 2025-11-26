package com.c05.kaz.ecommercebackend.config;

import com.c05.kaz.ecommercebackend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/login",
            "/api/auth/register/**",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/oauth/google",
            "/api/auth/oauth/facebook",
            "/api/public/**",
            "/api/products/**",
            "/api/ghn/provinces",
            "/api/ghn/districts",
            "/api/ghn/wards",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // 🔥 Quan trọng: Mở quyền cho GHN API
                        .requestMatchers("/ghn/**").permitAll()

                        .requestMatchers("/shipping/**").permitAll()

                        // 🔥 Quan trọng: Cho phép tất cả OPTIONS (preflight)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Các endpoint public cũ
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // Roles ADMIN + HR
                        .requestMatchers("/api/admin/users/*/report").hasAnyRole("ADMIN", "HR")
                        .requestMatchers("/api/admin/users/reports/**").hasAnyRole("ADMIN", "HR")
                        .requestMatchers("/api/admin/users/reports").hasAnyRole("ADMIN", "HR")
                        .requestMatchers("/api/admin/users/basic/**").authenticated()
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "HR")

                        // Roles SUPPLIER
                        .requestMatchers("/api/suppliers/**").hasRole("SUPPLIER")

                        // Roles CUSTOMER
                        .requestMatchers("/api/customers/**", "/api/cart/**", "/api/orders/**")
                        .hasRole("CUSTOMER")

                        // Require login
                        .requestMatchers("/api/authentic/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/api/chat/**").authenticated()
                        .requestMatchers(
                                "/ws/**",
                                "/info/**",
                                "/sockjs-node/**",
                                "/topic/**",
                                "/app/**"
                        ).permitAll()

                        // Tất cả còn lại cần JWT
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // FE domain
        config.setAllowedOrigins(List.of("http://localhost:8081"));
        config.setAllowedOriginPatterns(List.of("http://localhost:8081"));

        config.setAllowCredentials(true);  // SockJS cần cái này
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
