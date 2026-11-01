package com.xtv.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class SecurityConfig {
  private final JwtAuthFilter jwt;
  private final XtvProperties properties;

  public SecurityConfig(JwtAuthFilter jwt, XtvProperties properties) {
    this.jwt = jwt;
    this.properties = properties;
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsSource()))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, error) -> {
          res.setStatus(401);
          res.setContentType("application/json");
          res.getWriter().write("{\"message\":\"未登录\"}");
        }))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/v1/orders").authenticated()
            .requestMatchers("/api/v1/orders/**").authenticated()
            .requestMatchers("/api/v1/me", "/api/v1/me/**").authenticated()
            .requestMatchers("/api/v1/dev/**").authenticated()
            .anyRequest().permitAll())
        .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  private CorsConfigurationSource corsSource() {
    return request -> {
      CorsConfiguration config = new CorsConfiguration();
      config.setAllowedOriginPatterns(List.of(properties.getCorsOrigins().split(",")).stream()
          .map(String::trim)
          .filter(origin -> !origin.isEmpty())
          .toList());
      config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
      config.setAllowedHeaders(List.of("*"));
      config.setAllowCredentials(true);
      return config;
    };
  }
}
