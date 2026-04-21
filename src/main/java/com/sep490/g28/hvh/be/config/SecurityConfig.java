package com.sep490.g28.hvh.be.config;

import com.sep490.g28.hvh.be.auth.SupabaseJwtAuthenticationConverter;
import com.sep490.g28.hvh.be.logging.UserMdcFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for the application.
 *
 * <p>Configures JWT-based authentication using Supabase,
 * stateless session management, CORS, and global security rules.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${front-end.web.baseUrl}")
    private String frontendBaseUrl;

    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;
    private final SupabaseJwtAuthenticationConverter supabaseJwtAuthenticationConverter;
    private final UserMdcFilter userMdcFilter;
    /**
     * Defines the main security filter chain.
     *
     * <p>Key configurations:
     * <ul>
     *   <li>Disables CSRF (stateless REST API)</li>
     *   <li>Enables CORS with custom configuration</li>
     *   <li>Uses stateless session policy</li>
     *   <li>Allows public access to specific endpoints</li>
     *   <li>Protects all other endpoints with JWT authentication</li>
     * </ul>
     * </p>
     *
     * @param http {@link HttpSecurity} configuration
     * @return configured {@link SecurityFilterChain}
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) //make each request independently
                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(
                                        "/test/**", //todo xoa cai nay di
                                        "/test-sb/**", //todo xoa cai nay di
                                        "/v3/api-docs/**",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/api/v1/auth/forgot-password",
                                        "/api/v1/volunteers/register-vol-acc",
                                        "/api/v1/organizations/register-org",
                                        "/api/v1/email-otp/**",
                                        "/api/v1/events/feeds",
                                        "/api/v1/events/event-details/**",
                                        "/api/v1/organizations/**",
                                        "/api/v1/certificates/**",
                                        "/api/v1/activity-domains",
                                        "/volunteers/public-information/**"
                                ).permitAll() //public endpoint
                                .anyRequest().authenticated() //all other request require authentication
                        )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(supabaseJwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterAfter(userMdcFilter, BearerTokenAuthenticationFilter.class)
        ;
        return http.build();
    }

    /**
     * Global CORS configuration.
     *
     * @return configured {@link CorsConfigurationSource}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(frontendBaseUrl)); //allow only fe-web base url
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
